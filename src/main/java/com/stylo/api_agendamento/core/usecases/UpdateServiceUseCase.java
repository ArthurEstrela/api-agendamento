package com.stylo.api_agendamento.core.usecases;

import com.stylo.api_agendamento.core.common.UseCase;
import com.stylo.api_agendamento.core.domain.Service;
import com.stylo.api_agendamento.core.exceptions.EntityNotFoundException;
import com.stylo.api_agendamento.core.ports.IServiceRepository;
import com.stylo.api_agendamento.core.ports.IUserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@UseCase
@RequiredArgsConstructor
public class UpdateServiceUseCase {

    private final IServiceRepository serviceRepository;
    private final IUserContext userContext;

    @Transactional
    public Service execute(Input input) {
        // 1. Busca o serviço
        Service service = serviceRepository.findById(input.id())
                .orElseThrow(() -> new EntityNotFoundException("Serviço não localizado no catálogo."));

        // 2. Segurança SaaS: Garante que o serviço pertence ao salão do usuário logado
        UUID providerId = userContext.getCurrentUser().getProviderId();
        if (!service.getServiceProviderId().equals(providerId)) {
            throw new BusinessException("Acesso negado: Este serviço não pertence ao seu estabelecimento.");
        }

        // 3. Atualiza os detalhes no Domínio (Validações de preço e duração > 0)
        service.updateDetails(
                input.name(),
                input.description(),
                input.duration(),
                input.price()
        );

        return serviceRepository.save(service);
    }

    public record Input(
            UUID id,
            String name,
            String description,
            Integer duration,
            BigDecimal price
    ) {}
}