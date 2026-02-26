package com.stylo.api_agendamento.core.usecases;

import com.stylo.api_agendamento.core.common.UseCase;
import com.stylo.api_agendamento.core.domain.Service;
import com.stylo.api_agendamento.core.exceptions.BusinessException;
import com.stylo.api_agendamento.core.exceptions.EntityNotFoundException;
import com.stylo.api_agendamento.core.ports.IServiceRepository;
import com.stylo.api_agendamento.core.ports.IUserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@UseCase
@RequiredArgsConstructor
public class DeleteServiceUseCase {

    private final IServiceRepository serviceRepository;
    private final IUserContext userContext;

    @Transactional
    public Void execute(UUID serviceId) {
        // 1. Busca o serviço no banco de dados
        Service service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new EntityNotFoundException("Serviço não encontrado."));

        // 2. Segurança de Multi-Tenant (SaaS): Pega o ID do estabelecimento logado
        UUID loggedProviderId = userContext.getCurrentUser().getProviderId();

        // 3. Verifica se o serviço pertence realmente a este estabelecimento
        if (!service.getServiceProviderId().equals(loggedProviderId)) {
            log.warn("Tentativa de exclusão negada. Usuário {} tentou excluir o serviço {} que pertence ao provider {}.", 
                    userContext.getCurrentUser().getId(), serviceId, service.getServiceProviderId());
            throw new BusinessException("Você não tem permissão para excluir um serviço de outro estabelecimento.");
        }

        // 4. Executa a exclusão (Verifique se no seu IServiceRepository o método é delete(UUID) ou delete(Service))
        serviceRepository.delete(serviceId);
        
        // 5. Log de auditoria
        log.info("Serviço '{}' (ID: {}) excluído com sucesso pelo estabelecimento {}.", 
                service.getName(), serviceId, loggedProviderId);

        return null; // Void maiúsculo exige retorno nulo
    }
}