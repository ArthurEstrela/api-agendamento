package com.stylo.api_agendamento.core.usecases;

import com.stylo.api_agendamento.core.common.UseCase;
import com.stylo.api_agendamento.core.domain.Appointment;
import com.stylo.api_agendamento.core.domain.Payout;
import com.stylo.api_agendamento.core.exceptions.BusinessException;
import com.stylo.api_agendamento.core.ports.IAppointmentRepository;
import com.stylo.api_agendamento.core.ports.IFinancialRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Slf4j
@UseCase
@RequiredArgsConstructor
public class CloseProfessionalPeriodUseCase {

    private final IAppointmentRepository appointmentRepository;
    private final IFinancialRepository financialRepository;

    /**
     * Consolida as comissões pendentes de um profissional e gera o fechamento (Payout).
     */
    @Transactional
    public Payout execute(UUID professionalId) {
        // 1. Busca agendamentos concluídos e não liquidados (comissionSettled = false)
        List<Appointment> pendingAppointments = appointmentRepository.findPendingSettlementByProfessional(professionalId);

        if (pendingAppointments.isEmpty()) {
            log.info("Nenhuma comissão pendente encontrada para o profissional {}", professionalId);
            throw new BusinessException("Não há comissões pendentes para realizar o fechamento deste profissional.");
        }

        // 2. Consolida o valor total e IDs
        BigDecimal totalToPay = pendingAppointments.stream()
                .map(Appointment::getProfessionalCommission)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<UUID> appointmentIds = pendingAppointments.stream()
                .map(Appointment::getId)
                .toList();

        UUID serviceProviderId = pendingAppointments.get(0).getServiceProviderId();

        // 3. Cria a Entidade Payout usando o Factory de Domínio (Garante UUID e Status PENDING inicial)
        Payout payout = Payout.create(
                professionalId,
                serviceProviderId,
                totalToPay,
                appointmentIds
        );

        // Como o fechamento manual geralmente ocorre após o pagamento ter sido feito (ou via transferência), 
        // marcamos como pago imediatamente neste fluxo simples.
        payout.markAsPaid("MANUAL-SETTLEMENT-" + UUID.randomUUID().toString().substring(0, 8));

        // 4. Marca os agendamentos como liquidados (Proteção contra re-processamento)
        pendingAppointments.forEach(appt -> {
            appt.markCommissionAsSettled();
            appointmentRepository.save(appt);
        });

        // 5. Persiste o Payout no histórico financeiro
        Payout savedPayout = financialRepository.savePayout(payout);

        log.info("💰 Fechamento concluído. Profissional: {} | Total: R$ {} | Agendamentos: {}", 
                professionalId, totalToPay, appointmentIds.size());

        return savedPayout;
    }
}