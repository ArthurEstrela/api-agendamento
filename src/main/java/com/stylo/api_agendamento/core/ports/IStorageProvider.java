package com.stylo.api_agendamento.core.ports;

import java.io.InputStream;

public interface IStorageProvider {
    
    /**
     * Faz upload de um arquivo para nuvem (S3/Firebase).
     * * @param path Caminho/Nome do arquivo (ex: "users/{uuid}/avatar.jpg")
     * @param content Stream de dados
     * @param contentType MIME Type (ex: "image/jpeg")
     * @param contentLength Tamanho do arquivo em bytes (Essencial para performance de I/O na nuvem)
     * @return URL pública assinada ou direta do arquivo.
     */
    String uploadFile(String path, InputStream content, String contentType, long contentLength);

    /**
     * Remove o arquivo do bucket.
     */
    void deleteFile(String path);
}