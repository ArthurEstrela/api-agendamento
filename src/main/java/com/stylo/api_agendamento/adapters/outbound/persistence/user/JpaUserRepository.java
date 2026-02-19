package com.stylo.api_agendamento.adapters.outbound.persistence.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaUserRepository extends JpaRepository<UserEntity, UUID> {
    Optional<UserEntity> findByEmail(String email);

    boolean existsByEmail(String email);

    Optional<UserEntity> findByResetPasswordToken(String token);

    // Busca o dono do estabelecimento (Provider)
    Optional<UserEntity> findByServiceProviderId(UUID providerId);

    Optional<UserEntity> findByProviderId(UUID providerId);

    // Limpa tokens FCM de outros usuários para garantir que a notificação vá para a
    // pessoa certa
    @Modifying
    @Query("UPDATE UserEntity u SET u.fcmToken = null WHERE u.fcmToken = :token")
    void clearFcmToken(@Param("token") String token);

    @Query("SELECT u FROM UserEntity u WHERE u.email = (SELECT p.email FROM ProfessionalEntity p WHERE p.id = :professionalId)")
    Optional<UserEntity> findByProfessionalId(@Param("professionalId") UUID professionalId);
}