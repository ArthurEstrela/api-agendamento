package com.stylo.api_agendamento.core.usecases;

import com.stylo.api_agendamento.core.common.UseCase;
import com.stylo.api_agendamento.core.domain.Professional;
import com.stylo.api_agendamento.core.domain.RemunerationType;
import com.stylo.api_agendamento.core.domain.events.AuditLogEvent;
import com.stylo.api_agendamento.core.exceptions.BusinessException;
import com.stylo.api_agendamento.core.ports.IEventPublisher;
import com.stylo.api_agendamento.core.ports.IProfessionalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class UpdateProfessionalCommissionUseCase {

    private final IProfessionalRepository professionalRepository;
    private final IEventPublisher eventPublisher;

    public void execute(UpdateCommissionInput input) {
        // 1. Busca e valida o Solicitante
        Professional requester = professionalRepository.findById(input.requesterId())
                .orElseThrow(() -> new BusinessException("Solicitante não encontrado."));

        if (!requester.isOwner()) {
            throw new BusinessException("Apenas o proprietário pode alterar as configurações de comissão.");
        }

        // 2. Busca o Profissional Alvo
        Professional targetProfessional = professionalRepository.findById(input.targetProfessionalId())
                .orElseThrow(() -> new BusinessException("Profissional alvo não encontrado."));

        // 3. Validação de Segurança
        if (!targetProfessional.getServiceProviderId().equals(requester.getServiceProviderId())) {
            throw new BusinessException("Você não tem permissão para alterar profissionais de outro estabelecimento.");
        }

        // 4. ✨ Captura o Estado ANTERIOR para Auditoria
        // CORREÇÃO: Usamos getRemunerationValue() pois é o nome do campo na Entidade
        BigDecimal currentCommission = targetProfessional.getRemunerationValue(); 
        
        String oldValueFormatted = formatCommission(
                targetProfessional.getRemunerationType(), 
                currentCommission
        );

        String newValueFormatted = formatCommission(input.type(), input.value());

        // 5. Otimização
        if (oldValueFormatted.equals(newValueFormatted)) {
            return; 
        }

        // 6. Aplica a alteração
        targetProfessional.updateCommissionSettings(input.type(), input.value());

        // 7. Persiste
        professionalRepository.save(targetProfessional);

        // 8. Publica Auditoria
        AuditLogEvent auditEvent = AuditLogEvent.builder()
                .entityName("Professional")
                .entityId(targetProfessional.getId())
                .action("UPDATE_COMMISSION")
                .fieldName("commissionSettings")
                .oldValue(oldValueFormatted)
                .newValue(newValueFormatted)
                .modifiedBy(input.requesterId())
                .timestamp(LocalDateTime.now())
                .build();

        eventPublisher.publish(auditEvent);
        
        log.info("Comissão do profissional {} alterada por {}. Antes: {} -> Depois: {}", 
                targetProfessional.getId(), input.requesterId(), oldValueFormatted, newValueFormatted);
    }

    private String formatCommission(RemunerationType type, BigDecimal value) {
        if (type == null || value == null) return "N/A";
        return String.format("%s (%s)", type.name(), value.toPlainString());
    }

    public record UpdateCommissionInput(
            String requesterId,
            String targetProfessionalId,
            RemunerationType type,
            BigDecimal value
    ) {}
}