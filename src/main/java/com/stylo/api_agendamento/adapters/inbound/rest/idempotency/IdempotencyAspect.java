package com.stylo.api_agendamento.adapters.inbound.rest.idempotency;

import com.stylo.api_agendamento.core.exceptions.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.redisson.api.RBucket;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Duration; // ✨ NOVA IMPORTAÇÃO
import java.util.concurrent.TimeUnit;

@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class IdempotencyAspect {

    private final RedissonClient redissonClient;
    private static final String HEADER_NAME = "Idempotency-Key";
    private static final String CACHE_PREFIX = "idempotency:response:";
    private static final String LOCK_PREFIX = "idempotency:lock:";

    @Around("@annotation(idempotent)")
    public Object handleIdempotency(ProceedingJoinPoint joinPoint, Idempotent idempotent) throws Throwable {
        
        // 1. Captura o Request atual para pegar o Header
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) return joinPoint.proceed();
        
        HttpServletRequest request = attributes.getRequest();
        String idempotencyKey = request.getHeader(HEADER_NAME);

        // Se não mandar o header, processa normalmente (ou lança erro se for obrigatório)
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return joinPoint.proceed();
        }

        String cacheKey = CACHE_PREFIX + idempotencyKey;
        String lockKey = LOCK_PREFIX + idempotencyKey;

        RLock lock = redissonClient.getLock(lockKey);
        
        // 2. Proteção de Concorrência (Lock Distribuído)
        // Se 2 requests iguais chegarem no mesmo milissegundo, um espera o outro.
        try {
            boolean isLocked = lock.tryLock(5, 10, TimeUnit.SECONDS);
            if (!isLocked) {
                throw new BusinessException("Requisição duplicada em processamento. Aguarde.");
            }

            // 3. Verifica se já processamos essa chave antes (Check Cache)
            RBucket<IdempotencyCache> bucket = redissonClient.getBucket(cacheKey);
            IdempotencyCache cachedResponse = bucket.get();

            if (cachedResponse != null) {
                log.info("🔁 Retornando resposta em cache para Idempotency-Key: {}", idempotencyKey);
                return ResponseEntity.status(cachedResponse.getStatus()).body(cachedResponse.getBody());
            }

            // 4. Executa o método real do Controller (Processamento)
            Object result = joinPoint.proceed();

            // 5. Salva o resultado no Redis (Save Cache)
            if (result instanceof ResponseEntity<?> responseEntity) {
                // Só salvamos se for sucesso (2xx). Erros 400/500 geralmente podem ser tentados de novo.
                if (responseEntity.getStatusCode().is2xxSuccessful()) {
                    IdempotencyCache cacheValue = new IdempotencyCache(
                            responseEntity.getStatusCode().value(),
                            responseEntity.getBody()
                    );
                    
                    // ✨ CORREÇÃO: Usando a nova API do Redisson com java.time.Duration
                    Duration ttlDuration = Duration.ofMillis(idempotent.unit().toMillis(idempotent.ttl()));
                    bucket.set(cacheValue, ttlDuration);
                    
                    log.info("✅ Resposta salva com Idempotency-Key: {}", idempotencyKey);
                }
            }

            return result;

        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}