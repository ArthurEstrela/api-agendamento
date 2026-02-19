package com.stylo.api_agendamento.adapters.outbound.persistence.coupon;

import com.stylo.api_agendamento.core.common.PagedResult;
import com.stylo.api_agendamento.core.domain.coupon.Coupon;
import com.stylo.api_agendamento.core.ports.ICouponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

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
    public Optional<Coupon> findById(UUID id) {
        // Agora repassa o UUID diretamente, respeitando a tipagem do JpaRepository
        return jpaCouponRepository.findById(id)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<Coupon> findByCodeAndProviderId(String code, UUID providerId) {
        // Passando o UUID diretamente para o banco
        return jpaCouponRepository.findByCodeIgnoreCaseAndProviderId(code, providerId)
                .map(mapper::toDomain);
    }

    @Override
    public PagedResult<Coupon> findAllActiveByProviderId(UUID providerId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        // Passando o UUID diretamente
        Page<CouponEntity> entityPage = jpaCouponRepository.findAllByProviderIdAndActiveTrue(
                providerId,
                pageable
        );

        List<Coupon> domainItems = entityPage.getContent().stream()
                .map(mapper::toDomain)
                .toList();

        return new PagedResult<>(
                domainItems,
                entityPage.getNumber(),
                entityPage.getSize(),
                entityPage.getTotalElements(),
                entityPage.getTotalPages()
        );
    }
}