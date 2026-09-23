package com.leadproject.controller;

import com.leadproject.dto.LeadQualificationRequest;
import com.leadproject.dto.AiChatRequest;
import com.leadproject.service.AiQualificationService;
import com.leadproject.service.AiVoiceAgentService;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class AiController {

    private static final Logger logger = LoggerFactory.getLogger(AiController.class);

    private final AiQualificationService aiQualificationService;
    private final AiVoiceAgentService aiVoiceAgentService;

    public AiController(AiQualificationService aiQualificationService, AiVoiceAgentService aiVoiceAgentService) {
        this.aiQualificationService = aiQualificationService;
        this.aiVoiceAgentService = aiVoiceAgentService;
    }

    @PostMapping("/ai/qualify")
    public Map<String, Object> qualifyLead(@Valid @RequestBody LeadQualificationRequest request) {
        long startedAt = System.nanoTime();
        logger.info("AI qualification request: playbook={}, transcriptLength={}",
            request.getPlaybook(), request.getTranscript() == null ? 0 : request.getTranscript().length());
        try {
            Map<String, Object> response =
                aiQualificationService.qualifyLead(request.getTranscript(), request.getPlaybook());
            logger.info("AI qualification completed: responseFields={}, durationMs={}",
                response.keySet(), elapsedMillis(startedAt));
            return response;
        } catch (RuntimeException exception) {
            logger.error("AI qualification failed: playbook={}, durationMs={}",
                request.getPlaybook(), elapsedMillis(startedAt), exception);
            throw exception;
        }
    }

    @PostMapping("/ai/chat")
    @Operation(summary = "Chat with the local Ollama model",
            description = "Sends a development chat message to the configured local Ollama model without starting a Twilio call.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "AI reply generated"),
            @ApiResponse(responseCode = "400", description = "Message is missing", content = @Content),
            @ApiResponse(responseCode = "500", description = "Ollama is unavailable or the configured model is missing", content = @Content)
    })
    public Map<String, String> chat(@Valid @RequestBody AiChatRequest request) {
        long startedAt = System.nanoTime();
        logger.info("AI chat request: messageLength={}, conversationLength={}",
                request.getMessage() == null ? 0 : request.getMessage().length(),
                request.getConversation() == null ? 0 : request.getConversation().length());
        try {
            String reply = aiVoiceAgentService.chat(request.getMessage(), request.getConversation());
            logger.info("AI chat completed: responseLength={}, durationMs={}",
                    reply.length(), elapsedMillis(startedAt));
            return Map.of("reply", reply);
        } catch (RuntimeException exception) {
            logger.error("AI chat failed: durationMs={}", elapsedMillis(startedAt), exception);
            throw exception;
        }
    }

    private long elapsedMillis(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000;
    }
}
