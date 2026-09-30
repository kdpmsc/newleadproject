package com.leadproject.service;

import java.util.Map;

import com.leadproject.config.AiProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AiQualificationService {

    private static final Logger logger = LoggerFactory.getLogger(AiQualificationService.class);

    private final LlmChatClient llmChatClient;
    private final AiProperties aiProperties;

    public AiQualificationService(LlmChatClient llmChatClient, AiProperties aiProperties) {
        this.llmChatClient = llmChatClient;
        this.aiProperties = aiProperties;
        logger.info("AI qualification client configured: provider={}, chatUrl={}, model={}",
                llmChatClient.provider(), llmChatClient.chatUrl(), llmChatClient.model());
    }

    public Map<String, Object> qualifyLead(String transcript, String playbookName) {
        logger.info("Starting lead qualification: provider={}, chatUrl={}, model={}, playbook={}, transcriptLength={}",
                llmChatClient.provider(), llmChatClient.chatUrl(), llmChatClient.model(), playbookName,
                transcript == null ? 0 : transcript.length());
        String system = aiProperties.getQualificationSystemPrompt();
        String template = aiProperties.getQualificationUserPromptTemplate();
        if (system == null || system.isBlank() || template == null || template.isBlank()) {
            throw new IllegalStateException("app.ai.qualification-system-prompt and qualification-user-prompt-template must be configured");
        }
        String prompt = template
                .replace("{playbook}", playbookName == null ? "" : playbookName)
                .replace("{transcript}", transcript == null ? "" : transcript);

        String json;
        try {
            json = llmChatClient.complete(system, prompt);
        } catch (RuntimeException exception) {
            logger.error("Qualification request failed: provider={}, url={}, model={}",
                    llmChatClient.provider(), llmChatClient.chatUrl(), llmChatClient.model(), exception);
            throw exception;
        }

        logger.info("AI lead qualification completed: playbook={}, responseLength={}",
                playbookName, json == null ? 0 : json.length());
        return Map.of("raw_response", json, "playbook", playbookName);
    }
}