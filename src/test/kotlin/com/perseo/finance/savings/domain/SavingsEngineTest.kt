package com.perseo.finance.savings.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.abs

/**
 * Valores de referencia verificados con una implementación independiente (Python/Decimal)
 * que replica las reglas RN-A.1 a RN-A.12.
 */
class SavingsEngineTest {

    private fun bd(s: String) = BigDecimal(s)
    private fun pesos(v: Long) = BigDecimal(v)

    private fun assertPesos(expected: Long, actual: BigDecimal, message: String = "") =
        assertEquals(expected, actual.setScale(0, RoundingMode.HALF_UP).toLong(), message)

    private fun assertNear(expected: Double, actual: BigDecimal, tolerance: Double) =
        assertTrue(abs(actual.toDouble() - expected) < tolerance, "esperado ≈ $expected, fue $actual")

    private fun rate7(): (Int) -> BigDecimal = { bd("0.07") }

    /** Caso base: $4.200.000 + $600.000/mes al fin de mes, 11 % EA, 18 meses. */
    private val baseA = SavingsParams(
        initialBalance = pesos(4_200_000),
        monthlyContribution = pesos(600_000),
        annualRateEA = bd("0.11"),
        months = 18
    )

    // ---------- Tasas (RN-A.1, RN-A.2) ----------

    @Test
    fun `convierte tasas con la formula compuesta y no con EA dividido 12`() {
        assertNear(0.0087345938, SavingsRates.monthlyFromEA(bd("0.11")), 1e-9)
        assertNear(0.126825, SavingsRates.fromNominalMonthly(bd("0.12")), 1e-6)
        assertNear(0.1038129, SavingsRates.fromNominalQuarterly(bd("0.10")), 1e-6)
    }

    @Test
    fun `comision se descuenta con la formula de division`() {
        assertNear(0.0980392157, SavingsRates.netOfCommission(bd("0.12"), bd("0.02")), 1e-9)
        assertEquals(bd("0.12"), SavingsRates.netOfCommission(bd("0.12"), BigDecimal.ZERO))
    }

    // ---------- Simulación sin impuestos ----------

    @Test
    fun `plan base sin impuestos coincide con la referencia`() {
        val r = SavingsEngine.simulate(baseA)
        assertEquals(18, r.rows.size)
        assertPesos(36_685, r.rows[0].grossReturn)
        assertPesos(4_836_685, r.rows[0].closingBalance)
        assertPesos(5_478_931, r.rows[1].closingBalance)
        assertPesos(6_126_787, r.rows[2].closingBalance)
        assertPesos(6_780_302, r.rows[3].closingBalance)
        assertPesos(16_552_165, r.rows[17].closingBalance)

        assertPesos(15_000_000, r.summary.totalContributed)
        assertPesos(1_552_165, r.summary.totalGrossReturn)
        assertPesos(0, r.summary.totalTax)
        assertPesos(16_552_165, r.summary.finalNetBalance)
    }

    @Test
    fun `aporte al inicio rinde desde el primer mes`() {
        val r = SavingsEngine.simulate(baseA.copy(timing = ContributionTiming.INICIO))
        assertPesos(41_926, r.rows[0].grossReturn)
        assertPesos(4_841_926, r.rows[0].closingBalance)
        assertPesos(16_653_839, r.rows[17].closingBalance)
    }

    // ---------- Retención (RN-A.3) ----------

    @Test
    fun `retencion al vencimiento solo se cobra en el ultimo mes`() {
        val r = SavingsEngine.simulate(
            baseA.copy(withholdingMode = WithholdingMode.AL_VENCIMIENTO, withholdingRateForMonth = rate7())
        )
        assertPesos(0, r.rows[16].tax)
        assertTrue(r.rows[16].pendingTax.signum() > 0, "debe haber retención causada pendiente")
        assertPesos(108_652, r.rows[17].tax)
        assertPesos(0, r.rows[17].pendingTax)
        assertPesos(108_652, r.summary.totalTax)
        assertPesos(16_443_513, r.summary.finalNetBalance)
    }

    @Test
    fun `retencion mensual deja menos saldo que al vencimiento`() {
        val mensual = SavingsEngine.simulate(baseA.copy(withholdingRateForMonth = rate7()))
        val alVenc = SavingsEngine.simulate(
            baseA.copy(withholdingMode = WithholdingMode.AL_VENCIMIENTO, withholdingRateForMonth = rate7())
        )
        assertPesos(9_605, mensual.rows[17].tax)
        assertPesos(108_193, mensual.summary.totalTax)
        assertPesos(16_437_430, mensual.summary.finalNetBalance)
        assertTrue(alVenc.summary.finalNetBalance > mensual.summary.finalNetBalance)
    }

    @Test
    fun `la tarifa de retencion se consulta por mes`() {
        val r = SavingsEngine.simulate(
            baseA.copy(withholdingRateForMonth = { m -> if (m <= 6) bd("0.07") else bd("0.10") })
        )
        val row6 = r.rows[5]
        val row7 = r.rows[6]
        assertEquals(row6.grossReturn.multiply(bd("0.07")).setScale(0, RoundingMode.HALF_UP), row6.tax)
        assertEquals(row7.grossReturn.multiply(bd("0.10")).setScale(0, RoundingMode.HALF_UP), row7.tax)
    }

    // ---------- Comisión (RN-A.2) ----------

    @Test
    fun `comision reduce el saldo segun la EA neta`() {
        val sin = SavingsEngine.simulate(
            SavingsParams(pesos(1_000_000), BigDecimal.ZERO, bd("0.12"), 12)
        )
        val con = SavingsEngine.simulate(
            SavingsParams(pesos(1_000_000), BigDecimal.ZERO, bd("0.12"), 12, commissionEA = bd("0.02"))
        )
        assertPesos(1_120_001, sin.summary.finalNetBalance)
        assertPesos(1_098_039, con.summary.finalNetBalance) // ≈ 9,8039 % EA
        assertPesos(20_856, con.summary.totalCommission)
    }

    // ---------- Identidad contable (RN-A.12) ----------

    @Test
    fun `saldo neto final cumple la identidad contable`() {
        val configs = listOf(
            baseA,
            baseA.copy(withholdingRateForMonth = rate7()),
            baseA.copy(withholdingMode = WithholdingMode.AL_VENCIMIENTO, withholdingRateForMonth = rate7()),
            baseA.copy(
                commissionEA = bd("0.02"),
                timing = ContributionTiming.INICIO,
                withholdingRateForMonth = rate7(),
                annualContributionGrowth = bd("0.05")
            ),
            baseA.copy(
                commissionEA = bd("0.015"),
                withholdingMode = WithholdingMode.AL_VENCIMIENTO,
                withholdingRateForMonth = rate7()
            )
        )
        configs.forEach { cfg ->
            val s = SavingsEngine.simulate(cfg).summary
            val esperado = s.totalContributed + s.totalGrossReturn - s.totalCommission - s.totalTax
            assertEquals(esperado.setScale(0, RoundingMode.HALF_UP), s.finalNetBalance.setScale(0, RoundingMode.HALF_UP))
        }
    }

    // ---------- Aporte creciente ----------

    @Test
    fun `aporte creciente se aplica en cada aniversario`() {
        val r = SavingsEngine.simulate(
            SavingsParams(
                initialBalance = BigDecimal.ZERO,
                monthlyContribution = pesos(100_000),
                annualRateEA = BigDecimal.ZERO,
                months = 36,
                annualContributionGrowth = bd("0.10")
            )
        )
        assertPesos(100_000, r.rows[11].contribution)
        assertPesos(110_000, r.rows[12].contribution)
        assertPesos(110_000, r.rows[23].contribution)
        assertPesos(121_000, r.rows[24].contribution)
        assertPesos(3_972_000, r.summary.finalNetBalance)
    }

    // ---------- Seguimiento real (A.11, A.14, A.16) ----------

    private val realByMonth = mapOf(1 to 600_000L, 2 to 200_000L, 3 to 200_000L, 4 to 200_000L)

    private fun realParams(anchors: Map<Int, BigDecimal> = emptyMap()) = SavingsParams(
        initialBalance = pesos(4_200_000),
        monthlyContribution = BigDecimal.ZERO,
        annualRateEA = bd("0.11"),
        months = 4,
        contributionForMonth = { m -> pesos(realByMonth.getValue(m)) },
        balanceAnchors = anchors
    )

    @Test
    fun `aportes reales menores dejan menos saldo que el plan`() {
        val real = SavingsEngine.simulate(realParams()).rows[3].closingBalance
        val plan = SavingsEngine.simulate(baseA.copy(months = 4)).rows[3].closingBalance
        assertPesos(5_569_790, real)
        assertPesos(6_780_302, plan)
        assertPesos(-1_210_512, real - plan)
    }

    @Test
    fun `la conciliacion ancla el saldo y los meses siguientes parten de ahi`() {
        val rows = SavingsEngine.simulate(realParams(mapOf(2 to pesos(5_000_000)))).rows
        assertPesos(4_836_685, rows[0].closingBalance)
        assertPesos(5_000_000, rows[1].closingBalance)
        assertPesos(5_243_673, rows[2].closingBalance)
        assertPesos(5_489_474, rows[3].closingBalance)
    }

    @Test
    fun `proyeccion al ritmo actual desde el saldo real`() {
        val desdeReal = SavingsParams(
            initialBalance = pesos(5_569_790),
            monthlyContribution = pesos(200_000),
            annualRateEA = bd("0.11"),
            months = 1,
            withholdingMode = WithholdingMode.AL_VENCIMIENTO,
            withholdingRateForMonth = rate7()
        )
        // 34 meses después del mes 4 => mes 38 del plan
        assertEquals(34, SavingsEngine.monthsToReach(desdeReal, pesos(15_000_000)))
    }

    // ---------- Solvers (A.6, A.7) ----------

    @Test
    fun `meses para llegar a la meta`() {
        val alVenc = baseA.copy(withholdingMode = WithholdingMode.AL_VENCIMIENTO, withholdingRateForMonth = rate7())
        assertEquals(17, SavingsEngine.monthsToReach(alVenc, pesos(15_000_000)))
        assertEquals(16, SavingsEngine.monthsToReach(baseA, pesos(15_000_000)))
        assertEquals(0, SavingsEngine.monthsToReach(baseA, pesos(4_000_000)))
    }

    @Test
    fun `meses para llegar devuelve null si no se alcanza`() {
        val sinCrecer = SavingsParams(pesos(1_000_000), BigDecimal.ZERO, BigDecimal.ZERO, 12)
        assertNull(SavingsEngine.monthsToReach(sinCrecer, pesos(2_000_000)))
    }

    @Test
    fun `aporte requerido llega a la meta y es el minimo en pesos enteros`() {
        val p = SavingsParams(
            initialBalance = pesos(5_569_790),
            monthlyContribution = BigDecimal.ZERO,
            annualRateEA = bd("0.11"),
            months = 13,
            withholdingMode = WithholdingMode.AL_VENCIMIENTO,
            withholdingRateForMonth = rate7()
        )
        val target = pesos(15_000_000)
        val required = SavingsEngine.requiredMonthlyContribution(p, target)

        fun finalNet(c: BigDecimal) = SavingsEngine.simulate(p.copy(monthlyContribution = c)).summary.finalNetBalance
        assertPesos(645_231, required)
        assertTrue(finalNet(required) >= target)
        assertTrue(finalNet(required - BigDecimal.ONE) < target)
    }

    @Test
    fun `aporte requerido es cero si el saldo inicial ya cubre la meta`() {
        val p = SavingsParams(pesos(16_000_000), BigDecimal.ZERO, bd("0.11"), 12)
        assertPesos(0, SavingsEngine.requiredMonthlyContribution(p, pesos(15_000_000)))
    }

    // ---------- Valor real (RN-A.5) ----------

    @Test
    fun `valor real descuenta la inflacion`() {
        assertPesos(952_381, SavingsRates.realValue(pesos(1_000_000), bd("0.05"), 12))
        assertPesos(907_029, SavingsRates.realValue(pesos(1_000_000), bd("0.05"), 24))
        assertPesos(1_000_000, SavingsRates.realValue(pesos(1_000_000), BigDecimal.ZERO, 24))
    }

    // ---------- Validaciones ----------

    @Test
    fun `rechaza parametros invalidos`() {
        listOf(
            baseA.copy(months = 0),
            baseA.copy(months = SavingsEngine.MAX_MONTHS + 1),
            baseA.copy(initialBalance = pesos(-1)),
            baseA.copy(monthlyContribution = pesos(-1)),
            baseA.copy(annualRateEA = bd("-0.01")),
            baseA.copy(commissionEA = bd("1")),
            baseA.copy(commissionEA = bd("-0.01"))
        ).forEach { invalid ->
            assertThrows(IllegalArgumentException::class.java) { SavingsEngine.simulate(invalid) }
        }
    }
}
