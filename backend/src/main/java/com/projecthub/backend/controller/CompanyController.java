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
@RequestMapping("/api/v1")
public class CompanyController {
    private final CompanyService companyService;
    private final CompanyMapper companyMapper;

    public CompanyController(CompanyService companyService, CompanyMapper companyMapper) {
        this.companyService = companyService;
        this.companyMapper = companyMapper;
    }

    @GetMapping("companies/me")
    public ResponseEntity<CompanyResponse> getMyCompany()
    {
        Company company = companyService.getMyCompany();
        CompanyResponse companyResponse = companyMapper.toCompanyResponse(company);
        companyResponse.setCompanyOwnerName(companyService.getCompanyOwner(company));
        return ResponseEntity.ok(companyResponse);
    }

    @PostMapping("/companies")
    public ResponseEntity<CreateCompanyResponse> addCompany(@Valid @RequestBody CreateCompanyRequest createCompanyRequest)
    {
        return ResponseEntity.status(HttpStatus.CREATED).body(companyService.createCompany(createCompanyRequest));
    }

    @PostMapping("/companies/join")
    public ResponseEntity<String> joinCompany(@Valid @RequestBody JoinCompanyRequest joinCompanyRequest)
    {
        companyService.joinCompany(joinCompanyRequest.getInvitationCode());
        return ResponseEntity.ok("You have joined the company");
    }

    @PatchMapping("/companies/me/invitation-code")
    public ResponseEntity<String> rotateCode()
    {
        return ResponseEntity.ok(companyService.updateInviteCode());
    }

    @PutMapping("/companies/me/owner")
    public ResponseEntity<String> moveOwner(@Valid @RequestBody MoveOwnershipRequest moveOwnershipRequest)
    {
        companyService.moveOwnership(moveOwnershipRequest.getId());
        return ResponseEntity.ok("Company ownership has been successfully changed");
    }

}
