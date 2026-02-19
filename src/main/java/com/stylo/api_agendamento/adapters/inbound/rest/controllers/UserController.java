package com.stylo.api_agendamento.adapters.inbound.rest.controllers;

import com.stylo.api_agendamento.core.domain.User;
import com.stylo.api_agendamento.core.ports.IUserContext;
import com.stylo.api_agendamento.core.ports.IUserRepository;
import com.stylo.api_agendamento.core.usecases.UpdateFcmTokenUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/v1/users")
@RequiredArgsConstructor
@Tag(name = "Utilizadores", description = "Gestão de dados de conta, perfil e notificações push")
public class UserController {

    private final IUserRepository userRepository;
    private final UpdateFcmTokenUseCase updateFcmTokenUseCase;
    private final IUserContext userContext;

    @Operation(summary = "Obter meu perfil", description = "Retorna os dados do utilizador autenticado extraídos do Token JWT.")
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> getMyProfile() {
        User user = userContext.getCurrentUser();

        return ResponseEntity.ok(Map.of(
            "id", user.getId(),
            "name", user.getName(),
            "email", user.getEmail(),
            "role", user.getRole(),
            "providerId", user.getProviderId() != null ? user.getProviderId() : "N/A",
            "profilePicture", user.getProfilePictureUrl() != null ? user.getProfilePictureUrl() : ""
        ));
    }

    @Operation(summary = "Obter utilizador por ID (Staff)", description = "Permite que administradores consultem dados de utilizadores específicos.")
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SERVICE_PROVIDER')")
    public ResponseEntity<User> getById(@PathVariable UUID id) {
        return userRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Atualizar Token de Notificação (FCM)", description = "Regista o token do dispositivo para envio de notificações Push.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Token atualizado com sucesso"),
            @ApiResponse(responseCode = "401", description = "Acesso negado")
    })
    @PatchMapping("/fcm-token")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> updateFcmToken(@RequestBody @Valid FcmTokenRequest request) {
        // ID extraído do contexto de segurança
        UUID userId = userContext.getCurrentUserId(); 

        // ✨ CORREÇÃO: Utilizando o record 'Input' interno do UseCase
        var input = new UpdateFcmTokenUseCase.Input(userId, request.token());
        
        updateFcmTokenUseCase.execute(input);
        
        return ResponseEntity.ok().build();
    }

    // --- DTOs Internos ---

    public record FcmTokenRequest(
            @NotBlank(message = "O token do dispositivo é obrigatório") 
            String token
    ) {}
}