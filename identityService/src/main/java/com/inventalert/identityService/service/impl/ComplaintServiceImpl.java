package com.inventalert.identityService.service.impl;

import com.inventalert.identityService.dto.request.ResolveComplaintRequest;
import com.inventalert.identityService.dto.request.SubmitComplaintRequest;
import com.inventalert.identityService.dto.response.ComplaintResponse;
import com.inventalert.identityService.exception.ComplaintNotFoundException;
import com.inventalert.identityService.model.Complaint;
import com.inventalert.identityService.model.ComplaintStatus;
import com.inventalert.identityService.repository.ComplaintRepository;
import com.inventalert.identityService.repository.CompanyRepository;
import com.inventalert.identityService.repository.UserRepository;
import com.inventalert.identityService.service.ComplaintService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ComplaintServiceImpl implements ComplaintService {

    private final ComplaintRepository complaintRepository;
    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;

    @Override
    public ComplaintResponse submit(String companyId, String userId, SubmitComplaintRequest request) {
        Complaint complaint = Complaint.builder()
                .companyId(companyId)
                .submittedBy(userId)
                .subject(request.getSubject())
                .description(request.getDescription())
                .status(ComplaintStatus.OPEN)
                .build();
        return toResponse(complaintRepository.save(complaint));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ComplaintResponse> listForCompany(String companyId) {
        return complaintRepository.findAllByCompanyId(companyId)
                .stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ComplaintResponse> listAll() {
        return complaintRepository.findAll()
                .stream().map(this::toResponse).toList();
    }

    @Override
    public ComplaintResponse resolve(String id, ResolveComplaintRequest request) {
        Complaint complaint = complaintRepository.findById(id)
                .orElseThrow(() -> new ComplaintNotFoundException(id));
        complaint.setStatus(ComplaintStatus.RESOLVED);
        complaint.setResolution(request.getResolution());
        return toResponse(complaintRepository.save(complaint));
    }

    private ComplaintResponse toResponse(Complaint c) {
        String companyName = null;
        String submitterEmail = null;
        try {
            companyName = companyRepository.findById(c.getCompanyId())
                    .map(company -> company.getCompanyName())
                    .orElse(null);
            submitterEmail = userRepository.findById(c.getSubmittedBy())
                    .map(user -> user.getEmail())
                    .orElse(null);
        } catch (Exception ignored) {}
        return new ComplaintResponse(
                c.getId(), c.getCompanyId(), companyName,
                c.getSubmittedBy(), submitterEmail,
                c.getSubject(), c.getDescription(),
                c.getStatus(), c.getResolution(),
                c.getCreatedAt(), c.getUpdatedAt()
        );
    }
}
