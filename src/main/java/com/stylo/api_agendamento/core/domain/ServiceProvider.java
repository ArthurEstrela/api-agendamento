package com.stylo.api_agendamento.core.domain;

import com.stylo.api_agendamento.core.domain.vo.*;
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
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ServiceProvider {

    // --- 1. IDENTIDADE E PERFIL ---
    private UUID id;
    private String businessName;
    private Document document; // CPF ou CNPJ (Value Object)
    private String ownerEmail;

    private Slug publicProfileSlug; // URL amigável (ex: stylo.com/barbearia-do-ze)
    private Address businessAddress;
    private String businessPhone;

    // --- 2. BRANDING ---
    private String logoUrl;
    private String bannerUrl;

    // --- ✨ NOVOS CAMPOS: RANKING E AVALIAÇÃO (Busca Avançada) ---
    @Builder.Default
    private Double averageRating = 0.0;

    @Builder.Default
    private Integer totalReviews = 0;

    // --- 3. CONFIGURAÇÕES FINANCEIRAS (PIX & STRIPE) ---
    private String pixKey;
    private String pixKeyType; // Poderia ser um Enum (EMAIL, CPF, PHONE, RANDOM)

    // Configurações do Stripe Connect (Plataforma)
    private String stripeAccountId; // Conta conectada (ex: acct_xyz)
    private String stripeCustomerId; // Cliente na plataforma (para pagar a assinatura)

    @Builder.Default
    private boolean onlinePaymentsEnabled = false;

    @Builder.Default
    private BigDecimal platformFeePercentage = new BigDecimal("2.00"); // Taxa padrão 2%

    // Sistema de Comissões Internas (Profissional -> Parceiro)
    private boolean commissionsEnabled;

    // --- 4. ASSINATURA E STATUS ---
    private SubscriptionStatus subscriptionStatus;
    private LocalDateTime trialEndsAt;
    private LocalDateTime gracePeriodEndsAt;

    // --- 5. CONFIGURAÇÕES OPERACIONAIS ---
    @Builder.Default
    private List<PaymentMethod> paymentMethods = new ArrayList<>();

    private Integer cancellationMinHours; // Ex: 2 horas antes
    private Integer maxNoShowsAllowed; // Ex: 3 faltas bloqueia o cliente

    private String timeZone;

    private boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // --- FACTORY ---

    public static ServiceProvider create(String businessName, Document doc, Slug slug, Address address,
            String ownerEmail) {

        if (businessName == null || businessName.isBlank())
            throw new BusinessException("Nome do estabelecimento é obrigatório.");
        if (doc == null)
            throw new BusinessException("Documento (CPF/CNPJ) é obrigatório.");
        if (slug == null)
            throw new BusinessException("Slug do perfil é obrigatório.");
        if (ownerEmail == null || !ownerEmail.contains("@"))
            throw new BusinessException("E-mail do proprietário inválido.");

        return ServiceProvider.builder()
                .id(UUID.randomUUID()) // Identidade gerada
                .businessName(businessName)
                .document(doc)
                .publicProfileSlug(slug)
                .businessAddress(address)
                .ownerEmail(ownerEmail)
                // Padrões de Assinatura
                .subscriptionStatus(SubscriptionStatus.TRIAL)
                .trialEndsAt(LocalDateTime.now().plusDays(15)) // 15 dias grátis
                // Padrões Operacionais
                .cancellationMinHours(2)
                .maxNoShowsAllowed(3)
                .paymentMethods(new ArrayList<>()) // Lista mutável
                .commissionsEnabled(false)
                .onlinePaymentsEnabled(false)
                .platformFeePercentage(new BigDecimal("2.00"))
                .averageRating(0.0) // ✨ Inicialização segura
                .totalReviews(0) // ✨ Inicialização segura
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    // --- MÉTODOS DE NEGÓCIO: AVALIAÇÕES E RANKING ---

    /**
     * ✨ Recalcula a média de avaliação quando um novo Review é recebido.
     * Pode ser chamado pelo CreateReviewUseCase.
     */
    public void updateRating(int newRatingScore) {
        if (newRatingScore < 1 || newRatingScore > 5) {
            throw new BusinessException("A nota de avaliação deve ser entre 1 e 5.");
        }

        // Se for a primeira avaliação
        if (this.totalReviews == 0 || this.averageRating == 0.0) {
            this.averageRating = (double) newRatingScore;
            this.totalReviews = 1;
        } else {
            // Fórmula padrão para recalcular média progressiva sem buscar tudo do banco
            double currentTotalScore = this.averageRating * this.totalReviews;
            this.totalReviews++;

            double newAverage = (currentTotalScore + newRatingScore) / this.totalReviews;

            // Arredonda para 2 casas decimais (ex: 4.85)
            this.averageRating = Math.round(newAverage * 100.0) / 100.0;
        }

        this.updatedAt = LocalDateTime.now();
    }

    // --- MÉTODOS DE NEGÓCIO: FINANCEIRO ---

    /**
     * Verifica se o Prestador está apto a receber pagamentos online.
     * Regra: Habilitado E com conta Stripe vinculada.
     */
    public boolean canReceiveOnlinePayments() {
        return this.onlinePaymentsEnabled
                && this.stripeAccountId != null
                && !this.stripeAccountId.isBlank();
    }

    public void enableOnlinePayments(String stripeAccountId) {
        if (stripeAccountId == null || stripeAccountId.isBlank()) {
            throw new BusinessException("ID da conta Stripe é obrigatório para ativar pagamentos online.");
        }
        this.stripeAccountId = stripeAccountId;
        this.onlinePaymentsEnabled = true;
        this.updatedAt = LocalDateTime.now();
    }

    public void disableOnlinePayments() {
        this.onlinePaymentsEnabled = false;
        this.updatedAt = LocalDateTime.now();
    }

    public void toggleCommissions(boolean enabled) {
        this.commissionsEnabled = enabled;
        this.updatedAt = LocalDateTime.now();
    }

    public void updatePixKeys(String key, String type) {
        this.pixKey = key;
        this.pixKeyType = type;
        this.updatedAt = LocalDateTime.now();
    }

    // --- MÉTODOS DE NEGÓCIO: OPERACIONAL ---

    public void validateCancellationPolicy(LocalDateTime appointmentStartTime) {
        int minHours = this.cancellationMinHours != null ? this.cancellationMinHours : 0;
        if (minHours > 0) {
            LocalDateTime limit = LocalDateTime.now().plusHours(minHours);
            if (appointmentStartTime.isBefore(limit)) {
                throw new BusinessException(
                        "O prazo mínimo para cancelamento é de " + minHours + " horas de antecedência.");
            }
        }
    }

    public void updatePaymentMethods(List<PaymentMethod> methods) {
        if (methods == null || methods.isEmpty()) {
            throw new BusinessException("O estabelecimento deve aceitar pelo menos um método de pagamento.");
        }
        this.paymentMethods = new ArrayList<>(methods); // Cópia defensiva
        this.updatedAt = LocalDateTime.now();
    }

    public void updateProfile(String name, String phone, String logoUrl, String bannerUrl) {
        if (name != null && !name.isBlank())
            this.businessName = name;
        if (phone != null)
            this.businessPhone = phone;
        if (logoUrl != null)
            this.logoUrl = logoUrl;
        if (bannerUrl != null)
            this.bannerUrl = bannerUrl;
        this.updatedAt = LocalDateTime.now();
    }

    public void updateAddress(Address address) {
        if (address != null) {
            this.businessAddress = address;
            this.updatedAt = LocalDateTime.now();
        }
    }

    public void updateSlug(Slug newSlug) {
        if (newSlug == null)
            throw new BusinessException("A URL do perfil não pode ser vazia.");
        this.publicProfileSlug = newSlug;
        this.updatedAt = LocalDateTime.now();
    }

    // --- MÉTODOS DE NEGÓCIO: ASSINATURA ---

    public boolean isSubscriptionActive() {
        if (!this.isActive)
            return false;

        return this.subscriptionStatus == SubscriptionStatus.ACTIVE ||
                this.subscriptionStatus == SubscriptionStatus.TRIAL ||
                this.subscriptionStatus == SubscriptionStatus.GRACE_PERIOD;
    }

    public void startGracePeriod(int days) {
        this.subscriptionStatus = SubscriptionStatus.GRACE_PERIOD;
        this.gracePeriodEndsAt = LocalDateTime.now().plusDays(days);
        this.updatedAt = LocalDateTime.now();
    }

    public void updateSubscriptionStatus(SubscriptionStatus newStatus) {
        if (newStatus == null)
            return;

        this.subscriptionStatus = newStatus;

        if (newStatus == SubscriptionStatus.ACTIVE) {
            this.gracePeriodEndsAt = null; // Limpa período de carência se ativou
        }
        this.updatedAt = LocalDateTime.now();
    }

    // --- AUXILIARES ---

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

    // --- IDENTIDADE (DDD) ---

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        ServiceProvider that = (ServiceProvider) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    // --- ENUMS ---

    public enum SubscriptionStatus {
        TRIAL,
        ACTIVE,
        PAST_DUE, // Pagamento falhou, mas ainda acessa (curto prazo)
        GRACE_PERIOD, // Período de graça manual
        CANCELED,
        EXPIRED // Trial acabou e não pagou
    }
}