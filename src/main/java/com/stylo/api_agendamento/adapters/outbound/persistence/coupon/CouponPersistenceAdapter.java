package com.stylo.api_agendamento.adapters.outbound.persistence.coupon;

import com.stylo.api_agendamento.core.domain.coupon.Coupon;
import com.stylo.api_agendamento.core.ports.ICouponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class CouponPersistenceAdapter implements ICouponRepository {

    private final JpaCouponRepository jpaCouponRepository;
    private final CouponMapper mapper;

    @Override
    public Coupon save(Coupon coupon) {
        var entity = mapper.toEntity(coupon);
        var savedEntity = jpaCouponRepository.save(entity);
        return mapper.toDomain(savedEntity);
    }

    @Override
    public Optional<Coupon> findById(String id) {
        return jpaCouponRepository.findById(id)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<Coupon> findByCodeAndProvider(String code, String providerId) {
        // Garantimos que o código seja buscado em maiúsculo para evitar erros de digitação (case-insensitive logic)
        // embora a responsabilidade principal seja do UseCase, não custa reforçar ou delegar ao banco.
        return jpaCouponRepository.findByCodeAndProviderId(code, providerId)
                .map(mapper::toDomain);
    }
}