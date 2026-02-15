package com.stylo.api_agendamento.core.usecases;

import com.stylo.api_agendamento.core.common.UseCase;
import com.stylo.api_agendamento.core.domain.Service;
import com.stylo.api_agendamento.core.exceptions.BusinessException;
import com.stylo.api_agendamento.core.ports.IServiceRepository;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;

@UseCase
@RequiredArgsConstructor
public class UpdateServiceUseCase {

    private final IServiceRepository serviceRepository;

    public Service execute(UpdateServiceInput input) {
        // 1. Busca o serviço existente
        Service service = serviceRepository.findById(input.id())
                .orElseThrow(() -> new BusinessException("Serviço não encontrado."));

        // 2. Atualiza os detalhes (o domínio valida duração > 0 e preço)
        service.updateDetails(
                input.name(),
                input.description(),
                input.duration(),
                input.price()
        );

        // 3. Salva as alterações através do port
        return serviceRepository.save(service);
    }

    public record UpdateServiceInput(
            String id,
            String name,
            String description,
            Integer duration,
            BigDecimal price
    ) {}
}