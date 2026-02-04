package com.stylo.api_agendamento.adapters.outbound.persistence;

import java.time.LocalDateTime;

import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

import com.stylo.api_agendamento.core.domain.Appointment;
import com.stylo.api_agendamento.core.ports.IAppointmentRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AppointmentPersistenceAdapter implements IAppointmentRepository {
    private final SpringDataAppointmentRepository repository;

    @Override
    public Appointment save(Appointment domain) {
        // Aqui usamos um Mapper (ou manualmente) para converter Domain -> Entity
        AppointmentEntity entity = new AppointmentEntity();
        BeanUtils.copyProperties(domain, entity);
        
        AppointmentEntity saved = repository.save(entity);
        
        // Converte de volta Entity -> Domain
        Appointment result = new Appointment();
        BeanUtils.copyProperties(saved, result);
        return result;
    }

    @Override
    public boolean hasConflictingAppointment(String professionalId, LocalDateTime start, LocalDateTime end) {
        return repository.existsByProfessionalIdAndStartTimeBetween(professionalId, start, end);
    }
}