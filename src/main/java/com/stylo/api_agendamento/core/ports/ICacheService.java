package com.stylo.api_agendamento.core.ports;

public interface ICacheService {
    void set(String key, Object value, long ttlMinutes);
    Object get(String key);
    void evict(String key);
}