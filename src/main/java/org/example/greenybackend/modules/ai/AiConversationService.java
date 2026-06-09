package org.example.greenybackend.modules.ai;

import java.util.List;
import org.example.greenybackend.domain.entity.AiChat;
import org.example.greenybackend.domain.entity.UserEntity;
import org.example.greenybackend.modules.ai.dto.ChatHistoryDTO;
import org.example.greenybackend.modules.ai.dto.ChatMessageDTO;

public interface AiConversationService {

    AiChat findOrCreateConversation(UserEntity user, String conversationId);

    void touchConversation(String conversationId, String intent);

    ChatMessageDTO saveUserMessage(UserEntity user, String conversationId, String message, String intent);

    ChatMessageDTO saveAiMessage(UserEntity user, String conversationId, String message, String intent, int confidenceScore);

    List<ChatHistoryDTO> getHistory(UserEntity user);

    void deleteHistory(UserEntity user, String conversationId);

    List<ChatMessageDTO> recentMessages(UserEntity user);

}
