package com.stylo.api_agendamento.adapters.outbound.storage;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.Bucket;
import com.google.firebase.cloud.StorageClient;
import com.stylo.api_agendamento.core.ports.IStorageProvider;
import org.springframework.stereotype.Component;

import java.io.InputStream;

@Component
public class FirebaseStorageAdapter implements IStorageProvider {

    @Override
    public String uploadFile(String fileName, InputStream content, String contentType) {
        Bucket bucket = StorageClient.getInstance().bucket();
        
        try {
            Blob blob = bucket.create(fileName, content, contentType);
            
            // Opção A: Gerar URL assinada (privada, expira em X dias)
            // return blob.signUrl(7, TimeUnit.DAYS).toString();

            // Opção B: Tornar público (Ideal para fotos de perfil)
            // Nota: Requer configuração de regras no Firebase Console
            // A URL padrão do Firebase Storage é geralmente neste formato:
            // https://firebasestorage.googleapis.com/v0/b/<bucket-name>/o/<file-path>?alt=media
            
            // Hack para obter URL pública sem precisar de Token de Download (se o bucket for público)
            // Ou você pode configurar o bucket para retornar a mediaLink se tiver as permissões certas.
            // Para simplificar, assumindo bucket padrão do GCS:
            return String.format("https://storage.googleapis.com/%s/%s", bucket.getName(), fileName);
            
        } catch (Exception e) {
            throw new RuntimeException("Falha ao enviar imagem para o Firebase", e);
        }
    }

    @Override
    public void deleteFile(String fileName) {
        Bucket bucket = StorageClient.getInstance().bucket();
        try {
             bucket.get(fileName).delete();
        } catch (Exception e) {
            // Logar erro mas não quebrar a aplicação se a imagem antiga não existir
            System.err.println("Erro ao deletar imagem antiga: " + e.getMessage());
        }
    }
}