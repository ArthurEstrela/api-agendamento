package com.stylo.api_agendamento.adapters.outbound.persistence;

import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "professionals")
@Data
public class ProfessionalEntity {
    @Id
    private String id;
    private String name;
    private String email;
    private String avatarUrl;
    
    @Column(columnDefinition = "TEXT")
    private String bio; // Novo
    
    private String serviceProviderId;
    private boolean isOwner;
    
    @ManyToMany
    @JoinTable(name = "professional_services")
    private List<ServiceEntity> services; // Serviços que o profissional realiza
}