package org.example.greenybackend.modules.ai;

public interface AiPromptService {

	String buildSystemPrompt(String customPrompt);

    String buildUserPrompt(AiContextResult context, String question);

    String ensureExternalSourceNote(String content, AiContextResult context);

}
