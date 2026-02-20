package com.stylo.api_agendamento.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedissonConfig {

    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();
        // Configura para o Redis padrão rodando no Docker ou Localhost
        config.useSingleServer()
              .setAddress("redis://127.0.0.1:6379");
        
        return Redisson.create(config);
    }
}