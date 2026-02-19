package com.stylo.api_agendamento.adapters.outbound.persistence.waitlist;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface JpaWaitlistRepository extends JpaRepository<WaitlistEntity, UUID> {

    // Busca ativa por profissional e data (ordenado por ordem de chegada)
    List<WaitlistEntity> findAllByProfessionalIdAndDesiredDateAndNotifiedFalseOrderByRequestTimeAsc(UUID professionalId, LocalDate desiredDate);

    // Busca toda a fila de um estabelecimento (SaaS/Multitenancy)
    List<WaitlistEntity> findAllByServiceProviderIdAndNotifiedFalseOrderByRequestTimeDesc(UUID serviceProviderId);
}