package com.stylo.api_agendamento.core.ports;

import java.util.Optional;

public interface ICacheService {
    
    /**
     * Salva um valor no cache.
     * @param key Chave única
     * @param value Objeto a ser serializado
     * @param ttlMinutes Tempo de vida em minutos
     */
    void set(String key, Object value, long ttlMinutes);

    /**
     * Recupera um objeto tipado do cache.
     */
    <T> Optional<T> get(String key, Class<T> clazz);

    /**
     * Remove uma chave do cache.
     */
    void evict(String key);
    
    /**
     * Remove todas as chaves que começam com um prefixo (ex: "appt:prof:123:*").
     */
    void evictPattern(String pattern);
}