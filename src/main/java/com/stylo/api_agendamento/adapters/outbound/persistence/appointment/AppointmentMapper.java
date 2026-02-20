package com.stylo.api_agendamento.adapters.outbound.persistence.appointment;

import com.stylo.api_agendamento.adapters.outbound.persistence.service.ServiceMapper;
import com.stylo.api_agendamento.core.domain.Appointment;
import com.stylo.api_agendamento.core.domain.vo.ClientPhone;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(
        componentModel = "spring", 
        unmappedTargetPolicy = ReportingPolicy.IGNORE, // ✨ RESOLVE OS ERROS DE PROPRIEDADES NÃO MAPEADAS (totalPrice, paid, etc)
        uses = { ServiceMapper.class }
)
public interface AppointmentMapper {

    @Mapping(target = "clientPhone", source = "clientPhone.value")
    @Mapping(target = "isPersonalBlock", source = "personalBlock")
    @Mapping(target = "items", source = "products") 
    AppointmentEntity toEntity(Appointment domain);

    @Mapping(target = "clientPhone", source = "clientPhone")
    @Mapping(target = "isPersonalBlock", source = "personalBlock")
    @Mapping(target = "products", source = "items")
    Appointment toDomain(AppointmentEntity entity);

    // ✨ RESOLVE O ERRO DA LISTA: Ensina o MapStruct a converter o Record do Item para a Entidade JPA
    @Mapping(target = "id", ignore = true) // Ignora porque o banco gera esse ID sozinho
    @Mapping(target = "appointment.id", ignore = true) // Ignora para não dar loop infinito no Hibernate
    AppointmentItemEntity toItemEntity(Appointment.AppointmentItem item);

    // O MapStruct é inteligente o suficiente para usar este método 
    // automaticamente sempre que precisar converter String para ClientPhone.
    default ClientPhone mapToClientPhone(String phone) {
        return phone != null && !phone.isBlank() ? new ClientPhone(phone) : null;
    }
}