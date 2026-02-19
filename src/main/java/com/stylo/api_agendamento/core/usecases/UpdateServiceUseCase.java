package com.stylo.api_agendamento.core.usecases;

import com.stylo.api_agendamento.core.common.UseCase;
import com.stylo.api_agendamento.core.domain.Service;
import com.stylo.api_agendamento.core.exceptions.BusinessException; // ✨ CORREÇÃO 1: Import adicionado
import com.stylo.api_agendamento.core.exceptions.EntityNotFoundException;
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
            log.warn("Tentativa de alteração cross-tenant! Usuário do provider {} tentou alterar serviço {}", providerId, service.getId());
            throw new BusinessException("Acesso negado: Este serviço não pertence ao seu estabelecimento.");
        }

        // 3. Atualiza os detalhes no Domínio (Validações de preço e duração > 0)
        // ✨ CORREÇÃO 2: O método na Entidade chama-se 'update'
        service.update(
                input.name(),
                input.description(),
                input.duration(),
                input.price()
        );

        Service savedService = serviceRepository.save(service);
        
        log.info("Serviço '{}' atualizado com sucesso.", savedService.getName());
        
        return savedService;
    }

    public record Input(
            UUID id,
            String name,
            String description,
            Integer duration,
            BigDecimal price
    ) {}
}