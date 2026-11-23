package com.projecthub.backend.service;

import com.projecthub.backend.dto.CreateCompanyRequest;
import com.projecthub.backend.dto.CreateCompanyResponse;
import com.projecthub.backend.enums.CompanyRole;
import com.projecthub.backend.mapper.CompanyMapper;
import com.projecthub.backend.model.Company;
import com.projecthub.backend.model.ProjectMember;
import com.projecthub.backend.model.User;
import com.projecthub.backend.repository.*;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;

@Service
public class CompanyService {
    private final CompanyRepo companyRepo;
    private final UserRepo userRepo;
    private final CompanyMapper companyMapper;
    private final UserService userService;
    private final SecureRandom secureRandom = new SecureRandom();
    private final ProjectRepo projectRepo;
    private final ProjectMemberRepo projectMemberRepo;
    private final TaskAssignmentRepo taskAssignmentRepo;

    public CompanyService(CompanyRepo companyRepo, UserRepo userRepo, CompanyMapper companyMapper, UserService userService, ProjectRepo projectRepo, ProjectMemberRepo projectMemberRepo, TaskAssignmentRepo taskAssignmentRepo) {
        this.companyRepo = companyRepo;
        this.userRepo = userRepo;
        this.companyMapper = companyMapper;
        this.userService = userService;
        this.projectRepo = projectRepo;
        this.projectMemberRepo = projectMemberRepo;
        this.taskAssignmentRepo = taskAssignmentRepo;
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

    @Transactional
    public void removeMember(Long memberId)
    {
        if(!userService.isCompanyOwner() && !userService.isGlobalAdmin())
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not owner of a company.");

        User currentUser = userService.getLoggedInUser();
        User user = userService.getUserById(memberId);

        //checking if user does not belong to a company
        if(user.getCompany() == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User does not belong to a company.");

        if(!userService.isGlobalAdmin())
        {

            //Removing user of diff company
            if (!Objects.equals(user.getCompany().getId(), currentUser.getCompany().getId()))
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User is not part of your company.");

        }


        //Removing user who owns projects
        if (!projectRepo.findAllByOwner(user).isEmpty())
            throw new ResponseStatusException(HttpStatus.CONFLICT, "User owns projects");

        //Removing admin of company
        if (user.getCompanyRole() == CompanyRole.COMPANY_ADMIN)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot remove other company admin");

        //Removing global admin
        if (user.getRoles().stream().anyMatch(role -> role.getName().equals("ROLE_ADMIN")))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot remove global admin");

        //Removing yourself as company admin
        if (Objects.equals(currentUser.getId(), memberId))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot remove yourself while you are company admin");

        List<ProjectMember> memberProjects = projectMemberRepo.findAllByUserId(user.getId());

        //Deleting all task assignments and project memberships
        memberProjects.forEach(member ->
                {
                    taskAssignmentRepo.deleteByProjectMemberId(member.getId());
                    projectMemberRepo.deleteById(member.getId());
                }
        );

        user.setCompany(null);
        user.setCompanyRole(null);
    }

    @Transactional
    public void leaveCompany()
    {
        User currentUser = userService.getLoggedInUser();

        //checking if user does not belong to a company
        if(currentUser.getCompany() == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User does not belong to a company.");

        //Is company admin
        if (currentUser.getCompanyRole() == CompanyRole.COMPANY_ADMIN)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Company admin must transfer ownership before leaving");

        //Removing user who owns projects
        if (!projectRepo.findAllByOwner(currentUser).isEmpty())
            throw new ResponseStatusException(HttpStatus.CONFLICT, "User owns projects");

        List<ProjectMember> memberProjects = projectMemberRepo.findAllByUserId(currentUser.getId());

        //Deleting all task assignments and project memberships
        memberProjects.forEach(member ->
                {
                    taskAssignmentRepo.deleteByProjectMemberId(member.getId());
                    projectMemberRepo.deleteById(member.getId());
                }
        );

        currentUser.setCompany(null);
        currentUser.setCompanyRole(null);
    }
}
