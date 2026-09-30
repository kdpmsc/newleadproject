package com.leadproject.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.ai")
public class AiProperties {

    /** ollama | openai | openrouter | anthropic — default in application.yml */
    private String provider;
    private int connectTimeoutMs;
    private int readTimeoutMs;
    private double temperature;
    private int maxTokens;
    private String systemPrompt;
    private String chatSystemPrompt;
    private String voiceUserPromptTemplate;
    private String chatUserPromptTemplate;
    private String qualificationSystemPrompt;
    private String qualificationUserPromptTemplate;
    private String warmupSystemPrompt;
    private String warmupUserPrompt;
    private String emptyConversationPlaceholder;
    private String newConversationPlaceholder;
    private String fallbackLeadName;
    private int maxConversationChars;
    private final Ollama ollama = new Ollama();
    private final OpenAi openai = new OpenAi();
    private final OpenRouter openrouter = new OpenRouter();
    private final Anthropic anthropic = new Anthropic();

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public int getConnectTimeoutMs() { return connectTimeoutMs; }
    public void setConnectTimeoutMs(int connectTimeoutMs) { this.connectTimeoutMs = connectTimeoutMs; }
    public int getReadTimeoutMs() { return readTimeoutMs; }
    public void setReadTimeoutMs(int readTimeoutMs) { this.readTimeoutMs = readTimeoutMs; }
    public double getTemperature() { return temperature; }
    public void setTemperature(double temperature) { this.temperature = temperature; }
    public int getMaxTokens() { return maxTokens; }
    public void setMaxTokens(int maxTokens) { this.maxTokens = maxTokens; }
    public String getSystemPrompt() { return systemPrompt; }
    public void setSystemPrompt(String systemPrompt) { this.systemPrompt = systemPrompt; }
    public String getChatSystemPrompt() { return chatSystemPrompt; }
    public void setChatSystemPrompt(String chatSystemPrompt) { this.chatSystemPrompt = chatSystemPrompt; }
    public String getVoiceUserPromptTemplate() { return voiceUserPromptTemplate; }
    public void setVoiceUserPromptTemplate(String voiceUserPromptTemplate) { this.voiceUserPromptTemplate = voiceUserPromptTemplate; }
    public String getChatUserPromptTemplate() { return chatUserPromptTemplate; }
    public void setChatUserPromptTemplate(String chatUserPromptTemplate) { this.chatUserPromptTemplate = chatUserPromptTemplate; }
    public String getQualificationSystemPrompt() { return qualificationSystemPrompt; }
    public void setQualificationSystemPrompt(String qualificationSystemPrompt) { this.qualificationSystemPrompt = qualificationSystemPrompt; }
    public String getQualificationUserPromptTemplate() { return qualificationUserPromptTemplate; }
    public void setQualificationUserPromptTemplate(String qualificationUserPromptTemplate) { this.qualificationUserPromptTemplate = qualificationUserPromptTemplate; }
    public String getWarmupSystemPrompt() { return warmupSystemPrompt; }
    public void setWarmupSystemPrompt(String warmupSystemPrompt) { this.warmupSystemPrompt = warmupSystemPrompt; }
    public String getWarmupUserPrompt() { return warmupUserPrompt; }
    public void setWarmupUserPrompt(String warmupUserPrompt) { this.warmupUserPrompt = warmupUserPrompt; }
    public String getEmptyConversationPlaceholder() { return emptyConversationPlaceholder; }
    public void setEmptyConversationPlaceholder(String emptyConversationPlaceholder) { this.emptyConversationPlaceholder = emptyConversationPlaceholder; }
    public String getNewConversationPlaceholder() { return newConversationPlaceholder; }
    public void setNewConversationPlaceholder(String newConversationPlaceholder) { this.newConversationPlaceholder = newConversationPlaceholder; }
    public String getFallbackLeadName() { return fallbackLeadName; }
    public void setFallbackLeadName(String fallbackLeadName) { this.fallbackLeadName = fallbackLeadName; }
    public int getMaxConversationChars() { return maxConversationChars; }
    public void setMaxConversationChars(int maxConversationChars) { this.maxConversationChars = maxConversationChars; }
    public Ollama getOllama() { return ollama; }
    public OpenAi getOpenai() { return openai; }
    public OpenRouter getOpenrouter() { return openrouter; }
    public Anthropic getAnthropic() { return anthropic; }

    public String normalizedProvider() {
        return provider == null || provider.isBlank() ? "ollama" : provider.trim().toLowerCase();
    }

    public static class Ollama {
        private String baseUrl;
        private String model;
        private String chatPath;
        private int numPredict;
        private int numCtx;
        private String keepAlive;
        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }
        public String getChatPath() { return chatPath; }
        public void setChatPath(String chatPath) { this.chatPath = chatPath; }
        public int getNumPredict() { return numPredict; }
        public void setNumPredict(int numPredict) { this.numPredict = numPredict; }
        public int getNumCtx() { return numCtx; }
        public void setNumCtx(int numCtx) { this.numCtx = numCtx; }
        public String getKeepAlive() { return keepAlive; }
        public void setKeepAlive(String keepAlive) { this.keepAlive = keepAlive; }
    }

    public static class OpenAi {
        private String baseUrl;
        private String apiKey;
        private String model;
        private String chatPath;
        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }
        public String getChatPath() { return chatPath; }
        public void setChatPath(String chatPath) { this.chatPath = chatPath; }
    }

    public static class OpenRouter {
        private String baseUrl;
        private String apiKey;
        private String model;
        private String httpReferer;
        private String appTitle;
        private String chatPath;
        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }
        public String getHttpReferer() { return httpReferer; }
        public void setHttpReferer(String httpReferer) { this.httpReferer = httpReferer; }
        public String getAppTitle() { return appTitle; }
        public void setAppTitle(String appTitle) { this.appTitle = appTitle; }
        public String getChatPath() { return chatPath; }
        public void setChatPath(String chatPath) { this.chatPath = chatPath; }
    }

    public static class Anthropic {
        private String baseUrl;
        private String apiKey;
        private String model;
        private String version;
        private String messagesPath;
        public String getBaseUrl() { return baseUrl; }
        public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }
        public String getVersion() { return version; }
        public void setVersion(String version) { this.version = version; }
        public String getMessagesPath() { return messagesPath; }
        public void setMessagesPath(String messagesPath) { this.messagesPath = messagesPath; }
    }
}