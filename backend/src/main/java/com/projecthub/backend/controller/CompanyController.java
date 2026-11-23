package com.projecthub.backend.controller;

import com.projecthub.backend.dto.*;
import com.projecthub.backend.mapper.CompanyMapper;
import com.projecthub.backend.model.Company;
import com.projecthub.backend.service.CompanyService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/companies")
public class CompanyController {
    private final CompanyService companyService;
    private final CompanyMapper companyMapper;

    public CompanyController(CompanyService companyService, CompanyMapper companyMapper) {
        this.companyService = companyService;
        this.companyMapper = companyMapper;
    }

    @GetMapping("/me")
    public ResponseEntity<CompanyResponse> getMyCompany()
    {
        Company company = companyService.getMyCompany();
        CompanyResponse companyResponse = companyMapper.toCompanyResponse(company);
        companyResponse.setCompanyOwnerName(companyService.getCompanyOwner(company));
        return ResponseEntity.ok(companyResponse);
    }

    @PostMapping("/add")
    public ResponseEntity<CreateCompanyResponse> addCompany(@Valid @RequestBody CreateCompanyRequest createCompanyRequest)
    {
        return ResponseEntity.status(HttpStatus.CREATED).body(companyService.createCompany(createCompanyRequest));
    }

    @PostMapping("/join")
    public ResponseEntity<String> joinCompany(@Valid @RequestBody JoinCompanyRequest joinCompanyRequest)
    {
        companyService.joinCompany(joinCompanyRequest.getInvitationCode());
        return ResponseEntity.ok("You have joined the company");
    }

    @PatchMapping("/me/invitation-code")
    public ResponseEntity<String> rotateCode()
    {
        return ResponseEntity.ok(companyService.updateInviteCode());
    }

    @PutMapping("/me/owner")
    public ResponseEntity<String> moveOwner(@Valid @RequestBody MoveOwnershipRequest moveOwnershipRequest)
    {
        companyService.moveOwnership(moveOwnershipRequest.getId());
        return ResponseEntity.ok("Company ownership has been successfully changed");
    }

    @DeleteMapping("/me/members/{memberId}")
    public ResponseEntity<Void> removeMember(@PathVariable Long memberId)
    {
        companyService.removeMember(memberId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @DeleteMapping("/me/membership")
    public ResponseEntity<Void> leaveCompany()
    {
        companyService.leaveCompany();
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

}
