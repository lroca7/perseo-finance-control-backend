package com.perseo.finance.pdf

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate

class BancolombiaStatementParserTest {

    private val textoExtractoReal = """
        Tarjeta: *********5631
        LIZETH RODRIGUEZ CABRALES
        Cupo de tu tarjeta
        Deuda a la fecha de corte:
        ${'$'}2.316.122,00
        Cupo total: ${'$'} 3.055.000,00
        Disponible: ${'$'} 738.878,90

        Periodo facturado
        17 ago - 15 sep. 2026
        Pago Total:
        ${'$'} 2.316.122,00
        Pagar antes de:
        oct. 05, 2026
        Pago mínimo:
        ${'$'} 316.124,00

        Otros Cargos ${'$'} 0,00

        AVANCE SUCURSAL VIRTUAL329792 10/10/2025 ${'$'} 900.000,00 12/24 ${'$'} 37.500,00 1,8312 % 24,3283 % ${'$'} 450.000,00
        AVANCE SUCURSAL VIRTUAL373187 15/07/2025 ${'$'} 1.400.000,00 15/24 ${'$'} 58.333,33 1,8594 % 24,7421 % ${'$'} 525.000,05
        AVANCE SUCURSAL VIRTUAL052169 21/06/2025 ${'$'} 3.000.000,00 15/24 ${'$'} 125.000,00 1,9129 % 25,5306 % ${'$'} 1.024.997,88
        EXITO CARTAGENA002213 08/09/2026 ${'$'} 52.690,00 1/1 ${'$'} 52.690,00 0,0000 % 00,0000 % ${'$'} 0,00
    """.trimIndent()

    @Test
    fun `extrae los campos generales del extracto Bancolombia`() {
        val resultado = BancolombiaStatementParser.parse(textoExtractoReal)

        assertEquals(LocalDate.of(2026, 8, 17), resultado.periodoInicio)
        assertEquals(LocalDate.of(2026, 9, 15), resultado.periodoFin)
        assertEquals(LocalDate.of(2026, 10, 5), resultado.fechaLimitePago)
        assertEquals(0, BigDecimal("316124.00").compareTo(resultado.pagoMinimo))
        assertEquals(0, BigDecimal("2316122.00").compareTo(resultado.saldoTotal))
        assertEquals(0, BigDecimal("3055000.00").compareTo(resultado.cupoTotal))
        assertEquals(0, BigDecimal.ZERO.compareTo(resultado.cuotaManejo))
        assertTrue(resultado.camposFaltantes.isEmpty())
    }

    @Test
    fun `detecta los avances a cuotas pero ignora la compra a 1 cuota`() {
        val resultado = BancolombiaStatementParser.parse(textoExtractoReal)

        assertEquals(3, resultado.candidatosLineasCredito.size)
        assertTrue(resultado.candidatosLineasCredito.all { it.nombre == "AVANCE SUCURSAL VIRTUAL" })

        val primero = resultado.candidatosLineasCredito[0]
        assertEquals(12, primero.cuotaActual)
        assertEquals(24, primero.cuotasTotales)
        assertEquals(0, BigDecimal("450000.00").compareTo(primero.saldoPendiente))
        assertEquals(0, BigDecimal("24.3283").compareTo(primero.tasaEA))
    }

    @Test
    fun `parsea correctamente la notacion numerica invertida (punto de miles, coma decimal)`() {
        val resultado = BancolombiaStatementParser.parse(textoExtractoReal)
        // 316.124,00 en notación Bancolombia = 316124.00, no 316.124 con decimales truncados
        assertEquals(0, BigDecimal("316124.00").compareTo(resultado.pagoMinimo))
    }
}
