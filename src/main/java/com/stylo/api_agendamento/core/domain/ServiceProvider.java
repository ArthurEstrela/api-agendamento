package com.stylo.api_agendamento.core.domain;

import java.util.List;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ServiceProvider {
    private String id;
    private String businessName;
    private String cnpj;
    private String publicProfileSlug;
    private Address address; // Objeto de valor (Value Object)
    private List<Service> services;
    private String subscriptionStatus; // active, trialing, etc.
}