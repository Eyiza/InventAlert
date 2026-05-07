package com.inventalert.identityService.repository;

import com.inventalert.identityService.model.Role;
import com.inventalert.identityService.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, String> {
    Optional<User> findByCompanyIdAndEmail(String companyId, String email);
    List<User> findAllByCompanyId(String companyId);
    boolean existsByCompanyIdAndEmail(String companyId, String email);
    List<User> findAllByCompanyIdAndRole(String companyId, Role role);
    Optional<User> findByIdAndCompanyId(String id, String companyId);
}
