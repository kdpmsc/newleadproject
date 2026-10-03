package com.leadproject.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.leadproject.config.AiProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AiVoiceAgentServiceTest {

    @Mock
    private LlmChatClient llmChatClient;

    @Test
    void respondUsesConfiguredPolicyAndCompleteConversationHistory() {
        AiProperties properties = new AiProperties();
        properties.setFallbackLeadName("there");
        properties.setEmptyConversationPlaceholder("(no messages yet)");
        properties.setMaxConversationChars(1200);
        properties.setSystemPrompt("Respond to the caller's latest point and clarify uncertain details.");
        properties.setVoiceUserPromptTemplate("Lead: {name}\nHistory:\n{history}");

        when(llmChatClient.provider()).thenReturn("openrouter");
        when(llmChatClient.chatUrl()).thenReturn("https://example.test/chat");
        when(llmChatClient.model()).thenReturn("test-model");
        when(llmChatClient.complete(properties.getSystemPrompt(),
                "Lead: Hardik\nHistory:\nASSISTANT: How can I help?\nUSER: I want an investment villa in Arjan."))
                .thenReturn("An investment villa in Arjan, understood. What budget range are you considering?");

        AiVoiceAgentService service = new AiVoiceAgentService(llmChatClient, properties);
        String response = service.respond("Hardik",
                "ASSISTANT: How can I help?\nUSER: I want an investment villa in Arjan.");

        assertEquals("An investment villa in Arjan, understood. What budget range are you considering?", response);

        ArgumentCaptor<String> systemPrompt = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> userPrompt = ArgumentCaptor.forClass(String.class);
        verify(llmChatClient).complete(systemPrompt.capture(), userPrompt.capture());
        assertTrue(systemPrompt.getValue().contains("clarify uncertain details"));
        assertTrue(userPrompt.getValue().contains("USER: I want an investment villa in Arjan."));
    }
}