package com.projecthub.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class JoinCompanyRequest {
    @NotBlank
    private String invitationCode;
}
