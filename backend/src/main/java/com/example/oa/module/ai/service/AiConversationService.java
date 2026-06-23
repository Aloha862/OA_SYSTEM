package com.example.oa.module.ai.service;

import com.example.oa.module.ai.dto.AiChatRequest;
import com.example.oa.module.ai.dto.AiConversationRequest;
import com.example.oa.module.ai.entity.AiConversation;
import com.example.oa.module.ai.entity.AiMessage;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

public interface AiConversationService {
    List<AiConversation> listMine();
    AiConversation create(AiConversationRequest request);
    AiConversation rename(Long id, AiConversationRequest request);
    List<AiMessage> messages(Long conversationId);
    void delete(Long id);
    void clear(Long id);
    SseEmitter stream(Long conversationId, AiChatRequest request);
    SseEmitter regenerate(Long conversationId, Long assistantMessageId);
    SseEmitter regenerateLast(Long conversationId);
}
