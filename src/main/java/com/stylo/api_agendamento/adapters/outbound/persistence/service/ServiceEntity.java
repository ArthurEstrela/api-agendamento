package com.stylo.api_agendamento.adapters.outbound.persistence.service;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

import com.stylo.api_agendamento.adapters.outbound.persistence.serviceProvider.ServiceProviderEntity;

@Entity
@Table(name = "services")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ServiceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    private BigDecimal price;

    @Column(nullable = false)
    private Integer duration; // em minutos, para o calculateEndTime do core

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_provider_id")
    private ServiceProviderEntity serviceProvider;
}