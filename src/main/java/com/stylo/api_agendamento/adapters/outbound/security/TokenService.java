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

    /**
     * Valida o token gerado pelo Front-end (Firebase Auth)
     * 
     * @param token O ID Token (Bearer) enviado na requisição.
     * @return O e-mail do usuário caso o token seja válido, ou null se for
     *         inválido/expirado.
     */
    public String validateToken(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }

        try {
            // O Firebase Admin verifica a assinatura do token e confere se ele não expirou.
            FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(token);

            // Retornamos o e-mail associado ao token para buscar no banco local
            return decodedToken.getEmail();

        } catch (FirebaseAuthException e) {
            logger.error("Erro na validação do token Firebase: {}", e.getMessage());
            return null;
        } catch (IllegalArgumentException e) {
            logger.error("Token Firebase malformado: {}", e.getMessage());
            return null;
        }
    }
}