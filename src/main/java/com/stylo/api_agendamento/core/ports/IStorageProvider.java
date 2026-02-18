package com.stylo.api_agendamento.core.ports;

import java.io.InputStream;

public interface IStorageProvider {
    /**
     * Faz upload de um arquivo e retorna a URL pública.
     * @param fileName Nome do arquivo (ex: users/uuid/profile.jpg)
     * @param content Stream do conteúdo do arquivo
     * @param contentType Tipo do arquivo (ex: image/jpeg)
     * @return URL pública do arquivo
     */
    String uploadFile(String fileName, InputStream content, String contentType);

    /**
     * Deleta um arquivo (útil para quando o usuário troca de foto).
     */
    void deleteFile(String fileName);
}