package com.perseo.finance.savings.domain

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

/**
 * Conversión de tasas y valor real. Código puro, sin Spring.
 *
 * Convención: todas las tasas son FRACCIONES (0.11 = 11 %), nunca porcentajes.
 */
object SavingsRates {
    private val MC = MathContext(20, RoundingMode.HALF_EVEN)
    private val ONE = BigDecimal.ONE

    /** RN-A.1: nominal con `periodsPerYear` capitalizaciones al año -> efectiva anual. */
    fun effectiveAnnualFromNominal(nominal: BigDecimal, periodsPerYear: Int): BigDecimal {
        require(periodsPerYear > 0) { "Los periodos por año deben ser mayores a cero." }
        val perPeriod = nominal.divide(BigDecimal(periodsPerYear), MC)
        return (ONE + perPeriod).pow(periodsPerYear, MC) - ONE
    }

    /** Nominal mes vencido (NMV) -> EA. */
    fun fromNominalMonthly(nominal: BigDecimal): BigDecimal = effectiveAnnualFromNominal(nominal, 12)

    /** Nominal trimestre vencido (NTV) -> EA. */
    fun fromNominalQuarterly(nominal: BigDecimal): BigDecimal = effectiveAnnualFromNominal(nominal, 4)

    /** RN-A.2 (decisión D3): EA neta = (1 + EA) / (1 + comisión) - 1. */
    fun netOfCommission(ea: BigDecimal, commissionEA: BigDecimal): BigDecimal =
        if (commissionEA.signum() == 0) ea
        else (ONE + ea).divide(ONE + commissionEA, MC) - ONE

    /** RN-A.1: tasa mensual equivalente = (1 + EA)^(1/12) - 1 (nunca EA/12). */
    fun monthlyFromEA(ea: BigDecimal): BigDecimal {
        if (ea.signum() == 0) return BigDecimal.ZERO
        val monthly = Math.pow(1.0 + ea.toDouble(), 1.0 / 12.0) - 1.0
        return BigDecimal(monthly, MC)
    }

    /** RN-A.5: valor en pesos de hoy = nominal / (1 + inflación)^(meses/12), redondeado al peso. */
    fun realValue(nominal: BigDecimal, annualInflation: BigDecimal, months: Int): BigDecimal {
        if (annualInflation.signum() == 0 || months == 0) return nominal.setScale(0, RoundingMode.HALF_UP)
        val factor = Math.pow(1.0 + annualInflation.toDouble(), months / 12.0)
        return nominal.divide(BigDecimal(factor, MC), MC).setScale(0, RoundingMode.HALF_UP)
    }
}
