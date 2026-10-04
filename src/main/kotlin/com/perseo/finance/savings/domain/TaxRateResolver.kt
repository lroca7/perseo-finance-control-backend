package com.perseo.finance.savings.domain

import java.math.BigDecimal
import java.time.LocalDate

data class TaxRateEntry(
    val tarifa: BigDecimal,
    val vigenteDesde: LocalDate,
    val vigenteHasta: LocalDate?
)

enum class RateOrigin { VIGENTE, ULTIMA_CONOCIDA, PERSONALIZADO }

data class UsedRate(val entry: TaxRateEntry?, val tarifa: BigDecimal, val origin: RateOrigin)

data class ResolvedRates(
    val rates: List<BigDecimal>,
    val used: List<UsedRate>,
    val warnings: List<String>
) {
    /** Tarifa del mes `month` (1 = primer mes del plan). */
    fun forMonth(month: Int): BigDecimal = rates[month - 1]
}

/**
 * RN-A.10: la tarifa se busca por la fecha de cada mes simulado, no por la de hoy.
 * Código puro (sin Spring ni BD) para poder probarlo.
 */
object TaxRateResolver {

    fun resolve(
        entries: List<TaxRateEntry>,
        startDate: LocalDate,
        months: Int,
        override: BigDecimal? = null
    ): ResolvedRates {
        if (override != null) {
            return ResolvedRates(
                rates = List(months) { override },
                used = listOf(UsedRate(null, override, RateOrigin.PERSONALIZADO)),
                warnings = emptyList()
            )
        }

        val sorted = entries.sortedByDescending { it.vigenteDesde }
        if (sorted.isEmpty()) {
            return ResolvedRates(
                rates = List(months) { BigDecimal.ZERO },
                used = emptyList(),
                warnings = listOf("No hay tarifa de retención configurada; se asumió 0 %.")
            )
        }

        val firstOfMonth = startDate.withDayOfMonth(1)
        val fallback = sorted.first()
        var usedFallback = false

        val perMonth = (1..months).map { m ->
            val date = firstOfMonth.plusMonths((m - 1).toLong())
            val match = sorted.firstOrNull { e ->
                val hasta = e.vigenteHasta
                !e.vigenteDesde.isAfter(date) && (hasta == null || !hasta.isBefore(date))
            }
            if (match != null) {
                UsedRate(match, match.tarifa, RateOrigin.VIGENTE)
            } else {
                usedFallback = true
                UsedRate(fallback, fallback.tarifa, RateOrigin.ULTIMA_CONOCIDA)
            }
        }

        val warnings = if (usedFallback) {
            listOf(
                "Para algunos meses del plan no hay una tarifa de retención vigente; " +
                    "se usó la última conocida (${percent(fallback.tarifa)} %)."
            )
        } else emptyList()

        return ResolvedRates(
            rates = perMonth.map { it.tarifa },
            used = perMonth.distinctBy { it.entry to it.origin },
            warnings = warnings
        )
    }

    private fun percent(fraction: BigDecimal): String =
        fraction.multiply(BigDecimal(100)).stripTrailingZeros().toPlainString()
}
