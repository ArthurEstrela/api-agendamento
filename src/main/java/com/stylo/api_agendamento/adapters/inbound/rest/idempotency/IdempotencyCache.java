package com.stylo.api_agendamento.adapters.inbound.rest.idempotency;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IdempotencyCache implements Serializable {
    private int status;
    private Object body;
}