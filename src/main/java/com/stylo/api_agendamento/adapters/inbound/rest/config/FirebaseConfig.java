package com.stylo.api_agendamento.adapters.inbound.rest.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;

import java.io.InputStream;

@Configuration
public class FirebaseConfig {

    @PostConstruct
    public void initFirebase() {
        try {
            if (FirebaseApp.getApps().isEmpty()) {
                InputStream serviceAccount = getClass().getResourceAsStream("/firebase-service-account.json");
                
                FirebaseOptions options;
                if (serviceAccount != null) {
                    options = FirebaseOptions.builder()
                            .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                            .build();
                } else {
                    // Tenta o default (útil para produção no GCP), mas se falhar, vai cair no catch
                    options = FirebaseOptions.builder()
                            .setCredentials(GoogleCredentials.getApplicationDefault())
                            .build();
                }

                FirebaseApp.initializeApp(options);
                System.out.println("🔥 Firebase Admin inicializado com sucesso!");
            }
        } catch (Exception e) {
            // ✨ CORREÇÃO: Em vez de só imprimir, nós derrubamos a aplicação com um erro claro
            // Se o Firebase não ligar, a API de agendamento não pode funcionar.
            throw new IllegalStateException("ERRO CRÍTICO: Não foi possível inicializar o Firebase Admin. Verifique se o arquivo firebase-service-account.json está na pasta resources.", e);
        }
    }
}