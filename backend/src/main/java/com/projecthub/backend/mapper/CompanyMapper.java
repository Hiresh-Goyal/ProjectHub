package com.projecthub.backend.mapper;

import com.projecthub.backend.dto.CompanyResponse;
import com.projecthub.backend.dto.CreateCompanyResponse;
import com.projecthub.backend.model.Company;
import org.springframework.stereotype.Component;

@Component
public class CompanyMapper {
    public CompanyResponse toCompanyResponse(Company company) {
        CompanyResponse companyResponse = new CompanyResponse();
        companyResponse.setId(company.getId());
        companyResponse.setCompanyName(company.getName());
        companyResponse.setCreatedAt(company.getCreatedAt());
        return companyResponse;
    }

    public CreateCompanyResponse toCreateCompanyResponse(Company company)
    {
        CreateCompanyResponse companyResponse = new CreateCompanyResponse();
        companyResponse.setId(company.getId());
        companyResponse.setCompanyName(company.getName());
        companyResponse.setCreatedAt(company.getCreatedAt());
        return companyResponse;
    }
}
