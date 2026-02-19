package com.stylo.api_agendamento.adapters.inbound.rest.controllers;

import com.stylo.api_agendamento.core.domain.ServiceProvider;
import com.stylo.api_agendamento.core.ports.IServiceProviderRepository;
import com.stylo.api_agendamento.core.ports.IUserContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/v1/service-providers/settings")
@RequiredArgsConstructor
@Tag(name = "Configurações do Estabelecimento", description = "Gerenciamento de regras de cancelamento e políticas operacionais")
public class ServiceProviderSettingsController {

    private final IServiceProviderRepository providerRepository;
    private final IUserContext userContext;

    @Operation(summary = "Obter configurações atuais", description = "Retorna as regras operacionais vigentes para o estabelecimento logado.")
    @GetMapping
    @PreAuthorize("hasRole('SERVICE_PROVIDER') or hasAuthority('appointment:read')")
    public ResponseEntity<SchedulingSettingsResponse> getSettings() {
        // Extração segura do ID via Token JWT
        UUID providerId = userContext.getCurrentUser().getProviderId();
        
        return providerRepository.findById(providerId)
                .map(provider -> ResponseEntity.ok(new SchedulingSettingsResponse(
                        provider.getCancellationMinHours(), // ✨ CORREÇÃO: Nome correto do campo no domínio
                        provider.getMaxNoShowsAllowed()    // ✨ ADICIONADO: Campo que já existe no seu domínio
                )))
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Atualizar regras operacionais", description = "Altera prazos de cancelamento e limite de faltas permitidas.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Configurações atualizadas com sucesso"),
            @ApiResponse(responseCode = "403", description = "Acesso negado"),
            @ApiResponse(responseCode = "404", description = "Estabelecimento não encontrado")
    })
    @PatchMapping
    @PreAuthorize("hasRole('SERVICE_PROVIDER')")
    public ResponseEntity<Void> updateSettings(@RequestBody @Valid UpdateSettingsRequest request) {
        UUID providerId = userContext.getCurrentUser().getProviderId();

        ServiceProvider provider = providerRepository.findById(providerId)
                .orElseThrow(() -> new RuntimeException("Estabelecimento não encontrado"));

        // ✨ CORREÇÃO: Como o método updateSchedulingRules não existe no domínio, 
        // aplicamos a atualização via toBuilder para manter a imutabilidade se desejado,
        // ou via setters se a classe permitir. Baseado no seu ServiceProvider.java:
        
        ServiceProvider updatedProvider = provider.toBuilder()
                .cancellationMinHours(request.cancellationMinHours())
                .maxNoShowsAllowed(request.maxNoShowsAllowed())
                .build();

        providerRepository.save(updatedProvider);
        return ResponseEntity.ok().build();
    }

    // --- DTOs Ajustados para o seu Domínio ---

    public record SchedulingSettingsResponse(
            Integer cancellationMinHours,
            Integer maxNoShowsAllowed
    ) {}

    public record UpdateSettingsRequest(
            @NotNull(message = "O prazo mínimo de cancelamento é obrigatório") 
            @Min(0) Integer cancellationMinHours,
            
            @NotNull(message = "O limite de no-shows é obrigatório") 
            @Min(1) Integer maxNoShowsAllowed
    ) {}
}