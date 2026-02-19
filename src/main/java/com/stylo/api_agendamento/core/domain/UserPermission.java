package com.stylo.api_agendamento.core.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum UserPermission {

    // --- AGENDAMENTOS ---
    APPOINTMENT_READ("appointment:read"),          // Ver agendamentos (próprios)
    APPOINTMENT_WRITE("appointment:write"),        // Criar/Editar agendamentos simples
    APPOINTMENT_READ_ALL("appointment:read_all"),   // Ver agenda de TODOS (Recepção/Gerente)
    APPOINTMENT_MANAGE_ALL("appointment:manage_all"), // Mover/Cancelar agenda de qualquer um

    // --- LISTA DE ESPERA ---
    WAITLIST_READ("waitlist:read"),
    WAITLIST_WRITE("waitlist:write"),

    // --- CLIENTES ---
    CLIENT_READ("client:read"),     // Ver lista de clientes
    CLIENT_WRITE("client:write"),   // Cadastrar/Editar clientes e ver histórico

    // --- FINANCEIRO ---
    FINANCIAL_READ("financial:read"),    // Ver dashboard e relatórios
    FINANCIAL_WRITE("financial:write"),  // Alterar conta bancária, solicitar saque, lançar despesas

    // --- MARKETING (CUPONS & REVIEWS) ---
    MARKETING_READ("marketing:read"),
    MARKETING_WRITE("marketing:write"), // Criar cupons
    REVIEW_MODERATE("review:moderate"), // Responder ou ocultar avaliações

    // --- ESTABELECIMENTO & CONFIGURAÇÕES ---
    SETTINGS_READ("settings:read"),     // Ver configurações do estabelecimento
    SETTINGS_WRITE("settings:write"),   // Alterar logo, horário de funcionamento, serviços
    
    // --- GESTÃO DE EQUIPE ---
    TEAM_READ("team:read"),    // Ver lista de profissionais
    TEAM_WRITE("team:write");  // Cadastrar/Demitir profissionais e definir comissões

    private final String permission;
}