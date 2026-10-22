package com.projecthub.backend.service;

import com.projecthub.backend.dto.CreateCompanyRequest;
import com.projecthub.backend.dto.CreateCompanyResponse;
import com.projecthub.backend.enums.CompanyRole;
import com.projecthub.backend.mapper.CompanyMapper;
import com.projecthub.backend.model.Company;
import com.projecthub.backend.model.User;
import com.projecthub.backend.repository.CompanyRepo;
import com.projecthub.backend.repository.UserRepo;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.*;

@Service
public class CompanyService {
    private final CompanyRepo companyRepo;
    private final UserRepo userRepo;
    private final CompanyMapper companyMapper;
    private final UserService userService;
    private final SecureRandom secureRandom = new SecureRandom();

    public CompanyService(CompanyRepo companyRepo, UserRepo userRepo, CompanyMapper companyMapper, UserService userService) {
        this.companyRepo = companyRepo;
        this.userRepo = userRepo;
        this.companyMapper = companyMapper;
        this.userService = userService;
    }

    private String generateInvitationCode() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytes);
    }

    private String hashInvitationCode(String invitationCode) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            byte[] hash = digest.digest(
                    invitationCode.getBytes(StandardCharsets.UTF_8)
            );

            return HexFormat.of().formatHex(hash);

        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }

    @Transactional
    public CreateCompanyResponse createCompany(CreateCompanyRequest createCompanyRequest) {
        User user = userService.getLoggedInUser();
        if(user.getCompany() != null)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "You already belong to a company");

        Company company = new Company();
        String invitationCode = generateInvitationCode();
        String hashCode = hashInvitationCode(invitationCode);

        company.setName(createCompanyRequest.getCompanyName());
        company.setInvitationCodeHash(hashCode);
        Company savedCompany = companyRepo.save(company);
        user.setCompany(savedCompany);
        user.setCompanyRole(CompanyRole.COMPANY_ADMIN);
        userRepo.save(user);

        CreateCompanyResponse response = companyMapper.toCreateCompanyResponse(savedCompany);
        response.setInvitationCode(invitationCode);
        return response;
    }

    public Company getMyCompany()
    {
        User user = userService.getLoggedInUser();
        if(user.getCompany() != null)
            return user.getCompany();
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "You do not belong to a company");
    }

    @Transactional
    public String updateInviteCode()
    {
        User user = userService.getLoggedInUser();
        Company company = user.getCompany();
        if(company == null)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "You do not belong to a company");
        if(userService.isCompanyOwner())
        {
            String invitationCode = generateInvitationCode();
            String hashCode = hashInvitationCode(invitationCode);
            company.setInvitationCodeHash(hashCode);
            companyRepo.save(company);
            return invitationCode;
        }
        else
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,"You are not allowed to rotate invitation code");
    }

    public Company findByInviteCode(String inviteCode)
    {
        if((inviteCode == null) || (inviteCode.isBlank()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid invite code");
        String hash = hashInvitationCode(inviteCode);
        return companyRepo.findByInvitationCodeHash(hash).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid invite code"));
    }

    public String getCompanyOwner(Company company)
    {
        List<User> users = userRepo.findAllByCompany(company);

        return users.stream()
                .filter(user -> user.getCompanyRole() == CompanyRole.COMPANY_ADMIN)
                .findFirst().orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND, "Company owner not found"))
                .getName();
    }

    @Transactional
    public void joinCompany(String inviteCode)
    {
        User user = userService.getLoggedInUser();
        if(user.getCompany() != null)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "You already belong to a company");

        Company company = findByInviteCode(inviteCode);

        user.setCompany(company);
        user.setCompanyRole(CompanyRole.COMPANY_MEMBER);
        userRepo.save(user);
    }

//    @Transactional
//    public void leaveCompany()
//    {
//        User user = userService.getLoggedInUser();
//        user.setCompany(null);
//        user.setCompanyRole(null);
//        userRepo.save(user);
//    }

    @Transactional
    public void moveOwnership(Long userId)
    {
        User user = userService.getLoggedInUser();

        if (!(userService.isCompanyOwner())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not owner of the company.");
        }

        Company company = user.getCompany();
        if(company == null)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not part of a company.");

        if(Objects.equals(user.getId(), userId))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Entered Id matches your Id");

        User user2 = userRepo.findById(userId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found")
        );
        Company company2 = user2.getCompany();


        if(company2 == null || Objects.equals(company2.getId(), company.getId()))
        {
            user2.setCompany(company);
            user.setCompanyRole(CompanyRole.COMPANY_MEMBER);
            user2.setCompanyRole(CompanyRole.COMPANY_ADMIN);
            userRepo.save(user2);
            userRepo.save(user);
        }
        else
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You belong to different companies.");
    }

}
