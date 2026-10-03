package com.leadproject.controller;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.leadproject.config.AppProperties;
import com.leadproject.config.VoiceScriptProperties;
import com.leadproject.model.ConversationState;
import com.leadproject.service.AiVoiceAgentService;
import com.leadproject.service.ConversationStateService;
import com.leadproject.service.LeadCallService;
import com.leadproject.service.LeadService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@ExtendWith(MockitoExtension.class)
class VoiceControllerTest {

    @Mock
    private LeadService leadService;

    @Mock
    private LeadCallService leadCallService;

    @Mock
    private AiVoiceAgentService aiVoiceAgentService;

        @Mock
        private ConversationStateService conversationStateService;

    @Test
    void lowConfidenceSpeechGetsOneClarificationAndConfiguredHints() {
        VoiceScriptProperties voiceScript = new VoiceScriptProperties();
        voiceScript.setSpeechLanguage("en-US");
        voiceScript.setSpeechVoice("test-voice");
        voiceScript.setSpeechRecognitionHints("Arjan, Dubai Marina");
        voiceScript.setSpeechConfidenceThreshold(0.45);
        voiceScript.setMaxSpeechClarificationRetries(1);
        voiceScript.setSpeechClarificationPhrase("Sorry, I may have misheard that. Could you say it again?");
        voiceScript.setNoAnswerPhrase("Goodbye.");

        VoiceController controller = new VoiceController(leadService, leadCallService, aiVoiceAgentService,
                conversationStateService, new AppProperties(), voiceScript);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setServerName("localhost");
        request.setServerPort(8080);

        String twiml = controller.handleAiAgentTurn(7L, "Hardik", "an urgent area", "0.2", 0,
                "CA-test", request);

        assertTrue(twiml.contains("clarificationAttempt=1"));
        assertTrue(twiml.contains("hints=\"Arjan, Dubai Marina\""));
        assertTrue(twiml.contains("partialResultCallback=\"http://localhost:8080/api/v1/voice/ai-agent/partial\""));
        assertTrue(twiml.contains("Could you say it again?"));
        verify(leadCallService).appendAgentTurn(7L, "CA-test", "user", "an urgent area");
        verify(leadCallService).appendAgentTurn(7L, "CA-test", "assistant",
                "Sorry, I may have misheard that. Could you say it again?");
        verify(aiVoiceAgentService, never()).respondWithState(anyString(), anyString(), anyString());
    }

    @Test
    void completedVoiceReplyDoesNotWaitForFactExtraction() {
        String conversation = "USER: I meant JVC.";
        CompletableFuture<List<ConversationState.FactUpdate>> pendingUpdates = new CompletableFuture<>();
        CompletableFuture<String> generatedReply = new CompletableFuture<>();
        when(leadCallService.getConversation(7L, "CA-latency")).thenReturn(conversation);
        when(conversationStateService.getStateJson(7L, "CA-latency")).thenReturn("{}");
        when(aiVoiceAgentService.respondAsync("Hardik", conversation, "{}"))
                .thenReturn(generatedReply);
        when(aiVoiceAgentService.extractFactUpdatesAsync(conversation, "{}")).thenReturn(pendingUpdates);

        VoiceScriptProperties voiceScript = new VoiceScriptProperties();
        voiceScript.setSpeechLanguage("en-US");
        voiceScript.setSpeechVoice("test-voice");
        voiceScript.setNoAnswerPhrase("Goodbye.");
        VoiceController controller = new VoiceController(leadService, leadCallService, aiVoiceAgentService,
                conversationStateService, new AppProperties(), voiceScript);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setServerName("localhost");
        request.setServerPort(8080);

        String thinkingTwiml = controller.handleAiAgentTurn(7L, "Hardik", "I meant JVC.", null, 0,
                "CA-latency", request);
        assertTrue(thinkingTwiml.contains("<Redirect"));

        String spokenResponse = "Understood, JVC. What budget range are you considering?";
        generatedReply.complete(spokenResponse);
        verify(leadCallService).appendAgentTurn(7L, "CA-latency", "assistant", spokenResponse);

        String twiml = controller.handleAiAgentThink(7L, "Hardik", "CA-latency", "CA-latency", request);

        assertTrue(twiml.contains(spokenResponse));
        verify(leadCallService, times(1)).appendAgentTurn(7L, "CA-latency", "assistant", spokenResponse);
        verify(conversationStateService).persistUpdatesWhenReady(7L, "CA-latency", pendingUpdates);
    }

    @Test
    void callerGoodbyeEndsPolitelyEvenWhenSpeechConfidenceIsLow() {
        VoiceScriptProperties voiceScript = new VoiceScriptProperties();
        voiceScript.setSpeechLanguage("en-US");
        voiceScript.setSpeechVoice("test-voice");
        voiceScript.setSpeechConfidenceThreshold(0.45);
        voiceScript.setMaxSpeechClarificationRetries(1);
        voiceScript.setConversationEndKeywords(List.of("bye", "goodbye"));
        voiceScript.setConversationEndPhrase(
                "I'm sorry we didn't connect properly. Thank you for your time. We can reconnect later. Goodbye.");

        VoiceController controller = new VoiceController(leadService, leadCallService, aiVoiceAgentService,
                conversationStateService, new AppProperties(), voiceScript);

        String twiml = controller.handleAiAgentTurn(7L, "Hardik", "I didn't get a response, bye", "0.2", 0,
                "CA-goodbye", new MockHttpServletRequest());

        assertTrue(twiml.contains("I&apos;m sorry we didn&apos;t connect properly."));
        assertTrue(twiml.contains("Thank you for your time."));
        assertTrue(twiml.contains("<Hangup/>"));
        verify(leadCallService).appendAgentTurn(7L, "CA-goodbye", "user", "I didn't get a response, bye");
        verify(leadCallService).appendAgentTurn(7L, "CA-goodbye", "assistant",
                "I'm sorry we didn't connect properly. Thank you for your time. We can reconnect later. Goodbye.");
        verify(aiVoiceAgentService, never()).respondAsync(anyString(), anyString(), anyString());
    }

    @Test
    void repeatedHelloEndsPolitelyInsteadOfRepeatingQualificationQuestion() {
        VoiceScriptProperties voiceScript = new VoiceScriptProperties();
        voiceScript.setSpeechLanguage("en-US");
        voiceScript.setSpeechVoice("test-voice");
        voiceScript.setNoAnswerPhrase("Goodbye.");
        voiceScript.setConversationEndPhrase(
                "I'm sorry we didn't connect properly. Thank you for your time. We can reconnect later. Goodbye.");
        voiceScript.setMaxConsecutiveNoProgressTurns(1);
        when(leadCallService.getConversation(7L, "CA-loop"))
                .thenReturn("ASSISTANT: Are you looking to buy, rent, sell, or invest?\nUSER: Hello");

        VoiceController controller = new VoiceController(leadService, leadCallService, aiVoiceAgentService,
                conversationStateService, new AppProperties(), voiceScript);

        String twiml = controller.handleAiAgentTurn(7L, "Hardik", "Hello, anyone there?", null, 0,
                "CA-loop", new MockHttpServletRequest());

        assertTrue(twiml.contains("<Hangup/>"));
        assertTrue(twiml.contains("Thank you for your time."));
        verify(leadCallService).appendAgentTurn(7L, "CA-loop", "user", "Hello, anyone there?");
        verify(leadCallService).appendAgentTurn(7L, "CA-loop", "assistant",
                "I'm sorry we didn't connect properly. Thank you for your time. We can reconnect later. Goodbye.");
        verify(aiVoiceAgentService, never()).respondAsync(anyString(), anyString(), anyString());
    }
}