package com.stylo.api_agendamento.adapters.inbound.rest.controllers;

import com.stylo.api_agendamento.adapters.inbound.rest.dto.auth.ForgotPasswordRequest;
import com.stylo.api_agendamento.adapters.inbound.rest.dto.auth.RegisterClientRequest;
import com.stylo.api_agendamento.adapters.inbound.rest.dto.auth.ResetPasswordRequest;
import com.stylo.api_agendamento.core.domain.User;
import com.stylo.api_agendamento.core.usecases.RegisterClientUseCase;
import com.stylo.api_agendamento.core.usecases.RequestPasswordResetUseCase;
import com.stylo.api_agendamento.core.usecases.ResetPasswordUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticação", description = "Endpoints públicos para registro e sincronização de usuários via Firebase")
public class AuthController {

    private final RegisterClientUseCase registerClientUseCase;
    private final RequestPasswordResetUseCase requestPasswordResetUseCase;
    private final ResetPasswordUseCase resetPasswordUseCase;

    @Operation(summary = "Registrar Cliente", description = "Cria a conta do usuário e o perfil de cliente no banco de dados.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Cliente registrado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos ou e-mail já em uso")
    })
    @PostMapping("/register/client")
    public ResponseEntity<Map<String, Object>> register(@RequestBody @Valid RegisterClientRequest request) {

        // ✨ O UseCase faz todo o trabalho pesado
        User savedUser = registerClientUseCase.execute(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "id", savedUser.getId(),
                "name", savedUser.getName(),
                "email", savedUser.getEmail(),
                "role", savedUser.getRole(),
                "clientId", savedUser.getClientId() // ✨ Retornando o ID do cliente para o Front-end
        ));
    }

    @Operation(summary = "Obter dados do usuário", description = "Retorna os dados do banco após o front-end enviar o token do Firebase.")
    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getMe() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();

        // Se o usuário não existe no banco, retorna 404
        if (authentication == null || "anonymousUser".equals(authentication.getPrincipal())) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        User user = (User) authentication.getPrincipal();

        // ✨ SOLUÇÃO: Usar HashMap clássico no lugar de Map.of()
        // O HashMap permite valores nulos sem quebrar a aplicação.
        java.util.Map<String, Object> response = new java.util.HashMap<>();
        
        // Dados Base
        response.put("id", user.getId());
        response.put("name", user.getName());
        response.put("email", user.getEmail());
        response.put("role", user.getRole());
        
        // ✨ Vínculos (Multi-tenant) - O React precisa MUITO disso para gerenciar rotas
        response.put("clientId", user.getClientId()); 
        response.put("providerId", user.getProviderId());
        response.put("professionalId", user.getProfessionalId());

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Esqueci minha senha", description = "Inicia o fluxo de recuperação enviando um e-mail com o link de reset.")
    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(@RequestBody @Valid ForgotPasswordRequest request) {
        // DICA: No futuro, você pode remover isso e usar a função nativa do Firebase no
        // React: sendPasswordResetEmail()
        requestPasswordResetUseCase.execute(request.email());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Redefinir senha", description = "Define uma nova senha validando o token recebido no e-mail.")
    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@RequestBody @Valid ResetPasswordRequest request) {
        resetPasswordUseCase.execute(request.token(), request.newPassword());
        return ResponseEntity.noContent().build();
    }
}