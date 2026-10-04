package com.perseo.finance.savings.entity

import com.perseo.finance.savings.domain.GoalStatus
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@Entity
@Table(name = "savings_goals")
class SavingsGoalEntity(
    @Id
    @GeneratedValue
    val id: UUID? = null,

    @Column(name = "user_id", nullable = false)
    var userId: UUID = UUID.randomUUID(),

    @Column(nullable = false, length = 80)
    var nombre: String = "",

    @Column(nullable = false, length = 30)
    var icono: String = "piggy-bank",

    @Column(name = "monto_objetivo", nullable = false, precision = 18, scale = 2)
    var montoObjetivo: BigDecimal = BigDecimal.ZERO,

    /** Dinero que ya se tiene al crear la meta (decisión D4: vive en la meta, no en el plan). */
    @Column(name = "saldo_inicial", nullable = false, precision = 18, scale = 2)
    var saldoInicial: BigDecimal = BigDecimal.ZERO,

    @Column(name = "fecha_objetivo")
    var fechaObjetivo: LocalDate? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var estado: GoalStatus = GoalStatus.ACTIVA,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()
)
