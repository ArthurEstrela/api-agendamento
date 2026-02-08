package com.stylo.api_agendamento.core.domain;

import com.stylo.api_agendamento.core.domain.vo.*;
import com.stylo.api_agendamento.core.exceptions.BusinessException;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Collections;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ServiceProvider {
    private final String id;
    private String businessName;
    private Address businessAddress;

    // Agrupamos o documento em um Value Object
    private final Document document;

    private String businessPhone;
    private Slug publicProfileSlug;
    private final String ownerEmail;

    private String logoUrl;
    private String bannerUrl;
    private String pixKey;
    private String pixKeyType;

    // Usamos o Enum rico de PaymentMethod que criamos antes
    private List<PaymentMethod> paymentMethods;

    private Integer cancellationMinHours;
    private String subscriptionStatus; // "ACTIVE", "TRIAL", "EXPIRED", "CANCELED"

    // Fábrica estática para garantir que o estabelecimento nasça correto
    public static ServiceProvider create(String businessName, Document doc, Slug slug, Address address,
            String ownerEmail) {
        return ServiceProvider.builder()
                .businessName(businessName)
                .document(doc)
                .publicProfileSlug(slug)
                .businessAddress(address)
                .ownerEmail(ownerEmail)
                .subscriptionStatus("TRIAL")
                .cancellationMinHours(2)
                .paymentMethods(Collections.emptyList())
                .build();
    }
    /**
     * Valida se um agendamento ainda pode ser cancelado conforme a política do
     * salão
     */
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

    public void updateSubscription(String newStatus) {
        List<String> validStatuses = List.of("ACTIVE", "TRIAL", "EXPIRED", "CANCELED");
        if (!validStatuses.contains(newStatus)) {
            throw new BusinessException("Status de assinatura inválido.");
        }
        this.subscriptionStatus = newStatus;
    }

    public boolean isSubscriptionActive() {
        return "ACTIVE".equals(this.subscriptionStatus) || "TRIAL".equals(this.subscriptionStatus);
    }
}