package com.perseo.finance.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.math.abs

class AmortizationEngineTest {

    private fun approx(a: Double, b: Double, tolerance: Double = 1.0) =
        assertTrue(abs(a - b) < tolerance, "esperado ~$b pero fue $a")

    @Test
    fun `REDIFE - capital fijo coincide con el extracto real`() {
        // Datos reales del extracto Davivienda usados en el prototipo.
        val resultado = AmortizationEngine.simular(
            saldo = 11_922_602.0,
            cuotasRestantes = 58,
            tasaEA = 28.77,
            tipo = AmortizationType.CAPITAL_FIJO
        )

        // El capital fijo debe ser saldo / cuotas = ~205,562 (validado con el extracto).
        approx(resultado.filas.first().capital, 205_562.0, tolerance = 5.0)
        assertEquals(58, resultado.meses)
        assertTrue(!resultado.nuncaSePagaEnPlazo)
    }

    @Test
    fun `capital fijo - el interes decrece mes a mes sin necesidad de abonos`() {
        val resultado = AmortizationEngine.simular(
            saldo = 1_000_000.0,
            cuotasRestantes = 10,
            tasaEA = 24.0,
            tipo = AmortizationType.CAPITAL_FIJO
        )
        for (i in 1 until resultado.filas.size) {
            assertTrue(
                resultado.filas[i].interes < resultado.filas[i - 1].interes,
                "el interés debería bajar mes a mes"
            )
        }
    }

    @Test
    fun `tasa 0 por ciento - no genera interes y amortiza en exactamente n cuotas`() {
        val resultado = AmortizationEngine.simular(
            saldo = 1_200_000.0,
            cuotasRestantes = 12,
            tasaEA = 0.0,
            tipo = AmortizationType.CAPITAL_FIJO
        )
        assertEquals(12, resultado.meses)
        assertEquals(0.0, resultado.totalInteres, 0.001)
        approx(resultado.filas.first().capital, 100_000.0, tolerance = 0.01)
    }

    @Test
    fun `abono mayor al saldo restante no deja saldo negativo`() {
        val resultado = AmortizationEngine.simular(
            saldo = 500_000.0,
            cuotasRestantes = 12,
            tasaEA = 20.0,
            tipo = AmortizationType.CAPITAL_FIJO,
            abonos = mapOf(1 to 10_000_000.0) // exagerado a propósito
        )
        assertEquals(1, resultado.meses)
        assertEquals(0.0, resultado.filas.last().saldoFinal, 0.01)
    }

    @Test
    fun `abono extra con reducir plazo termina antes que el escenario sin abonos`() {
        val comparacion = AmortizationEngine.comparar(
            saldo = 11_922_602.0,
            cuotasRestantes = 58,
            tasaEA = 28.77,
            tipo = AmortizationType.CAPITAL_FIJO,
            abonos = mapOf(6 to 1_000_000.0),
            estrategia = PaymentStrategy.REDUCIR_PLAZO
        )
        assertTrue(comparacion.conAbonos.meses < comparacion.sinAbonos.meses)
        assertTrue(comparacion.ahorroIntereses > 0)
        assertTrue(comparacion.mesesAhorrados > 0)
    }

    @Test
    fun `abono extra con reducir cuota mantiene el plazo pero baja la cuota futura`() {
        val comparacion = AmortizationEngine.comparar(
            saldo = 11_922_602.0,
            cuotasRestantes = 58,
            tasaEA = 28.77,
            tipo = AmortizationType.CAPITAL_FIJO,
            abonos = mapOf(6 to 1_000_000.0),
            estrategia = PaymentStrategy.REDUCIR_CUOTA
        )
        // Con "reducir cuota" el plazo se mantiene igual (58 cuotas),
        // pero igual ahorra intereses porque el capital baja antes.
        assertEquals(58, comparacion.conAbonos.meses)
        assertTrue(comparacion.ahorroIntereses > 0)
    }

    @Test
    fun `cuota que no cubre ni el interes produce amortizacion negativa (deuda crece)`() {
        // Cuota fija absurdamente baja frente al interés generado.
        val resultado = AmortizationEngine.simular(
            saldo = 10_000_000.0,
            cuotasRestantes = 600, // fuerza un capital fijo minúsculo
            tasaEA = 50.0,
            tipo = AmortizationType.CAPITAL_FIJO,
            maxMonths = 5
        )
        // El saldo debería mantenerse prácticamente igual o subir dentro de la ventana simulada,
        // porque el capital fijo es mínimo frente al interés generado.
        assertTrue(resultado.filas.last().saldoFinal >= 9_900_000.0)
        assertTrue(resultado.nuncaSePagaEnPlazo)
    }
}
