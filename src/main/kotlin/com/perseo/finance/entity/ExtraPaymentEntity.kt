package com.perseo.finance.entity

import com.perseo.finance.domain.PaymentStrategy
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "extra_payments")
class ExtraPaymentEntity(
    @Id
    @GeneratedValue
    val id: UUID? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "credit_line_id", nullable = false)
    var creditLine: CreditLineEntity? = null,

    @Column(name = "numero_cuota", nullable = false)
    var numeroCuota: Int = 1,

    @Column(nullable = false, precision = 14, scale = 2)
    var monto: BigDecimal = BigDecimal.ZERO,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var estrategia: PaymentStrategy = PaymentStrategy.REDUCIR_PLAZO,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now()
)
