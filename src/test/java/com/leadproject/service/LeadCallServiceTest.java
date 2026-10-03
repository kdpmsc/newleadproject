package com.leadproject.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import com.leadproject.model.LeadCall;
import com.leadproject.repository.LeadCallRepository;
import com.leadproject.repository.LeadRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LeadCallServiceTest {

    @Mock
    private LeadCallRepository leadCallRepository;

    @Mock
    private LeadRepository leadRepository;

    @Test
    void appendAgentTurnUsesFirstSidWhenWebhookContainsDuplicateParameters() {
        LeadCall call = new LeadCall();
        call.setConversation("ASSISTANT: Hello");
        when(leadCallRepository.findByProviderCallSid("CAbd92afbf884065d5410a138fd16c9431"))
                .thenReturn(Optional.of(call));
        when(leadCallRepository.save(call)).thenReturn(call);

        LeadCallService service = new LeadCallService(leadCallRepository, leadRepository);

        service.appendAgentTurn(7L,
                "CAbd92afbf884065d5410a138fd16c9431,CAbd92afbf884065d5410a138fd16c9431",
                "user", "Yes");

        assertEquals("ASSISTANT: Hello\nUSER: Yes", call.getConversation());
        verify(leadCallRepository).findByProviderCallSid("CAbd92afbf884065d5410a138fd16c9431");
    }

    @Test
    void attachProviderCallStoresCanonicalSid() {
        LeadCall call = new LeadCall();
        when(leadCallRepository.findById(42L)).thenReturn(Optional.of(call));
        when(leadCallRepository.save(call)).thenReturn(call);

        LeadCallService service = new LeadCallService(leadCallRepository, leadRepository);

        service.attachProviderCall(42L, "CA-first,CA-first");

        assertEquals("CA-first", call.getProviderCallSid());
    }

    @Test
    void updateProviderStatusStoresCallDuration() {
        LeadCall call = new LeadCall();
        when(leadCallRepository.findByProviderCallSid("CA-call"))
                .thenReturn(Optional.of(call));
        when(leadCallRepository.save(call)).thenReturn(call);

        LeadCallService service = new LeadCallService(leadCallRepository, leadRepository);
        service.updateProviderStatus("CA-call", "completed", 125L);

        assertEquals("COMPLETED", call.getStatus());
        assertEquals(125L, call.getDurationSeconds());
    }

    @Test
    void updateProviderStatusArchivesPendingPartialSpeechAsUnconfirmed() {
        LeadCall call = new LeadCall();
        call.setConversation("ASSISTANT: Which area do you mean?");
        call.setPendingPartialSpeechResult("You don't know JVC; it is one location in Dubai");
        when(leadCallRepository.findByProviderCallSid("CA-call"))
                .thenReturn(Optional.of(call));
        when(leadCallRepository.save(call)).thenReturn(call);

        LeadCallService service = new LeadCallService(leadCallRepository, leadRepository);
        service.updateProviderStatus("CA-call", "completed", 71L);

        assertEquals("COMPLETED", call.getStatus());
        assertEquals(71L, call.getDurationSeconds());
        assertEquals("ASSISTANT: Which area do you mean?\nUSER (PARTIAL, UNCONFIRMED): You don't know JVC; it is one location in Dubai",
                call.getConversation());
        assertEquals(call.getConversation(), call.getTranscript());
        assertEquals(null, call.getPendingPartialSpeechResult());
        verify(leadCallRepository).save(call);
    }

    @Test
    void latePartialCallbackAfterCallEndIsArchivedOnlyOnce() {
        LeadCall call = new LeadCall();
        call.setStatus("COMPLETED");
        when(leadCallRepository.findByProviderCallSid("CA-call"))
                .thenReturn(Optional.of(call));
        when(leadCallRepository.save(call)).thenReturn(call);

        LeadCallService service = new LeadCallService(leadCallRepository, leadRepository);
        service.updatePartialSpeechResult("CA-call", "JVC, a location in Dubai");
        service.updatePartialSpeechResult("CA-call", "JVC, a location in Dubai");

        assertEquals("USER (PARTIAL, UNCONFIRMED): JVC, a location in Dubai", call.getConversation());
        verify(leadCallRepository).save(call);
    }
}