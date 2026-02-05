package com.stylo.api_agendamento.adapters.outbound.persistence;

import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "service_providers")
@Data
public class ServiceProviderEntity {
    @Id
    private String id;
    
    private String businessName;
    
    @Embedded
    private AddressVo businessAddress; // Value Object para morada
    
    private String cnpj;
    private String documentType;
    private String businessPhone;
    
    @Column(unique = true)
    private String publicProfileSlug; // Para a URL de reserva
    
    private String logoUrl;
    private String pixKey;
    private String pixKeyType;
    
    @ElementCollection
    private List<String> paymentMethods; // Ex: ["pix", "credit_card"]
    
    private String subscriptionStatus; // free, active, past_due
}