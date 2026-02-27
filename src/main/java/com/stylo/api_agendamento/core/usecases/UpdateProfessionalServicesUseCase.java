package com.stylo.api_agendamento.core.usecases;

import com.stylo.api_agendamento.core.common.UseCase;
import com.stylo.api_agendamento.core.domain.Professional;
import com.stylo.api_agendamento.core.domain.Service;
import com.stylo.api_agendamento.core.exceptions.EntityNotFoundException;
import com.stylo.api_agendamento.core.ports.IProfessionalRepository;
import com.stylo.api_agendamento.core.ports.IServiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@UseCase
@RequiredArgsConstructor
public class UpdateProfessionalServicesUseCase {

    private final IProfessionalRepository professionalRepository;
    private final IServiceRepository serviceRepository;

    @Transactional
    public void execute(UUID professionalId, List<UUID> serviceIds) {
        // 1. Busca o profissional
        Professional professional = professionalRepository.findById(professionalId)
                .orElseThrow(() -> new EntityNotFoundException("Profissional não encontrado"));

        // 2. Busca todos os serviços que vieram no Array do Frontend
        List<Service> newServices = serviceRepository.findAllByIds(serviceIds);

        // 3. Atualiza os serviços no domínio
        professional.updateServices(newServices);

        // 4. Salva no banco (O Hibernate JPA vai cuidar de deletar e inserir na tabela `professional_services` automaticamente)
        professionalRepository.save(professional);
    }
}