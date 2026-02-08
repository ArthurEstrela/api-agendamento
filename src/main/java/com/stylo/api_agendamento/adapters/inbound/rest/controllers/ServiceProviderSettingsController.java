package com.stylo.api_agendamento.adapters.inbound.rest.controllers;

import com.stylo.api_agendamento.core.usecases.UpdateServiceProviderProfileUseCase;
import com.stylo.api_agendamento.adapters.inbound.rest.dto.serviceProvider.UpdateServiceProviderInput;
import com.stylo.api_agendamento.core.domain.ServiceProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/service-providers/settings")
@RequiredArgsConstructor
public class ServiceProviderSettingsController {

    private final UpdateServiceProviderProfileUseCase updateProfileUseCase;

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('SERVICE_PROVIDER')")
    public ResponseEntity<ServiceProvider> updateProfile(
            @PathVariable String id,
            @RequestBody UpdateServiceProviderInput request) {
        
        // Em um cenário real, você validaria se o ID do token é o mesmo do PathVariable
        var updated = updateProfileUseCase.execute(id, request);
        return ResponseEntity.ok(updated);
    }
}