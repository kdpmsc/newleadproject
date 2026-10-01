package com.leadproject.dto;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class LeadActivityUpdateRequest {

    private String qualificationNotes;
    private LocalDateTime followUpAt;
    private String followUpStatus;
}