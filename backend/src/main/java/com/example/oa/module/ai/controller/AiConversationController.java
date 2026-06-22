package com.example.oa.module.ai.controller;

import com.example.oa.common.result.Result;
import com.example.oa.module.ai.dto.AiChatRequest;
import com.example.oa.module.ai.dto.AiConversationRequest;
import com.example.oa.module.ai.entity.AiConversation;
import com.example.oa.module.ai.entity.AiMessage;
import com.example.oa.module.ai.service.AiConversationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
@RequestMapping("/api/ai/v2/conversations")
@RequiredArgsConstructor
public class AiConversationController {
    private final AiConversationService conversationService;

    @GetMapping
    public Result<List<AiConversation>> list() {
        return Result.success(conversationService.listMine());
    }

    @PostMapping
    public Result<AiConversation> create(@RequestBody(required = false) AiConversationRequest request) {
        return Result.success(conversationService.create(request == null ? new AiConversationRequest() : request));
    }

    @PatchMapping("/{id}")
    public Result<AiConversation> rename(@PathVariable Long id, @RequestBody AiConversationRequest request) {
        return Result.success(conversationService.rename(id, request));
    }

    @GetMapping("/{id}/messages")
    public Result<List<AiMessage>> messages(@PathVariable Long id) {
        return Result.success(conversationService.messages(id));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        conversationService.delete(id);
        return Result.success(null);
    }

    @DeleteMapping("/{id}/messages")
    public Result<Void> clear(@PathVariable Long id) {
        conversationService.clear(id);
        return Result.success(null);
    }

    @PostMapping(value = "/{id}/messages/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@PathVariable Long id, @Valid @RequestBody AiChatRequest request) {
        return conversationService.stream(id, request);
    }

    @PostMapping(value = "/{id}/messages/{messageId}/regenerate", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter regenerate(@PathVariable Long id, @PathVariable Long messageId) {
        return conversationService.regenerate(id, messageId);
    }
}
