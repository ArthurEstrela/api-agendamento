package com.stylo.api_agendamento.adapters.inbound.rest.controllers;

import com.stylo.api_agendamento.core.usecases.UpdateFcmTokenUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UpdateFcmTokenUseCase updateFcmTokenUseCase;

    @PatchMapping("/fcm-token")
    public ResponseEntity<Void> updateFcmToken(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody String token) {
        
        // Assume-se que o teu UserDetails devolve o ID ou que podes buscá-lo pelo email
        // Aqui usamos o ID do utilizador autenticado para segurança
        String userEmail = userDetails.getUsername();
        
        // Nota: Podes precisar de um método findByEmail no repository para pegar o ID real
        // Mas para simplificar, vamos assumir que o input recebe o token direto:
        updateFcmTokenUseCase.execute(new com.stylo.api_agendamento.core.usecases.dto.UpdateFcmTokenInput(userEmail, token));
        
        return ResponseEntity.noContent().build();
    }
}