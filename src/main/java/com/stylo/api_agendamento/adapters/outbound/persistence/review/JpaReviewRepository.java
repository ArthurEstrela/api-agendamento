package com.stylo.api_agendamento.adapters.outbound.persistence.review;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface JpaReviewRepository extends JpaRepository<ReviewEntity, UUID> {
    
    // Busca paginada por profissional
    Page<ReviewEntity> findAllByProfessionalId(UUID professionalId, Pageable pageable);
    
    // Busca paginada por estabelecimento
    Page<ReviewEntity> findAllByServiceProviderId(UUID serviceProviderId, Pageable pageable);
    
    boolean existsByAppointmentId(UUID appointmentId);

    @Query("SELECT AVG(r.rating) FROM ReviewEntity r WHERE r.professionalId = :profId")
    Double getAverageRatingByProfessionalId(@Param("profId") UUID professionalId);
}