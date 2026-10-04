package com.perseo.finance.savings.domain

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate

enum class TrackingStatus { EN_CAMINO, ATRASADA, ALCANZADA }

enum class MonthStatus { CERRADO, EN_CURSO }

/** Movimiento real con signo: aporte positivo, retiro negativo. */
data class Movement(val date: LocalDate, val amount: BigDecimal)

data class BalanceCheck(val date: LocalDate, val balance: BigDecimal)

/**
 * @property plan parámetros del plan de referencia (saldo inicial = el de la meta). La tarifa de retención
 *   debe estar definida al menos hasta `plan.months + 600` meses (se usa al proyectar).
 * @property deadlineMonth mes del plan correspondiente a la fecha objetivo de la meta, si existe.
 * @property tolerance margen bajo el cual se sigue considerando "en camino" (decisión D5: 5 %).
 */
data class TrackingInput(
    val plan: SavingsParams,
    val startDate: LocalDate,
    val target: BigDecimal,
    val movements: List<Movement>,
    val checks: List<BalanceCheck>,
    val today: LocalDate,
    val paceOverride: BigDecimal? = null,
    val deadlineMonth: Int? = null,
    val tolerance: BigDecimal = BigDecimal("0.05")
)

data class TrackingMonth(
    val month: Int,
    val date: LocalDate,
    val status: MonthStatus,
    val plannedContribution: BigDecimal,
    /** null = mes en curso todavía sin registros (RN-A.8). */
    val realContribution: BigDecimal?,
    val difference: BigDecimal?,
    val plannedBalance: BigDecimal,
    val realBalance: BigDecimal?,
    val reconciled: Boolean
)

data class TrackingResult(
    val currentMonth: Int,
    val evalMonth: Int,
    val plannedBalance: BigDecimal,
    val realBalance: BigDecimal,
    val realNetBalance: BigDecimal,
    val difference: BigDecimal,
    val rows: List<TrackingMonth>,
    val pace: BigDecimal,
    val planTargetMonth: Int?,
    val projectedTargetMonth: Int?,
    val delayMonths: Int?,
    val requiredContribution: BigDecimal?,
    val requiredForMonths: Int?,
    val goalReached: Boolean,
    val status: TrackingStatus,
    val warnings: List<String>
)

/**
 * Compara el plan contra lo realmente ahorrado y proyecta al ritmo actual (HU-A.14, A.16).
 * Código puro, sin Spring.
 *
 * Reglas (RN-A.6 a RN-A.9):
 * - El saldo real parte del saldo inicial, aplica los movimientos mensuales con la tasa del plan y se
 *   "ancla" al saldo conciliado de cada mes que tenga una conciliación (más los movimientos posteriores
 *   a la fecha de la conciliación dentro de ese mismo mes).
 * - El mes en curso solo entra a la comparación si ya tiene movimientos o una conciliación.
 * - El ritmo actual es el promedio de los aportes netos de los últimos 3 meses cerrados.
 * - Movimientos anteriores al inicio del plan se cuentan en el mes 1.
 */
object SavingsTracker {
    private val ZERO = BigDecimal.ZERO

    /** Mes del plan (1 = mes de `startDate`) al que pertenece `date`. Puede ser <= 0 si es anterior. */
    fun monthIndex(startDate: LocalDate, date: LocalDate): Int =
        (date.year - startDate.year) * 12 + (date.monthValue - startDate.monthValue) + 1

    fun track(input: TrackingInput): TrackingResult {
        val plan = input.plan
        val plazo = plan.months
        val target = input.target
        val start = input.startDate.withDayOfMonth(1)
        val warnings = mutableListOf<String>()

        fun monthOf(date: LocalDate): Int = maxOf(monthIndex(start, date), 1)

        val currentMonth = monthIndex(start, input.today)

        val netByMonth: Map<Int, BigDecimal> = input.movements
            .groupBy { monthOf(it.date) }
            .mapValues { (_, list) -> list.fold(ZERO) { acc, m -> acc + m.amount } }

        val latestCheckByMonth: Map<Int, BalanceCheck> = input.checks
            .groupBy { monthOf(it.date) }
            .mapValues { (_, list) -> list.maxByOrNull { it.date }!! }

        fun registered(month: Int): Boolean =
            netByMonth.containsKey(month) || latestCheckByMonth.containsKey(month)

        val evalMonth = when {
            currentMonth <= 0 -> 0
            currentMonth > plazo -> plazo
            registered(currentMonth) -> currentMonth
            else -> currentMonth - 1
        }

        val anchors: Map<Int, BigDecimal> = latestCheckByMonth
            .filterKeys { it in 1..evalMonth }
            .mapValues { (month, check) ->
                val after = input.movements
                    .filter { monthOf(it.date) == month && it.date.isAfter(check.date) }
                    .fold(ZERO) { acc, m -> acc + m.amount }
                check.balance + after
            }

        val planned = SavingsEngine.simulate(plan).rows

        // Se simula el plazo completo y se lee hasta evalMonth, para que la retención "al vencimiento"
        // solo se cobre en el último mes real del plan y no al cierre del periodo evaluado.
        val realRows = if (evalMonth >= 1) {
            SavingsEngine.simulate(
                plan.copy(
                    monthlyContribution = ZERO,
                    annualContributionGrowth = ZERO,
                    contributionForMonth = { m -> netByMonth[m] ?: ZERO },
                    balanceAnchors = anchors
                )
            ).rows
        } else emptyList()

        val realBalance = if (evalMonth >= 1) realRows[evalMonth - 1].closingBalance else plan.initialBalance
        val realNetBalance = if (evalMonth >= 1) realRows[evalMonth - 1].netBalance else plan.initialBalance
        val plannedBalance = if (evalMonth >= 1) planned[evalMonth - 1].closingBalance else plan.initialBalance

        val visibleMonths = if (currentMonth <= 0) 0 else minOf(currentMonth, plazo)
        val rows = (1..visibleMonths).map { m ->
            val closed = currentMonth > plazo || m < currentMonth
            val realContribution: BigDecimal? = if (closed || registered(m)) (netByMonth[m] ?: ZERO) else null
            val plannedContribution = planned[m - 1].contribution
            TrackingMonth(
                month = m,
                date = start.plusMonths((m - 1).toLong()),
                status = if (closed) MonthStatus.CERRADO else MonthStatus.EN_CURSO,
                plannedContribution = plannedContribution,
                realContribution = realContribution,
                difference = realContribution?.let { it - plannedContribution },
                plannedBalance = planned[m - 1].closingBalance,
                realBalance = if (m <= evalMonth) realRows[m - 1].closingBalance else null,
                reconciled = anchors.containsKey(m)
            )
        }

        // RN-A.9: promedio de los últimos 3 meses cerrados.
        val lastClosed = if (currentMonth > plazo) plazo else currentMonth - 1
        val computedPace = if (lastClosed >= 1) {
            val from = maxOf(1, lastClosed - 2)
            val total = (from..lastClosed).fold(ZERO) { acc, m -> acc + (netByMonth[m] ?: ZERO) }
            total.divide(BigDecimal(lastClosed - from + 1), 0, RoundingMode.HALF_UP)
        } else plan.monthlyContribution
        val pace = input.paceOverride ?: computedPace

        val planTargetMonth: Int? =
            if (plan.initialBalance >= target) 0 else planned.firstOrNull { it.netBalance >= target }?.month
        val goalReached = realNetBalance >= target

        // Proyección desde el saldo real. El rendimiento ya causado (saldo real - lo aportado) cuenta
        // para la retención al vencimiento.
        val principal = plan.initialBalance + (1..evalMonth).fold(ZERO) { acc, m -> acc + (netByMonth[m] ?: ZERO) }
        val accrued = (realBalance - principal).max(ZERO)
        val projectionBase = plan.copy(
            initialBalance = realBalance.max(ZERO),
            monthlyContribution = ZERO,
            annualContributionGrowth = ZERO,
            contributionForMonth = null,
            balanceAnchors = emptyMap(),
            withholdingRateForMonth = { m -> plan.withholdingRateForMonth(m + evalMonth) },
            initialAccumulatedReturn = accrued
        )

        val projectedTargetMonth: Int? = when {
            goalReached -> null
            pace.signum() > 0 -> SavingsEngine
                .simulate(projectionBase.copy(monthlyContribution = pace, months = SavingsEngine.MAX_MONTHS))
                .rows.firstOrNull { it.netBalance >= target }?.month?.let { evalMonth + it }
            else -> {
                warnings.add("Con aportes de cero o menos la meta no se alcanza; sube el ritmo para ver una proyección.")
                null
            }
        }

        val deadline = planTargetMonth ?: plazo
        val remaining = deadline - evalMonth
        val requiredContribution: BigDecimal? = when {
            goalReached -> null
            remaining > 0 -> SavingsEngine.requiredMonthlyContribution(projectionBase.copy(months = remaining), target)
            else -> {
                warnings.add(
                    "El plazo del plan ya venció; crea un plan nuevo con una fecha posterior para recalcular el aporte necesario."
                )
                null
            }
        }
        val requiredForMonths: Int? = if (requiredContribution != null) remaining else null

        val delayMonths: Int? =
            if (projectedTargetMonth != null && planTargetMonth != null) projectedTargetMonth - planTargetMonth else null

        val floor = plannedBalance.multiply(BigDecimal.ONE - input.tolerance)
        val deadlineMonth = input.deadlineMonth
        val status = when {
            goalReached -> TrackingStatus.ALCANZADA
            deadlineMonth != null && (projectedTargetMonth == null || projectedTargetMonth > deadlineMonth) ->
                TrackingStatus.ATRASADA
            realBalance < floor -> TrackingStatus.ATRASADA
            else -> TrackingStatus.EN_CAMINO
        }

        return TrackingResult(
            currentMonth = currentMonth,
            evalMonth = evalMonth,
            plannedBalance = plannedBalance,
            realBalance = realBalance,
            realNetBalance = realNetBalance,
            difference = realBalance - plannedBalance,
            rows = rows,
            pace = pace,
            planTargetMonth = planTargetMonth,
            projectedTargetMonth = projectedTargetMonth,
            delayMonths = delayMonths,
            requiredContribution = requiredContribution,
            requiredForMonths = requiredForMonths,
            goalReached = goalReached,
            status = status,
            warnings = warnings
        )
    }
}
