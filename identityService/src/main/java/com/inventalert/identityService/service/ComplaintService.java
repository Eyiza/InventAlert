package com.inventalert.identityService.service;

import com.inventalert.identityService.dto.request.ResolveComplaintRequest;
import com.inventalert.identityService.dto.request.SubmitComplaintRequest;
import com.inventalert.identityService.dto.response.ComplaintResponse;

import java.util.List;

public interface ComplaintService {
    ComplaintResponse submit(String companyId, String userId, SubmitComplaintRequest request);
    List<ComplaintResponse> listForCompany(String companyId);
    List<ComplaintResponse> listAll();
    ComplaintResponse resolve(String id, ResolveComplaintRequest request);
}
