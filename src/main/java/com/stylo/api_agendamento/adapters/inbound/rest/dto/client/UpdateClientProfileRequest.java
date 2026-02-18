package com.stylo.api_agendamento.adapters.inbound.rest.dto.client;

import jakarta.validation.constraints.Size;

public record UpdateClientProfileRequest(
    @Size(min = 2, message = "Nome deve ter pelo menos 2 caracteres")
    String name,
    
    @Size(min = 10, max = 11, message = "Telefone deve ter 10 ou 11 dígitos (apenas números)")
    String phone
) {}