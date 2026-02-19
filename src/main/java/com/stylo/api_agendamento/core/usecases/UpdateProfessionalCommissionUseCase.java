package com.stylo.api_agendamento.core.usecases;

import com.stylo.api_agendamento.core.common.UseCase;
import com.stylo.api_agendamento.core.domain.Professional;
import com.stylo.api_agendamento.core.domain.RemunerationType;
import com.stylo.api_agendamento.core.domain.events.AuditLogEvent;
import com.stylo.api_agendamento.core.exceptions.BusinessException;
import com.stylo.api_agendamento.core.exceptions.EntityNotFoundException;
import com.stylo.api_agendamento.core.ports.IEventPublisher;
import com.stylo.api_agendamento.core.ports.IProfessionalRepository;
import com.stylo.api_agendamento.core.ports.IUserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@UseCase
@RequiredArgsConstructor
public class UpdateProfessionalCommissionUseCase {

    private final IProfessionalRepository professionalRepository;
    private final IEventPublisher eventPublisher;
    private final IUserContext userContext;

    @Transactional
    public void execute(Input input) {
        // 1. Identifica quem está operando (Dono do Estabelecimento) via Contexto Seguro
        UUID requesterId = userContext.getCurrentUserId();
        Professional requester = professionalRepository.findById(requesterId)
                .orElseThrow(() -> new BusinessException("Acesso negado: Solicitante não identificado."));

        if (!requester.isOwner()) {
            throw new BusinessException("Permissão negada: Apenas proprietários podem alterar regras de comissão.");
        }

        // 2. Busca o Profissional Alvo
        Professional target = professionalRepository.findById(input.targetProfessionalId())
                .orElseThrow(() -> new EntityNotFoundException("Profissional alvo não encontrado."));

        // 3. ✨ Segurança Multi-Tenant (Proteção contra Cross-Site Scripting/Injeção de ID)
        if (!target.getServiceProviderId().equals(requester.getServiceProviderId())) {
            log.error("Tentativa de quebra de segurança: {} tentou alterar comissão de profissional externo {}", 
                    requesterId, target.getId());
            throw new BusinessException("Você não tem permissão para gerenciar profissionais de outros estabelecimentos.");
        }

        // 4. Captura Estado Atual para Auditoria Detalhada
        String oldValue = formatCommission(target.getRemunerationType(), target.getRemunerationValue());
        String newValue = formatCommission(input.type(), input.value());

        // Se não houver mudança real, encerramos para evitar logs desnecessários
        if (oldValue.equals(newValue)) return;

        // 5. Aplica alteração no Domínio
        target.updateCommissionSettings(input.type(), input.value());
        professionalRepository.save(target);

        // 6. Publica Evento de Auditoria (Essencial para Compliance Financeiro)
        publishAuditLog(target, requesterId, oldValue, newValue);

        log.info("Comissão atualizada para {}: {} -> {}", target.getName(), oldValue, newValue);
    }

    private void publishAuditLog(Professional target, UUID modifiedBy, String oldVal, String newVal) {
        eventPublisher.publish(AuditLogEvent.createUpdate(
                "Professional",
                target.getId(),
                target.getServiceProviderId(),
                "remunerationSettings",
                oldVal,
                newVal,
                modifiedBy
        ));
    }

    private String formatCommission(RemunerationType type, BigDecimal value) {
        if (type == null || value == null) return "NOT_SET";
        String symbol = (type == RemunerationType.PERCENTAGE) ? "%" : "R$";
        return symbol + " " + value.toPlainString();
    }

    public record Input(
            UUID targetProfessionalId,
            RemunerationType type,
            BigDecimal value
    ) {}
}