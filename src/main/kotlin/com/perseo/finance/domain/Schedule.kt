package com.perseo.finance.domain

/**
 * Una fila del cronograma de pago para un mes/cuota específico.
 *
 * Nota de precisión: se usa Double para reflejar exactamente el prototipo
 * validado en el chat. Antes de manejar dinero real en producción, considerar
 * migrar a BigDecimal con un MathContext explícito para evitar arrastre de
 * error de punto flotante en simulaciones largas (50+ cuotas).
 */
data class ScheduleRow(
    val numeroCuota: Int,
    val interes: Double,
    val capital: Double,
    val cuotaTotal: Double,
    val saldoFinal: Double,
    val abonoExtra: Double
)

data class SimulationResult(
    val filas: List<ScheduleRow>,
    val meses: Int,
    val totalInteres: Double,
    val totalCapital: Double,
    val totalPagado: Double,
    /** true si la deuda no llega a $0 dentro del límite de seguridad de meses. */
    val nuncaSePagaEnPlazo: Boolean
)

/** Comparativo entre el escenario sin abonos y el escenario con los abonos del usuario. */
data class ComparisonResult(
    val sinAbonos: SimulationResult,
    val conAbonos: SimulationResult,
    val ahorroIntereses: Double,
    val mesesAhorrados: Int
)
