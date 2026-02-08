package com.stylo.api_agendamento.adapters.inbound.rest.controllers;

import com.stylo.api_agendamento.adapters.inbound.rest.dto.serviceProvider.RegisterServiceProviderRequest;
import com.stylo.api_agendamento.core.domain.ServiceProvider;
import com.stylo.api_agendamento.core.domain.vo.Address;
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
    private final PasswordEncoder passwordEncoder; // Adicionado para segurança

    @PostMapping("/register")
    public ResponseEntity<ServiceProvider> register(@RequestBody @Valid RegisterServiceProviderRequest request) {
        
        // Mapeamento para Value Objects do Domínio utilizando o Builder do Address
        var address = Address.builder()
            .street(request.address().street())
            .number(request.address().number())
            .neighborhood(request.address().neighborhood())
            .city(request.address().city())
            .state(request.address().state())
            .zipCode(request.address().zipCode())
            .build();

        // Geração automática de Slug baseada no nome da empresa
        String generatedSlugValue = request.businessName().toLowerCase()
                .trim()
                .replaceAll("[^a-z0-9\\s]", "")
                .replaceAll("\\s+", "-");

        // Criptografia da senha antes de enviar para a camada de Use Case
        String encryptedPassword = passwordEncoder.encode(request.ownerPassword());

        // Criação do Input corrigido para o RegisterServiceProviderUseCase
        var input = new RegisterServiceProviderUseCase.ServiceProviderInput(
            request.businessName(),
            new Document(request.document(), "CNPJ"), 
            new Slug(generatedSlugValue),
            address,
            request.ownerName(),
            request.ownerEmail(),
            encryptedPassword, // Nova propriedade obrigatória de segurança
            true 
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(registerUseCase.execute(input));
    }
}