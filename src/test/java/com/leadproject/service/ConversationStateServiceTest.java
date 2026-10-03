package com.leadproject.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.leadproject.model.ConversationState;
import com.leadproject.model.ConversationState.FactUpdate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@ExtendWith(MockitoExtension.class)
class ConversationStateServiceTest {

    @Mock
    private LeadCallService leadCallService;

    @Test
    void applyUpdatesPersistsConfirmedCorrectionAndRetainsPriorValue() throws Exception {
        String previousState = """
                {"turnCount":1,"facts":{"preferred_area":{"value":"Downtown Dubai", "status":"CONFIRMED", "confidence":0.9}},"corrections":[]}
                """;
        when(leadCallService.getConversationStateJson(7L, "CA-test")).thenReturn(previousState);

        ConversationStateService service = new ConversationStateService(leadCallService, new ObjectMapper());
        service.applyUpdates(7L, "CA-test", List.of(new FactUpdate("preferred_area", "Arjan", "CONFIRMED",
                0.98, "Actually, I meant Arjan", "CORRECTION")));

        ArgumentCaptor<String> persistedJson = ArgumentCaptor.forClass(String.class);
        verify(leadCallService).updateConversationStateJson(org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.eq("CA-test"), persistedJson.capture());
        ConversationState persistedState = new ObjectMapper().readValue(persistedJson.getValue(), ConversationState.class);
        assertEquals("Arjan", persistedState.getFacts().get("preferred_area").getValue());
        assertEquals("Downtown Dubai", persistedState.getCorrections().get(0).getPreviousValue());
    }

    @Test
    void nextTurnWaitsForBackgroundCorrectionWrite() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        String previousState = """
                {"turnCount":1,"facts":{"preferred_area":{"value":"Downtown Dubai", "status":"CONFIRMED", "confidence":0.9}},"corrections":[]}
                """;
        String updatedState = """
                {"turnCount":2,"facts":{"preferred_area":{"value":"Arjan", "status":"CONFIRMED", "confidence":0.98}},"corrections":[]}
                """;
        when(leadCallService.getConversationStateJson(7L, "CA-test"))
                .thenReturn(previousState, updatedState);

        ConversationStateService service = new ConversationStateService(leadCallService, objectMapper);
        CompletableFuture<List<FactUpdate>> extraction = new CompletableFuture<>();
        service.persistUpdatesWhenReady(7L, "CA-test", extraction);
        CompletableFuture<String> nextTurnState = CompletableFuture.supplyAsync(
                () -> service.getStateJson(7L, "CA-test"));

        assertFalse(nextTurnState.isDone());
        extraction.complete(List.of(new FactUpdate("preferred_area", "Arjan", "CONFIRMED",
                0.98, "Actually, I meant Arjan", "CORRECTION")));

        assertEquals("Arjan", objectMapper.readTree(nextTurnState.get(2, TimeUnit.SECONDS))
                .path("facts").path("preferred_area").path("value").asText());
        verify(leadCallService).updateConversationStateJson(org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.eq("CA-test"), org.mockito.ArgumentMatchers.anyString());
    }
}