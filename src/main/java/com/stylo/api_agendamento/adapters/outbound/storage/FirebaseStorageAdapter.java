package com.stylo.api_agendamento.adapters.outbound.storage;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.Bucket;
import com.google.firebase.cloud.StorageClient;
import com.stylo.api_agendamento.core.ports.IStorageProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
public class FirebaseStorageAdapter implements IStorageProvider {

    @Override
    public String uploadFile(String fileName, InputStream content, String contentType, long size) {
        Bucket bucket = StorageClient.getInstance().bucket();
        
        try {
            // ✨ MELHORIA: Criamos o blob com metadados de tipo e tamanho
            // O uso de InputStream com o tamanho definido é mais eficiente para o Google Cloud Storage
            Blob blob = bucket.create(fileName, content, contentType);
            
            log.info("Arquivo enviado com sucesso para o Firebase: {} ({} bytes)", fileName, size);

            // ✨ RESOLUÇÃO DA URL: Formato compatível com Firebase Storage para visualização imediata
            // Usamos o formato firebasestorage.googleapis.com que é o padrão para o SDK Web/Mobile
            String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8);
            
            return String.format(
                "https://firebasestorage.googleapis.com/v0/b/%s/o/%s?alt=media",
                bucket.getName(),
                encodedFileName
            );
            
        } catch (Exception e) {
            log.error("Erro ao realizar upload para o Firebase: {}", e.getMessage());
            throw new RuntimeException("Falha ao enviar imagem para o armazenamento em nuvem", e);
        }
    }

    @Override
    public void deleteFile(String fileName) {
        if (fileName == null || fileName.isBlank()) return;

        try {
            Bucket bucket = StorageClient.getInstance().bucket();
            // Extrai o caminho do arquivo caso venha a URL completa
            String path = extractPathFromUrl(fileName);
            
            Blob blob = bucket.get(path);
            if (blob != null) {
                blob.delete();
                log.info("Arquivo removido do storage: {}", path);
            }
        } catch (Exception e) {
            // Em deleções, apenas logamos para não interromper fluxos de negócio
            log.warn("Não foi possível remover o arquivo antigo: {}", e.getMessage());
        }
    }

    private String extractPathFromUrl(String url) {
        if (!url.contains("/o/")) return url;
        try {
            String path = url.split("/o/")[1].split("\\?")[0];
            return java.net.URLDecoder.decode(path, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return url;
        }
    }
}