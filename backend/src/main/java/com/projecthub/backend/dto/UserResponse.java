package com.projecthub.backend.dto;

import com.projecthub.backend.enums.CompanyRole;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserResponse
{
    private Long id;
    private String name;
    private String email;
    private Long companyId;
    private String companyName;
    private CompanyRole companyRole;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
