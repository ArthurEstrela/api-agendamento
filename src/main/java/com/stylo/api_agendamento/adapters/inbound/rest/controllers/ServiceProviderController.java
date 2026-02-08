package com.stylo.api_agendamento.adapters.inbound.rest.controllers;

import com.stylo.api_agendamento.adapters.inbound.rest.dto.serviceProvider.RegisterServiceProviderRequest;
import com.stylo.api_agendamento.core.domain.ServiceProvider;
import com.stylo.api_agendamento.core.domain.vo.Document;
import com.stylo.api_agendamento.core.domain.vo.Slug;
import com.stylo.api_agendamento.core.usecases.RegisterServiceProviderUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/service-providers")
@RequiredArgsConstructor
public class ServiceProviderController {

    private final RegisterServiceProviderUseCase registerUseCase;
    private final PasswordEncoder passwordEncoder;

    @PostMapping("/register")
    public ResponseEntity<ServiceProvider> register(@RequestBody @Valid RegisterServiceProviderRequest request) {

        // 1. Geração do Slug baseada no nome
        String generatedSlugValue = request.businessName().toLowerCase()
                .trim()
                .replaceAll("[^a-z0-9\\s]", "")
                .replaceAll("\\s+", "-");

        // 2. Criptografia da senha
        String encryptedPassword = passwordEncoder.encode(request.ownerPassword());

        // 3. Conversão do endereço (Utilizando o método toDomain que criamos no DTO)
        // Isso resolve o erro de "builder() undefined" e evita duplicidade de variável
        var addressDomain = request.address().toDomain();

        // 4. Instância do Input para o Use Case (8 argumentos conforme o Record)
        var input = new RegisterServiceProviderUseCase.ServiceProviderInput(
                request.businessName(),
                new Document(request.document(), "CNPJ"),
                new Slug(generatedSlugValue),
                addressDomain,
                request.ownerName(),
                request.ownerEmail(),
                encryptedPassword,
                true // ownerIsProfessional
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(registerUseCase.execute(input));
    }
}