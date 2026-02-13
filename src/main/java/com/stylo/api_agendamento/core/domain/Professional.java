package com.stylo.api_agendamento.core.domain;

import com.stylo.api_agendamento.core.domain.vo.DailyAvailability;
import com.stylo.api_agendamento.core.exceptions.BusinessException;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Collections;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Professional {
    private final String id;
    private final String serviceProviderId;
    private final String serviceProviderName;
    private String name;
    private final String email;
    private String avatarUrl;
    private String bio;

    // ✨ Apenas estes dois campos controlam todo o financeiro agora
    private RemunerationType remunerationType; 
    private BigDecimal remunerationValue; 

    private final List<Service> services;
    private List<DailyAvailability> availability;

    private final Integer slotInterval;
    private boolean isOwner;

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
                .slotInterval(30)
                .isOwner(false)
                // Padrão: Sem comissão definida inicialmente
                .remunerationType(null)
                .remunerationValue(BigDecimal.ZERO)
                .build();
    }

    // --- REGRAS DE NEGÓCIO ---

    public boolean isAvailable(LocalDateTime dateTime, int totalDuration) {
        return availability.stream()
                .filter(a -> a.dayOfWeek() == dateTime.getDayOfWeek())
                .findFirst()
                .map(a -> a.contains(dateTime.toLocalTime(), totalDuration))
                .orElse(false);
    }

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

    public void updateAvailability(List<DailyAvailability> newAvailability) {
        if (newAvailability == null || newAvailability.isEmpty()) {
            throw new BusinessException("O profissional deve ter pelo menos um dia de disponibilidade configurado.");
        }

        for (DailyAvailability daily : newAvailability) {
            if (daily.isOpen() && !daily.startTime().isBefore(daily.endTime())) {
                throw new BusinessException(
                        "O horário de início deve ser anterior ao horário de término para o dia: " + daily.dayOfWeek());
            }
        }
        this.availability = newAvailability;
    }

    // ✨ Método unificado para atualizar a comissão (Strategy)
    public void updateCommissionSettings(RemunerationType type, BigDecimal value) {
        if (value == null || value.compareTo(BigDecimal.ZERO) < 0) {
             throw new BusinessException("O valor da remuneração não pode ser negativo.");
        }
        
        // Validação específica se for porcentagem
        if (type == RemunerationType.PERCENTAGE && value.compareTo(new BigDecimal("100")) > 0) {
             throw new BusinessException("A porcentagem não pode ser maior que 100.");
        }

        this.remunerationType = type;
        this.remunerationValue = value;
    }

    // ✨ O cálculo usa a estratégia definida no Enum
    public BigDecimal calculateCommissionFor(BigDecimal finalPrice) {
        if (this.remunerationType == null || this.remunerationValue == null) {
            return BigDecimal.ZERO;
        }
        return this.remunerationType.calculate(finalPrice, this.remunerationValue);
    }
}