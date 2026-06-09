package org.example.greenybackend.modules.ai.impl;

import org.example.greenybackend.modules.ai.AiConversationService;
import org.example.greenybackend.modules.ai.AiChatRepository;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.example.greenybackend.domain.entity.AiChat;
import org.example.greenybackend.domain.entity.Messenger;
import org.example.greenybackend.domain.entity.UserEntity;
import org.example.greenybackend.modules.ai.dto.ChatHistoryDTO;
import org.example.greenybackend.modules.ai.dto.ChatMessageDTO;
import org.example.greenybackend.modules.notification.MessengerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AiConversationServiceImpl implements AiConversationService {

    private static final int STATUS_ACTIVE = 1;
    private static final int LEGACY_MESSAGE_TEXT_LIMIT = 240;
    private static final String SENDER_USER = "USER";
    private static final String SENDER_AI = "AI";
    private static final String TRUNCATED_SUFFIX = "\n[Da rut gon khi luu lich su vi cot MESSAGE_TEXT trong database dang qua ngan.]";

    private final AiChatRepository aiChatRepository;
    private final MessengerRepository messengerRepository;

    public AiConversationServiceImpl(AiChatRepository aiChatRepository, MessengerRepository messengerRepository) {
        this.aiChatRepository = aiChatRepository;
        this.messengerRepository = messengerRepository;
    }

    @Transactional
    @Override
    public AiChat findOrCreateConversation(UserEntity user, String conversationId) {
        if (conversationId != null && !conversationId.isBlank()) {
            return aiChatRepository.findByIdSessionAndUserEntityUserId(conversationId.trim(), user.getUserId())
                    .orElseThrow(() -> new IllegalArgumentException("Khong tim thay cuoc tro chuyen AI"));
        }

        LocalDateTime now = LocalDateTime.now();
        AiChat aiChat = new AiChat();
        aiChat.setIdSession(UUID.randomUUID().toString());
        aiChat.setCurrentStep("CHAT");
        aiChat.setStatus(STATUS_ACTIVE);
        aiChat.setStartedAt(now);
        aiChat.setCreatedAt(now);
        aiChat.setUpdatedAt(now);
        aiChat.setUserEntity(user);
        return aiChatRepository.save(aiChat);
    }

    @Transactional
    @Override
    public void touchConversation(String conversationId, String intent) {
        AiChat aiChat = aiChatRepository.findById(conversationId)
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay cuoc tro chuyen AI"));
        aiChat.setCurrentStep(intent);
        aiChat.setStatus(STATUS_ACTIVE);
        aiChat.setUpdatedAt(LocalDateTime.now());
    }

    @Transactional
    @Override
    public ChatMessageDTO saveUserMessage(UserEntity user, String conversationId, String message, String intent) {
        return saveMessage(user, conversationId, SENDER_USER, message, intent, 100);
    }

    @Transactional
    @Override
    public ChatMessageDTO saveAiMessage(UserEntity user, String conversationId, String message, String intent, int confidenceScore) {
        return saveMessage(user, conversationId, SENDER_AI, message, intent, confidenceScore);
    }

    @Transactional(readOnly = true)
    @Override
    public List<ChatHistoryDTO> getHistory(UserEntity user) {
        return aiChatRepository.findByUserEntityUserIdOrderByUpdatedAtDesc(user.getUserId()).stream()
                .map(conversation -> new ChatHistoryDTO(
                        conversation.getIdSession(),
                        conversation.getCurrentStep(),
                        conversation.getStatus(),
                        conversation.getStartedAt(),
                        conversation.getUpdatedAt(),
                        messagesFor(user, conversation.getIdSession())
                ))
                .toList();
    }

    @Transactional
    @Override
    public void deleteHistory(UserEntity user, String conversationId) {
        AiChat conversation = aiChatRepository.findByIdSessionAndUserEntityUserId(conversationId, user.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay cuoc tro chuyen AI"));
        messengerRepository.deleteByUserEntityUserIdAndExtractedData(user.getUserId(), conversationId);
        aiChatRepository.delete(conversation);
    }

    @Transactional(readOnly = true)
    @Override
    public List<ChatMessageDTO> recentMessages(UserEntity user) {
        return messengerRepository.findTop12ByUserEntityUserIdOrderByCreatedAtDesc(user.getUserId()).stream()
                .sorted(Comparator.comparing(Messenger::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(this::toMessageDto)
                .toList();
    }

    private ChatMessageDTO saveMessage(
            UserEntity user,
            String conversationId,
            String senderType,
            String message,
            String intent,
            Integer confidenceScore
    ) {
        LocalDateTime now = LocalDateTime.now();
        String messageId = UUID.randomUUID().toString();
        Messenger messenger = new Messenger();
        messenger.setIdMessenger(messageId);
        messenger.setSenderType(senderType);
        messenger.setMessageText(toPersistedMessage(message));
        messenger.setIntentDetected(intent);
        messenger.setExtractedData(conversationId);
        messenger.setConfidentScore(confidenceScore);
        messenger.setCreatedAt(now);
        messenger.setUserEntity(user);
        messengerRepository.save(messenger);
        return new ChatMessageDTO(
                messageId,
                conversationId,
                senderType,
                message,
                intent,
                now
        );
    }

    private List<ChatMessageDTO> messagesFor(UserEntity user, String conversationId) {
        return messengerRepository.findByUserEntityUserIdAndExtractedDataOrderByCreatedAtAsc(user.getUserId(), conversationId)
                .stream()
                .map(this::toMessageDto)
                .toList();
    }

    private ChatMessageDTO toMessageDto(Messenger messenger) {
        return new ChatMessageDTO(
                messenger.getIdMessenger(),
                messenger.getExtractedData(),
                messenger.getSenderType(),
                messenger.getMessageText(),
                messenger.getIntentDetected(),
                messenger.getCreatedAt()
        );
    }

    private String toPersistedMessage(String message) {
        if (message == null || message.length() <= LEGACY_MESSAGE_TEXT_LIMIT) {
            return message;
        }
        int maxContentLength = Math.max(0, LEGACY_MESSAGE_TEXT_LIMIT - TRUNCATED_SUFFIX.length());
        return message.substring(0, maxContentLength) + TRUNCATED_SUFFIX;
    }
}
