package com.stylo.api_agendamento.core.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum UserPermission {

    // --- AGENDAMENTOS ---
    APPOINTMENT_READ("appointment:read"),           // Ver agendamentos (próprios ou públicos)
    APPOINTMENT_WRITE("appointment:write"),         // Criar/Editar agendamentos simples
    APPOINTMENT_READ_ALL("appointment:read_all"),   // Ver agenda de TODOS (Recepção/Gerente)
    APPOINTMENT_MANAGE_ALL("appointment:manage_all"), // Mover/Cancelar agenda de qualquer um

    // --- CLIENTES ---
    CLIENT_READ("client:read"),     // Ver lista de clientes
    CLIENT_WRITE("client:write"),   // Cadastrar/Editar clientes

    // --- FINANCEIRO ---
    FINANCIAL_READ("financial:read"),    // Ver dashboard e relatórios
    FINANCIAL_WRITE("financial:write"),  // Alterar conta bancária, solicitar saque

    // --- ESTABELECIMENTO & CONFIGURAÇÕES ---
    SETTINGS_READ("settings:read"),     // Ver configurações do estabelecimento
    SETTINGS_WRITE("settings:write"),   // Alterar logo, horário de funcionamento, serviços
    
    // --- GESTÃO DE EQUIPE ---
    TEAM_READ("team:read"),    // Ver lista de profissionais
    TEAM_WRITE("team:write");  // Cadastrar/Demitir profissionais

    private final String permission;
}