package com.stylo.api_agendamento.adapters.inbound.rest.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonFormat;

public record RegisterClientRequest(
                @NotBlank String name,
                @NotBlank @Email String email,
                @NotBlank @Size(min = 6) String password,
                @NotBlank String phoneNumber,
                @NotBlank String cpf,

                @JsonFormat(pattern = "dd/MM/yyyy") LocalDate dateOfBirth,
                String gender,
                String firebaseUid) {
}