package com.stylo.api_agendamento.adapters.outbound.persistence.mapper;

import org.mapstruct.Mapper;

import com.stylo.api_agendamento.core.domain.Client;
import com.stylo.api_agendamento.core.domain.Professional;
import com.stylo.api_agendamento.core.domain.User;

@Mapper(componentModel = "spring")
public interface UserMapper {
    // Converte de Entidade do Banco para o Domínio puro
    User toDomain(UserEntity entity);
    
    // Converte do Domínio para a Entidade do Banco (JPA)
    UserEntity toEntity(User domain);
    
    // Mapeamentos específicos para perfis
    Client toClientDomain(UserEntity entity);
    Professional toProfessionalDomain(UserEntity entity);
}