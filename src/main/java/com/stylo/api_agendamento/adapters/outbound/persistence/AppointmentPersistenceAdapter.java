package com.stylo.api_agendamento.adapters.outbound.persistence;

import com.stylo.api_agendamento.core.domain.Appointment;
import com.stylo.api_agendamento.core.ports.IAppointmentRepository;
import com.stylo.api_agendamento.adapters.outbound.persistence.mapper.AppointmentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class AppointmentPersistenceAdapter implements IAppointmentRepository {

    private final SpringDataAppointmentRepository springRepository;
    private final AppointmentMapper mapper; // Injeção do Mapper gerado pelo MapStruct

    @Override
    public Appointment save(Appointment appointment) {
        AppointmentEntity entity = mapper.toEntity(appointment);
        AppointmentEntity savedEntity = springRepository.save(entity);
        return mapper.toDomain(savedEntity);
    }

    @Override
    public Optional<Appointment> findById(String id) {
        return springRepository.findById(id)
                .map(mapper::toDomain);
    }
}