package com.stylo.api_agendamento.adapters.inbound.rest.controllers;

import com.stylo.api_agendamento.adapters.inbound.rest.dto.client.UpdateClientProfileRequest;
import com.stylo.api_agendamento.core.domain.Client;
import com.stylo.api_agendamento.core.domain.User;
import com.stylo.api_agendamento.core.domain.vo.ClientPhone;
import com.stylo.api_agendamento.core.ports.IClientRepository;
import com.stylo.api_agendamento.core.ports.IUserContext;
import com.stylo.api_agendamento.core.usecases.GetClientHistoryUseCase;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/v1/clients")
@RequiredArgsConstructor
@Tag(name = "Área do Cliente", description = "Endpoints para gestão de perfil e histórico do cliente")
public class ClientController {

    private final IClientRepository clientRepository;
    private final GetClientHistoryUseCase getClientHistoryUseCase;

    private final IUserContext userContext;

    @Operation(summary = "Obter perfil do cliente", description = "Retorna os dados cadastrais do cliente. Acesso permitido ao próprio cliente ou ao Staff.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Perfil encontrado"),
            @ApiResponse(responseCode = "403", description = "Acesso negado (Tentativa de ver ficha de outro cliente)"),
            @ApiResponse(responseCode = "404", description = "Cliente não encontrado")
    })
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Client> getById(@PathVariable UUID id) {
        User loggedUser = userContext.getCurrentUser();

        // ✨ CORREÇÃO: Compara com o getClientId() e não com getId()
        if (loggedUser.isClient() && !id.equals(loggedUser.getClientId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return clientRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Atualizar Perfil", description = "Atualiza nome e telefone do cliente.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Perfil atualizado com sucesso"),
            @ApiResponse(responseCode = "403", description = "Acesso negado"),
            @ApiResponse(responseCode = "404", description = "Cliente não encontrado")
    })
    @PatchMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Client> updateProfile(
            @PathVariable UUID id,
            @RequestBody @Valid UpdateClientProfileRequest request) {

        User loggedUser = userContext.getCurrentUser();

        // ✨ CORREÇÃO: Compara com o getClientId() e não com getId()
        if (loggedUser.isClient() && !id.equals(loggedUser.getClientId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return clientRepository.findById(id)
                .map(existingClient -> {
                    String newName = request.name() != null ? request.name() : existingClient.getName();
                    ClientPhone newPhone = existingClient.getPhoneNumber();
                    if (request.phone() != null) {
                        newPhone = new ClientPhone(request.phone());
                    }

                    existingClient.updateContact(newName, newPhone);

                    return ResponseEntity.ok(clientRepository.save(existingClient));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Meu Histórico de Agendamentos", description = "Retorna o histórico paginado de serviços realizados pelo cliente logado.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Histórico recuperado com sucesso")
    })
    @GetMapping("/history")
    @PreAuthorize("hasRole('CLIENT')")
    public ResponseEntity<GetClientHistoryUseCase.Response> getMyHistory(
            @Parameter(description = "Número da página (0..N)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Itens por página") @RequestParam(defaultValue = "10") int size) {

        User loggedUser = userContext.getCurrentUser();

        // ✨ CORREÇÃO: Extrai o ID do perfil de cliente (e não o ID de autenticação)
        UUID clientId = loggedUser.getClientId();

        if (clientId == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build(); // Evita NullPointerException se perfil não
                                                                        // existir
        }

        var historyResponse = getClientHistoryUseCase.execute(clientId, page, size);
        return ResponseEntity.ok(historyResponse);
    }

    @Operation(summary = "Histórico de Agendamentos do Cliente (Staff)", description = "Retorna o histórico paginado de um cliente específico para visualização da clínica/salão.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Histórico recuperado com sucesso")
    })
    @GetMapping("/{id}/history")
    @PreAuthorize("hasAuthority('appointment:manage_all') or hasRole('PROFESSIONAL')")
    public ResponseEntity<GetClientHistoryUseCase.Response> getClientHistoryByStaff(
            @PathVariable UUID id,
            @Parameter(description = "Número da página (0..N)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Itens por página") @RequestParam(defaultValue = "10") int size) {

        var historyResponse = getClientHistoryUseCase.execute(id, page, size);
        return ResponseEntity.ok(historyResponse);
    }
}