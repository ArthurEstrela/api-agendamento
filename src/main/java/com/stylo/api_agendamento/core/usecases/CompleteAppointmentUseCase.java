package com.stylo.api_agendamento.core.usecases;

import com.stylo.api_agendamento.core.domain.Appointment;
import com.stylo.api_agendamento.core.domain.vo.PaymentMethod;
import com.stylo.api_agendamento.core.exceptions.BusinessException;
import com.stylo.api_agendamento.core.exceptions.EntityNotFoundException;
import com.stylo.api_agendamento.core.ports.IAppointmentRepository;
import com.stylo.api_agendamento.core.ports.IFinancialRepository;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;

@RequiredArgsConstructor
public class CompleteAppointmentUseCase {

    private final IAppointmentRepository appointmentRepository;
    private final IFinancialRepository financialRepository;

    public void execute(CompleteAppointmentInput input) {
        // 1. Busca o agendamento
        Appointment appointment = appointmentRepository.findById(input.appointmentId())
                .orElseThrow(() -> new EntityNotFoundException("Agendamento não encontrado."));

        // 2. Regra de Negócio: Finaliza o objeto de domínio
        // Passamos o método de pagamento e o valor final (que pode ter tido acréscimos ou descontos extras na hora)
        appointment.complete(input.paymentMethod(), input.finalPrice());

        // 3. Persistência do Agendamento
        appointmentRepository.save(appointment);

        // 4. Integração Financeira: Registra a entrada no caixa do ServiceProvider
        // Isso alimenta os gráficos que você tem no front-end (FinancialManagement.tsx)
        financialRepository.registerRevenue(
                appointment.getProviderId(),
                appointment.getFinalPrice(),
                "Serviço: " + appointment.getServices().get(0).getName(), // Descrição simples
                appointment.getCompletedAt()
        );
    }

    public record CompleteAppointmentInput(
            String appointmentId,
            PaymentMethod paymentMethod,
            BigDecimal finalPrice
    ) {}
}