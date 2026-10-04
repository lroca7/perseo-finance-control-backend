package com.perseo.finance.savings.entity

import com.perseo.finance.savings.domain.MovementType
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

/** Aporte o retiro real de una meta (HU-A.11, A.15). `monto` siempre positivo; el signo lo da `tipo`. */
@Entity
@Table(name = "savings_contributions")
class SavingsContributionEntity(
    @Id
    @GeneratedValue
    val id: UUID? = null,

    @Column(name = "goal_id", nullable = false)
    var goalId: UUID = UUID.randomUUID(),

    @Column(nullable = false)
    var fecha: LocalDate = LocalDate.now(),

    @Column(nullable = false, precision = 18, scale = 2)
    var monto: BigDecimal = BigDecimal.ZERO,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    var tipo: MovementType = MovementType.APORTE,

    @Column(length = 200)
    var nota: String? = null,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now()
)
