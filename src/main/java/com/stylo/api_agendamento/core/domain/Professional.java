package com.stylo.api_agendamento.core.domain;

import java.util.List;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class Professional extends User {
    private String serviceProviderId;
    private String bio;
    private List<Service> services;
}