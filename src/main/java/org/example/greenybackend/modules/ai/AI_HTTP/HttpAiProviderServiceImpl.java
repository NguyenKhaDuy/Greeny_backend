package org.example.greenybackend.modules.ai.AI_HTTP;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.example.greenybackend.domain.entity.UserEntity;
import org.example.greenybackend.modules.ai.AiContextResult;
import org.example.greenybackend.modules.ai.AiGenerationResult;
import org.example.greenybackend.modules.ai.AiProviderService;
import org.example.greenybackend.modules.ai.dto.AiUsageLimitDTO;
import org.example.greenybackend.modules.ai.support.AiProviderProperties;
import org.example.greenybackend.modules.ai.support.AiProviderRuntime;
import org.example.greenybackend.modules.ai.support.AiRuntimeSettings;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(prefix = "greeny.ai.http", name = "enabled", havingValue = "true", matchIfMissing = true)
public class HttpAiProviderServiceImpl implements AiProviderService {

    private final AiProviderRuntime runtime;
    private final AiProviderProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public HttpAiProviderServiceImpl(AiProviderRuntime runtime, AiProviderProperties properties) {
        this.runtime = runtime;
        this.properties = properties;
        this.objectMapper = new ObjectMapper();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
                .build();
    }

    @Override
    public AiGenerationResult generate(UserEntity user, String question, AiContextResult context) {
        return runtime.generate(user, question, context, this::callProvider, "HTTP");
    }

    @Override
    public AiUsageLimitDTO usageLimits(UserEntity user) {
        return runtime.usageLimits(user);
    }

    @Override
    public AiUsageLimitDTO usageLimits(String provider, String model) {
        return runtime.usageLimits(provider, model);
    }

    private AiGenerationResult callProvider(AiRuntimeSettings settings, String question, AiContextResult context) throws Exception {
        if (AiProviderRuntime.PROVIDER_GEMINI.equals(settings.provider())) {
            return callGeminiGenerateContent(settings, question, context);
        }
        if (AiProviderRuntime.PROVIDER_CHATGPT.equals(settings.provider())) {
            return callChatGptChatCompletions(settings, question, context);
        }
        throw new IllegalStateException("Unsupported AI provider: " + settings.provider());
    }

    private AiGenerationResult callChatGptChatCompletions(
            AiRuntimeSettings settings,
            String question,
            AiContextResult context
    ) throws Exception {
        String userPrompt = runtime.buildUserPrompt(context, question);
        Map<String, Object> requestBody = Map.of(
                "model", settings.model(),
                "messages", List.of(
                        Map.of("role", "system", "content", settings.systemPrompt()),
                        Map.of("role", "user", "content", userPrompt)
                ),
                "temperature", settings.temperature(),
                "max_tokens", settings.maxTokens()
        );

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(settings.endpoint()))
                .timeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
                .header("Authorization", "Bearer " + settings.apiKey())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(requestBody)))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("ChatGPT returned error code " + response.statusCode());
        }

        JsonNode root = objectMapper.readTree(response.body());
        String content = root.path("choices").path(0).path("message").path("content").asText("");
        if (content.isBlank()) {
            throw new IllegalStateException("ChatGPT returned empty content");
        }
        Integer promptTokens = intOrNull(root.path("usage").path("prompt_tokens"));
        Integer completionTokens = intOrNull(root.path("usage").path("completion_tokens"));
        return new AiGenerationResult(content.trim(), settings.provider(), settings.model(), false, promptTokens, completionTokens);
    }

    private AiGenerationResult callGeminiGenerateContent(
            AiRuntimeSettings settings,
            String question,
            AiContextResult context
    ) throws Exception {
        String userPrompt = runtime.buildUserPrompt(context, question);
        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("systemInstruction", Map.of(
                "parts", List.of(Map.of("text", settings.systemPrompt()))
        ));
        requestBody.put("contents", List.of(Map.of(
                "role", "user",
                "parts", List.of(Map.of("text", userPrompt))
        )));
        requestBody.put("generationConfig", Map.of(
                "temperature", settings.temperature(),
                "maxOutputTokens", settings.maxTokens()
        ));
        if (runtime.shouldEnableGeminiSearch(settings.model())) {
            requestBody.put("tools", List.of(Map.of("google_search", Map.of())));
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(settings.endpoint().replace("{model}", settings.model())))
                .timeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
                .header("x-goog-api-key", settings.apiKey())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(requestBody)))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("Google Gemini returned error code " + response.statusCode());
        }

        JsonNode root = objectMapper.readTree(response.body());
        String content = geminiText(root);
        if (content.isBlank()) {
            throw new IllegalStateException("Google Gemini returned empty content");
        }
        content = appendGeminiGroundingSources(content.trim(), root);
        Integer promptTokens = intOrNull(root.path("usageMetadata").path("promptTokenCount"));
        Integer completionTokens = intOrNull(root.path("usageMetadata").path("candidatesTokenCount"));
        return new AiGenerationResult(content.trim(), settings.provider(), settings.model(), false, promptTokens, completionTokens);
    }

    private Integer intOrNull(JsonNode node) {
        return node == null || node.isMissingNode() || node.isNull() ? null : node.asInt();
    }

    private String geminiText(JsonNode root) {
        JsonNode parts = root.path("candidates").path(0).path("content").path("parts");
        if (!parts.isArray()) {
            return "";
        }
        StringBuilder content = new StringBuilder();
        for (JsonNode part : parts) {
            String text = part.path("text").asText("");
            if (!text.isBlank()) {
                content.append(text);
            }
        }
        return content.toString();
    }

    private String appendGeminiGroundingSources(String content, JsonNode root) {
        List<String> sources = geminiGroundingSources(root);
        if (sources.isEmpty() || content.toLowerCase(Locale.ROOT).contains("nguon ngoai tham khao")) {
            return content;
        }
        return content + "\n\nNguon ngoai tham khao:\n- " + String.join("\n- ", sources);
    }

    private List<String> geminiGroundingSources(JsonNode root) {
        JsonNode chunks = root.path("candidates").path(0).path("groundingMetadata").path("groundingChunks");
        if (!chunks.isArray()) {
            return List.of();
        }

        LinkedHashSet<String> sources = new LinkedHashSet<>();
        for (JsonNode chunk : chunks) {
            JsonNode web = chunk.path("web");
            String uri = web.path("uri").asText("");
            if (uri.isBlank()) {
                continue;
            }
            String title = web.path("title").asText("");
            sources.add(firstNonBlank(title, "Nguon") + " - " + uri);
            if (sources.size() >= 3) {
                break;
            }
        }
        return new ArrayList<>(sources);
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }
}
