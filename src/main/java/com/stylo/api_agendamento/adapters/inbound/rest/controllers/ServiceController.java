package com.stylo.api_agendamento.adapters.inbound.rest.controllers;

import com.stylo.api_agendamento.adapters.inbound.rest.dto.professional.CreateServiceRequest;
import com.stylo.api_agendamento.adapters.inbound.rest.dto.professional.UpdateServiceRequest;
import com.stylo.api_agendamento.core.domain.Service;
import com.stylo.api_agendamento.core.ports.IServiceRepository;
import com.stylo.api_agendamento.core.usecases.CreateServiceUseCase;
import com.stylo.api_agendamento.core.usecases.UpdateServiceUseCase;
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

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/services")
@RequiredArgsConstructor
@Tag(name = "Catálogo de Serviços", description = "Gestão dos serviços prestados (Cortes, Colorações, Consultas, etc.)")
public class ServiceController {

    private final CreateServiceUseCase createServiceUseCase;
    private final UpdateServiceUseCase updateServiceUseCase;
    private final IServiceRepository serviceRepository;

    // --- ESCRITA (Staff) ---

    @Operation(summary = "Criar Serviço", description = "Adiciona um novo serviço ao catálogo do estabelecimento logado.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Serviço criado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos")
    })
    @PostMapping
    @PreAuthorize("hasAuthority('appointment:manage_all') or hasRole('SERVICE_PROVIDER')")
    public ResponseEntity<Service> create(@RequestBody @Valid CreateServiceRequest request) {
        
        // Tratamento seguro do UUID da categoria vindo do DTO (que o recebe como String)
        UUID categoryId = (request.categoryId() != null && !request.categoryId().isBlank()) 
                ? UUID.fromString(request.categoryId()) 
                : null;

        // Utilizamos o Record 'Input' corretamente tipado
        var input = new CreateServiceUseCase.Input(
                request.name(),
                request.description(),
                request.duration(),
                request.price(),
                categoryId,
                null // serviceProviderId nulo indica que o UseCase deve extrair do Token JWT do utilizador logado
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(createServiceUseCase.execute(input));
    }

    @Operation(summary = "Atualizar Serviço", description = "Modifica os dados (preço, duração, nome) de um serviço existente.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Serviço atualizado com sucesso"),
            @ApiResponse(responseCode = "403", description = "Tentativa de alterar serviço de outro estabelecimento (Bloqueado)")
    })
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('appointment:manage_all') or hasRole('SERVICE_PROVIDER')")
    public ResponseEntity<Service> update(
            @PathVariable UUID id,
            @RequestBody @Valid UpdateServiceRequest request) {

        var input = new UpdateServiceUseCase.Input(
                id,
                request.name(),
                request.description(),
                request.duration(),
                request.price()
        );

        return ResponseEntity.ok(updateServiceUseCase.execute(input));
    }

    // --- LEITURA (Público / Autenticado) ---

    @Operation(summary = "Listar serviços ativos de um estabelecimento", description = "Retorna o catálogo de serviços disponíveis para agendamento. (Uso do Front-end Cliente)")
    @GetMapping("/provider/{providerId}/active")
    // Sem @PreAuthorize: Rota pública para que clientes não autenticados possam ver o preçário do salão
    public ResponseEntity<List<Service>> listActiveByProvider(@PathVariable UUID providerId) {
        return ResponseEntity.ok(serviceRepository.findAllActiveByProviderId(providerId));
    }

    @Operation(summary = "Listar todos os serviços de um estabelecimento", description = "Retorna todos os serviços (ativos e inativos) para o painel de gestão do salão.")
    @GetMapping("/provider/{providerId}")
    @PreAuthorize("hasAuthority('appointment:read') or hasRole('SERVICE_PROVIDER')")
    public ResponseEntity<List<Service>> listAllByProvider(@PathVariable UUID providerId) {
        return ResponseEntity.ok(serviceRepository.findAllByProviderId(providerId));
    }

    @Operation(summary = "Listar por Categoria", description = "Retorna todos os serviços pertencentes a uma categoria específica.")
    @GetMapping("/category/{categoryId}")
    public ResponseEntity<List<Service>> listByCategory(@PathVariable UUID categoryId) {
        return ResponseEntity.ok(serviceRepository.findByCategoryId(categoryId));
    }

    @Operation(summary = "Listar todos os serviços do sistema", description = "Apenas para Administradores do sistema (SuperAdmin).")
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')") // Rota perigosa protegida para uso interno da plataforma
    public ResponseEntity<List<Service>> listAll() {
        return ResponseEntity.ok(serviceRepository.findAll());
    }
}