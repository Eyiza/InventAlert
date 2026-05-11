package com.inventalert.identityService.service.impl;

import com.inventalert.identityService.dto.request.ResolveComplaintRequest;
import com.inventalert.identityService.dto.request.SubmitComplaintRequest;
import com.inventalert.identityService.dto.response.ComplaintResponse;
import com.inventalert.identityService.exception.ComplaintNotFoundException;
import com.inventalert.identityService.model.Complaint;
import com.inventalert.identityService.model.ComplaintStatus;
import com.inventalert.identityService.repository.ComplaintRepository;
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

    @Override
    public ComplaintResponse submit(String companyId, String userId, SubmitComplaintRequest request) {
        Complaint complaint = Complaint.builder()
                .companyId(companyId)
                .submittedBy(userId)
                .subject(request.getSubject())
                .description(request.getDescription())
                .status(ComplaintStatus.OPEN)
                .build();
        return ComplaintResponse.from(complaintRepository.save(complaint));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ComplaintResponse> listForCompany(String companyId) {
        return complaintRepository.findAllByCompanyId(companyId)
                .stream().map(ComplaintResponse::from).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ComplaintResponse> listAll() {
        return complaintRepository.findAll()
                .stream().map(ComplaintResponse::from).toList();
    }

    @Override
    public ComplaintResponse resolve(String id, ResolveComplaintRequest request) {
        Complaint complaint = complaintRepository.findById(id)
                .orElseThrow(() -> new ComplaintNotFoundException(id));
        complaint.setStatus(ComplaintStatus.RESOLVED);
        complaint.setResolution(request.getResolution());
        return ComplaintResponse.from(complaintRepository.save(complaint));
    }
}
