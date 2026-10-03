package com.leadproject.config;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class VoiceScriptPropertiesTest {

    @Test
    void requiresClarificationOnlyForValidScoresBelowThreshold() {
        VoiceScriptProperties properties = new VoiceScriptProperties();
        properties.setSpeechConfidenceThreshold(0.45);

        assertTrue(properties.requiresSpeechClarification("0.2"));
        assertFalse(properties.requiresSpeechClarification("0.8"));
        assertFalse(properties.requiresSpeechClarification(null));
        assertFalse(properties.requiresSpeechClarification("not-a-score"));
        assertFalse(properties.requiresSpeechClarification("1.2"));
    }

    @Test
    void recognizesFarewellAsWholePhraseAcrossPunctuation() {
        VoiceScriptProperties properties = new VoiceScriptProperties();
        properties.setConversationEndKeywords(List.of("bye", "goodbye", "talk later"));

        assertTrue(properties.matchesConversationEnd("I didn't get a response, bye."));
        assertTrue(properties.matchesConversationEnd("We can talk later!"));
        assertFalse(properties.matchesConversationEnd("Maybe I'll buy."));
    }

    @Test
    void identifiesOnlyShortGreetingAndConnectionChecksAsNoProgress() {
        VoiceScriptProperties properties = new VoiceScriptProperties();

        assertTrue(properties.isNoProgressSpeech("Hello, hello. Anyone there?"));
        assertTrue(properties.isNoProgressSpeech("Please"));
        assertFalse(properties.isNoProgressSpeech("Hello, I want to buy a house."));
        assertFalse(properties.isNoProgressSpeech("I need a rental property."));
    }
}