package com.perseo.finance.savings.entity

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

/** Saldo real copiado del banco en una fecha (HU-A.14). */
@Entity
@Table(name = "savings_balance_checks")
class SavingsBalanceCheckEntity(
    @Id
    @GeneratedValue
    val id: UUID? = null,

    @Column(name = "goal_id", nullable = false)
    var goalId: UUID = UUID.randomUUID(),

    @Column(nullable = false)
    var fecha: LocalDate = LocalDate.now(),

    @Column(name = "saldo_real", nullable = false, precision = 18, scale = 2)
    var saldoReal: BigDecimal = BigDecimal.ZERO,

    @Column(length = 200)
    var nota: String? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now()
)
