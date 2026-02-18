package com.stylo.api_agendamento.adapters.outbound.persistence.financial;

import com.stylo.api_agendamento.adapters.outbound.persistence.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "cash_registers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CashRegisterEntity extends BaseEntity {

    @Id
    @Column(length = 36)
    private String id;

    @Column(nullable = false, length = 36)
    private String providerId;

    @Column(nullable = false)
    private LocalDateTime openTime;

    private LocalDateTime closeTime;

    @Column(nullable = false)
    private BigDecimal initialBalance;

    private BigDecimal finalBalance;

    @Column(nullable = false)
    private BigDecimal calculatedBalance;

    @Column(nullable = false)
    private boolean isOpen;

    @Column(nullable = false, length = 36)
    private String openedByUserId;

    @Column(length = 36)
    private String closedByUserId;

    @OneToMany(mappedBy = "cashRegister", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @Builder.Default
    private List<CashTransactionEntity> transactions = new ArrayList<>();
}