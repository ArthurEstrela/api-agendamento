package com.stylo.api_agendamento.core.domain;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class User {
    private String id;
    private String email;
    private String name;
    private UserRole role; // CLIENT, SERVICE_PROVIDER, PROFESSIONAL
    private LocalDateTime createdAt;
    private String phoneNumber;
    private String profilePictureUrl;
}