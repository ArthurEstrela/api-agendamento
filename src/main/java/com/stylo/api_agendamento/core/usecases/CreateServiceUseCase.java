package com.stylo.api_agendamento.core.usecases;

import com.stylo.api_agendamento.core.domain.Service;
import com.stylo.api_agendamento.core.ports.IServiceRepository;
import com.stylo.api_agendamento.core.exceptions.BusinessException;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;

@RequiredArgsConstructor
public class CreateServiceUseCase {

    private final IServiceRepository serviceRepository;

    public Service execute(CreateServiceInput input) {
        // 1. Regra de Negócio: Criar a instância de domínio validada
        Service service = Service.create(
                input.name(),
                input.duration(),
                input.price(),
                input.providerId()
        );

        // 2. Persistência através da porta (Port)
        return serviceRepository.save(service);
    }

    public record CreateServiceInput(
            String name,
            Integer duration,
            BigDecimal price,
            String providerId
    ) {}
}