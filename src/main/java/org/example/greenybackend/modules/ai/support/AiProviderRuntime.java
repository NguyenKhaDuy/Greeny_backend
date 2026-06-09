package org.example.greenybackend.modules.ai.support;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.example.greenybackend.domain.entity.AiSettings;
import org.example.greenybackend.domain.entity.AiUsageLogs;
import org.example.greenybackend.domain.entity.UserEntity;
import org.example.greenybackend.modules.ai.AiContextResult;
import org.example.greenybackend.modules.ai.AiGenerationResult;
import org.example.greenybackend.modules.ai.AiPromptService;
import org.example.greenybackend.modules.ai.AiProviderConfigurationException;
import org.example.greenybackend.modules.ai.AiSettingsRepository;
import org.example.greenybackend.modules.ai.AiUsageLogsRepository;
import org.example.greenybackend.modules.ai.dto.AiUsageLimitDTO;
import org.springframework.stereotype.Component;

@Component
public class AiProviderRuntime {

    public static final String PROVIDER_GEMINI = "gemini";
    public static final String PROVIDER_CHATGPT = "chatgpt";

    private final AiSettingsRepository aiSettingsRepository;
    private final AiUsageLogsRepository aiUsageLogsRepository;
    private final AiPromptService aiPromptService;
    private final AiProviderProperties properties;

    public AiProviderRuntime(
            AiSettingsRepository aiSettingsRepository,
            AiUsageLogsRepository aiUsageLogsRepository,
            AiPromptService aiPromptService,
            AiProviderProperties properties
    ) {
        this.aiSettingsRepository = aiSettingsRepository;
        this.aiUsageLogsRepository = aiUsageLogsRepository;
        this.aiPromptService = aiPromptService;
        this.properties = properties;
    }

    public AiGenerationResult generate(
            UserEntity user,
            String question,
            AiContextResult context,
            ProviderCaller caller,
            String engineName
    ) {
        List<AiRuntimeSettings> candidates = resolveSettings(user);
        boolean hasConfiguredProvider = false;

        for (AiRuntimeSettings settings : candidates) {
            long started = System.currentTimeMillis();
            if (settings.apiKey().isBlank()) {
                logUsage(user, settings.provider(), settings.model(), null, null, started, false,
                        "Missing " + apiKeyNameFor(settings.provider()));
                continue;
            }

            hasConfiguredProvider = true;
            enforceBillingGuard(settings);
            try {
                AiGenerationResult result = applySourcePolicy(caller.generate(settings, question, context), context);
                logUsage(user, result.provider(), result.model(), result.promptTokens(), result.completionTokens(), started, true, null);
                return result;
            } catch (Exception exception) {
                String message = settings.provider() + " " + engineName + " response failed: " + exception.getMessage();
                logUsage(user, settings.provider(), settings.model(), null, null, started, false, message);
            }
        }

        if (!hasConfiguredProvider) {
            throw new AiProviderConfigurationException(
                    "Chua cau hinh API key AI. Hay dien greeny.ai.gemini-api-key "
                            + "hoac greeny.ai.chatgpt-api-key trong application.properties."
            );
        }
        throw new IllegalStateException("AI dang tam thoi khong phan hoi, vui long thu lai sau");
    }

    public AiUsageLimitDTO usageLimits(UserEntity user) {
        return resolveSettings(user).stream()
                .findFirst()
                .map(settings -> usageLimits(settings.provider(), settings.model()))
                .orElseGet(() -> usageLimits(PROVIDER_GEMINI, "gemini-2.5-flash-lite"));
    }

    public AiUsageLimitDTO usageLimits(String provider, String model) {
        String safeProvider = normalizeProviderOrDefault(provider);
        String safeModel = firstNonBlank(model, modelFor(safeProvider));
        LocalDateTime start = LocalDate.now().atStartOfDay();
        LocalDateTime end = start.plusDays(1);
        long usedRequests = aiUsageLogsRepository.countSuccessfulProviderCallsToday(safeProvider, start, end);
        long usedTokens = aiUsageLogsRepository.sumSuccessfulProviderTokensToday(safeProvider, start, end);

        return new AiUsageLimitDTO(
                safeProvider,
                safeModel,
                properties.isBillingGuardEnabled(),
                properties.isAllowPaidModels(),
                properties.getMaxTokens(),
                properties.getAppDailyRequestLimit(),
                usedRequests,
                remaining(properties.getAppDailyRequestLimit(), usedRequests),
                properties.getAppDailyTokenLimit(),
                usedTokens,
                remaining(properties.getAppDailyTokenLimit(), usedTokens),
                providerFreeRpm(safeProvider, safeModel),
                providerFreeTpm(safeProvider, safeModel),
                providerFreeRpd(safeProvider, safeModel),
                PROVIDER_GEMINI.equals(safeProvider) ? properties.getGeminiSearchFreeRpd() : 0,
                PROVIDER_GEMINI.equals(safeProvider) && properties.isGeminiSearchGroundingEnabled(),
                end,
                limitNote(safeProvider)
        );
    }

    public List<AiRuntimeSettings> resolveSettings(UserEntity user) {
        AiSettings activeSettings = user == null ? null : aiSettingsRepository
                .findFirstByUserEntityUserIdAndIsActiveTrueOrderByUpdatedAtDescCreatedAtDesc(user.getUserId())
                .orElse(null);

        String systemPrompt = aiPromptService.buildSystemPrompt(activeSettings == null ? null : activeSettings.getSystemPrompt());
        BigDecimal temperature = activeSettings != null && activeSettings.getTemperature() != null
                ? activeSettings.getTemperature()
                : properties.getTemperature();
        int maxTokens = activeSettings != null && activeSettings.getMaxTokens() != null && activeSettings.getMaxTokens() > 0
                ? activeSettings.getMaxTokens()
                : properties.getMaxTokens();

        List<AiRuntimeSettings> candidates = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();

        String activeProvider = normalizeProvider(activeSettings == null ? null : activeSettings.getProvider());
        if (!activeProvider.isBlank()) {
            addCandidate(
                    candidates,
                    seen,
                    activeProvider,
                    firstNonBlank(activeSettings.getModelName(), modelFor(activeProvider)),
                    systemPrompt,
                    temperature,
                    maxTokens
            );
        }

        for (String provider : providerOrder()) {
            addCandidate(candidates, seen, provider, modelFor(provider), systemPrompt, temperature, maxTokens);
        }

        if (candidates.isEmpty()) {
            addCandidate(candidates, seen, PROVIDER_GEMINI, "gemini-2.5-flash-lite", systemPrompt, temperature, maxTokens);
        }
        return candidates;
    }

    public String buildUserPrompt(AiContextResult context, String question) {
        return aiPromptService.buildUserPrompt(context, question);
    }

    public boolean shouldEnableGeminiSearch(String model) {
        return properties.isGeminiSearchGroundingEnabled() && supportsGeminiSearch(model);
    }

    private void addCandidate(
            List<AiRuntimeSettings> candidates,
            Set<String> seen,
            String provider,
            String model,
            String systemPrompt,
            BigDecimal temperature,
            int maxTokens
    ) {
        String normalizedProvider = normalizeProvider(provider);
        String normalizedModel = trimToBlank(model);
        if (normalizedProvider.isBlank() || normalizedModel.isBlank()) {
            return;
        }
        if (!isAllowedModel(normalizedProvider, normalizedModel)) {
            return;
        }

        String key = normalizedProvider + "::" + normalizedModel.toLowerCase(Locale.ROOT);
        if (!seen.add(key)) {
            return;
        }

        candidates.add(new AiRuntimeSettings(
                normalizedProvider,
                normalizedModel,
                systemPrompt,
                apiKeyFor(normalizedProvider),
                endpointFor(normalizedProvider),
                temperature,
                maxTokens
        ));
    }

    private List<String> providerOrder() {
        LinkedHashSet<String> providers = new LinkedHashSet<>();
        addProvider(providers, properties.getProvider());
        if (properties.getProviderOrder() != null) {
            for (String provider : properties.getProviderOrder().split(",")) {
                addProvider(providers, provider);
            }
        }
        if (providers.isEmpty()) {
            providers.add(PROVIDER_GEMINI);
            providers.add(PROVIDER_CHATGPT);
        }
        return new ArrayList<>(providers);
    }

    private void addProvider(Set<String> providers, String provider) {
        String normalizedProvider = normalizeProvider(provider);
        if (!normalizedProvider.isBlank()) {
            providers.add(normalizedProvider);
        }
    }

    private void enforceBillingGuard(AiRuntimeSettings settings) {
        if (!properties.isBillingGuardEnabled()) {
            return;
        }

        AiUsageLimitDTO limits = usageLimits(settings.provider(), settings.model());
        if (limits.appDailyRequestLimit() > 0 && limits.remainingRequestsToday() <= 0) {
            throw new IllegalStateException(
                    "Da cham gioi han AI hom nay: "
                            + limits.usedRequestsToday()
                            + "/"
                            + limits.appDailyRequestLimit()
                            + " request. Tang greeny.ai.app-daily-request-limit neu muon dung tiep."
            );
        }
        if (limits.appDailyTokenLimit() > 0 && limits.remainingTokensToday() <= 0) {
            throw new IllegalStateException(
                    "Da cham gioi han token AI hom nay: "
                            + limits.usedTokensToday()
                            + "/"
                            + limits.appDailyTokenLimit()
                            + " token. Tang greeny.ai.app-daily-token-limit neu muon dung tiep."
            );
        }
    }

    private AiGenerationResult applySourcePolicy(AiGenerationResult result, AiContextResult context) {
        return new AiGenerationResult(
                aiPromptService.ensureExternalSourceNote(result.content(), context),
                result.provider(),
                result.model(),
                !context.hasDatabaseData(),
                result.promptTokens(),
                result.completionTokens()
        );
    }

    private void logUsage(
            UserEntity user,
            String provider,
            String model,
            Integer promptTokens,
            Integer completionTokens,
            long started,
            boolean success,
            String errorMessage
    ) {
        if (user == null) {
            return;
        }
        AiUsageLogs log = new AiUsageLogs();
        log.setAiUsageLogsId(UUID.randomUUID().toString());
        log.setProvider(provider);
        log.setModelName(model);
        log.setPromptTokens(promptTokens);
        log.setCompletionTokens(completionTokens);
        log.setLatencyMs((int) Math.min(Integer.MAX_VALUE, System.currentTimeMillis() - started));
        log.setSuccess(success);
        log.setErrorMessage(errorMessage == null ? null : trimError(errorMessage));
        log.setCreatedAt(LocalDateTime.now());
        log.setUserEntity(user);
        try {
            aiUsageLogsRepository.save(log);
        } catch (RuntimeException ignored) {
            // Usage logs are diagnostic only and must not block the chat response.
        }
    }

    private String apiKeyFor(String provider) {
        if (PROVIDER_GEMINI.equals(provider)) {
            return firstNonBlank(properties.getGeminiApiKey(), properties.getApiKey());
        }
        if (PROVIDER_CHATGPT.equals(provider)) {
            return firstNonBlank(properties.getChatgptApiKey(), properties.getApiKey());
        }
        return "";
    }

    private String endpointFor(String provider) {
        if (PROVIDER_GEMINI.equals(provider)) {
            return firstNonBlank(properties.getGeminiEndpoint(), "https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent");
        }
        if (PROVIDER_CHATGPT.equals(provider)) {
            return firstNonBlank(properties.getChatgptEndpoint(), "https://api.openai.com/v1/chat/completions");
        }
        return "";
    }

    private String modelFor(String provider) {
        String defaultProviderModel = normalizeProvider(properties.getProvider()).equals(provider) ? properties.getModel() : null;
        if (PROVIDER_GEMINI.equals(provider)) {
            return firstNonBlank(defaultProviderModel, properties.getGeminiModel(), "gemini-2.5-flash-lite");
        }
        if (PROVIDER_CHATGPT.equals(provider)) {
            return firstNonBlank(defaultProviderModel, properties.getChatgptModel(), "gpt-4o-mini");
        }
        return "";
    }

    private String apiKeyNameFor(String provider) {
        if (PROVIDER_GEMINI.equals(provider)) {
            return "greeny.ai.gemini-api-key";
        }
        if (PROVIDER_CHATGPT.equals(provider)) {
            return "greeny.ai.chatgpt-api-key";
        }
        return "greeny.ai.api-key";
    }

    private boolean isAllowedModel(String provider, String model) {
        if (PROVIDER_CHATGPT.equals(provider)) {
            return true;
        }
        if (!PROVIDER_GEMINI.equals(provider)) {
            return false;
        }
        if (properties.isAllowPaidModels()) {
            return true;
        }

        String normalizedModel = model == null ? "" : model.trim().toLowerCase(Locale.ROOT);
        return normalizedModel.startsWith("gemini-3.5-flash")
                || normalizedModel.startsWith("gemini-3.1-flash-lite")
                || normalizedModel.startsWith("gemini-2.5-flash")
                || normalizedModel.startsWith("gemini-2.0-flash")
                || normalizedModel.equals("gemini-2.5-pro")
                || normalizedModel.startsWith("gemini-3.1-pro-preview");
    }

    private boolean supportsGeminiSearch(String model) {
        String normalizedModel = model == null ? "" : model.trim().toLowerCase(Locale.ROOT);
        return normalizedModel.startsWith("gemini-3.5-flash")
                || normalizedModel.startsWith("gemini-3.1-flash")
                || normalizedModel.startsWith("gemini-3.1-pro-preview")
                || normalizedModel.startsWith("gemini-3-flash")
                || normalizedModel.startsWith("gemini-2.5")
                || normalizedModel.startsWith("gemini-2.0");
    }

    private int providerFreeRpm(String provider, String model) {
        if (!PROVIDER_GEMINI.equals(provider)) {
            return 0;
        }
        String normalizedModel = model == null ? "" : model.trim().toLowerCase(Locale.ROOT);
        if (normalizedModel.startsWith("gemini-2.0-flash-lite")) {
            return 30;
        }
        if (normalizedModel.startsWith("gemini-2.5-flash-lite")
                || normalizedModel.startsWith("gemini-2.0-flash")) {
            return 15;
        }
        if (normalizedModel.startsWith("gemini-2.5-flash")) {
            return 10;
        }
        if (normalizedModel.startsWith("gemini-2.5-pro")) {
            return 5;
        }
        return properties.getGeminiFreeRpm();
    }

    private int providerFreeTpm(String provider, String model) {
        if (!PROVIDER_GEMINI.equals(provider)) {
            return 0;
        }
        String normalizedModel = model == null ? "" : model.trim().toLowerCase(Locale.ROOT);
        if (normalizedModel.startsWith("gemini-2.0-flash")) {
            return 1_000_000;
        }
        return properties.getGeminiFreeTpm();
    }

    private int providerFreeRpd(String provider, String model) {
        if (!PROVIDER_GEMINI.equals(provider)) {
            return 0;
        }
        String normalizedModel = model == null ? "" : model.trim().toLowerCase(Locale.ROOT);
        if (normalizedModel.startsWith("gemini-2.5-flash-lite")) {
            return 1_000;
        }
        if (normalizedModel.startsWith("gemini-2.5-flash")) {
            return 250;
        }
        if (normalizedModel.startsWith("gemini-2.5-pro")) {
            return 100;
        }
        if (normalizedModel.startsWith("gemini-2.0-flash")) {
            return 200;
        }
        return properties.getGeminiFreeRpd();
    }

    private String limitNote(String provider) {
        if (PROVIDER_CHATGPT.equals(provider)) {
            return "ChatGPT dang o che do chua co API key. Dien greeny.ai.chatgpt-api-key khi san sang kich hoat.";
        }
        return "App cap dang thap hon free tier Gemini de tao vung dem. Neu bat billing tren Google AI Studio, hay giu allow-paid-models=false va app cap thap.";
    }

    private long remaining(int limit, long used) {
        if (limit <= 0) {
            return -1;
        }
        return Math.max(0, limit - used);
    }

    private String normalizeProviderOrDefault(String provider) {
        String normalizedProvider = normalizeProvider(provider);
        return normalizedProvider.isBlank() ? PROVIDER_GEMINI : normalizedProvider;
    }

    private String normalizeProvider(String provider) {
        if (provider == null || provider.isBlank()) {
            return "";
        }
        String normalizedProvider = provider.trim().toLowerCase(Locale.ROOT);
        if ("openai".equals(normalizedProvider) || "gpt".equals(normalizedProvider)) {
            return PROVIDER_CHATGPT;
        }
        if (PROVIDER_GEMINI.equals(normalizedProvider) || PROVIDER_CHATGPT.equals(normalizedProvider)) {
            return normalizedProvider;
        }
        return "";
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }

    private String trimToBlank(String value) {
        return value == null ? "" : value.trim();
    }

    private String trimError(String errorMessage) {
        String trimmed = errorMessage.replace("\n", " ").replace("\r", " ").trim();
        return trimmed.length() <= 240 ? trimmed : trimmed.substring(0, 240);
    }

    @FunctionalInterface
    public interface ProviderCaller {
        AiGenerationResult generate(AiRuntimeSettings settings, String question, AiContextResult context) throws Exception;
    }
}
