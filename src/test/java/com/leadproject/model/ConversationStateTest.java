package com.leadproject.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class ConversationStateTest {

    @Test
    void correctionReplacesConfirmedFactAndPreservesCorrectionHistory() {
        ConversationState state = new ConversationState();
        state.apply(List.of(
                new ConversationState.FactUpdate("preferred_area", "Downtown Dubai", "CONFIRMED",
                        0.91, "I prefer Downtown Dubai", "NEW")));

        state.apply(List.of(
                new ConversationState.FactUpdate("preferred_area", "Arjan", "CONFIRMED",
                        0.96, "Actually, I meant Arjan", "CORRECTION")));

        assertEquals("Arjan", state.getFacts().get("preferred_area").getValue());
        assertEquals("CONFIRMED", state.getFacts().get("preferred_area").getStatus());
        assertEquals(1, state.getCorrections().size());
        assertEquals("Downtown Dubai", state.getCorrections().get(0).getPreviousValue());
        assertEquals("Arjan", state.getCorrections().get(0).getCorrectedValue());
        assertEquals(2, state.getTurnCount());
    }

    @Test
    void tentativeObservationDoesNotOverwriteConfirmedFactWithoutCorrection() {
        ConversationState state = new ConversationState();
        state.apply(List.of(new ConversationState.FactUpdate("intent", "buy", "CONFIRMED",
                0.9, "I want to buy", "NEW")));

        state.apply(List.of(new ConversationState.FactUpdate("intent", "rent", "TENTATIVE",
                0.51, "Maybe rent", "UNCERTAIN")));

        assertEquals("buy", state.getFacts().get("intent").getValue());
        assertTrue(state.getCorrections().isEmpty());
    }

        @Test
        void zeroConfidenceExtractionCannotCreateConfirmedFact() {
                ConversationState state = new ConversationState();
                state.apply(List.of(new ConversationState.FactUpdate("time_frame", "January", "CONFIRMED",
                                0.0, "in the January", "NEW")));

                assertEquals("TENTATIVE", state.getFacts().get("time_frame").getStatus());
        }
}