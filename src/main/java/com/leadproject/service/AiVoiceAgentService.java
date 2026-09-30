package com.leadproject.service;

import com.leadproject.config.AiProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Service
public class AiVoiceAgentService {

    private static final Logger logger = LoggerFactory.getLogger(AiVoiceAgentService.class);

    private final LlmChatClient llmChatClient;
    private final AiProperties aiProperties;
    private final Executor warmupExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "llm-warmup");
        t.setDaemon(true);
        return t;
    });

    public AiVoiceAgentService(LlmChatClient llmChatClient, AiProperties aiProperties) {
        this.llmChatClient = llmChatClient;
        this.aiProperties = aiProperties;
        logger.info("AI voice agent ready: provider={}, chatUrl={}, model={}",
                llmChatClient.provider(), llmChatClient.chatUrl(), llmChatClient.model());
    }

    public String respond(String leadName, String conversation) {
        logger.info("Calling voice agent: provider={}, url={}, model={}, conversationLength={}",
                llmChatClient.provider(), llmChatClient.chatUrl(), llmChatClient.model(),
                conversation == null ? 0 : conversation.length());

        String nameFallback = requireConfigured(aiProperties.getFallbackLeadName(), "app.ai.fallback-lead-name");
        String emptyPlaceholder = requireConfigured(aiProperties.getEmptyConversationPlaceholder(),
                "app.ai.empty-conversation-placeholder");
        String name = leadName == null || leadName.isBlank() ? nameFallback : leadName;
        String history = conversation == null || conversation.isBlank() ? emptyPlaceholder : trimConversation(conversation);
        String template = requireConfigured(aiProperties.getVoiceUserPromptTemplate(),
                "app.ai.voice-user-prompt-template");
        String userPrompt = template.replace("{name}", name).replace("{history}", history);

        long startedAt = System.nanoTime();
        try {
            String response = llmChatClient.complete(aiProperties.getSystemPrompt(), userPrompt);
            logger.info("Voice agent reply ready: provider={}, model={}, responseLength={}, durationMs={}",
                    llmChatClient.provider(), llmChatClient.model(), response.length(),
                    (System.nanoTime() - startedAt) / 1_000_000);
            return response;
        } catch (RuntimeException exception) {
            logger.error("Voice-agent request failed: provider={}, url={}, model={}, durationMs={}",
                    llmChatClient.provider(), llmChatClient.chatUrl(), llmChatClient.model(),
                    (System.nanoTime() - startedAt) / 1_000_000, exception);
            throw exception;
        }
    }

    /** Fire-and-forget warmup (Ollama keep_alive). No-op for cloud providers. */
    public void warmupAsync() {
        if (!llmChatClient.supportsWarmup()) {
            logger.debug("Skipping LLM warmup for provider={}", llmChatClient.provider());
            return;
        }
        CompletableFuture.runAsync(() -> {
            try {
                logger.info("Warming model: provider={}, url={}, model={}",
                        llmChatClient.provider(), llmChatClient.chatUrl(), llmChatClient.model());
                String system = requireConfigured(aiProperties.getWarmupSystemPrompt(), "app.ai.warmup-system-prompt");
                String user = requireConfigured(aiProperties.getWarmupUserPrompt(), "app.ai.warmup-user-prompt");
                llmChatClient.complete(system, user);
                logger.info("Model warmup complete: provider={}, model={}",
                        llmChatClient.provider(), llmChatClient.model());
            } catch (Exception exception) {
                logger.warn("Model warmup failed: provider={}, model={}, error={}",
                        llmChatClient.provider(), llmChatClient.model(), exception.toString());
            }
        }, warmupExecutor);
    }

    public String getChatUrl() {
        return llmChatClient.chatUrl();
    }

    public String getModel() {
        return llmChatClient.model();
    }

    public String getProvider() {
        return llmChatClient.provider();
    }

    public String chat(String message, String conversation) {
        logger.info("Calling chat API: provider={}, url={}, model={}, messageLength={}, conversationLength={}",
                llmChatClient.provider(), llmChatClient.chatUrl(), llmChatClient.model(),
                message == null ? 0 : message.length(),
                conversation == null ? 0 : conversation.length());
        String newPlaceholder = requireConfigured(aiProperties.getNewConversationPlaceholder(), "app.ai.new-conversation-placeholder");
        String history = conversation == null || conversation.isBlank() ? newPlaceholder : conversation;
        String template = requireConfigured(aiProperties.getChatUserPromptTemplate(), "app.ai.chat-user-prompt-template");
        String prompt = template.replace("{conversation}", history).replace("{message}", message == null ? "" : message);

        String response;
        try {
            response = llmChatClient.complete(aiProperties.getChatSystemPrompt(), prompt);
        } catch (RuntimeException exception) {
            logger.error("Chat request failed: provider={}, url={}, model={}",
                    llmChatClient.provider(), llmChatClient.chatUrl(), llmChatClient.model(), exception);
            throw exception;
        }

        if (response == null || response.isBlank()) {
            throw new IllegalStateException("AI model returned an empty chat response");
        }
        return response.trim();
    }

    private String trimConversation(String conversation) {
        String trimmed = conversation.trim();
        int max = aiProperties.getMaxConversationChars();
        if (max <= 0) {
            throw new IllegalStateException("app.ai.max-conversation-chars must be configured");
        }
        if (trimmed.length() <= max) {
            return trimmed;
        }
        return trimmed.substring(trimmed.length() - max);
    }

    private static String requireConfigured(String value, String key) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(key + " must be configured in application.yml");
        }
        return value;
    }
}