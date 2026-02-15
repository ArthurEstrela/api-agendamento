package com.stylo.api_agendamento.core.usecases;

import com.stylo.api_agendamento.core.common.UseCase;
import com.stylo.api_agendamento.core.domain.Service;
import com.stylo.api_agendamento.core.ports.IServiceRepository;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;

@UseCase
@RequiredArgsConstructor
public class CreateServiceUseCase {

    private final IServiceRepository serviceRepository;

    public Service execute(CreateServiceInput input) {
        // Certifique-se de que o método Service.create suporta esses parâmetros
        Service service = Service.create(
                input.name(),
                input.description(), // Adicionado
                input.duration(),
                input.price(),
                input.categoryId() // Alinhado com o input
        );

        return serviceRepository.save(service);
    }

    // Record atualizado para aceitar os 5 parâmetros do Controller
    public record CreateServiceInput(
            String name,
            String description,
            Integer duration,
            BigDecimal price,
            String categoryId
    ) {}
}