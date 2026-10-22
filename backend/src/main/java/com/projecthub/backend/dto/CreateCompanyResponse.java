package com.projecthub.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateCompanyResponse {

    private Long id;
    private String companyName;
    private String invitationCode;
    private LocalDateTime createdAt;
}

