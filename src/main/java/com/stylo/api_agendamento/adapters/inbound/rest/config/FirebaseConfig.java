package com.stylo.api_agendamento.adapters.inbound.rest.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.io.InputStream;

@Configuration
public class FirebaseConfig {

    // Lê o nome do bucket que vamos definir no ficheiro application.properties
    @Value("${firebase.storage.bucket}")
    private String storageBucket;

    @PostConstruct
    public void initFirebase() {
        try {
            if (FirebaseApp.getApps().isEmpty()) {
                InputStream serviceAccount = getClass().getResourceAsStream("/firebase-service-account.json");

                // Usamos o Builder para ir adicionando as configurações aos poucos
                FirebaseOptions.Builder optionsBuilder = FirebaseOptions.builder();

                // 1. Configura as Credenciais
                if (serviceAccount != null) {
                    optionsBuilder.setCredentials(GoogleCredentials.fromStream(serviceAccount));
                } else {
                    // Tenta o default (útil para produção no GCP), mas se falhar, vai cair no catch
                    optionsBuilder.setCredentials(GoogleCredentials.getApplicationDefault());
                }

                // 2. ✨ CORREÇÃO: Configura o Bucket do Storage
                if (storageBucket != null && !storageBucket.isBlank()) {
                    optionsBuilder.setStorageBucket(storageBucket);
                } else {
                    throw new IllegalArgumentException(
                            "O bucket do Firebase Storage não está configurado. Defina 'firebase.storage.bucket' no application.properties.");
                }

                // 3. Inicializa o Firebase
                FirebaseApp.initializeApp(optionsBuilder.build());
                System.out.println("🔥 Firebase Admin inicializado com sucesso (com Storage Bucket configurado)!");
            }
        } catch (Exception e) {
            // ✨ Em vez de só imprimir, nós derrubamos a aplicação com um erro claro
            // Se o Firebase não ligar, a API de agendamento não pode funcionar.
            throw new IllegalStateException(
                    "ERRO CRÍTICO: Não foi possível inicializar o Firebase Admin. Verifique as credenciais e o nome do bucket.",
                    e);
        }
    }
}