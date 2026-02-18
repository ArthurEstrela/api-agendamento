package com.stylo.api_agendamento.adapters.outbound.persistence.financial;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface JpaCashRegisterRepository extends JpaRepository<CashRegisterEntity, String> {

    @Query("SELECT c FROM CashRegisterEntity c WHERE c.providerId = :providerId AND c.isOpen = true")
    Optional<CashRegisterEntity> findOpenByProviderId(@Param("providerId") String providerId);
}