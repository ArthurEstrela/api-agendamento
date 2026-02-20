package com.stylo.api_agendamento.adapters.outbound.persistence.user;

import com.stylo.api_agendamento.core.domain.User;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserMapper {

    User toDomain(UserEntity entity);

    UserEntity toEntity(User domain);
}