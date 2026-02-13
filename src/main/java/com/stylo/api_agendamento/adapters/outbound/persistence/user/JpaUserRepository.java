package com.stylo.api_agendamento.adapters.outbound.persistence.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaUserRepository extends JpaRepository<UserEntity, UUID> {
    Optional<UserEntity> findByEmail(String email);

    Optional<UserEntity> findByResetPasswordToken(String token);

    // No JpaUserRepository.java
    @Query(value = "SELECT * FROM users WHERE professional_id = :profId", nativeQuery = true)
    Optional<UserEntity> findByProfessionalId(@Param("profId") String professionalId);
}