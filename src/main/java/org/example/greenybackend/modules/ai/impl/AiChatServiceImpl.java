package org.example.greenybackend.modules.ai.impl;

import org.example.greenybackend.modules.ai.AiChatService;
import org.example.greenybackend.modules.ai.AiContextResult;
import org.example.greenybackend.modules.ai.AiContextService;
import org.example.greenybackend.modules.ai.AiConversationService;
import org.example.greenybackend.modules.ai.AiGenerationResult;
import org.example.greenybackend.modules.ai.AiProviderService;
import java.io.IOException;
import java.util.concurrent.Executor;
import java.util.List;
import org.example.greenybackend.domain.entity.AiChat;
import org.example.greenybackend.domain.entity.UserEntity;
import org.example.greenybackend.modules.ai.dto.AiUsageLimitDTO;
import org.example.greenybackend.modules.ai.dto.ChatHistoryDTO;
import org.example.greenybackend.modules.ai.dto.ChatMessageDTO;
import org.example.greenybackend.modules.ai.dto.ChatRequest;
import org.example.greenybackend.modules.ai.dto.ChatResponse;
import org.example.greenybackend.modules.ai.dto.ChatStreamEvent;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
public class AiChatServiceImpl implements AiChatService {

    private static final int MAX_MESSAGE_LENGTH = 1000;

    private final AiConversationService conversationService;
    private final AiContextService contextService;
    private final AiProviderService aiProviderService;
    private final Executor aiChatTaskExecutor;

    public AiChatServiceImpl(
            AiConversationService conversationService,
            AiContextService contextService,
            AiProviderService aiProviderService,
            @Qualifier("aiChatTaskExecutor") Executor aiChatTaskExecutor
    ) {
        this.conversationService = conversationService;
        this.contextService = contextService;
        this.aiProviderService = aiProviderService;
        this.aiChatTaskExecutor = aiChatTaskExecutor;
    }

    @Override
    public ChatResponse chat(UserEntity user, ChatRequest request) {
        String message = validateMessage(request);
        AiChat conversation = conversationService.findOrCreateConversation(user, request == null ? null : request.conversationId()); //mess cũ
        AiContextResult context = contextService.buildContext(user, message); //tạo
        ChatMessageDTO userMessage = conversationService.saveUserMessage(user, conversation.getIdSession(), message, context.intent());//lưu câu hỏi

        AiGenerationResult generation = aiProviderService.generate(user, message, context);//gọi AI

        ChatMessageDTO aiMessage = conversationService.saveAiMessage( //Lưu trả lời AI
                user,
                conversation.getIdSession(),
                generation.content(),
                context.intent(),
                context.hasDatabaseData() ? 90 : 0
        );
        conversationService.touchConversation(conversation.getIdSession(), context.intent());

        return new ChatResponse(
                conversation.getIdSession(),
                userMessage,
                aiMessage,
                context.intent(),
                context.toDto(),
                generation.provider(),
                generation.model(),
                generation.databaseFallback(),
                aiProviderService.usageLimits(generation.provider(), generation.model())
        );
    }

    @Override
    public SseEmitter stream(UserEntity user, ChatRequest request) { //lấy câu trả lời, cắt đoạn nhỏ - gửi lên fontend
        SseEmitter emitter = new SseEmitter(120_000L);
        aiChatTaskExecutor.execute(() -> {
            try {
                safeSend(emitter, "status", ChatStreamEvent.status(null, "connected"));
                ChatResponse response = chat(user, request);
                safeSend(emitter, "metadata", ChatStreamEvent.status(response.conversationId(), "answering"));
                for (String chunk : chunks(response.aiMessage().messageText())) {
                    safeSend(emitter, "chunk", ChatStreamEvent.chunk(response.conversationId(), chunk));
                }
                safeSend(emitter, "done", ChatStreamEvent.done(response));
                safeComplete(emitter);
            } catch (Exception exception) {
                safeSend(emitter, "error", ChatStreamEvent.error(exception.getMessage()));
                safeComplete(emitter);
            }
        });
        return emitter;
    }

    @Override
    public List<ChatHistoryDTO> history(UserEntity user) {
        return conversationService.getHistory(user);
    }

    @Override
    public AiUsageLimitDTO usageLimits(UserEntity user) {
        return aiProviderService.usageLimits(user);
    }

    @Override
    public void deleteHistory(UserEntity user, String conversationId) {
        if (conversationId == null || conversationId.isBlank()) {
            throw new IllegalArgumentException("conversationId khong hop le");
        }
        conversationService.deleteHistory(user, conversationId);
    }

    private String validateMessage(ChatRequest request) {
        if (request == null || request.message() == null || request.message().isBlank()) {
            throw new IllegalArgumentException("Noi dung chat khong duoc de trong");
        }
        String message = request.message().trim();
        if (message.length() > MAX_MESSAGE_LENGTH) {
            throw new IllegalArgumentException("Noi dung chat toi da " + MAX_MESSAGE_LENGTH + " ky tu");
        }
        return message;
    }

    private List<String> chunks(String content) {
        if (content == null || content.isBlank()) {
            return List.of("");
        }
        return content.lines()
                .flatMap(line -> splitLine(line).stream())
                .toList();
    }

    private List<String> splitLine(String line) {
        if (line.length() <= 90) {
            return List.of(line + "\n");
        }
        java.util.ArrayList<String> chunks = new java.util.ArrayList<>();
        int start = 0;
        while (start < line.length()) {
            int end = Math.min(start + 90, line.length());
            chunks.add(line.substring(start, end));
            start = end;
        }
        chunks.set(chunks.size() - 1, chunks.get(chunks.size() - 1) + "\n");
        return chunks;
    }

    private void send(SseEmitter emitter, String eventName, ChatStreamEvent event) throws IOException {
        emitter.send(SseEmitter.event().name(eventName).data(event));
    }

    private void safeSend(SseEmitter emitter, String eventName, ChatStreamEvent event) {
        try {
            send(emitter, eventName, event);
        } catch (Exception ignored) {
            
        }
    }

    private void safeComplete(SseEmitter emitter) {
        try {
            emitter.complete();
        } catch (Exception ignored) {
            
        }
    }
}
