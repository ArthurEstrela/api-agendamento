package com.stylo.api_agendamento.adapters.outbound.persistence;

import com.stylo.api_agendamento.core.domain.vo.PaymentMethod;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "service_providers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServiceProviderEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String businessName;

    @Embedded
    private AddressVo businessAddress;

    @Embedded // Agora usando o VO estruturado
    private DocumentVo document;

    private String businessPhone;

    @Column(unique = true, nullable = false)
    private String publicProfileSlug;

    private String logoUrl;
    private String bannerUrl;
    private String pixKey;
    private String pixKeyType;

    @ElementCollection(targetClass = PaymentMethod.class)
    @CollectionTable(name = "provider_payment_methods", joinColumns = @JoinColumn(name = "provider_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method")
    private List<PaymentMethod> paymentMethods;

    private Integer cancellationMinHours;

    @Column(nullable = false)
    private String subscriptionStatus;
}