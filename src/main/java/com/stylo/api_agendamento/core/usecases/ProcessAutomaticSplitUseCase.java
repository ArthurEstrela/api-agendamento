package com.stylo.api_agendamento.core.usecases;

import com.stylo.api_agendamento.core.common.UseCase;
import com.stylo.api_agendamento.core.domain.Appointment;
import com.stylo.api_agendamento.core.domain.Professional;
import com.stylo.api_agendamento.core.ports.IAppointmentRepository;
import com.stylo.api_agendamento.core.ports.IProfessionalRepository;
import com.stylo.api_agendamento.core.ports.IPaymentProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@UseCase
@RequiredArgsConstructor
public class ProcessAutomaticSplitUseCase {

    private final IProfessionalRepository professionalRepository;
    private final IAppointmentRepository appointmentRepository;
    private final IPaymentProvider paymentProvider;

    @Transactional
    public void execute(UUID appointmentId) {
        // 1. Busca o agendamento
        Appointment appt = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("Agendamento não encontrado: " + appointmentId));

        // 2. Validações de Elegibilidade para Split
        if (!appt.isPaid()) {
            log.info("Agendamento {} não foi pago online. Ignorando Split.", appointmentId);
            return;
        }

        if (appt.isCommissionSettled()) {
            log.info("Comissão do agendamento {} já foi liquidada anteriormente.", appointmentId);
            return;
        }

        // 3. Busca o profissional para obter a conta destino (Stripe Connect)
        Professional prof = professionalRepository.findById(appt.getProfessionalId())
                .orElseThrow(() -> new RuntimeException("Profissional não encontrado: " + appt.getProfessionalId()));

        // 4. Execução do Split se houver conta conectada
        if (prof.hasConnectedAccount()) {
            try {
                log.info("Iniciando Split automático para {} | Comissão: R$ {}", 
                        prof.getName(), appt.getProfessionalCommission());

                // O Provider de pagamento executa a transferência entre subcontas
                paymentProvider.executeTransfer(
                        appt.getExternalPaymentId(),
                        prof.getGatewayAccountId(),
                        appt.getProfessionalCommission()
                );

                // 5. Marca como liquidado no domínio para auditoria
                appt.markCommissionAsSettled();
                appointmentRepository.save(appt);
                
                log.info("✅ Split concluído com sucesso: Agendamento {}", appointmentId);
                
            } catch (Exception e) {
                log.error("❌ Falha crítica no split do agendamento {}: {}", appointmentId, e.getMessage());
                // Em produção, aqui dispararíamos um alerta para o Sentry ou Slack do Admin
            }
        } else {
            log.warn("⚠️ Profissional {} (ID: {}) sem conta Stripe. Comissão pendente para saque manual.", 
                    prof.getName(), prof.getId());
        }
    }
}