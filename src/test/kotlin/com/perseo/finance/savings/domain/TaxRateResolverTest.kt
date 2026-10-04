package com.perseo.finance.savings.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate

class TaxRateResolverTest {

    private fun bd(s: String) = BigDecimal(s)
    private fun d(s: String) = LocalDate.parse(s)

    private val general = TaxRateEntry(bd("0.07"), d("2020-01-01"), null)

    @Test
    fun `una sola tarifa vigente aplica a todos los meses sin advertencias`() {
        val r = TaxRateResolver.resolve(listOf(general), d("2026-10-01"), 6)
        assertEquals(6, r.rates.size)
        assertTrue(r.rates.all { it.compareTo(bd("0.07")) == 0 })
        assertEquals(1, r.used.size)
        assertEquals(RateOrigin.VIGENTE, r.used[0].origin)
        assertTrue(r.warnings.isEmpty())
    }

    @Test
    fun `la tarifa cambia segun la fecha de cada mes y no la de hoy`() {
        val antigua = TaxRateEntry(bd("0.07"), d("2020-01-01"), d("2027-12-31"))
        val nueva = TaxRateEntry(bd("0.10"), d("2028-01-01"), null)
        // Meses: nov-2027, dic-2027, ene-2028, feb-2028
        val r = TaxRateResolver.resolve(listOf(antigua, nueva), d("2027-11-15"), 4)

        assertEquals(0, r.forMonth(1).compareTo(bd("0.07")))
        assertEquals(0, r.forMonth(2).compareTo(bd("0.07")))
        assertEquals(0, r.forMonth(3).compareTo(bd("0.10")))
        assertEquals(0, r.forMonth(4).compareTo(bd("0.10")))
        assertEquals(2, r.used.size)
        assertTrue(r.warnings.isEmpty())
    }

    @Test
    fun `sin tarifa vigente usa la ultima conocida y avisa`() {
        val vencida = TaxRateEntry(bd("0.07"), d("2020-01-01"), d("2022-12-31"))
        val r = TaxRateResolver.resolve(listOf(vencida), d("2027-01-01"), 3)

        assertTrue(r.rates.all { it.compareTo(bd("0.07")) == 0 })
        assertEquals(RateOrigin.ULTIMA_CONOCIDA, r.used[0].origin)
        assertEquals(1, r.warnings.size)
        assertTrue(r.warnings[0].contains("7 %"), r.warnings[0])
    }

    @Test
    fun `sin parametros asume cero y avisa`() {
        val r = TaxRateResolver.resolve(emptyList(), d("2026-10-01"), 3)
        assertTrue(r.rates.all { it.signum() == 0 })
        assertTrue(r.used.isEmpty())
        assertEquals(1, r.warnings.size)
    }

    @Test
    fun `una tarifa personalizada reemplaza a los parametros`() {
        val r = TaxRateResolver.resolve(listOf(general), d("2026-10-01"), 5, bd("0.04"))
        assertTrue(r.rates.all { it.compareTo(bd("0.04")) == 0 })
        assertEquals(RateOrigin.PERSONALIZADO, r.used[0].origin)
        assertTrue(r.warnings.isEmpty())
    }
}
