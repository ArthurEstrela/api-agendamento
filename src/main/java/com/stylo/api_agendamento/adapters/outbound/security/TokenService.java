package com.stylo.api_agendamento.adapters.outbound.security;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class TokenService {

    private static final Logger logger = LoggerFactory.getLogger(TokenService.class);

    // ✨ AGORA RETORNA O OBJETO COMPLETO DO FIREBASE
    public FirebaseToken validateToken(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }

        try {
            return FirebaseAuth.getInstance().verifyIdToken(token);
        } catch (FirebaseAuthException e) {
            logger.error("Erro na validação do token Firebase: {}", e.getMessage());
            return null;
        } catch (IllegalArgumentException e) {
            logger.error("Token Firebase malformado: {}", e.getMessage());
            return null;
        }
    }
}