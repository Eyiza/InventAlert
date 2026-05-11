package com.inventalert.identityService.repository;

import com.inventalert.identityService.model.Complaint;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ComplaintRepository extends JpaRepository<Complaint, String> {
    List<Complaint> findAllByCompanyId(String companyId);
    List<Complaint> findAllBySubmittedBy(String submittedBy);
}
