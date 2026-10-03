package com.leadproject.model;

import java.util.List;

public record AiVoiceAgentTurn(String spokenResponse, List<ConversationState.FactUpdate> factUpdates) {
    public AiVoiceAgentTurn {
        factUpdates = factUpdates == null ? List.of() : List.copyOf(factUpdates);
    }
}