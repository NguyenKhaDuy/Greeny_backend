package org.example.greenybackend.modules.ai.dto;

public record ChatStreamEvent(
        String type,
        String conversationId,
        String content,
        ChatResponse response,
        String error
) {

    public static ChatStreamEvent status(String conversationId, String content) {
        return new ChatStreamEvent("status", conversationId, content, null, null);
    }

    public static ChatStreamEvent chunk(String conversationId, String content) {
        return new ChatStreamEvent("chunk", conversationId, content, null, null);
    }

    public static ChatStreamEvent done(ChatResponse response) {
        return new ChatStreamEvent("done", response.conversationId(), null, response, null);
    }

    public static ChatStreamEvent error(String message) {
        return new ChatStreamEvent("error", null, null, null, message);
    }
}
