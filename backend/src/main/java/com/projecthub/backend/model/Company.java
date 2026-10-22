package com.projecthub.backend.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "companies")
public class Company {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false)
    @Size(max = 150, message = "Company name cannot be greater than 150 characters")
    private String name;

    @Column(unique = true, nullable = false, length = 64)
    @Size(max = 64, message = "Hash is longer than 64 characters")
    private String invitationCodeHash;

    @CreationTimestamp
    private LocalDateTime createdAt;


}
