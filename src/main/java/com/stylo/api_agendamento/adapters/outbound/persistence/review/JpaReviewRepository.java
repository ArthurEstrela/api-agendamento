package com.stylo.api_agendamento.adapters.outbound.persistence.review;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface JpaReviewRepository extends JpaRepository<ReviewEntity, UUID> {
    
    // Busca todas as avaliações de um profissional específico
    List<ReviewEntity> findAllByProfessionalId(UUID professionalId);
    
    // Busca todas as avaliações de um salão/prestador
    List<ReviewEntity> findAllByServiceProviderId(UUID serviceProviderId);
    
    // Verifica se um agendamento já foi avaliado para evitar duplicidade
    boolean existsByAppointmentId(UUID appointmentId);
}