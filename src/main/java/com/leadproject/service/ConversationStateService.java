package com.leadproject.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.leadproject.model.ConversationState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletionException;

@Service
public class ConversationStateService {

    private static final Logger logger = LoggerFactory.getLogger(ConversationStateService.class);

    private final LeadCallService leadCallService;
    private final ObjectMapper objectMapper;
    private final ConcurrentHashMap<String, CompletableFuture<Void>> pendingStateWrites = new ConcurrentHashMap<>();

    public ConversationStateService(LeadCallService leadCallService, ObjectMapper objectMapper) {
        this.leadCallService = leadCallService;
        this.objectMapper = objectMapper;
    }

    public String getStateJson(Long leadId, String providerCallSid) {
        CompletableFuture<Void> pendingWrite = pendingStateWrites.get(stateKey(leadId, providerCallSid));
        if (pendingWrite != null) {
            try {
                pendingWrite.join();
            } catch (CompletionException exception) {
                logger.warn("Prior conversation state write failed: leadId={}, callSid={}",
                        leadId, providerCallSid, exception);
            }
        }
        String stateJson = leadCallService.getConversationStateJson(leadId, providerCallSid);
        if (stateJson == null || stateJson.isBlank()) {
            return "{}";
        }
        ConversationState promptState = readState(stateJson);
        if (promptState == null) {
            logger.error("Ignoring malformed conversation state: leadId={}, callSid={}", leadId, providerCallSid);
            return "{}";
        }
        promptState.setCorrections(List.of());
        if (promptState.getFacts() != null) {
            promptState.getFacts().values().forEach(fact -> fact.setEvidence(null));
        }
        try {
            return objectMapper.writeValueAsString(promptState);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize prompt conversation state", exception);
        }
    }

    public void applyUpdates(Long leadId, String providerCallSid, List<ConversationState.FactUpdate> updates) {
        String storedState = leadCallService.getConversationStateJson(leadId, providerCallSid);
        ConversationState state = storedState == null || storedState.isBlank()
                ? new ConversationState()
                : readState(storedState);
        if (state == null) {
            logger.error("Skipping updates to malformed conversation state: leadId={}, callSid={}",
                    leadId, providerCallSid);
            return;
        }

        state.apply(updates);
        try {
            leadCallService.updateConversationStateJson(leadId, providerCallSid,
                    objectMapper.writeValueAsString(state));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize conversation state", exception);
        }
    }

    public void persistUpdatesWhenReady(Long leadId, String providerCallSid,
                                        CompletableFuture<List<ConversationState.FactUpdate>> updatesFuture) {
        String key = stateKey(leadId, providerCallSid);
        CompletableFuture<Void> pendingWrite = pendingStateWrites.compute(key, (stateKey, previousWrite) -> {
            CompletableFuture<Void> prior = previousWrite == null
                    ? CompletableFuture.completedFuture(null)
                    : previousWrite.handle((ignored, exception) -> null);
            return prior.thenCompose(ignored -> updatesFuture.handle((updates, exception) -> {
                if (exception != null) {
                    logger.warn("Skipping failed conversation state extraction: leadId={}, callSid={}",
                            leadId, providerCallSid, exception);
                } else {
                    applyUpdates(leadId, providerCallSid, updates);
                }
                return null;
            }));
        });
        pendingWrite.whenComplete((ignored, exception) -> pendingStateWrites.remove(key, pendingWrite));
    }

    private String stateKey(Long leadId, String providerCallSid) {
        if (providerCallSid == null || providerCallSid.isBlank()) {
            return "lead-" + leadId;
        }
        return providerCallSid.split(",", 2)[0].trim();
    }

    private ConversationState readState(String stateJson) {
        try {
            return objectMapper.readValue(stateJson, ConversationState.class);
        } catch (JsonProcessingException exception) {
            return null;
        }
    }
}