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
import java.util.Objects;
import java.util.UUID;

@Getter
@Builder(toBuilder = true)
@Setter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Appointment {

    // ✨ MELHORIA: Identidade forte com UUID
    private UUID id;
    private UUID clientId;
    private String clientName;
    private String clientEmail;
    private String businessName;
    private ClientPhone clientPhone;

    // ✨ MELHORIA: Padronizado para UUID e nome único (removeu duplicidade de
    // providerId)
    private UUID serviceProviderId;

    private UUID professionalId;
    private String professionalName;

    @Builder.Default
    private List<Service> services = new ArrayList<>();

    @Builder.Default
    private List<AppointmentItem> products = new ArrayList<>();

    private String timeZone;

    @Setter
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    // --- FINANCEIRO ---
    private BigDecimal price; // Preço Base (Serviços + Produtos)

    @Setter
    private BigDecimal finalPrice; // Preço Final Cobrado (Com descontos)

    // ✨ MELHORIA: CouponId agora é UUID (se o cupom for uma entidade)
    // Se o cupom for apenas um código texto ("VERAO10"), mantenha String.
    // Assumindo padronização de IDs de entidades:
    private UUID couponId;
    private BigDecimal discountAmount;

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

    // IDs externos (Stripe/Google) continuam String pois não controlamos o formato
    @Setter
    private String externalEventId;

    private boolean isPersonalBlock;

    private boolean paid;
    private String externalPaymentId;

    private boolean commissionSettled;
    private LocalDateTime settledAt;

    // --- FACTORIES (Atualizadas com UUID e Geração de ID) ---

    public static Appointment create(UUID clientId, String clientName, String clientEmail,
            String businessName, ClientPhone phone,
            UUID serviceProviderId, UUID profId, String profName,
            List<Service> services, LocalDateTime start, Integer reminderMinutes,
            String timeZone) {

        validateServices(services);
        BigDecimal serviceTotal = calculateServiceTotal(services);

        if (serviceTotal.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("O valor total dos serviços não pode ser negativo.");
        }

        Appointment appointment = Appointment.builder()
                .id(UUID.randomUUID()) // ✨ A entidade já nasce com ID
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
                .timeZone(timeZone != null ? timeZone : "America/Sao_Paulo")
                .status(AppointmentStatus.PENDING)
                .price(serviceTotal)
                .finalPrice(serviceTotal)
                .discountAmount(BigDecimal.ZERO)
                .reminderMinutes(reminderMinutes != null ? reminderMinutes : 0)
                .reminderSent(false)
                .notified(false)
                .isPersonalBlock(false)
                .createdAt(LocalDateTime.now())
                .build();

        appointment.calculateEndTime();

        if (!appointment.getEndTime().isAfter(appointment.getStartTime())) {
            throw new BusinessException("A duração total do agendamento deve ser maior que zero.");
        }

        return appointment;
    }

    public static Appointment createManual(String clientName, ClientPhone phone,
            UUID serviceProviderId, UUID profId, String profName,
            List<Service> services, LocalDateTime start, String notes,
            String timeZone) {

        validateServices(services);
        BigDecimal serviceTotal = calculateServiceTotal(services);

        if (serviceTotal.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("O valor total dos serviços não pode ser negativo.");
        }

        Appointment appointment = Appointment.builder()
                .id(UUID.randomUUID()) // ✨ ID Gerado
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
                .discountAmount(BigDecimal.ZERO)
                .notes(notes)
                .reminderMinutes(0)
                .createdAt(LocalDateTime.now())
                .build();

        appointment.calculateEndTime();

        if (!appointment.getEndTime().isAfter(appointment.getStartTime())) {
            throw new BusinessException("A duração total do agendamento deve ser maior que zero.");
        }

        return appointment;
    }

    public static Appointment createPersonalBlock(UUID profId, String profName, UUID serviceProviderId,
            LocalDateTime start, LocalDateTime end, String reason, String timeZone) {

        if (end == null || !end.isAfter(start)) {
            throw new BusinessException("O horário de término do bloqueio deve ser posterior ao início.");
        }

        return Appointment.builder()
                .id(UUID.randomUUID()) // ✨ ID Gerado
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
                .discountAmount(BigDecimal.ZERO)
                .services(new ArrayList<>())
                .products(new ArrayList<>())
                .reminderMinutes(0)
                .notified(false)
                .createdAt(LocalDateTime.now())
                .build();
    }

    // --- MÉTODOS AUXILIARES DE DATA ---

    public ZoneId getZoneId() {
        try {
            return ZoneId.of(this.timeZone);
        } catch (Exception e) {
            return ZoneId.of("America/Sao_Paulo");
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
                    .productId(p.getId()) // Assumindo que Product.getId() agora retorna UUID
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
            if (discountValue.compareTo(this.price) > 0) {
                throw new BusinessException("O desconto não pode ser maior que o valor total.");
            }
            this.finalPrice = this.price.subtract(discountValue);
            this.discountAmount = discountValue;
        } else {
            this.finalPrice = this.price;
        }

        if (this.finalPrice.compareTo(BigDecimal.ZERO) < 0) {
            this.finalPrice = BigDecimal.ZERO;
        }

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

        if (!this.endTime.isAfter(this.startTime)) {
            throw new BusinessException("Erro ao reagendar: duração inválida.");
        }

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

    public void removeProduct(UUID productId) { // ✨ Atualizado para UUID
        this.products.removeIf(item -> item.getProductId().equals(productId));
        recalculateTotals();
    }

    public boolean hasProducts() {
        return this.products != null && !this.products.isEmpty();
    }

    public void confirmPayment(String externalPaymentId) {
        if (this.paid && this.status == AppointmentStatus.SCHEDULED) {
            return;
        }

        if (this.status == AppointmentStatus.CANCELLED) {
            throw new BusinessException(
                    "Recebemos pagamento para um agendamento cancelado. Necessário estorno manual.");
        }

        this.paid = true;
        this.externalPaymentId = externalPaymentId;

        if (this.status == AppointmentStatus.PENDING) {
            this.status = AppointmentStatus.SCHEDULED;
        }

        this.settledAt = LocalDateTime.now();
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

    // --- IMPLEMENTAÇÃO DE IDENTIDADE DE ENTIDADE ---
    // Em DDD, Entidades são iguais se seus IDs são iguais.

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Appointment that = (Appointment) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    // --- SUBCLASSES ---
    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class AppointmentItem {
        private UUID productId; // ✨ UUID
        private String productName;
        private BigDecimal unitPrice;
        private Integer quantity;

        public BigDecimal getTotal() {
            if (unitPrice == null || quantity == null)
                return BigDecimal.ZERO;
            return unitPrice.multiply(new BigDecimal(quantity));
        }
    }

    public int calculateTotalDuration() {
        if (this.services == null || this.services.isEmpty()) {
            return 0;
        }
        return this.services.stream()
                .mapToInt(Service::getDuration)
                .sum();
    }

    public String getServicesSnapshot() {
        if (this.services == null || this.services.isEmpty()) {
            return "Atendimento Padrão";
        }
        return this.services.stream()
                .map(Service::getName)
                .collect(java.util.stream.Collectors.joining(", "));
    }
}