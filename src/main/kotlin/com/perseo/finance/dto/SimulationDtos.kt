package com.perseo.finance.dto

import com.perseo.finance.domain.PaymentStrategy
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Min
import java.math.BigDecimal

data class CreateExtraPaymentRequest(
    @field:Min(value = 1, message = "El # de cuota debe ser al menos 1")
    val numeroCuota: Int,

    @field:DecimalMin(value = "0.01", message = "El monto debe ser mayor a 0")
    val monto: BigDecimal,

    val estrategia: PaymentStrategy = PaymentStrategy.REDUCIR_PLAZO
)

data class ExtraPaymentResponse(
    val id: java.util.UUID,
    val numeroCuota: Int,
    val monto: BigDecimal,
    val estrategia: PaymentStrategy
)

data class ScheduleRowResponse(
    val numeroCuota: Int,
    val interes: BigDecimal,
    val capital: BigDecimal,
    val cuotaTotal: BigDecimal,
    val saldoFinal: BigDecimal,
    val abonoExtra: BigDecimal
)

data class SimulationResponse(
    val filas: List<ScheduleRowResponse>,
    val meses: Int,
    val totalInteres: BigDecimal,
    val totalCapital: BigDecimal,
    val totalPagado: BigDecimal,
    val nuncaSePagaEnPlazo: Boolean
)

/**
 * Respuesta del endpoint de comparación (HU-4.2).
 * Usa los abonos ya guardados de la línea (HU-T.7); no requiere que el
 * cliente los reenvíe en cada request.
 */
data class ComparisonResponse(
    val sinAbonos: SimulationResponse,
    val conAbonos: SimulationResponse,
    val ahorroIntereses: BigDecimal,
    val mesesAhorrados: Int
)
