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
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/service-providers")
@RequiredArgsConstructor
public class ServiceProviderController {

    private final RegisterServiceProviderUseCase registerUseCase;

    @PostMapping("/register")
    public ResponseEntity<ServiceProvider> register(@RequestBody @Valid RegisterServiceProviderRequest request) {
        
        // Mapeamento para Value Objects do Domínio
        var address = Address.builder()
            .street(request.address().street())
            .number(request.address().number())
            .neighborhood(request.address().neighborhood())
            .city(request.address().city())
            .state(request.address().state())
            .zipCode(request.address().zipCode())
            .build();

        // Geração simples de Slug (ou poderia vir no request)
        String generatedSlugValue = request.businessName().toLowerCase().replaceAll("\\s+", "-");

        var input = new RegisterServiceProviderUseCase.ServiceProviderInput(
            request.businessName(),
            new Document(request.document(), "CNPJ"), // Assume CNPJ por padrão no cadastro de business
            new Slug(generatedSlugValue),
            address,
            request.ownerName(),
            request.ownerEmail(),
            true // Por padrão, o dono é criado como um profissional disponível na agenda
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(registerUseCase.execute(input));
    }
}