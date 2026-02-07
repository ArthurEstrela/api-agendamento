package com.stylo.api_agendamento.adapters.inbound.rest.controllers;

import com.stylo.api_agendamento.adapters.inbound.rest.dto.auth.AuthenticationRequest;
import com.stylo.api_agendamento.adapters.inbound.rest.dto.auth.RegisterClientRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    // Aqui você injetaria seus UseCases de Auth (ex: LoginUseCase, RegisterUserUseCase)

    @PostMapping("/register/client")
    public ResponseEntity<Void> registerClient(@RequestBody @Valid RegisterClientRequest request) {
        // Lógica de registro delegada ao UseCase
        return ResponseEntity.ok().build();
    }

    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody @Valid AuthenticationRequest request) {
        // Retornaria o Token JWT
        return ResponseEntity.ok("token-jwt-gerado");
    }
}