package com.stylo.api_agendamento.core.usecases;

import com.stylo.api_agendamento.core.common.UseCase;
import com.stylo.api_agendamento.core.domain.Service;
import com.stylo.api_agendamento.core.ports.IServiceRepository;
import com.stylo.api_agendamento.core.ports.IUserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@UseCase
@RequiredArgsConstructor
public class CreateServiceUseCase {

    private final IServiceRepository serviceRepository;
    private final IUserContext userContext;

    @Transactional
    public Service execute(Input input) {
        // Se o providerId não vier no input, usamos o do usuário logado (Dono do Salão)
        UUID providerId = input.serviceProviderId() != null 
                ? input.serviceProviderId() 
                : userContext.getCurrentUser().getProviderId();

        // Criação usando a Factory de Domínio para garantir integridade.
        // ✨ CORREÇÃO: Ordem dos parâmetros ajustada para (providerId, name, description, duration, price)
        Service service = Service.create(
                providerId,
                input.name(),
                input.description(),
                input.duration(),
                input.price()
        );

        // Se houver uma categoria específica vinculada
        if (input.categoryId() != null) {
            service = service.toBuilder()
                    .categoryId(input.categoryId()) 
                    .build(); 
        }

        Service savedService = serviceRepository.save(service);
        
        log.info("Serviço '{}' criado com sucesso para o estabelecimento {}.", 
                savedService.getName(), providerId);

        return savedService;
    }

    public record Input(
            String name,
            String description,
            Integer duration,
            BigDecimal price,
            UUID categoryId,
            UUID serviceProviderId // Opcional, se for criado por um Admin Master
    ) {}
}