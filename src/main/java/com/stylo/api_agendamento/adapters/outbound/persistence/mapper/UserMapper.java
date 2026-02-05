package com.stylo.api_agendamento.adapters.outbound.persistence.mapper;

import org.mapstruct.Mapper;
import com.stylo.api_agendamento.adapters.outbound.persistence.UserEntity;
import com.stylo.api_agendamento.core.domain.User;

@Mapper(componentModel = "spring")
public interface UserMapper {
    // Converte a base de autenticação (ID, Email, Senha, Role)
    User toDomain(UserEntity entity);
    
    UserEntity toEntity(User domain);
}