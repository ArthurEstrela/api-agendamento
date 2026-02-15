package com.stylo.api_agendamento.core.usecases;

import com.stylo.api_agendamento.core.common.UseCase;
import com.stylo.api_agendamento.core.domain.Professional;
import com.stylo.api_agendamento.core.domain.RemunerationType;
import com.stylo.api_agendamento.core.exceptions.BusinessException;
import com.stylo.api_agendamento.core.ports.IProfessionalRepository;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;

@UseCase
@RequiredArgsConstructor
public class UpdateProfessionalCommissionUseCase {

    private final IProfessionalRepository professionalRepository;

    public void execute(UpdateCommissionInput input) {
        // 1. Busca o profissional que está realizando a alteração (o solicitante)
        Professional requester = professionalRepository.findById(input.requesterId())
                .orElseThrow(() -> new BusinessException("Solicitante não encontrado."));

        // 2. Valida se o solicitante é o dono do salão
        if (!requester.isOwner()) {
            throw new BusinessException("Apenas o proprietário pode alterar as configurações de comissão.");
        }

        // 3. Busca o profissional que terá a comissão alterada
        Professional targetProfessional = professionalRepository.findById(input.targetProfessionalId())
                .orElseThrow(() -> new BusinessException("Profissional não encontrado."));

        // 4. Valida se ambos pertencem ao mesmo estabelecimento para segurança
        if (!targetProfessional.getServiceProviderId().equals(requester.getServiceProviderId())) {
            throw new BusinessException("Você não tem permissão para alterar profissionais de outro estabelecimento.");
        }

        // 5. Aplica a alteração usando o método de domínio
        targetProfessional.updateCommissionSettings(input.type(), input.value());

        // 6. Persiste a alteração
        professionalRepository.save(targetProfessional);
    }

    public record UpdateCommissionInput(
            String requesterId,           // ID do profissional que está logado
            String targetProfessionalId,  // ID de quem vai receber a nova comissão
            RemunerationType type,
            BigDecimal value
    ) {}
}