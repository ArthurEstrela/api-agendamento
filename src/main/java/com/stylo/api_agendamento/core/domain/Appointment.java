package com.stylo.api_agendamento.core.domain;

import com.stylo.api_agendamento.core.domain.vo.ClientPhone;
import com.stylo.api_agendamento.core.domain.vo.PaymentMethod;
import com.stylo.api_agendamento.core.exceptions.BusinessException;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.math.BigDecimal;
import java.util.Collections;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Appointment {
    private final String id;
    private final String clientId;
    private final String clientName;
    private final ClientPhone clientPhone;
    private final String providerId;
    private final String professionalId;
    private final String professionalName;
    private final List<Service> services;

    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private AppointmentStatus status;
    private PaymentMethod paymentMethod;

    private final BigDecimal totalPrice;
    private BigDecimal finalPrice;
    private String notes;
    private final LocalDateTime createdAt;
    private LocalDateTime completedAt;

    // Fábrica estática: garante que todo agendamento nasça com status PENDING e
    // cálculos feitos
    public static Appointment create(String clientId, String clientName, ClientPhone phone,
            String providerId, String profId, String profName,
            List<Service> services, LocalDateTime start) {

        BigDecimal total = services.stream()
                .map(Service::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Appointment appointment = Appointment.builder()
                .clientId(clientId)
                .clientName(clientName)
                .clientPhone(phone)
                .providerId(providerId)
                .professionalId(profId)
                .professionalName(profName)
                .services(Collections.unmodifiableList(services))
                .startTime(start)
                .status(AppointmentStatus.PENDING)
                .totalPrice(total)
                .finalPrice(total)
                .createdAt(LocalDateTime.now())
                .build();

        appointment.calculateEndTime();
        return appointment;
    }

    // --- REGRAS DE NEGÓCIO (ENCAPSULAMENTO) ---

    private void calculateEndTime() {
        int duration = services.stream().mapToInt(Service::getDuration).sum();
        this.endTime = this.startTime.plusMinutes(duration);
    }

    public void confirm() {
        if (this.status != AppointmentStatus.PENDING) {
            throw new BusinessException("Apenas agendamentos pendentes podem ser confirmados."); //
        }
        this.status = AppointmentStatus.SCHEDULED; //
    }

    public void complete(PaymentMethod method, BigDecimal finalPrice) {
        if (this.status != AppointmentStatus.SCHEDULED) {
            throw new BusinessException("O agendamento deve estar confirmado para ser finalizado.");
        }
        this.status = AppointmentStatus.COMPLETED;
        this.paymentMethod = method;
        this.finalPrice = finalPrice;
        this.completedAt = LocalDateTime.now();
    }

    public void cancel() {
        if (this.status == AppointmentStatus.COMPLETED) {
            throw new IllegalStateException("Não é possível cancelar um agendamento já finalizado.");
        }
        this.status = AppointmentStatus.CANCELLED;
    }

    public void reschedule(LocalDateTime newStartTime) {
        if (this.status == AppointmentStatus.COMPLETED) {
            throw new BusinessException("Agendamentos finalizados não podem ser reagendados.");
        }
        this.startTime = newStartTime;
        calculateEndTime();
        this.status = AppointmentStatus.PENDING; // Volta para pendente para nova validação do profissional
        this.completedAt = null;
        this.paymentMethod = null;
    }

    public void applyDiscount(BigDecimal discountAmount) {
        if (discountAmount.compareTo(totalPrice) > 0) {
            throw new BusinessException("O desconto não pode ser maior que o valor total.");
        }
        this.finalPrice = this.totalPrice.subtract(discountAmount);
    }
}