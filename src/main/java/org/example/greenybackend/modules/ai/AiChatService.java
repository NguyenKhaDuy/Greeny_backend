package org.example.greenybackend.modules.ai;

import java.util.List;
import org.example.greenybackend.domain.entity.UserEntity;
import org.example.greenybackend.modules.ai.dto.AiUsageLimitDTO;
import org.example.greenybackend.modules.ai.dto.ChatHistoryDTO;
import org.example.greenybackend.modules.ai.dto.ChatRequest;
import org.example.greenybackend.modules.ai.dto.ChatResponse;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface AiChatService {

    ChatResponse chat(UserEntity user, ChatRequest request);

    SseEmitter stream(UserEntity user, ChatRequest request);

    List<ChatHistoryDTO> history(UserEntity user);

    AiUsageLimitDTO usageLimits(UserEntity user);

    void deleteHistory(UserEntity user, String conversationId);

}
