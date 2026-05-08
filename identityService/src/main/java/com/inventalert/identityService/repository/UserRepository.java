package com.inventalert.identityService.repository;

import com.inventalert.identityService.model.Role;
import com.inventalert.identityService.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, String> {
    // Added at Dev B's request — needed for login (email is globally unique per V2 migration)
    Optional<User> findByEmail(String email);
    Optional<User> findByCompanyIdAndEmail(String companyId, String email);
    List<User> findAllByCompanyId(String companyId);
    boolean existsByCompanyIdAndEmail(String companyId, String email);
    List<User> findAllByCompanyIdAndRole(String companyId, Role role);
    Optional<User> findByIdAndCompanyId(String id, String companyId);
}
