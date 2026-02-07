package com.stylo.api_agendamento.adapters.outbound.persistence.review;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.UUID;

public interface JpaReviewRepository extends JpaRepository<ReviewEntity, UUID> {
    
    List<ReviewEntity> findAllByProfessionalId(UUID professionalId);
    
    List<ReviewEntity> findAllByServiceProviderId(UUID serviceProviderId);
    
    boolean existsByAppointmentId(UUID appointmentId);

    @Query("SELECT AVG(r.rating) FROM ReviewEntity r WHERE r.professionalId = :profId")
    Double getAverageRatingByProfessionalId(@Param("profId") UUID professionalId);
}