package org.example.greenybackend.modules.ai.AI_Spring;

import com.google.genai.Client;
import io.micrometer.observation.ObservationRegistry;
import java.net.URI;
import java.util.List;
import org.example.greenybackend.domain.entity.UserEntity;
import org.example.greenybackend.modules.ai.AiContextResult;
import org.example.greenybackend.modules.ai.AiGenerationResult;
import org.example.greenybackend.modules.ai.AiProviderService;
import org.example.greenybackend.modules.ai.dto.AiUsageLimitDTO;
import org.example.greenybackend.modules.ai.support.AiProviderRuntime;
import org.example.greenybackend.modules.ai.support.AiRuntimeSettings;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.ai.google.genai.GoogleGenAiChatOptions;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.core.retry.RetryTemplate;
import org.springframework.stereotype.Service;

@Service
@Primary
@ConditionalOnProperty(prefix = "greeny.ai.spring", name = "enabled", havingValue = "true")
public class SpringAiProviderServiceImpl implements AiProviderService {

    private final AiProviderRuntime runtime;
    private final ToolCallingManager toolCallingManager;
    private final RetryTemplate retryTemplate;
    private final ObservationRegistry observationRegistry;

    public SpringAiProviderServiceImpl(AiProviderRuntime runtime) {
        this.runtime = runtime;
        this.toolCallingManager = ToolCallingManager.builder().build();
        this.retryTemplate = new RetryTemplate();
        this.observationRegistry = ObservationRegistry.NOOP;
    }

    @Override
    public AiGenerationResult generate(UserEntity user, String question, AiContextResult context) {
        return runtime.generate(user, question, context, this::callProvider, "Spring AI");
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
            return callGoogleGenAi(settings, question, context);
        }
        if (AiProviderRuntime.PROVIDER_CHATGPT.equals(settings.provider())) {
            return callChatGpt(settings, question, context);
        }
        throw new IllegalStateException("Unsupported AI provider: " + settings.provider());
    }

    private AiGenerationResult callGoogleGenAi(AiRuntimeSettings settings, String question, AiContextResult context) throws Exception {
        Client client = Client.builder()
                .apiKey(settings.apiKey())
                .build();
        GoogleGenAiChatOptions options = new GoogleGenAiChatOptions();
        options.setModel(settings.model());
        options.setTemperature(settings.temperature().doubleValue());
        options.setMaxOutputTokens(settings.maxTokens());
        options.setGoogleSearchRetrieval(runtime.shouldEnableGeminiSearch(settings.model()));

        GoogleGenAiChatModel model = GoogleGenAiChatModel.builder()
                .genAiClient(client)
                .defaultOptions(options)
                .toolCallingManager(toolCallingManager)
                .retryTemplate(retryTemplate)
                .observationRegistry(observationRegistry)
                .build();

        try {
            return callModel(model, settings, question, context);
        } finally {
            try {
                model.destroy();
            } catch (Exception ignored) {
                // Closing the short-lived Google client must not hide the AI response.
            }
        }
    }

    private AiGenerationResult callChatGpt(AiRuntimeSettings settings, String question, AiContextResult context) {
        OpenAiEndpoint endpoint = OpenAiEndpoint.from(settings.endpoint());
        OpenAiApi api = OpenAiApi.builder()
                .baseUrl(endpoint.baseUrl())
                .completionsPath(endpoint.completionsPath())
                .apiKey(settings.apiKey())
                .build();

        OpenAiChatOptions options = new OpenAiChatOptions();
        options.setModel(settings.model());
        options.setTemperature(settings.temperature().doubleValue());
        options.setMaxTokens(settings.maxTokens());

        OpenAiChatModel model = OpenAiChatModel.builder()
                .openAiApi(api)
                .defaultOptions(options)
                .toolCallingManager(toolCallingManager)
                .retryTemplate(retryTemplate)
                .observationRegistry(observationRegistry)
                .build();

        return callModel(model, settings, question, context);
    }

    private AiGenerationResult callModel(ChatModel model, AiRuntimeSettings settings, String question, AiContextResult context) {
        Prompt prompt = new Prompt(
                List.of(
                        new SystemMessage(settings.systemPrompt()),
                        new UserMessage(runtime.buildUserPrompt(context, question))
                )
        );
        ChatResponse response = model.call(prompt);
        String content = response == null
                || response.getResult() == null
                || response.getResult().getOutput() == null
                ? ""
                : response.getResult().getOutput().getText();
        if (content == null || content.isBlank()) {
            throw new IllegalStateException(settings.provider() + " returned empty content");
        }
        Usage usage = response.getMetadata() == null ? null : response.getMetadata().getUsage();
        return new AiGenerationResult(
                content.trim(),
                settings.provider(),
                settings.model(),
                false,
                usage == null ? null : usage.getPromptTokens(),
                usage == null ? null : usage.getCompletionTokens()
        );
    }

    private record OpenAiEndpoint(String baseUrl, String completionsPath) {

        static OpenAiEndpoint from(String endpoint) {
            URI uri = URI.create(endpoint);
            String port = uri.getPort() > -1 ? ":" + uri.getPort() : "";
            String baseUrl = uri.getScheme() + "://" + uri.getHost() + port;
            String path = uri.getRawPath();
            return new OpenAiEndpoint(baseUrl, path == null || path.isBlank() ? "/v1/chat/completions" : path);
        }
    }
}
