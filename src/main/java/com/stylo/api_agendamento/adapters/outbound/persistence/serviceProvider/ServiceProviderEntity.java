package com.stylo.api_agendamento.adapters.outbound.persistence.serviceProvider;

import com.stylo.api_agendamento.adapters.outbound.persistence.AddressVo;
import com.stylo.api_agendamento.adapters.outbound.persistence.BaseEntity;
import com.stylo.api_agendamento.adapters.outbound.persistence.DocumentVo;
import com.stylo.api_agendamento.adapters.outbound.persistence.service.ServiceEntity;
import com.stylo.api_agendamento.core.domain.vo.PaymentMethod;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "service_providers", indexes = {
        // ✨ CORREÇÃO: Usando o nome da coluna no banco (Snake Case)
        @Index(name = "idx_provider_slug", columnList = "public_profile_slug"),
        @Index(name = "idx_provider_email", columnList = "owner_email")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class ServiceProviderEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "business_name", nullable = false, length = 150)
    private String businessName;

    @Embedded
    private AddressVo businessAddress;

    @Column(name = "owner_email", nullable = false, length = 100)
    private String ownerEmail;

    @Embedded
    private DocumentVo document;

    @Column(name = "business_phone", length = 20)
    private String businessPhone;

    @Column(name = "public_profile_slug", unique = true, nullable = false, length = 100)
    private String publicProfileSlug;

    @Column(name = "logo_url", columnDefinition = "TEXT")
    private String logoUrl;

    @Column(name = "banner_url", columnDefinition = "TEXT")
    private String bannerUrl;

    @Column(name = "pix_key", length = 100)
    private String pixKey;

    @Column(name = "pix_key_type", length = 20)
    private String pixKeyType;

    // ✨ PROTEÇÃO: Inicialização segura de coleção
    @ElementCollection(targetClass = PaymentMethod.class, fetch = FetchType.LAZY)
    @CollectionTable(name = "provider_payment_methods", joinColumns = @JoinColumn(name = "provider_id"), foreignKey = @ForeignKey(name = "fk_payment_methods_provider"))
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method")
    @Builder.Default
    private List<PaymentMethod> paymentMethods = new ArrayList<>();

    @Column(name = "cancellation_min_hours", nullable = false)
    @Builder.Default
    private Integer cancellationMinHours = 2;

    @Column(name = "subscription_status", nullable = false, length = 20)
    private String subscriptionStatus;

    @Column(name = "trial_ends_at")
    private LocalDateTime trialEndsAt;

    @Column(name = "stripe_customer_id")
    private String stripeCustomerId;

    @Column(name = "commissions_enabled", nullable = false)
    private boolean commissionsEnabled;

    @Column(name = "grace_period_ends_at")
    private LocalDateTime gracePeriodEndsAt;

    @OneToMany(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_provider_id", insertable = false, updatable = false)
    @Builder.Default
    private Set<ServiceEntity> services = new HashSet<>();

   @Column(name = "average_rating")
    @Builder.Default
    private Double averageRating = 0.0;

    @Column(name = "total_reviews")
    @Builder.Default
    private Integer totalReviews = 0;

    // --- CAMPOS DE AUDITORIA ---
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "time_zone")
    private String timeZone;

    // --- NOVOS CAMPOS STRIPE CONNECT ---
    @Column(name = "stripe_account_id")
    private String stripeAccountId;

    @Column(name = "online_payments_enabled")
    @Builder.Default
    private Boolean onlinePaymentsEnabled = false;

    @Column(name = "platform_fee_percentage", precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal platformFeePercentage = new BigDecimal("2.00");

    @PrePersist
    protected void onCreate() {
        if (this.subscriptionStatus == null) {
            this.subscriptionStatus = "TRIAL";
            this.trialEndsAt = LocalDateTime.now().plusDays(15);
        }
        if (this.onlinePaymentsEnabled == null) {
            this.onlinePaymentsEnabled = false;
        }
        // Garante que não ficam nulos no banco
        if (this.averageRating == null)
            this.averageRating = 0.0;
        if (this.totalReviews == null)
            this.totalReviews = 0;
    }
}