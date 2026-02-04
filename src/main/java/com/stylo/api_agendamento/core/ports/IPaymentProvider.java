// Arquivo: src/main/java/com/stylo/api_agendamento/core/ports/IPaymentProvider.java
package com.stylo.api_agendamento.core.ports;

import java.math.BigDecimal;

public interface IPaymentProvider {
    boolean processPayment(String customerId, BigDecimal amount, String paymentMethodId);
    String generatePaymentLink(String appointmentId, BigDecimal amount);
}