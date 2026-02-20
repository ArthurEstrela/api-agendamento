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
                // Opção 1: Usando arquivo JSON baixado do console do Firebase (Recomendado para local/desenvolvimento)
                // Coloque o arquivo 'firebase-service-account.json' na pasta src/main/resources
                InputStream serviceAccount = getClass().getResourceAsStream("/firebase-service-account.json");
                
                FirebaseOptions options;
                if (serviceAccount != null) {
                    options = FirebaseOptions.builder()
                            .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                            .build();
                } else {
                    // Opção 2: Application Default Credentials (ideal para deploy no Google Cloud/produção)
                    options = FirebaseOptions.builder()
                            .setCredentials(GoogleCredentials.getApplicationDefault())
                            .build();
                }

                FirebaseApp.initializeApp(options);
                System.out.println("Firebase Admin inicializado com sucesso!");
            }
        } catch (Exception e) {
            System.err.println("Erro ao inicializar o Firebase Admin SDK: " + e.getMessage());
        }
    }
}