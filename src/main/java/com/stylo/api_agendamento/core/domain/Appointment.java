package com.stylo.api_agendamento.core.domain;

import com.stylo.api_agendamento.core.domain.vo.ClientPhone;
import com.stylo.api_agendamento.core.domain.vo.PaymentMethod;
import com.stylo.api_agendamento.core.exceptions.BusinessException;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Appointment {
    private final String id;
    private final String clientId;
    private final String clientName;
    private final String clientEmail; // Adicionado para notificações
    private final String businessName; // Adicionado para o corpo do e-mail
    private final ClientPhone clientPhone;
    private final String providerId;
    private final String professionalId;
    private final String professionalName;
    private final List<Service> services;

    private LocalDateTime startTime;
    private LocalDateTime endTime; // Essencial para validação de conflitos
    private BigDecimal professionalCommission; // Valor em R$ para o profissional
    private BigDecimal serviceProviderFee; // Valor em R$ para o salão
    private AppointmentStatus status;
    private PaymentMethod paymentMethod;

    private final BigDecimal totalPrice;
    private BigDecimal finalPrice;
    private String notes;
    private final LocalDateTime createdAt;
    private LocalDateTime completedAt;
    private final Integer reminderMinutes;
    private boolean reminderSent;

    @Setter 
    private String externalEventId;

    private boolean notified;
    private final boolean isPersonalBlock;

    // Fábrica para agendamentos via APP (Pelo Cliente)
    public static Appointment create(String clientId, String clientName, String clientEmail,
            String businessName, ClientPhone phone,
            String providerId, String profId, String profName,
            List<Service> services, LocalDateTime start, Integer reminderMinutes) {

        validateServices(services);

        BigDecimal total = services.stream()
                .map(Service::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Appointment appointment = Appointment.builder()
                .clientId(clientId)
                .clientName(clientName)
                .clientEmail(clientEmail)
                .businessName(businessName)
                .clientPhone(phone)
                .providerId(providerId)
                .professionalId(profId)
                .professionalName(profName)
                .services(Collections.unmodifiableList(services))
                .startTime(start)
                .status(AppointmentStatus.PENDING)
                .totalPrice(total)
                .finalPrice(total)
                .reminderMinutes(reminderMinutes != null ? reminderMinutes : 0)
                .reminderSent(false)
                .notified(false) // Garante que nasce sem notificação
                .isPersonalBlock(false)
                .createdAt(LocalDateTime.now())
                .build();

        appointment.calculateEndTime();
        return appointment;
    }

    // Fábrica para agendamentos Manuais (Walk-in / Balcão)
    public static Appointment createManual(String clientName, ClientPhone phone,
            String providerId, String profId, String profName,
            List<Service> services, LocalDateTime start, String notes) {

        validateServices(services);

        BigDecimal total = services.stream()
                .map(Service::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Appointment appointment = Appointment.builder()
                .clientId(null)
                .clientName(clientName)
                .clientPhone(phone)
                .providerId(providerId)
                .professionalId(profId)
                .professionalName(profName)
                .services(Collections.unmodifiableList(services))
                .startTime(start)
                .status(AppointmentStatus.SCHEDULED) // Manual já nasce confirmado
                .totalPrice(total)
                .finalPrice(total)
                .reminderMinutes(0) // Walk-in geralmente não tem lembrete de app
                .notified(false)
                .notes(notes)
                .createdAt(LocalDateTime.now())
                .build();

        appointment.calculateEndTime(); // <--- OBRIGATÓRIO AQUI TBM
        return appointment;
    }

    // --- REGRAS DE NEGÓCIO ---

    private void calculateEndTime() {
        int duration = services.stream().mapToInt(Service::getDuration).sum();
        this.endTime = this.startTime.plusMinutes(duration);
    }

    private static void validateServices(List<Service> services) {
        if (services == null || services.isEmpty()) {
            throw new BusinessException("Ao menos um serviço deve ser selecionado.");
        }
    }

    public void confirm() {
        if (this.status != AppointmentStatus.PENDING) {
            throw new BusinessException("Apenas agendamentos pendentes podem ser confirmados.");
        }
        this.status = AppointmentStatus.SCHEDULED;
    }

    public void complete(Professional professional) {
        if (this.status != AppointmentStatus.SCHEDULED) {
            throw new BusinessException("Apenas agendamentos confirmados podem ser finalizados.");
        }

        // O agendamento pede ao profissional para calcular a parte dele
        this.professionalCommission = professional.calculateCommission(this.totalPrice);

        // O que sobra é do dono do SaaS (ServiceProvider)
        this.serviceProviderFee = this.totalPrice.subtract(this.professionalCommission);

        this.status = AppointmentStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
    }

    public void cancel() {
        if (this.status == AppointmentStatus.COMPLETED) {
            throw new BusinessException("Não é possível cancelar um agendamento já finalizado.");
        }
        this.status = AppointmentStatus.CANCELLED;
    }

    public void reschedule(LocalDateTime newStartTime) {
        if (this.status == AppointmentStatus.COMPLETED) {
            throw new BusinessException("Agendamentos finalizados não podem ser reagendados.");
        }
        this.startTime = newStartTime;
        calculateEndTime();
        this.status = AppointmentStatus.PENDING;
        this.completedAt = null;
        this.paymentMethod = null;
    }

    public void markAsNotified() {
        this.notified = true;
    }

    public static Appointment createPersonalBlock(String profId, String profName, String providerId,
            LocalDateTime start, LocalDateTime end, String reason) {
        return Appointment.builder()
                .professionalId(profId)
                .professionalName(profName)
                .providerId(providerId)
                .startTime(start)
                .endTime(end)
                .status(AppointmentStatus.BLOCKED)
                .isPersonalBlock(true) // Crucial para o Financeiro ignorar
                .notes("BLOQUEIO PESSOAL: " + reason)
                .totalPrice(BigDecimal.ZERO)
                .finalPrice(BigDecimal.ZERO)
                .services(Collections.emptyList())
                .reminderMinutes(0)
                .notified(false)
                .createdAt(LocalDateTime.now())
                .build();
    }

    public void markReminderAsSent() {
        this.reminderSent = true;
    }
}