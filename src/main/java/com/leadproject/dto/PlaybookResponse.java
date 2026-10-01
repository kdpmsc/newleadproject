package com.leadproject.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.leadproject.model.Playbook;
import com.leadproject.model.PlaybookVersion;

public record PlaybookResponse(
        Long id,
        String industry,
        String name,
        String status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<VersionResponse> versions) {

    public static PlaybookResponse from(Playbook playbook) {
        List<VersionResponse> versions = playbook.getVersions() == null
                ? List.of()
                : playbook.getVersions().stream().map(VersionResponse::from).toList();
        return new PlaybookResponse(
                playbook.getId(),
                playbook.getIndustry(),
                playbook.getName(),
                playbook.getStatus(),
                playbook.getCreatedAt(),
                playbook.getUpdatedAt(),
                versions);
    }

    public record VersionResponse(
            Long id,
            String version,
            String configurationJson,
            String promptVersion,
            String approvedBy,
            LocalDateTime approvedAt,
            String approvalState,
            LocalDateTime createdAt) {

        private static VersionResponse from(PlaybookVersion version) {
            return new VersionResponse(
                    version.getId(),
                    version.getVersion(),
                    version.getConfigurationJson(),
                    version.getPromptVersion(),
                    version.getApprovedBy(),
                    version.getApprovedAt(),
                    version.getApprovalState(),
                    version.getCreatedAt());
        }
    }
}