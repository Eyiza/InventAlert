package com.inventalert.identityService.dto.response;

import com.inventalert.identityService.model.Complaint;
import com.inventalert.identityService.model.ComplaintStatus;

import java.time.LocalDateTime;

public record ComplaintResponse(
        String id,
        String companyId,
        String submittedBy,
        String subject,
        String description,
        ComplaintStatus status,
        String resolution,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static ComplaintResponse from(Complaint c) {
        return new ComplaintResponse(
                c.getId(),
                c.getCompanyId(),
                c.getSubmittedBy(),
                c.getSubject(),
                c.getDescription(),
                c.getStatus(),
                c.getResolution(),
                c.getCreatedAt(),
                c.getUpdatedAt()
        );
    }
}
