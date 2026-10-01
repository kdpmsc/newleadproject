package com.leadproject.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import com.leadproject.model.Lead;
import com.leadproject.repository.LeadRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LeadServiceTest {

    @Mock
    private LeadRepository leadRepository;

    @Test
    void updateActivitySavesNotesAndScheduledFollowUp() {
        Lead lead = new Lead();
        LocalDateTime followUpAt = LocalDateTime.of(2026, 10, 2, 14, 30);
        when(leadRepository.findById(42L)).thenReturn(Optional.of(lead));
        when(leadRepository.save(lead)).thenReturn(lead);

        LeadService service = new LeadService(leadRepository);
        service.updateActivity(42L, "Asked to call after property viewing", followUpAt, "scheduled");

        assertEquals("Asked to call after property viewing", lead.getQualificationNotes());
        assertEquals(followUpAt, lead.getFollowUpAt());
        assertEquals("SCHEDULED", lead.getFollowUpStatus());
        verify(leadRepository).save(lead);
    }
}