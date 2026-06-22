package com.example.oa.module.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.oa.common.exception.BusinessException;
import com.example.oa.common.util.SecurityUtils;
import com.example.oa.module.ai.dto.AiChatRequest;
import com.example.oa.module.ai.dto.AiConversationRequest;
import com.example.oa.module.ai.dto.AiQaRequest;
import com.example.oa.module.ai.dto.AiResponse;
import com.example.oa.module.ai.entity.AiConversation;
import com.example.oa.module.ai.entity.AiMessage;
import com.example.oa.module.ai.mapper.AiConversationMapper;
import com.example.oa.module.ai.mapper.AiMessageMapper;
import com.example.oa.module.ai.service.AiConversationService;
import com.example.oa.module.ai.service.AiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiConversationServiceImpl implements AiConversationService {
    private final AiConversationMapper conversationMapper;
    private final AiMessageMapper messageMapper;
    private final AiService aiService;
    @Qualifier("applicationTaskExecutor")
    private final Executor executor;

    @Override
    public List<AiConversation> listMine() {
        Long userId = requiredUserId();
        return conversationMapper.selectList(new LambdaQueryWrapper<AiConversation>()
                .eq(AiConversation::getUserId, userId)
                .orderByDesc(AiConversation::getLastMessageAt)
                .orderByDesc(AiConversation::getId)
                .last("LIMIT 100"));
    }

    @Override
    public AiConversation create(AiConversationRequest request) {
        AiConversation conversation = new AiConversation();
        conversation.setUserId(requiredUserId());
        conversation.setTitle(StringUtils.hasText(request.getTitle()) ? request.getTitle().trim() : "新对话");
        conversation.setMode(StringUtils.hasText(request.getMode()) ? request.getMode().trim().toUpperCase() : "QA");
        conversation.setLastMessageAt(LocalDateTime.now());
        conversationMapper.insert(conversation);
        return conversation;
    }

    @Override
    public AiConversation rename(Long id, AiConversationRequest request) {
        AiConversation conversation = requiredConversation(id, requiredUserId());
        if (!StringUtils.hasText(request.getTitle())) {
            throw new BusinessException("会话标题不能为空");
        }
        conversation.setTitle(request.getTitle().trim().substring(0, Math.min(200, request.getTitle().trim().length())));
        conversationMapper.updateById(conversation);
        return conversation;
    }

    @Override
    public List<AiMessage> messages(Long conversationId) {
        requiredConversation(conversationId, requiredUserId());
        return messageMapper.selectList(new LambdaQueryWrapper<AiMessage>()
                .eq(AiMessage::getConversationId, conversationId)
                .orderByAsc(AiMessage::getId)
                .last("LIMIT 500"));
    }

    @Override
    public void delete(Long id) {
        AiConversation conversation = requiredConversation(id, requiredUserId());
        conversationMapper.deleteById(conversation);
        messageMapper.delete(new LambdaQueryWrapper<AiMessage>().eq(AiMessage::getConversationId, id));
    }

    @Override
    public void clear(Long id) {
        AiConversation conversation = requiredConversation(id, requiredUserId());
        messageMapper.delete(new LambdaQueryWrapper<AiMessage>().eq(AiMessage::getConversationId, id));
        conversation.setTitle("新对话");
        conversation.setLastMessageAt(LocalDateTime.now());
        conversationMapper.updateById(conversation);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SseEmitter stream(Long conversationId, AiChatRequest request) {
        Long userId = requiredUserId();
        AiConversation conversation = requiredConversation(conversationId, userId);
        AiMessage duplicate = messageMapper.selectOne(new LambdaQueryWrapper<AiMessage>()
                .eq(AiMessage::getUserId, userId)
                .eq(AiMessage::getClientMessageId, request.getClientMessageId())
                .last("LIMIT 1"));
        if (duplicate != null) {
            throw new BusinessException(409, "请勿重复发送同一条消息");
        }

        AiMessage userMessage = new AiMessage();
        userMessage.setConversationId(conversationId);
        userMessage.setUserId(userId);
        userMessage.setRole("USER");
        userMessage.setClientMessageId(request.getClientMessageId());
        userMessage.setContent(request.getContent().trim());
        userMessage.setStatus("COMPLETED");
        try {
            messageMapper.insert(userMessage);
        } catch (DuplicateKeyException e) {
            throw new BusinessException(409, "请勿重复发送同一条消息");
        }

        conversation.setLastMessageAt(LocalDateTime.now());
        if ("新对话".equals(conversation.getTitle())) {
            String title = request.getContent().trim().replaceAll("\\s+", " ");
            conversation.setTitle(title.substring(0, Math.min(30, title.length())));
        }
        conversationMapper.updateById(conversation);

        List<AiMessage> context = messageMapper.selectList(new LambdaQueryWrapper<AiMessage>()
                .eq(AiMessage::getConversationId, conversationId)
                .in(AiMessage::getRole, List.of("USER", "ASSISTANT"))
                .orderByDesc(AiMessage::getId)
                .last("LIMIT 20"));

        SseEmitter emitter = new SseEmitter(65_000L);
        executor.execute(() -> generate(emitter, conversation, userMessage, context));
        return emitter;
    }

    @Override
    public SseEmitter regenerate(Long conversationId, Long assistantMessageId) {
        Long userId = requiredUserId();
        AiConversation conversation = requiredConversation(conversationId, userId);
        AiMessage previous = messageMapper.selectById(assistantMessageId);
        if (previous == null || !conversationId.equals(previous.getConversationId())
                || !userId.equals(previous.getUserId()) || !"ASSISTANT".equals(previous.getRole())
                || previous.getParentMessageId() == null) {
            throw new BusinessException(404, "可重新生成的回答不存在");
        }
        AiMessage userMessage = messageMapper.selectById(previous.getParentMessageId());
        if (userMessage == null || !conversationId.equals(userMessage.getConversationId())) {
            throw new BusinessException(409, "原始问题已不存在，无法重新生成");
        }
        List<AiMessage> context = messageMapper.selectList(new LambdaQueryWrapper<AiMessage>()
                .eq(AiMessage::getConversationId, conversationId)
                .in(AiMessage::getRole, List.of("USER", "ASSISTANT"))
                .le(AiMessage::getId, userMessage.getId())
                .orderByDesc(AiMessage::getId)
                .last("LIMIT 20"));
        SseEmitter emitter = new SseEmitter(65_000L);
        executor.execute(() -> generate(emitter, conversation, userMessage, context));
        return emitter;
    }

    private void generate(SseEmitter emitter, AiConversation conversation, AiMessage userMessage, List<AiMessage> context) {
        long start = System.currentTimeMillis();
        AiMessage assistant = new AiMessage();
        assistant.setConversationId(conversation.getId());
        assistant.setUserId(conversation.getUserId());
        assistant.setRole("ASSISTANT");
        assistant.setParentMessageId(userMessage.getId());
        assistant.setStatus("STREAMING");
        messageMapper.insert(assistant);
        try {
            send(emitter, "meta", Map.of(
                    "conversationId", conversation.getId(),
                    "userMessageId", userMessage.getId(),
                    "assistantMessageId", assistant.getId()));
            AiQaRequest qaRequest = new AiQaRequest();
            qaRequest.setQuestion(buildContext(context, conversation.getMode()));
            AiResponse response = aiService.streamQa(qaRequest, delta -> {
                try {
                    send(emitter, "delta", Map.of("content", delta));
                } catch (IOException e) {
                    throw new IllegalStateException("客户端已断开连接", e);
                }
            });
            String content = response.getContent() == null ? "" : response.getContent();
            if (!StringUtils.hasText(content)) {
                throw new BusinessException(502, "AI 未返回有效内容");
            }
            assistant.setContent(content);
            assistant.setStatus("COMPLETED");
            assistant.setModelName(response.getProvider());
            assistant.setCostTimeMs(System.currentTimeMillis() - start);
            messageMapper.updateById(assistant);
            send(emitter, "done", Map.of(
                    "messageId", assistant.getId(),
                    "finishReason", "stop",
                    "costTimeMs", assistant.getCostTimeMs()));
            emitter.complete();
        } catch (Exception e) {
            log.error("AI 会话生成失败: conversationId={}", conversation.getId(), e);
            assistant.setStatus("FAILED");
            assistant.setErrorCode(e instanceof BusinessException be ? String.valueOf(be.getCode()) : "AI_ERROR");
            assistant.setErrorMessage(safeMessage(e));
            assistant.setCostTimeMs(System.currentTimeMillis() - start);
            messageMapper.updateById(assistant);
            try {
                send(emitter, "error", Map.of("code", assistant.getErrorCode(), "message", assistant.getErrorMessage(), "retryable", true));
            } catch (Exception ignored) {
                // connection already closed
            }
            emitter.complete();
        }
    }

    private String buildContext(List<AiMessage> newestFirst, String mode) {
        String instruction = switch (mode == null ? "QA" : mode) {
            case "NEWS_GENERATE" -> "你正在执行企业内部新闻创作任务，请输出结构清晰、可编辑的新闻草稿。";
            case "NEWS_POLISH" -> "你正在执行内容润色任务，请保留事实，改善结构、语气和可读性。";
            case "SCHEDULE_PARSE" -> "你正在执行日程助手任务，请提取时间、地点、参与人和待办，并清晰列出。";
            default -> "你是企业 OA 助手，请准确、简洁、可执行地回答。";
        };
        StringBuilder builder = new StringBuilder(instruction).append("\n以下是当前会话上下文，请连贯回答最后一个用户问题：\n");
        for (int i = newestFirst.size() - 1; i >= 0; i--) {
            AiMessage item = newestFirst.get(i);
            builder.append("ASSISTANT".equals(item.getRole()) ? "助手：" : "用户：")
                    .append(item.getContent()).append('\n');
            if (builder.length() > 12_000) {
                return builder.substring(builder.length() - 12_000);
            }
        }
        return builder.toString();
    }

    private void send(SseEmitter emitter, String event, Object data) throws IOException {
        emitter.send(SseEmitter.event().name(event).data(data));
    }

    private AiConversation requiredConversation(Long id, Long userId) {
        AiConversation conversation = conversationMapper.selectById(id);
        if (conversation == null || !conversation.getUserId().equals(userId)) {
            throw new BusinessException(404, "会话不存在");
        }
        return conversation;
    }

    private Long requiredUserId() {
        Long userId = SecurityUtils.currentUserId();
        if (userId == null) {
            throw new BusinessException(401, "请先登录");
        }
        return userId;
    }

    private String safeMessage(Exception e) {
        String message = e.getMessage();
        return StringUtils.hasText(message) ? message.substring(0, Math.min(message.length(), 400)) : "AI 服务暂时不可用，请稍后重试";
    }
}
