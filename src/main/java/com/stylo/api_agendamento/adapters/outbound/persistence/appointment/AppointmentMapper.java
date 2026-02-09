package com.stylo.api_agendamento.adapters.outbound.persistence.appointment;

import com.stylo.api_agendamento.adapters.outbound.persistence.service.ServiceMapper;
import com.stylo.api_agendamento.core.domain.Appointment;
import com.stylo.api_agendamento.core.domain.vo.ClientPhone;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring", uses = { ServiceMapper.class })
public interface AppointmentMapper {
    @Mapping(target = "clientPhone", source = "clientPhone.value")
    @Mapping(target = "isPersonalBlock", source = "personalBlock")
    // MapStruct mapeará professionalCommission e serviceProviderFee automaticamente
    // por terem o mesmo nome
    AppointmentEntity toEntity(Appointment domain);

    @Mapping(target = "clientPhone", source = "clientPhone", qualifiedByName = "mapPhone")
    @Mapping(target = "isPersonalBlock", source = "personalBlock")
    Appointment toDomain(AppointmentEntity entity);

    @Named("mapPhone")
    default ClientPhone mapPhone(String phone) {
        return phone != null ? new ClientPhone(phone) : null;
    }
}