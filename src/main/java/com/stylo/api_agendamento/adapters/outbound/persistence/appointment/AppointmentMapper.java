package com.stylo.api_agendamento.adapters.outbound.persistence.appointment;

import com.stylo.api_agendamento.adapters.outbound.persistence.service.ServiceMapper;
import com.stylo.api_agendamento.adapters.outbound.persistence.product.ProductMapper; // Se tiver mapper de produto
import com.stylo.api_agendamento.core.domain.Appointment;
import com.stylo.api_agendamento.core.domain.vo.ClientPhone;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring", uses = { ServiceMapper.class })
public interface AppointmentMapper {

    @Mapping(target = "clientPhone", source = "clientPhone.value")
    @Mapping(target = "isPersonalBlock", source = "personalBlock")
    // Mapeia a lista de itens de produto (Você precisará criar AppointmentItemEntity ou usar @ElementCollection no JPA)
    @Mapping(target = "items", source = "products") 
    AppointmentEntity toEntity(Appointment domain);

    @Mapping(target = "clientPhone", source = "clientPhone", qualifiedByName = "mapPhone")
    @Mapping(target = "isPersonalBlock", source = "personalBlock")
    @Mapping(target = "products", source = "items")
    Appointment toDomain(AppointmentEntity entity);

    @Named("mapPhone")
    default ClientPhone mapPhone(String phone) {
        return phone != null ? new ClientPhone(phone) : null;
    }
    
    // Mapeamento simples para a classe estática interna
    // Se o MapStruct reclamar, você pode definir métodos manuais aqui para converter AppointmentItem <-> AppointmentItemEntity
}