package com.stylo.api_agendamento.adapters.inbound.rest.controllers;

import com.stylo.api_agendamento.adapters.inbound.rest.dto.auth.AuthenticationRequest;
import com.stylo.api_agendamento.adapters.inbound.rest.dto.auth.RegisterClientRequest;
import com.stylo.api_agendamento.adapters.outbound.security.TokenService;
import com.stylo.api_agendamento.core.domain.User;
import com.stylo.api_agendamento.core.domain.UserRole;
import com.stylo.api_agendamento.core.ports.IUserRepository;
import com.stylo.api_agendamento.core.exceptions.BusinessException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final IUserRepository userRepository;
    private final TokenService tokenService; // Injeção necessária para resolver o erro
    private final PasswordEncoder passwordEncoder; // Injeção necessária para resolver o erro

    @PostMapping("/register/client")
    public ResponseEntity<User> register(@RequestBody @Valid RegisterClientRequest request) {
        // Criptografar a senha antes de salvar no banco
        String encryptedPassword = passwordEncoder.encode(request.password());
        
        User newUser = User.create(
            request.name(), 
            request.email(), 
            UserRole.CLIENT
        );
        
        // Adiciona a senha criptografada ao domínio
        User.withPassword(newUser, encryptedPassword);
        
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(userRepository.save(newUser));
    }

    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody @Valid AuthenticationRequest request) {
        // 1. Busca o usuário pelo e-mail
        var user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new BusinessException("Usuário ou senha inválidos."));

        // 2. Valida a senha usando o PasswordEncoder
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BusinessException("Usuário ou senha inválidos.");
        }

        // 3. Gera o Token JWT
        String token = tokenService.generateToken(user);

        return ResponseEntity.ok(token);
    }
}