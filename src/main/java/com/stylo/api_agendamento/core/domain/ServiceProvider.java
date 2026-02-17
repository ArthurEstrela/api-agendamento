package com.stylo.api_agendamento.core.domain;

import com.stylo.api_agendamento.core.domain.vo.*;
import com.stylo.api_agendamento.core.exceptions.BusinessException;
import lombok.*;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Collections;

@Getter
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

    // ✨ NOVO CAMPO: O "Interruptor" de Comissões
    private boolean commissionsEnabled;

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
                .commissionsEnabled(false) // ✨ Nasce desligado por padrão (segurança)
                .build();
    }

    // --- MÉTODOS DE NEGÓCIO ---

    /**
     * Verifica se o sistema de comissões está ativo para este estabelecimento.
     * Usado pelo CompleteAppointmentUseCase.
     */
    public boolean areCommissionsEnabled() {
        return this.commissionsEnabled;
    }

    /**
     * Ativa ou desativa o cálculo de comissões.
     */
    public void toggleCommissions(boolean enabled) {
        this.commissionsEnabled = enabled;
    }

    // ... (Métodos de validação e update existentes mantidos abaixo) ...

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
                "GRACE_PERIOD".equals(this.subscriptionStatus); // ✨ Agora retorna true aqui também
    }

    public void startGracePeriod(int days) {
        this.subscriptionStatus = "GRACE_PERIOD";
        this.gracePeriodEndsAt = LocalDateTime.now().plusDays(days);
    }

    public void updateSubscription(String newStatus) {
        // ✨ Adicionado "GRACE_PERIOD" à lista de válidos
        List<String> validStatuses = List.of("ACTIVE", "TRIAL", "EXPIRED", "CANCELED", "GRACE_PERIOD");
        if (!validStatuses.contains(newStatus)) {
            throw new BusinessException("Status de assinatura inválido.");
        }
        this.subscriptionStatus = newStatus;

        // Se voltar a ficar ativo, limpamos o prazo de carência
        if ("ACTIVE".equals(newStatus)) {
            this.gracePeriodEndsAt = null;
        }
    }

    public String getTimeZone() {
        return timeZone != null ? timeZone : "America/Sao_Paulo"; // Fallback seguro
    }

    // Método helper para facilitar conversão
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