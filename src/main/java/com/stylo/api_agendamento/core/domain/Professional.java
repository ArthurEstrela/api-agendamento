package com.stylo.api_agendamento.core.domain;

import com.stylo.api_agendamento.core.domain.vo.DailyAvailability;
import com.stylo.api_agendamento.core.exceptions.BusinessException;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Collections;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Professional {
    private final String id;
    private final String serviceProviderId; // Multi-tenant: Sempre vinculado a um salão
    private String name;
    private final String email;
    private String avatarUrl;
    private String bio;

    private final List<Service> services; // Serviços que ele está habilitado a fazer
    private List<DailyAvailability> availability; // Grade de horários semanal

    private final Integer slotInterval; // Ex: Atende de 30 em 30 min
    private boolean isOwner;

    // Fábrica para criar um profissional com segurança
    public static Professional create(String name, String email, String providerId,
            List<Service> services, List<DailyAvailability> availability) {
        if (services == null || services.isEmpty()) {
            throw new BusinessException("Um profissional deve estar habilitado em pelo menos um serviço.");
        }

        return Professional.builder()
                .name(name)
                .email(email)
                .serviceProviderId(providerId)
                .services(Collections.unmodifiableList(services))
                .availability(availability)
                .slotInterval(30) // Default
                .isOwner(false)
                .build();
    }

    // --- REGRAS DE NEGÓCIO ---

    /**
     * Valida se o profissional trabalha no dia e hora solicitados
     */
    public boolean isAvailable(LocalDateTime dateTime, int totalDuration) {
        return availability.stream()
                .filter(a -> a.dayOfWeek() == dateTime.getDayOfWeek())
                .findFirst()
                .map(a -> a.contains(dateTime.toLocalTime(), totalDuration))
                .orElse(false);
    }

    /**
     * Verifica se o profissional é capaz de realizar a lista de serviços solicitada
     */
    public void validateCanPerform(List<Service> requestedServices) {
        boolean allSupported = requestedServices.stream()
                .allMatch(rs -> this.services.stream()
                        .anyMatch(ps -> ps.getId().equals(rs.getId())));

        if (!allSupported) {
            throw new BusinessException("Este profissional não realiza um ou mais dos serviços selecionados.");
        }
    }

    public void updateBio(String newBio) {
        if (newBio != null && newBio.length() > 500) {
            throw new BusinessException("A bio não pode exceder 500 caracteres.");
        }
        this.bio = newBio;
    }

    public void validateCanBlockTime(LocalDateTime start, LocalDateTime end, List<Appointment> existing) {
        boolean hasConflict = existing.stream()
                .anyMatch(app -> (app.getStatus() == AppointmentStatus.SCHEDULED
                        || app.getStatus() == AppointmentStatus.PENDING) &&
                        app.getStartTime().isBefore(end) &&
                        app.getEndTime().isAfter(start));

        if (hasConflict) {
            throw new BusinessException("Não é possível bloquear: você já tem um cliente agendado nesse horário.");
        }
    }

    // No seu Professional.java
    public void updateAvailability(List<DailyAvailability> newAvailability) {
        if (newAvailability == null || newAvailability.isEmpty()) {
            throw new BusinessException("O profissional deve ter pelo menos um dia de disponibilidade configurado.");
        }

        // Validação lógica: startTime sempre antes de endTime
        for (DailyAvailability daily : newAvailability) {
            if (daily.isOpen() && !daily.startTime().isBefore(daily.endTime())) {
                throw new BusinessException(
                        "O horário de início deve ser anterior ao horário de término para o dia: " + daily.dayOfWeek());
            }
        }

        this.availability = newAvailability;
    }
}