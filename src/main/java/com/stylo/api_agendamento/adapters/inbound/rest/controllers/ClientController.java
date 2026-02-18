package com.stylo.api_agendamento.adapters.inbound.rest.controllers;

import com.stylo.api_agendamento.adapters.inbound.rest.dto.client.UpdateClientProfileRequest;
import com.stylo.api_agendamento.core.domain.Client;
import com.stylo.api_agendamento.core.domain.User;
import com.stylo.api_agendamento.core.domain.vo.ClientPhone;
import com.stylo.api_agendamento.core.ports.IClientRepository;
import com.stylo.api_agendamento.core.usecases.GetClientHistoryUseCase;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/clients")
@RequiredArgsConstructor
@Tag(name = "Área do Cliente", description = "Endpoints para gestão de perfil e histórico do cliente")
public class ClientController {

    private final IClientRepository clientRepository;
    private final GetClientHistoryUseCase getClientHistoryUseCase;

    @Operation(summary = "Obter perfil do cliente", description = "Retorna os dados cadastrais do cliente logado ou por ID.")
    @GetMapping("/{id}")
    public ResponseEntity<Client> getById(
            @PathVariable String id,
            @AuthenticationPrincipal User loggedUser) {

        // Segurança: Impede que um cliente veja dados de outro
        if (!loggedUser.getId().equals(id)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return clientRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Atualizar Perfil", description = "Atualiza nome e telefone do cliente.")
    @PatchMapping("/{id}")
    public ResponseEntity<Client> updateProfile(
            @PathVariable String id,
            @RequestBody @Valid UpdateClientProfileRequest request,
            @AuthenticationPrincipal User loggedUser) {

        // Segurança: Impede alteração de outros perfis
        if (!loggedUser.getId().equals(id)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return clientRepository.findById(id)
                .map(existingClient -> {
                    // 1. Determina o Nome (Novo ou mantém o antigo)
                    String newName = request.name() != null ? request.name() : existingClient.getName();

                    // 2. Determina o Telefone (Novo convertido para VO ou mantém o antigo)
                    ClientPhone newPhone = existingClient.getPhoneNumber();
                    if (request.phone() != null) {
                        newPhone = new ClientPhone(request.phone());
                    }

                    // 3. Aplica a atualização usando o método de domínio seguro
                    // Isso resolve os erros de 'setName' e 'setPhone' que não existiam
                    existingClient.updateContact(newName, newPhone);

                    return ResponseEntity.ok(clientRepository.save(existingClient));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Histórico de Agendamentos", description = "Retorna o histórico paginado de serviços realizados.")
    @GetMapping("/history")
    public ResponseEntity<GetClientHistoryUseCase.ClientHistoryResponse> getHistory(
            @AuthenticationPrincipal User user,
            @Parameter(description = "Número da página (0..N)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Itens por página") @RequestParam(defaultValue = "10") int size) {

        var historyResponse = getClientHistoryUseCase.execute(user.getId(), page, size);
        return ResponseEntity.ok(historyResponse);
    }
}