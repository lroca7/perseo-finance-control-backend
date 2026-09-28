package com.perseo.finance.pdf

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate

class DaviviendaStatementParserTest {

    // Texto simplificado a partir del extracto real usado para validar el
    // resto del proyecto (Davivienda, periodo Ago-Sep 2026).
    private val textoExtractoReal = """
        Extracto septiembre
        Periodo de facturación:
        08/Ago/2026 - 08/Sep/2026
        LIZETH RODRIGUEZ CABRALES

        Fecha límite de pago
        25/Sep/2026

        Pago mínimo
        ${'$'}672,672
        Pago total
        ${'$'}12,595,274

        Cupo disponible
        ${'$'}4,726
        Cupo total
        ${'$'}12,600,000

        +Cuota de manejo ${'$'}41,000
        +Otros cargos ${'$'}5,990

        REDIFE CAPITAL ${'$'}12,333,726 2 de 60 ${'$'}260,811 ${'$'}205,562 ${'$'}11,922,602 0 1000001 28.77
        REDIFE COSTO AD ${'$'}54,490 2 de 2 ${'$'}0 ${'$'}27,245 ${'$'}0 0 1000002 0.00
        GOOGLE *PLAY YOUTUBE*DL ${'$'}19,900 1 de 1 ${'$'}0 ${'$'}19,900 ${'$'}0 7 0000671 29.23
    """.trimIndent()

    @Test
    fun `extrae los campos generales del extracto`() {
        val resultado = DaviviendaStatementParser.parse(textoExtractoReal)

        assertEquals(LocalDate.of(2026, 8, 8), resultado.periodoInicio)
        assertEquals(LocalDate.of(2026, 9, 8), resultado.periodoFin)
        assertEquals(LocalDate.of(2026, 9, 25), resultado.fechaLimitePago)
        assertEquals(0, BigDecimal("672672").compareTo(resultado.pagoMinimo))
        assertEquals(0, BigDecimal("12595274").compareTo(resultado.saldoTotal))
        assertEquals(0, BigDecimal("12600000").compareTo(resultado.cupoTotal))
        assertEquals(0, BigDecimal("41000").compareTo(resultado.cuotaManejo))
        assertTrue(resultado.camposFaltantes.isEmpty(), "no debería faltar ningún campo obligatorio")
    }

    @Test
    fun `detecta REDIFE CAPITAL como candidato pero ignora compras a 1 cuota y diferidos saldados`() {
        val resultado = DaviviendaStatementParser.parse(textoExtractoReal)

        assertEquals(1, resultado.candidatosLineasCredito.size)
        val redife = resultado.candidatosLineasCredito.first()
        assertEquals("REDIFE CAPITAL", redife.nombre)
        assertEquals(2, redife.cuotaActual)
        assertEquals(60, redife.cuotasTotales)
        assertEquals(0, BigDecimal("11922602").compareTo(redife.saldoPendiente))
        assertEquals(0, BigDecimal("28.77").compareTo(redife.tasaEA))
    }

    @Test
    fun `campos ausentes quedan reportados en camposFaltantes en vez de asumidos`() {
        val textoIncompleto = "Un PDF que no tiene el formato esperado en absoluto."
        val resultado = DaviviendaStatementParser.parse(textoIncompleto)

        assertNull(resultado.pagoMinimo)
        assertNull(resultado.saldoTotal)
        assertTrue(resultado.camposFaltantes.contains("pagoMinimo"))
        assertTrue(resultado.candidatosLineasCredito.isEmpty())
    }
}
