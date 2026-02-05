package com.stylo.api_agendamento.adapters.outbound.persistence.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.SubclassMapping;
import com.stylo.api_agendamento.adapters.outbound.persistence.UserEntity;
import com.stylo.api_agendamento.adapters.outbound.persistence.ClientEntity;
import com.stylo.api_agendamento.adapters.outbound.persistence.ProfessionalEntity;
import com.stylo.api_agendamento.core.domain.User;
import com.stylo.api_agendamento.core.domain.Client;
import com.stylo.api_agendamento.core.domain.Professional;

@Mapper(componentModel = "spring")
public interface UserMapper {

    // Converte de Entidade para Domínio (O Hibernate já traz a instância correta)
    User toDomain(UserEntity entity);
    
    // Converte do Domínio para Entidade (Aqui resolvemos o erro do abstract)
    @SubclassMapping(source = Client.class, target = ClientEntity.class)
    @SubclassMapping(source = Professional.class, target = ProfessionalEntity.class)
    UserEntity toEntity(User domain);
}