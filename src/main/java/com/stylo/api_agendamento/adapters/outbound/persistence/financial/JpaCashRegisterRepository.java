package com.stylo.api_agendamento.adapters.outbound.persistence.financial;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaCashRegisterRepository extends JpaRepository<CashRegisterEntity, UUID> {
    
    Optional<CashRegisterEntity> findByProviderIdAndIsOpenTrue(UUID providerId);

    // ✨ NOVO MÉTODO: O Spring Data JPA traduz isso nativamente para SQL
    List<CashRegisterEntity> findByProviderIdAndIsOpenFalseAndCloseTimeBetween(
            UUID providerId, 
            LocalDateTime start, 
            LocalDateTime end
    );
}