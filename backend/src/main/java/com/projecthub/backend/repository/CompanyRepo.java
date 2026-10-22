package com.projecthub.backend.repository;

import com.projecthub.backend.model.Company;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CompanyRepo extends JpaRepository<Company, Long>
{
    Optional<Company> findByInvitationCodeHash(String inviteCode);
}
