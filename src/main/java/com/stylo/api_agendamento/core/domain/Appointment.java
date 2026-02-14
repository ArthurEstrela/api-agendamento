package com.stylo.api_agendamento.core.domain;

import com.stylo.api_agendamento.core.domain.vo.ClientPhone;
import com.stylo.api_agendamento.core.domain.vo.PaymentMethod;
import com.stylo.api_agendamento.core.exceptions.BusinessException;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Getter
@Builder
@Setter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Appointment {

    private String id;
    private String clientId;
    private String clientName;
    private String clientEmail;
    private String businessName;
    private ClientPhone clientPhone;

    private String serviceProviderId;
    private String providerId; 

    private String professionalId;
    private String professionalName;

    @Builder.Default
    private List<Service> services = new ArrayList<>();

    @Builder.Default
    private List<AppointmentItem> products = new ArrayList<>();

    // ✨ TIMEZONE & DATES
    // As datas aqui são persistidas sem fuso no banco (LocalDateTime),
    // mas SEMPRE devem ser interpretadas no contexto do timeZone abaixo.
    private String timeZone; 

    @Setter
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    // --- FINANCEIRO ---
    private BigDecimal price; // Preço Base (Serviços + Produtos)

    @Setter
    private BigDecimal finalPrice; // Preço Final Cobrado (Com descontos)

    private BigDecimal professionalCommission; 
    private BigDecimal serviceProviderFee; 

    @Setter
    private AppointmentStatus status;

    private String cancellationReason;
    private String cancelledBy;

    @Setter
    private PaymentMethod paymentMethod;

    @Setter
    private String notes;

    private LocalDateTime createdAt;
    private LocalDateTime completedAt;

    private Integer reminderMinutes;
    private boolean reminderSent;
    private boolean notified;

    @Setter
    private String externalEventId;

    private boolean isPersonalBlock;

    private boolean paid;
    private String externalPaymentId;

    private boolean commissionSettled;
    private LocalDateTime settledAt;

    // --- FACTORIES (Atualizadas com TimeZone) ---

    public static Appointment create(String clientId, String clientName, String clientEmail,
            String businessName, ClientPhone phone,
            String serviceProviderId, String profId, String profName,
            List<Service> services, LocalDateTime start, Integer reminderMinutes,
            String timeZone) { // ✨ Novo parâmetro obrigatório

        validateServices(services);
        BigDecimal serviceTotal = calculateServiceTotal(services);

        Appointment appointment = Appointment.builder()
                .clientId(clientId)
                .clientName(clientName)
                .clientEmail(clientEmail)
                .businessName(businessName)
                .clientPhone(phone)
                .serviceProviderId(serviceProviderId)
                .professionalId(profId)
                .professionalName(profName)
                .services(new ArrayList<>(services))
                .products(new ArrayList<>())
                .startTime(start)
                .timeZone(timeZone != null ? timeZone : "America/Sao_Paulo") // Fallback seguro
                .status(AppointmentStatus.PENDING)
                .price(serviceTotal)
                .finalPrice(serviceTotal)
                .reminderMinutes(reminderMinutes != null ? reminderMinutes : 0)
                .reminderSent(false)
                .notified(false)
                .isPersonalBlock(false)
                .createdAt(LocalDateTime.now())
                .build();

        appointment.calculateEndTime();
        return appointment;
    }

    public static Appointment createManual(String clientName, ClientPhone phone,
            String serviceProviderId, String profId, String profName,
            List<Service> services, LocalDateTime start, String notes,
            String timeZone) { // ✨ Novo parâmetro

        validateServices(services);
        BigDecimal serviceTotal = calculateServiceTotal(services);

        Appointment appointment = Appointment.builder()
                .clientName(clientName)
                .clientPhone(phone)
                .serviceProviderId(serviceProviderId)
                .professionalId(profId)
                .professionalName(profName)
                .services(new ArrayList<>(services))
                .products(new ArrayList<>())
                .startTime(start)
                .timeZone(timeZone != null ? timeZone : "America/Sao_Paulo")
                .status(AppointmentStatus.SCHEDULED)
                .price(serviceTotal)
                .finalPrice(serviceTotal)
                .notes(notes)
                .reminderMinutes(0)
                .createdAt(LocalDateTime.now())
                .build();

        appointment.calculateEndTime();
        return appointment;
    }

    public static Appointment createPersonalBlock(String profId, String profName, String serviceProviderId,
            LocalDateTime start, LocalDateTime end, String reason, String timeZone) {
        
        return Appointment.builder()
                .professionalId(profId)
                .professionalName(profName)
                .serviceProviderId(serviceProviderId)
                .startTime(start)
                .endTime(end)
                .timeZone(timeZone != null ? timeZone : "America/Sao_Paulo")
                .status(AppointmentStatus.BLOCKED)
                .isPersonalBlock(true)
                .notes("BLOQUEIO PESSOAL: " + reason)
                .price(BigDecimal.ZERO)
                .finalPrice(BigDecimal.ZERO)
                .services(new ArrayList<>())
                .products(new ArrayList<>())
                .reminderMinutes(0)
                .notified(false)
                .createdAt(LocalDateTime.now())
                .build();
    }

    // --- MÉTODOS AUXILIARES DE DATA ---

    /**
     * Retorna o ZoneId configurado para este agendamento.
     * Útil para integrações com Google Calendar e cálculos de disponibilidade.
     */
    public ZoneId getZoneId() {
        try {
            return ZoneId.of(this.timeZone);
        } catch (Exception e) {
            return ZoneId.of("America/Sao_Paulo"); // Fallback padrão
        }
    }

    // --- MÉTODOS DE NEGÓCIO ---

    public void confirm() {
        if (this.status != AppointmentStatus.PENDING) {
            throw new BusinessException("Apenas agendamentos pendentes podem ser confirmados.");
        }
        this.status = AppointmentStatus.SCHEDULED;
    }

    public void markReminderAsSent() {
        this.reminderSent = true;
    }

    public void addProducts(List<Product> productsToAdd, List<Integer> quantities) {
        if (productsToAdd.size() != quantities.size()) {
            throw new BusinessException("Quantidade de produtos e quantidades não batem.");
        }

        for (int i = 0; i < productsToAdd.size(); i++) {
            Product p = productsToAdd.get(i);
            Integer qty = quantities.get(i);

            this.products.add(AppointmentItem.builder()
                    .productId(p.getId())
                    .productName(p.getName())
                    .unitPrice(p.getPrice())
                    .quantity(qty)
                    .build());
        }
        recalculateTotals();
    }

    public void complete(Professional professional, BigDecimal discountValue, BigDecimal commissionValue) {
        if (this.status == AppointmentStatus.CANCELLED) {
            throw new BusinessException("Não é possível finalizar um agendamento cancelado.");
        }

        recalculateTotals();

        // Aplica Desconto
        if (discountValue != null && discountValue.compareTo(BigDecimal.ZERO) > 0) {
            this.finalPrice = this.price.subtract(discountValue);
            if (this.finalPrice.compareTo(BigDecimal.ZERO) < 0) {
                this.finalPrice = BigDecimal.ZERO;
            }
        } else {
            this.finalPrice = this.price;
        }

        // Registra Comissão e Taxa do Salão
        this.professionalCommission = commissionValue != null ? commissionValue : BigDecimal.ZERO;
        this.serviceProviderFee = this.finalPrice.subtract(this.professionalCommission);

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

    public BigDecimal calculateOriginalServiceTotal() {
        return calculateServiceTotal(this.services);
    }

    // --- MÉTODOS PRIVADOS ---

    private void calculateEndTime() {
        if (services != null && startTime != null) {
            int duration = services.stream().mapToInt(Service::getDuration).sum();
            this.endTime = this.startTime.plusMinutes(duration);
        }
    }

    private void recalculateTotals() {
        BigDecimal serviceTotal = calculateServiceTotal(this.services);

        BigDecimal productTotal = this.products.stream()
                .map(AppointmentItem::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        this.price = serviceTotal.add(productTotal);
        this.finalPrice = this.price;
    }

    private static BigDecimal calculateServiceTotal(List<Service> services) {
        if (services == null)
            return BigDecimal.ZERO;
        return services.stream()
                .map(Service::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static void validateServices(List<Service> services) {
        if (services == null || services.isEmpty()) {
            throw new BusinessException("Ao menos um serviço deve ser selecionado.");
        }
    }

    // --- SUBCLASSES ---
    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class AppointmentItem {
        private String productId;
        private String productName;
        private BigDecimal unitPrice;
        private Integer quantity;

        public BigDecimal getTotal() {
            if (unitPrice == null || quantity == null)
                return BigDecimal.ZERO;
            return unitPrice.multiply(new BigDecimal(quantity));
        }
    }

    public void markAsNoShow() {
        if (this.status != AppointmentStatus.SCHEDULED) {
            throw new BusinessException("Apenas agendamentos confirmados podem ser marcados como No-Show.");
        }
        this.status = AppointmentStatus.NO_SHOW;
    }

    public boolean isEligibleForRefund(int minHoursBefore) {
        if (!this.paid || this.externalPaymentId == null)
            return false;

        LocalDateTime limit = LocalDateTime.now().plusHours(minHoursBefore);
        return this.startTime.isAfter(limit);
    }

    public void markCommissionAsSettled() {
        this.commissionSettled = true;
        this.settledAt = LocalDateTime.now();
    }

    public boolean isPaid() {
        return this.externalPaymentId != null;
    }
}