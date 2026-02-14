package com.stylo.api_agendamento.core.usecases;

import com.stylo.api_agendamento.core.domain.Appointment;
import com.stylo.api_agendamento.core.domain.Payout;
import com.stylo.api_agendamento.core.exceptions.BusinessException;
import com.stylo.api_agendamento.core.ports.IAppointmentRepository;
import com.stylo.api_agendamento.core.ports.IFinancialRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.LocalDateTime; // ✨ Import necessário
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class CloseProfessionalPeriodUseCase {

    private final IAppointmentRepository appointmentRepository;
    private final IFinancialRepository financialRepository;

    public Payout execute(String professionalId) {
        // 1. Busca agendamentos concluídos e não liquidados do profissional
        List<Appointment> pendingAppointments = appointmentRepository.findPendingSettlementByProfessional(professionalId);

        if (pendingAppointments.isEmpty()) {
            throw new BusinessException("Não há comissões pendentes para este profissional.");
        }

        // 2. Consolida o valor total
        // SINTAXE CORRIGIDA: BigDecimal::add é uma referência de método, não um campo.
        BigDecimal totalToPay = pendingAppointments.stream()
                .map(Appointment::getProfessionalCommission)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 3. Cria o registro de Payout (Snapshot do fechamento)
        Payout payout = Payout.builder()
                .professionalId(professionalId)
                .serviceProviderId(pendingAppointments.get(0).getServiceProviderId())
                .totalAmount(totalToPay)
                .appointmentIds(pendingAppointments.stream().map(Appointment::getId).toList())
                .processedAt(LocalDateTime.now()) // ✨ Agora o LocalDateTime funciona
                .status("PAID")
                .build();

        // 4. Marca agendamentos como liquidados para evitar pagamento duplo
        pendingAppointments.forEach(appt -> {
            appt.markCommissionAsSettled();
            appointmentRepository.save(appt);
        });

        log.info("💰 Fechamento de período realizado para o profissional {}. Repasse total: R${}", 
                professionalId, totalToPay);

        // 5. Salva o Payout no repositório financeiro
        return financialRepository.savePayout(payout);
    }
}