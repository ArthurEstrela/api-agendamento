package com.stylo.api_agendamento.core.domain;

import com.stylo.api_agendamento.core.domain.vo.ClientPhone;
import com.stylo.api_agendamento.core.domain.vo.PaymentMethod;
import com.stylo.api_agendamento.core.exceptions.BusinessException;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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
    private String providerId; // (Se for o mesmo que serviceProviderId, pode remover um dos dois no futuro)

    private String professionalId;
    private String professionalName;

    @Builder.Default
    private List<Service> services = new ArrayList<>();

    @Builder.Default
    private List<AppointmentItem> products = new ArrayList<>();

    @Setter
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    // --- FINANCEIRO ---
    private BigDecimal price; // Preço Base (Serviços + Produtos)

    @Setter
    private BigDecimal finalPrice; // Preço Final Cobrado (Com descontos)

    private BigDecimal professionalCommission; // ✨ O campo que você pediu (Snapshot)
    private BigDecimal serviceProviderFee; // ✨ Quanto sobra para o salão

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

    // --- FACTORIES ---

    public static Appointment create(String clientId, String clientName, String clientEmail,
            String businessName, ClientPhone phone,
            String serviceProviderId, String profId, String profName,
            List<Service> services, LocalDateTime start, Integer reminderMinutes) {

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
            List<Service> services, LocalDateTime start, String notes) {

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
            LocalDateTime start, LocalDateTime end, String reason) {
        return Appointment.builder()
                .professionalId(profId)
                .professionalName(profName)
                .serviceProviderId(serviceProviderId)
                .startTime(start)
                .endTime(end)
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
        // Recalcula o total (serviços + novos produtos)
        recalculateTotals();
    }

    /**
     * Finaliza o agendamento aplicando descontos e registrando a comissão
     * calculada.
     * * @param professional (Para fins de registro/auditoria se necessário)
     * 
     * @param discountValue   Valor do desconto a ser aplicado no total
     * @param commissionValue Valor da comissão JÁ CALCULADA pelo UseCase (Snapshot
     *                        Pattern)
     */
    public void complete(Professional professional, BigDecimal discountValue, BigDecimal commissionValue) {
        if (this.status == AppointmentStatus.CANCELLED) {
            throw new BusinessException("Não é possível finalizar um agendamento cancelado.");
        }

        // 1. Garante que o preço base (services + products) está atualizado
        recalculateTotals();

        // 2. Aplica Desconto no Preço Final
        if (discountValue != null && discountValue.compareTo(BigDecimal.ZERO) > 0) {
            this.finalPrice = this.price.subtract(discountValue);
            if (this.finalPrice.compareTo(BigDecimal.ZERO) < 0) {
                this.finalPrice = BigDecimal.ZERO;
            }
        } else {
            this.finalPrice = this.price;
        }

        // 3. Registra a Comissão (Snapshot do valor calculado no momento da conclusão)
        this.professionalCommission = commissionValue != null ? commissionValue : BigDecimal.ZERO;

        // 4. Calcula a taxa do salão (O que sobra)
        // Fee = Final Price - Commission
        this.serviceProviderFee = this.finalPrice.subtract(this.professionalCommission);

        // 5. Atualiza Status
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

    // ✨ Método auxiliar público para o UseCase calcular comissões apenas sobre
    // serviços
    public BigDecimal calculateOriginalServiceTotal() {
        return calculateServiceTotal(this.services);
    }

    // --- MÉTODOS PRIVADOS ---

    private void calculateEndTime() {
        if (services != null) {
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
        // Reseta o finalPrice para o price base antes de aplicar descontos no
        // complete()
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
}