package org.example.greenybackend.modules.ai;

import java.util.List;
import org.example.greenybackend.common.response.MessageResponse;
import org.example.greenybackend.domain.entity.UserEntity;
import org.example.greenybackend.modules.ai.dto.AiUsageLimitDTO;
import org.example.greenybackend.modules.ai.dto.ChatHistoryDTO;
import org.example.greenybackend.modules.ai.dto.ChatRequest;
import org.example.greenybackend.modules.ai.dto.ChatResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/ai/chat")
public class AiChatController {

    private final AiChatService aiChatService;

    public AiChatController(AiChatService aiChatService) {
        this.aiChatService = aiChatService;
    }

    @PostMapping
    public ChatResponse chat(
            @AuthenticationPrincipal(expression = "user") UserEntity currentUser,
            @RequestBody ChatRequest request
    ) {
        return aiChatService.chat(currentUser, request);
    }

    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(
            @AuthenticationPrincipal(expression = "user") UserEntity currentUser,
            @RequestBody ChatRequest request
    ) {
        return aiChatService.stream(currentUser, request);
    }

    @GetMapping("/history")
    public List<ChatHistoryDTO> history(@AuthenticationPrincipal(expression = "user") UserEntity currentUser) {
        return aiChatService.history(currentUser);
    }

    @GetMapping("/limits")
    public AiUsageLimitDTO limits(@AuthenticationPrincipal(expression = "user") UserEntity currentUser) {
        return aiChatService.usageLimits(currentUser);
    }

    @DeleteMapping("/history/{conversationId}")
    public ResponseEntity<MessageResponse> deleteHistory(
            @AuthenticationPrincipal(expression = "user") UserEntity currentUser,
            @PathVariable String conversationId
    ) {
        aiChatService.deleteHistory(currentUser, conversationId);
        return ResponseEntity.status(HttpStatus.OK).body(new MessageResponse("Da xoa lich su chat AI"));
    }
}
