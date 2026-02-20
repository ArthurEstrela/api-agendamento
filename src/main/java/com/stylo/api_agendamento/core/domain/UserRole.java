package com.stylo.api_agendamento.core.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.stylo.api_agendamento.core.domain.UserPermission.*;

@Getter
@RequiredArgsConstructor
public enum UserRole {
    CLIENT(Set.of(
            APPOINTMENT_READ, 
            APPOINTMENT_WRITE
    )),

    PROFESSIONAL(Set.of(
            APPOINTMENT_READ,       // Vê a própria agenda
            APPOINTMENT_WRITE,      // Gere a própria agenda
            CLIENT_READ             // Vê histórico básico dos clientes que atende
    )),

    RECEPTIONIST(Set.of(
            APPOINTMENT_READ, 
            APPOINTMENT_WRITE,
            APPOINTMENT_READ_ALL,   // ✨ Vê agenda de todos
            APPOINTMENT_MANAGE_ALL, // ✨ Gerencia agenda de todos
            CLIENT_READ, 
            CLIENT_WRITE,           // Cadastra clientes novos
            TEAM_READ               // Vê quem são os profissionais
            // ❌ SEM ACESSO FINANCEIRO OU CONFIGURAÇÕES CRÍTICAS
    )),

    MANAGER(Set.of(
            APPOINTMENT_READ, APPOINTMENT_WRITE, APPOINTMENT_READ_ALL, APPOINTMENT_MANAGE_ALL,
            CLIENT_READ, CLIENT_WRITE,
            TEAM_READ, TEAM_WRITE,       // ✨ Pode gerenciar a equipe
            SETTINGS_READ, SETTINGS_WRITE, // ✨ Pode mudar horários e serviços
            FINANCIAL_READ               // ✨ Vê relatórios, mas não saca dinheiro (opcional)
    )),

    SERVICE_PROVIDER(Set.of(
            // ✨ O DONO TEM TUDO
            APPOINTMENT_READ, APPOINTMENT_WRITE, APPOINTMENT_READ_ALL, APPOINTMENT_MANAGE_ALL,
            CLIENT_READ, CLIENT_WRITE,
            FINANCIAL_READ, FINANCIAL_WRITE,
            SETTINGS_READ, SETTINGS_WRITE,
            TEAM_READ, TEAM_WRITE
    ));

    private final Set<UserPermission> permissions;

    public List<SimpleGrantedAuthority> getAuthorities() {
        var authorities = getPermissions()
                .stream()
                .map(permission -> new SimpleGrantedAuthority(permission.getPermission()))
                .collect(Collectors.toList());
        
        // Adiciona também o ROLE_Nome para compatibilidade com códigos legados que usem .hasRole()
        authorities.add(new SimpleGrantedAuthority("ROLE_" + this.name()));
        
        return authorities;
    }
}