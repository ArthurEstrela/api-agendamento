package com.stylo.api_agendamento.adapters.outbound.persistence.serviceProvider;

import com.stylo.api_agendamento.adapters.outbound.persistence.AddressVo;
import com.stylo.api_agendamento.adapters.outbound.persistence.DocumentVo;
import com.stylo.api_agendamento.core.domain.vo.PaymentMethod;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import com.stylo.api_agendamento.adapters.outbound.persistence.BaseEntity;


import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "service_providers", indexes = {
        @Index(name = "idx_provider_slug", columnList = "publicProfileSlug"),
        @Index(name = "idx_provider_email", columnList = "owner_email")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServiceProviderEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 150)
    private String businessName;

    @Embedded
    private AddressVo businessAddress;

    @Column(name = "owner_email", nullable = false, length = 100)
    private String ownerEmail;

    @Embedded
    private DocumentVo document;

    @Column(length = 20)
    private String businessPhone;

    @Column(unique = true, nullable = false, length = 100)
    private String publicProfileSlug;

    @Column(columnDefinition = "TEXT")
    private String logoUrl;

    @Column(columnDefinition = "TEXT")
    private String bannerUrl;

    @Column(length = 100)
    private String pixKey;

    @Column(length = 20)
    private String pixKeyType;

    @ElementCollection(targetClass = PaymentMethod.class)
    @CollectionTable(name = "provider_payment_methods", joinColumns = @JoinColumn(name = "provider_id"), foreignKey = @ForeignKey(name = "fk_payment_methods_provider"))
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method")
    private List<PaymentMethod> paymentMethods;

    @Column(nullable = false)
    @Builder.Default
    private Integer cancellationMinHours = 2;

    @Column(nullable = false, length = 20)
    private String subscriptionStatus;

    @Column(name = "trial_ends_at")
    private LocalDateTime trialEndsAt;

    @Column(name = "stripe_customer_id")
    private String stripeCustomerId;

    @Column(name = "commissions_enabled", nullable = false)
    private boolean commissionsEnabled;

    private LocalDateTime gracePeriodEndsAt;

    // --- CAMPOS DE AUDITORIA (ESSENCIAIS PARA SAAS) ---

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "time_zone")
    private String timeZone;

    @PrePersist
    protected void onCreate() {
        if (this.subscriptionStatus == null) {
            this.subscriptionStatus = "TRIAL";
            this.trialEndsAt = LocalDateTime.now().plusDays(15); // ✨ Garante consistência
        }
    }
}