package com.stylo.api_agendamento.core.usecases;

import com.stylo.api_agendamento.core.common.UseCase;
import com.stylo.api_agendamento.core.domain.Appointment;
import com.stylo.api_agendamento.core.domain.Professional;
import com.stylo.api_agendamento.core.ports.IAppointmentRepository;
import com.stylo.api_agendamento.core.ports.IProfessionalRepository;
import com.stylo.api_agendamento.core.ports.IPaymentProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@UseCase
@RequiredArgsConstructor
public class ProcessAutomaticSplitUseCase {

    private final IProfessionalRepository professionalRepository;
    private final IAppointmentRepository appointmentRepository;
    private final IPaymentProvider paymentProvider;

    public void execute(String appointmentId) {
        // 1. Busca o agendamento
        Appointment appt = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("Agendamento não encontrado: " + appointmentId));

        // 2. Valida se o agendamento foi pago online e se tem comissão calculada
        if (!appt.isPaid()) {
            log.info("Agendamento {} pago localmente. Ignorando Split automático.", appointmentId);
            return;
        }

        // 3. Busca o profissional para obter a conta de destino
        Professional prof = professionalRepository.findById(appt.getProfessionalId())
                .orElseThrow(() -> new RuntimeException("Profissional não encontrado: " + appt.getProfessionalId()));

        // 4. Se o profissional tem conta conectada, executa o split
        if (prof.hasConnectedAccount()) {
            try {
                log.info("Iniciando Split automático para o profissional {} no valor de R${}", 
                        prof.getName(), appt.getProfessionalCommission());

                paymentProvider.executeSplit(
                        appt.getExternalPaymentId(),
                        prof.getGatewayAccountId(),
                        appt.getProfessionalCommission(),
                        appt.getServiceProviderFee()
                );

                // 5. Marca como liquidado e salva
                appt.markCommissionAsSettled();
                appointmentRepository.save(appt);
                
                log.info("✅ Split concluído com sucesso para o agendamento {}", appointmentId);
                
            } catch (Exception e) {
                log.error("❌ Erro ao processar split automático do agendamento {}: {}", 
                        appointmentId, e.getMessage());
                // Importante: Não lançamos exceção aqui para não travar o fluxo do Webhook, 
                // mas o log disparará um alerta para intervenção manual.
            }
        } else {
            log.warn("⚠️ Profissional {} não possui conta conectada. A comissão de R${} ficará pendente para fechamento manual.", 
                    prof.getName(), appt.getProfessionalCommission());
        }
    }
}