package com.perseo.finance.savings.domain

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

enum class ContributionTiming { INICIO, FIN }

enum class WithholdingMode { MENSUAL, AL_VENCIMIENTO }

/**
 * Parámetros de una simulación. Tasas como fracciones (0.11 = 11 %).
 *
 * @property withholdingRateForMonth tarifa de retención vigente en el mes `m` (RN-A.10).
 *   El servicio la arma desde `tax_parameters`; el motor no conoce tarifas.
 * @property contributionForMonth si se entrega, reemplaza el aporte base + crecimiento.
 *   Se usa para el seguimiento real (aportes netos registrados por mes).
 * @property balanceAnchors saldos conciliados: mes -> saldo de cierre real (A.14).
 * @property initialAccumulatedReturn rendimiento ya causado antes del primer mes simulado; solo
 *   importa con retención AL_VENCIMIENTO, porque la retención se calcula sobre todo el rendimiento
 *   acumulado. Se usa al proyectar desde un saldo real (A.16).
 */
data class SavingsParams(
    val initialBalance: BigDecimal,
    val monthlyContribution: BigDecimal,
    val annualRateEA: BigDecimal,
    val months: Int,
    val commissionEA: BigDecimal = BigDecimal.ZERO,
    val timing: ContributionTiming = ContributionTiming.FIN,
    val withholdingMode: WithholdingMode = WithholdingMode.MENSUAL,
    val withholdingRateForMonth: (Int) -> BigDecimal = { BigDecimal.ZERO },
    val annualContributionGrowth: BigDecimal = BigDecimal.ZERO,
    val contributionForMonth: ((Int) -> BigDecimal)? = null,
    val balanceAnchors: Map<Int, BigDecimal> = emptyMap(),
    val initialAccumulatedReturn: BigDecimal = BigDecimal.ZERO
)

/**
 * Una fila del cronograma.
 *
 * @property grossReturn rendimiento antes de comisión e impuestos.
 * @property commission comisión de administración del mes.
 * @property tax retención efectivamente descontada ese mes.
 * @property closingBalance saldo al cierre (ya descontada la retención cobrada).
 * @property pendingTax retención causada y aún no cobrada (solo AL_VENCIMIENTO).
 * @property netBalance lo que quedaría en mano si se retirara hoy: closingBalance - pendingTax.
 */
data class SavingsMonth(
    val month: Int,
    val openingBalance: BigDecimal,
    val contribution: BigDecimal,
    val grossReturn: BigDecimal,
    val commission: BigDecimal,
    val tax: BigDecimal,
    val closingBalance: BigDecimal,
    val pendingTax: BigDecimal,
    val netBalance: BigDecimal
)

data class SavingsSummary(
    val totalContributed: BigDecimal,
    val totalGrossReturn: BigDecimal,
    val totalCommission: BigDecimal,
    val totalTax: BigDecimal,
    val finalNetBalance: BigDecimal
)

data class SavingsResult(val rows: List<SavingsMonth>, val summary: SavingsSummary)

/** Motor de ahorro puro (sin Spring), igual que AmortizationEngine. */
object SavingsEngine {
    const val MAX_MONTHS = 600

    private val ZERO = BigDecimal.ZERO
    private val ONE = BigDecimal.ONE
    private val TWO = BigDecimal(2)
    private val HALF = BigDecimal("0.5")
    private val MC = MathContext(20, RoundingMode.HALF_EVEN)

    private fun BigDecimal.peso(): BigDecimal = setScale(0, RoundingMode.HALF_UP)

    fun simulate(p: SavingsParams): SavingsResult {
        validate(p)
        val grossMonthly = SavingsRates.monthlyFromEA(p.annualRateEA)
        val netMonthly = SavingsRates.monthlyFromEA(
            SavingsRates.netOfCommission(p.annualRateEA, p.commissionEA)
        )

        var balance = p.initialBalance
        var principal = p.initialBalance
        var accumulated = p.initialAccumulatedReturn // rendimiento acumulado neto de comisión (base de la retención al vencimiento)
        val rows = ArrayList<SavingsMonth>(p.months)

        for (m in 1..p.months) {
            val isLast = m == p.months
            val contribution = contributionFor(p, m)
            val opening = balance
            val afterContribution = opening + contribution
            val base = (if (p.timing == ContributionTiming.INICIO) afterContribution else opening).max(ZERO)

            val grossReturn = (base * grossMonthly).peso()
            val netReturn = (base * netMonthly).peso()
            val commission = grossReturn - netReturn
            val rate = p.withholdingRateForMonth(m)

            var tax = ZERO
            var pending = ZERO
            when (p.withholdingMode) {
                WithholdingMode.MENSUAL -> tax = (netReturn.max(ZERO) * rate).peso()
                WithholdingMode.AL_VENCIMIENTO -> {
                    accumulated += netReturn
                    val due = (accumulated.max(ZERO) * rate).peso()
                    if (isLast) tax = due else pending = due
                }
            }

            var closing = afterContribution + netReturn - tax
            principal += contribution

            p.balanceAnchors[m]?.let { anchor ->
                closing = anchor
                if (p.withholdingMode == WithholdingMode.AL_VENCIMIENTO) {
                    accumulated = (closing - principal).max(ZERO)
                    pending = if (isLast) ZERO else (accumulated * rate).peso()
                }
            }

            rows.add(
                SavingsMonth(
                    month = m,
                    openingBalance = opening,
                    contribution = contribution,
                    grossReturn = grossReturn,
                    commission = commission,
                    tax = tax,
                    closingBalance = closing,
                    pendingTax = pending,
                    netBalance = closing - pending
                )
            )
            balance = closing
        }

        val summary = SavingsSummary(
            totalContributed = p.initialBalance + rows.sumOf { it.contribution },
            totalGrossReturn = rows.sumOf { it.grossReturn },
            totalCommission = rows.sumOf { it.commission },
            totalTax = rows.sumOf { it.tax },
            finalNetBalance = rows.last().netBalance
        )
        return SavingsResult(rows, summary)
    }

    /**
     * Primer mes en que el saldo NETO alcanza el objetivo. 0 = ya alcanzada; null = no se alcanza
     * dentro de `maxMonths` (A.7, A.16).
     */
    fun monthsToReach(p: SavingsParams, target: BigDecimal, maxMonths: Int = MAX_MONTHS): Int? {
        if (p.initialBalance >= target) return 0
        val rows = simulate(p.copy(months = maxMonths)).rows
        return rows.firstOrNull { it.netBalance >= target }?.month
    }

    /**
     * Aporte mensual mínimo (en pesos enteros) para que el saldo neto final alcance el objetivo
     * en `p.months` (A.6, A.16). Ignora aportes por mes y conciliaciones de `p`.
     */
    fun requiredMonthlyContribution(p: SavingsParams, target: BigDecimal): BigDecimal {
        val base = p.copy(contributionForMonth = null, balanceAnchors = emptyMap())
        fun finalNet(c: BigDecimal) = simulate(base.copy(monthlyContribution = c)).summary.finalNetBalance

        if (finalNet(ZERO) >= target) return ZERO

        var lo = ZERO
        var hi = target
        var guard = 0
        while (finalNet(hi) < target) {
            check(guard++ < 5) { "No se pudo acotar el aporte requerido." }
            hi *= TWO
        }
        while (hi - lo > ONE) {
            val mid = (lo + hi) * HALF
            if (finalNet(mid) >= target) hi = mid else lo = mid
        }
        var result = hi.setScale(0, RoundingMode.CEILING)
        while (result > ZERO && finalNet(result - ONE) >= target) result -= ONE
        return result
    }

    private fun contributionFor(p: SavingsParams, month: Int): BigDecimal {
        p.contributionForMonth?.let { return it(month) }
        val factor = (ONE + p.annualContributionGrowth).pow((month - 1) / 12, MC)
        return (p.monthlyContribution * factor).peso()
    }

    private fun validate(p: SavingsParams) {
        require(p.months in 1..MAX_MONTHS) { "El plazo debe estar entre 1 y $MAX_MONTHS meses." }
        require(p.initialBalance.signum() >= 0) { "El saldo inicial no puede ser negativo." }
        require(p.monthlyContribution.signum() >= 0) { "El aporte mensual no puede ser negativo." }
        require(p.annualRateEA.signum() >= 0) { "La tasa no puede ser negativa." }
        require(p.commissionEA.signum() >= 0 && p.commissionEA < ONE) {
            "La comisión debe ser mayor o igual a 0 y menor a 100 %."
        }
        require(p.annualContributionGrowth.signum() >= 0) { "El crecimiento del aporte no puede ser negativo." }
        require(p.initialAccumulatedReturn.signum() >= 0) { "El rendimiento acumulado inicial no puede ser negativo." }
    }
}
