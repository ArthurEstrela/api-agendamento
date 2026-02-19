package com.stylo.api_agendamento.adapters.outbound.persistence.coupon;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaCouponRepository extends JpaRepository<CouponEntity, UUID> {
    
    // ✨ Corrigido: Agora recebe UUID no providerId
    Optional<CouponEntity> findByCodeIgnoreCaseAndProviderId(String code, UUID providerId);

    // ✨ Corrigido: Agora recebe UUID no providerId
    Page<CouponEntity> findAllByProviderIdAndActiveTrue(UUID providerId, Pageable pageable);
}