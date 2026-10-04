package com.perseo.finance.savings.dto

import com.perseo.finance.savings.domain.MonthStatus
import com.perseo.finance.savings.domain.MovementType
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Size
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

enum class GoalTrackingStatus { EN_CAMINO, ATRASADA, ALCANZADA, SIN_PLAN }

// ---------- Movimientos reales (HU-A.11, A.15, A.22) ----------

data class SavingsMovementRequest(
    val fecha: LocalDate,

    @field:DecimalMin(value = "0.01", message = "El monto debe ser mayor a cero.")
    val monto: BigDecimal,

    val tipo: MovementType = MovementType.APORTE,

    @field:Size(max = 200, message = "La nota no puede superar 200 caracteres.")
    val nota: String? = null,

    /** Un retiro mayor al saldo estimado exige confirmación explícita (decisión D6). */
    val confirmarExceso: Boolean = false
)

data class SavingsMovementResponse(
    val id: UUID,
    val goalId: UUID,
    val fecha: LocalDate,
    val monto: BigDecimal,
    val tipo: MovementType,
    val nota: String?,
    val createdAt: Instant
)

// ---------- Conciliaciones (HU-A.14) ----------

data class SavingsBalanceCheckRequest(
    val fecha: LocalDate,

    @field:DecimalMin(value = "0", message = "El saldo no puede ser negativo.")
    val saldoReal: BigDecimal,

    @field:Size(max = 200, message = "La nota no puede superar 200 caracteres.")
    val nota: String? = null
)

data class SavingsBalanceCheckResponse(
    val id: UUID,
    val goalId: UUID,
    val fecha: LocalDate,
    val saldoReal: BigDecimal,
    val nota: String?,
    val createdAt: Instant
)

// ---------- Seguimiento: plan vs. real (HU-A.16) ----------

data class SavingsTrackingMonthResponse(
    val mes: Int,
    val fecha: LocalDate,
    val estado: MonthStatus,
    val aportePlaneado: BigDecimal,
    /** null = mes en curso sin registrar. */
    val aporteReal: BigDecimal?,
    val diferencia: BigDecimal?,
    val saldoPlaneado: BigDecimal,
    val saldoReal: BigDecimal?,
    val conciliado: Boolean
)

data class SavingsProgressResponse(
    val goalId: UUID,
    val planReferenciaId: UUID?,
    val estado: GoalTrackingStatus,
    val montoObjetivo: BigDecimal,
    val saldoRealEstimado: BigDecimal,
    val saldoRealNeto: BigDecimal? = null,
    /** saldoRealEstimado / meta como fracción (0.28 = 28 %). */
    val avanceMeta: BigDecimal,
    val mesActual: Int? = null,
    val mesEvaluado: Int? = null,
    val saldoPlaneado: BigDecimal? = null,
    val diferencia: BigDecimal? = null,
    val ritmoActual: BigDecimal? = null,
    val mesMetaPlan: Int? = null,
    val mesMetaProyectado: Int? = null,
    val fechaMetaProyectada: LocalDate? = null,
    val mesesDeRetraso: Int? = null,
    /** Meses que faltan, contados desde el mes evaluado, para llegar a la meta al ritmo actual. */
    val mesesParaMeta: Int? = null,
    val aporteNecesario: BigDecimal? = null,
    val mesesParaAporteNecesario: Int? = null,
    val meses: List<SavingsTrackingMonthResponse> = emptyList(),
    val advertencias: List<String> = emptyList(),
    val aviso: String
)
