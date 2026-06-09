package org.example.greenybackend.modules.ai.support;

import java.math.BigDecimal;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "greeny.ai")
public class AiProviderProperties {

    private String provider = AiProviderRuntime.PROVIDER_GEMINI;
    private String model = "gemini-2.5-flash-lite";
    private String providerOrder = "gemini,chatgpt";
    private String apiKey = "";
    private String geminiApiKey = "";
    private String chatgptApiKey = "";
    private String geminiModel = "gemini-2.5-flash-lite";
    private String chatgptModel = "gpt-4o-mini";
    private String geminiEndpoint = "https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent";
    private String chatgptEndpoint = "https://api.openai.com/v1/chat/completions";
    private BigDecimal temperature = BigDecimal.valueOf(0.2);
    private int maxTokens = 700;
    private int timeoutSeconds = 45;
    private boolean allowPaidModels = false;
    private boolean geminiSearchGroundingEnabled = true;
    private boolean billingGuardEnabled = true;
    private int appDailyRequestLimit = 200;
    private int appDailyTokenLimit = 120000;
    private int geminiFreeRpm = 15;
    private int geminiFreeTpm = 250000;
    private int geminiFreeRpd = 1000;
    private int geminiSearchFreeRpd = 500;

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getProviderOrder() {
        return providerOrder;
    }

    public void setProviderOrder(String providerOrder) {
        this.providerOrder = providerOrder;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getGeminiApiKey() {
        return geminiApiKey;
    }

    public void setGeminiApiKey(String geminiApiKey) {
        this.geminiApiKey = geminiApiKey;
    }

    public String getChatgptApiKey() {
        return chatgptApiKey;
    }

    public void setChatgptApiKey(String chatgptApiKey) {
        this.chatgptApiKey = chatgptApiKey;
    }

    public String getGeminiModel() {
        return geminiModel;
    }

    public void setGeminiModel(String geminiModel) {
        this.geminiModel = geminiModel;
    }

    public String getChatgptModel() {
        return chatgptModel;
    }

    public void setChatgptModel(String chatgptModel) {
        this.chatgptModel = chatgptModel;
    }

    public String getGeminiEndpoint() {
        return geminiEndpoint;
    }

    public void setGeminiEndpoint(String geminiEndpoint) {
        this.geminiEndpoint = geminiEndpoint;
    }

    public String getChatgptEndpoint() {
        return chatgptEndpoint;
    }

    public void setChatgptEndpoint(String chatgptEndpoint) {
        this.chatgptEndpoint = chatgptEndpoint;
    }

    public BigDecimal getTemperature() {
        return temperature;
    }

    public void setTemperature(BigDecimal temperature) {
        this.temperature = temperature;
    }

    public int getMaxTokens() {
        return maxTokens;
    }

    public void setMaxTokens(int maxTokens) {
        this.maxTokens = maxTokens;
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(int timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }

    public boolean isAllowPaidModels() {
        return allowPaidModels;
    }

    public void setAllowPaidModels(boolean allowPaidModels) {
        this.allowPaidModels = allowPaidModels;
    }

    public boolean isGeminiSearchGroundingEnabled() {
        return geminiSearchGroundingEnabled;
    }

    public void setGeminiSearchGroundingEnabled(boolean geminiSearchGroundingEnabled) {
        this.geminiSearchGroundingEnabled = geminiSearchGroundingEnabled;
    }

    public boolean isBillingGuardEnabled() {
        return billingGuardEnabled;
    }

    public void setBillingGuardEnabled(boolean billingGuardEnabled) {
        this.billingGuardEnabled = billingGuardEnabled;
    }

    public int getAppDailyRequestLimit() {
        return appDailyRequestLimit;
    }

    public void setAppDailyRequestLimit(int appDailyRequestLimit) {
        this.appDailyRequestLimit = appDailyRequestLimit;
    }

    public int getAppDailyTokenLimit() {
        return appDailyTokenLimit;
    }

    public void setAppDailyTokenLimit(int appDailyTokenLimit) {
        this.appDailyTokenLimit = appDailyTokenLimit;
    }

    public int getGeminiFreeRpm() {
        return geminiFreeRpm;
    }

    public void setGeminiFreeRpm(int geminiFreeRpm) {
        this.geminiFreeRpm = geminiFreeRpm;
    }

    public int getGeminiFreeTpm() {
        return geminiFreeTpm;
    }

    public void setGeminiFreeTpm(int geminiFreeTpm) {
        this.geminiFreeTpm = geminiFreeTpm;
    }

    public int getGeminiFreeRpd() {
        return geminiFreeRpd;
    }

    public void setGeminiFreeRpd(int geminiFreeRpd) {
        this.geminiFreeRpd = geminiFreeRpd;
    }

    public int getGeminiSearchFreeRpd() {
        return geminiSearchFreeRpd;
    }

    public void setGeminiSearchFreeRpd(int geminiSearchFreeRpd) {
        this.geminiSearchFreeRpd = geminiSearchFreeRpd;
    }
}
