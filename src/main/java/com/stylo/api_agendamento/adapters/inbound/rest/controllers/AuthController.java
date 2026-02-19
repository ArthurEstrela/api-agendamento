package com.stylo.api_agendamento.adapters.inbound.rest.controllers;

import com.stylo.api_agendamento.adapters.inbound.rest.dto.auth.AuthenticationRequest;
import com.stylo.api_agendamento.adapters.inbound.rest.dto.auth.ForgotPasswordRequest;
import com.stylo.api_agendamento.adapters.inbound.rest.dto.auth.RegisterClientRequest;
import com.stylo.api_agendamento.adapters.inbound.rest.dto.auth.ResetPasswordRequest;
import com.stylo.api_agendamento.adapters.outbound.security.TokenService;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticação", description = "Endpoints públicos para login, registro de clientes e recuperação de senha")
public class AuthController {

    private final IUserRepository userRepository;
    private final TokenService tokenService;
    private final PasswordEncoder passwordEncoder;
    
    private final RequestPasswordResetUseCase requestPasswordResetUseCase;
    private final ResetPasswordUseCase resetPasswordUseCase;

    @Operation(summary = "Registrar Cliente", description = "Cria uma nova conta para um cliente final usar o aplicativo.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Cliente registrado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos ou e-mail já em uso")
    })
    @PostMapping("/register/client")
    public ResponseEntity<Map<String, Object>> register(@RequestBody @Valid RegisterClientRequest request) {
        
        // 1. Validação Antecipada: Evita erro 500 de Constraint do Banco de Dados
        if (userRepository.findByEmail(request.email()).isPresent()) {
            throw new BusinessException("Este e-mail já está cadastrado em nosso sistema.");
        }

        // 2. Criptografar a senha antes de salvar no banco
        String encryptedPassword = passwordEncoder.encode(request.password());
        
        // 3. Criação da entidade de Domínio
        // ✨ CORREÇÃO: Usando a assinatura exata do seu Factory Method (4 parâmetros)
        User newUser = User.create(
            request.name(), 
            request.email(), 
            request.phoneNumber(), // Injetando o telefone direto na criação
            UserRole.CLIENT
        );

        // ✨ CORREÇÃO: Utilizando o método de negócio correto para a senha
        newUser.changePassword(encryptedPassword);
        
        // 4. Persistência
        User savedUser = userRepository.save(newUser);
        
        // Retorna um DTO/Map limpo (Segurança: Sem devolver a Hash)
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
            "id", savedUser.getId(),
            "name", savedUser.getName(),
            "email", savedUser.getEmail(),
            "role", savedUser.getRole()
        ));
    }

    @Operation(summary = "Login", description = "Autentica um usuário (Cliente ou Staff) e retorna o Token JWT.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login bem-sucedido. Retorna o Token JWT."),
            @ApiResponse(responseCode = "400", description = "Credenciais inválidas")
    })
    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@RequestBody @Valid AuthenticationRequest request) {
        // 1. Busca o usuário pelo e-mail
        var user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new BusinessException("Usuário ou senha inválidos."));

        // 2. Valida a senha usando o PasswordEncoder
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BusinessException("Usuário ou senha inválidos.");
        }

        // 3. Gera o Token JWT
        String token = tokenService.generateToken(user);

        // Retornando como JSON (Map) para o front-end fazer o parse corretamente
        return ResponseEntity.ok(Map.of("token", token));
    }

    @Operation(summary = "Esqueci minha senha", description = "Inicia o fluxo de recuperação enviando um e-mail com o link de reset.")
    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(@RequestBody @Valid ForgotPasswordRequest request) {
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