package com.stylo.api_agendamento.adapters.inbound.rest.controllers;

import com.stylo.api_agendamento.adapters.inbound.rest.dto.auth.ForgotPasswordRequest;
import com.stylo.api_agendamento.adapters.inbound.rest.dto.auth.RegisterClientRequest;
import com.stylo.api_agendamento.adapters.inbound.rest.dto.auth.ResetPasswordRequest;
import com.stylo.api_agendamento.core.domain.User;
import com.stylo.api_agendamento.core.domain.UserRole;
import com.stylo.api_agendamento.core.exceptions.BusinessException;
import com.stylo.api_agendamento.core.ports.IUserRepository;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticação", description = "Endpoints públicos para registro e sincronização de usuários via Firebase")
public class AuthController {

    private final IUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    
    private final RequestPasswordResetUseCase requestPasswordResetUseCase;
    private final ResetPasswordUseCase resetPasswordUseCase;

    @Operation(summary = "Registrar Cliente", description = "Cria uma nova conta no banco de dados após o usuário ser criado no Firebase.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Cliente registrado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos ou e-mail já em uso")
    })
    @PostMapping("/register/client")
    public ResponseEntity<Map<String, Object>> register(@RequestBody @Valid RegisterClientRequest request) {
        
        if (userRepository.findByEmail(request.email()).isPresent()) {
            throw new BusinessException("Este e-mail já está cadastrado em nosso sistema.");
        }

        // Mantemos a criptografia local caso você ainda queira ter a senha salva no PostgreSQL
        // (embora o Firebase também vá gerenciar essa senha no front-end)
        String encryptedPassword = passwordEncoder.encode(request.password());
        
        User newUser = User.create(
            request.name(), 
            request.email(), 
            request.phoneNumber(), 
            UserRole.CLIENT
        );

        newUser.changePassword(encryptedPassword);
        
        User savedUser = userRepository.save(newUser);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
            "id", savedUser.getId(),
            "name", savedUser.getName(),
            "email", savedUser.getEmail(),
            "role", savedUser.getRole()
        ));
    }

    @Operation(summary = "Obter dados do usuário (Substitui o Login antigo)", description = "Retorna os dados do banco após o front-end enviar o token do Firebase.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Retorna os dados do usuário sincronizado."),
            @ApiResponse(responseCode = "401", description = "Token ausente ou inválido.")
    })
    @GetMapping("/me") // Mudamos de POST /login para GET /me
    public ResponseEntity<Map<String, Object>> getMe() {
        // Como o SecurityFilter já validou o token do Firebase que veio no cabeçalho,
        // o usuário já estará logado no contexto do Spring!
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || "anonymousUser".equals(authentication.getPrincipal())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        User user = (User) authentication.getPrincipal();

        // Retornamos os dados do banco para o React preencher o Dashboard
        return ResponseEntity.ok(Map.of(
            "id", user.getId(),
            "name", user.getName(),
            "email", user.getEmail(),
            "role", user.getRole()
        ));
    }

    @Operation(summary = "Esqueci minha senha", description = "Inicia o fluxo de recuperação enviando um e-mail com o link de reset.")
    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(@RequestBody @Valid ForgotPasswordRequest request) {
        // DICA: No futuro, você pode remover isso e usar a função nativa do Firebase no React: sendPasswordResetEmail()
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