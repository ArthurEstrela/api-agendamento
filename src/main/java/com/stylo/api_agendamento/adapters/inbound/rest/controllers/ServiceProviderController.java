package com.stylo.api_agendamento.adapters.inbound.rest.controllers;

import com.stylo.api_agendamento.adapters.inbound.rest.dto.serviceProvider.AddressRequest;
import com.stylo.api_agendamento.adapters.inbound.rest.dto.serviceProvider.RegisterServiceProviderRequest;
import com.stylo.api_agendamento.core.domain.ServiceProvider;
import com.stylo.api_agendamento.core.domain.vo.Address;
import com.stylo.api_agendamento.core.domain.vo.Document;
import com.stylo.api_agendamento.core.domain.vo.Slug;
import com.stylo.api_agendamento.core.usecases.RegisterServiceProviderUseCase;
import com.stylo.api_agendamento.core.usecases.UpdateServiceProviderProfileUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/service-providers")
@RequiredArgsConstructor
@Tag(name = "Estabelecimentos (Tenant)", description = "Onboarding e gestão de perfil de salões, clínicas e barbearias")
public class ServiceProviderController {

    private final RegisterServiceProviderUseCase registerUseCase;
    private final UpdateServiceProviderProfileUseCase updateProfileUseCase;

    @Operation(summary = "Criar Conta (Onboarding)", description = "Regista um novo estabelecimento e cria automaticamente o utilizador administrador/proprietário.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Estabelecimento registado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos, e-mail já em uso ou CPF/CNPJ duplicado")
    })
    @PostMapping("/register")
    public ResponseEntity<ServiceProvider> register(@RequestBody @Valid RegisterServiceProviderRequest request) {

        // 1. Geração do Slug baseada no nome
        String generatedSlugValue = request.businessName().toLowerCase()
                .trim()
                .replaceAll("[^a-z0-9\\s]", "")
                .replaceAll("\\s+", "-");

        // 2. Conversão segura do endereço
        Address addressDomain = request.address().toDomain();

        // 3. ✨ CORREÇÃO DEFINITIVA: Instanciando Document conforme a definição do Record
        String cleanDoc = request.document().replaceAll("\\D", "");
        
        // Determinamos o tipo baseado no tamanho (11 = CPF, 14 = CNPJ)
        Document.DocumentType type = cleanDoc.length() > 11 
                ? Document.DocumentType.CNPJ 
                : Document.DocumentType.CPF;

        Document document = new Document(cleanDoc, type);

        // 4. Instância do Record 'Input'
        var input = new RegisterServiceProviderUseCase.Input(
                request.businessName(),
                document, 
                new Slug(generatedSlugValue),
                addressDomain,
                request.ownerName(),
                request.ownerEmail(),
                request.ownerPassword(),
                true 
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(registerUseCase.execute(input));
    }

    @Operation(summary = "Atualizar Perfil do Estabelecimento", description = "Atualiza os dados públicos (logo, banner, url/slug, morada) do salão.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Perfil atualizado com sucesso"),
            @ApiResponse(responseCode = "400", description = "O Slug escolhido já está em uso"),
            @ApiResponse(responseCode = "403", description = "Acesso negado: Apenas o dono pode alterar isto")
    })
    @PutMapping("/profile")
    @PreAuthorize("hasAuthority('finance:manage') or hasRole('SERVICE_PROVIDER')")
    public ResponseEntity<ServiceProvider> updateProfile(@RequestBody @Valid UpdateProfileRequest request) {
        
        Address addressDomain = request.address() != null ? request.address().toDomain() : null;

        var input = new UpdateServiceProviderProfileUseCase.Input(
                request.name(),
                request.phoneNumber(),
                request.logoUrl(),
                request.bannerUrl(),
                request.slug(),
                addressDomain
        );

        return ResponseEntity.ok(updateProfileUseCase.execute(input));
    }

    public record UpdateProfileRequest(
            String name,
            String phoneNumber,
            String logoUrl,
            String bannerUrl,
            String slug,
            AddressRequest address
    ) {}
}