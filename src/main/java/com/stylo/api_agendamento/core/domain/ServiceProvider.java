package com.stylo.api_agendamento.core.domain;

import com.stylo.api_agendamento.core.domain.vo.*;
import com.stylo.api_agendamento.core.exceptions.BusinessException;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;

@Getter
@Setter // Adicionado Setter para facilitar updates via Mapper/UseCases
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ServiceProvider {

    private final String id;
    private String businessName;
    private Address businessAddress;
    private final Document document;
    private String businessPhone;
    private Slug publicProfileSlug;
    private final String ownerEmail;

    private String logoUrl;
    private String bannerUrl;
    private String pixKey;
    private String pixKeyType;

    private LocalDateTime trialEndsAt;
    private String stripeCustomerId;

    private List<PaymentMethod> paymentMethods;
    private Integer cancellationMinHours;
    private String subscriptionStatus;
    private LocalDateTime gracePeriodEndsAt;

    private Integer maxNoShowsAllowed;

    private String timeZone;

    // --- CONFIGURAÇÕES FINANCEIRAS INTERNAS ---
    // Controla se o profissional calcula comissões para seus funcionários/parceiros
    private boolean commissionsEnabled;

    // --- CONFIGURAÇÕES STRIPE CONNECT (PLATAFORMA) ---
    // ID da conta conectada no Stripe (ex: acct_12345)
    private String stripeAccountId;

    // Se o estabelecimento ativou recebimento online via App
    @Builder.Default
    private Boolean onlinePaymentsEnabled = false;

    // Taxa da plataforma sobre cada transação (ex: 2.00%)
    @Builder.Default
    private BigDecimal platformFeePercentage = new BigDecimal("2.00");

    public static ServiceProvider create(String businessName, Document doc, Slug slug, Address address,
            String ownerEmail) {
        return ServiceProvider.builder()
                .businessName(businessName)
                .document(doc)
                .publicProfileSlug(slug)
                .businessAddress(address)
                .ownerEmail(ownerEmail)
                .subscriptionStatus("TRIAL")
                .trialEndsAt(LocalDateTime.now().plusDays(15))
                .cancellationMinHours(2)
                .paymentMethods(Collections.emptyList())
                .commissionsEnabled(false)
                .onlinePaymentsEnabled(false)
                .platformFeePercentage(new BigDecimal("2.00"))
                .build();
    }

    // --- MÉTODOS DE NEGÓCIO ---

    /**
     * Verifica se o Prestador está apto a receber pagamentos online (Stripe Connect).
     * Regra de Negócio: Deve estar habilitado E ter uma conta Stripe vinculada.
     */
    public boolean canReceiveOnlinePayments() {
        return Boolean.TRUE.equals(this.onlinePaymentsEnabled) 
            && this.stripeAccountId != null 
            && !this.stripeAccountId.isBlank();
    }

    /**
     * Verifica se o sistema de comissões internas está ativo.
     */
    public boolean areCommissionsEnabled() {
        return this.commissionsEnabled;
    }

    public void toggleCommissions(boolean enabled) {
        this.commissionsEnabled = enabled;
    }

    // --- Validações e Updates ---

    public void validateCancellationPolicy(LocalDateTime appointmentStartTime) {
        LocalDateTime limit = LocalDateTime.now().plusHours(this.cancellationMinHours);
        if (appointmentStartTime.isBefore(limit)) {
            throw new BusinessException("O prazo mínimo para cancelamento é de " + cancellationMinHours + " horas.");
        }
    }

    public void updatePaymentMethods(List<PaymentMethod> methods) {
        if (methods == null || methods.isEmpty()) {
            throw new BusinessException("O estabelecimento deve aceitar pelo menos um método de pagamento.");
        }
        this.paymentMethods = methods;
    }

    public void updateSlug(Slug newSlug) {
        if (newSlug == null)
            throw new BusinessException("O endereço da URL não pode ser vazio.");
        this.publicProfileSlug = newSlug;
    }

    public void updateProfile(String name, String phone, String logo) {
        if (name != null && !name.isBlank())
            this.businessName = name;
        if (phone != null && !phone.isBlank())
            this.businessPhone = phone;
        if (logo != null)
            this.logoUrl = logo;
    }

    public void updateAddress(Address address) {
        if (address != null)
            this.businessAddress = address;
    }

    public boolean isSubscriptionActive() {
        return "ACTIVE".equals(this.subscriptionStatus) ||
                "TRIAL".equals(this.subscriptionStatus) ||
                "GRACE_PERIOD".equals(this.subscriptionStatus);
    }

    public void startGracePeriod(int days) {
        this.subscriptionStatus = "GRACE_PERIOD";
        this.gracePeriodEndsAt = LocalDateTime.now().plusDays(days);
    }

    public void updateSubscription(String newStatus) {
        List<String> validStatuses = List.of("ACTIVE", "TRIAL", "EXPIRED", "CANCELED", "GRACE_PERIOD");
        if (!validStatuses.contains(newStatus)) {
            throw new BusinessException("Status de assinatura inválido.");
        }
        this.subscriptionStatus = newStatus;

        if ("ACTIVE".equals(newStatus)) {
            this.gracePeriodEndsAt = null;
        }
    }

    public String getTimeZone() {
        return timeZone != null ? timeZone : "America/Sao_Paulo";
    }

    public ZoneId getZoneId() {
        try {
            return ZoneId.of(getTimeZone());
        } catch (Exception e) {
            return ZoneId.of("America/Sao_Paulo");
        }
    }

    public int getMaxNoShowsAllowed() {
        return maxNoShowsAllowed != null ? maxNoShowsAllowed : 3;
    }
}