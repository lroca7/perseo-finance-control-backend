package com.perseo.finance.savings.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import kotlin.math.abs

/**
 * Valores de referencia verificados con una implementación independiente (Python/Decimal).
 * Escenario base: $4.200.000 iniciales, $600.000/mes, 11 % EA, 18 meses, CDT con retención 7 % al
 * vencimiento, plan iniciado el 2026-06-01, meta de $15.000.000. "Hoy" = 2026-10-01 (mes 5 del plan).
 */
class SavingsTrackerTest {

    private fun bd(s: String) = BigDecimal(s)
    private fun pesos(v: Long) = BigDecimal(v)
    private fun d(s: String) = LocalDate.parse(s)
    private fun mov(date: String, amount: Long) = Movement(d(date), pesos(amount))

    private fun assertPesos(expected: Long, actual: BigDecimal?, message: String = "") {
        assertEquals(expected, actual!!.setScale(0, RoundingMode.HALF_UP).toLong(), message)
    }

    private val plan = SavingsParams(
        initialBalance = pesos(4_200_000),
        monthlyContribution = pesos(600_000),
        annualRateEA = bd("0.11"),
        months = 18,
        withholdingMode = WithholdingMode.AL_VENCIMIENTO,
        withholdingRateForMonth = { bd("0.07") }
    )
    private val target = pesos(15_000_000)

    /** Junio a septiembre: $600.000 el primer mes y $200.000 los demás. */
    private val faltante = listOf(
        mov("2026-06-15", 600_000), mov("2026-07-15", 200_000),
        mov("2026-08-15", 200_000), mov("2026-09-15", 200_000)
    )
    private val alDia = listOf(
        mov("2026-06-15", 600_000), mov("2026-07-15", 600_000),
        mov("2026-08-15", 600_000), mov("2026-09-15", 600_000)
    )

    private fun track(
        movements: List<Movement>,
        checks: List<BalanceCheck> = emptyList(),
        today: String = "2026-10-01",
        pace: BigDecimal? = null,
        deadline: Int? = null,
        params: SavingsParams = plan
    ) = SavingsTracker.track(
        TrackingInput(
            plan = params, startDate = d("2026-06-01"), target = target,
            movements = movements, checks = checks, today = d(today),
            paceOverride = pace, deadlineMonth = deadline
        )
    )

    @Test
    fun `aportes menores al plan dejan atrasada la meta y proyectan al ritmo real`() {
        val r = track(faltante)

        assertEquals(5, r.currentMonth)
        assertEquals(4, r.evalMonth)
        assertPesos(6_780_302, r.plannedBalance)
        assertPesos(5_569_790, r.realBalance)
        assertPesos(5_557_905, r.realNetBalance)
        assertPesos(-1_210_512, r.difference)
        assertPesos(200_000, r.pace)
        assertEquals(17, r.planTargetMonth)
        assertEquals(38, r.projectedTargetMonth)
        assertEquals(21, r.delayMonths)
        assertPesos(646_101, r.requiredContribution)
        assertEquals(13, r.requiredForMonths)
        assertFalse(r.goalReached)
        assertEquals(TrackingStatus.ATRASADA, r.status)

        assertEquals(5, r.rows.size)
        assertEquals(d("2026-06-01"), r.rows[0].date)
        assertPesos(0, r.rows[0].difference)
        assertPesos(-400_000, r.rows[1].difference)
        assertPesos(5_569_790, r.rows[3].realBalance)
        assertPesos(6_780_302, r.rows[3].plannedBalance)
        // Mes en curso sin registros: "sin registrar", no $0 (RN-A.8)
        assertEquals(MonthStatus.EN_CURSO, r.rows[4].status)
        assertNull(r.rows[4].realContribution)
        assertNull(r.rows[4].difference)
        assertNull(r.rows[4].realBalance)
    }

    @Test
    fun `al dia con el plan no hay diferencia y se llega en el mes planeado`() {
        val r = track(alDia)
        assertPesos(0, r.difference)
        assertPesos(600_000, r.pace)
        assertEquals(17, r.projectedTargetMonth)
        assertEquals(0, r.delayMonths)
        assertPesos(547_632, r.requiredContribution)
        assertEquals(TrackingStatus.EN_CAMINO, r.status)
    }

    @Test
    fun `la fecha limite de la meta marca atrasada aunque se vaya al dia`() {
        assertEquals(TrackingStatus.ATRASADA, track(alDia, deadline = 10).status)
        assertEquals(TrackingStatus.EN_CAMINO, track(alDia, deadline = 20).status)
    }

    @Test
    fun `la conciliacion ancla el saldo de su mes y suma lo posterior`() {
        val movs = faltante + mov("2026-08-25", 50_000)
        val check = BalanceCheck(d("2026-08-20"), pesos(5_100_000))
        val r = track(movs, listOf(check))

        assertTrue(r.rows[2].reconciled)
        assertPesos(5_150_000, r.rows[2].realBalance) // 5.100.000 + 50.000 posteriores al 20/08
        assertPesos(5_394_983, r.realBalance)
        assertPesos(216_667, r.pace)
        assertEquals(37, r.projectedTargetMonth)
        assertPesos(659_458, r.requiredContribution)
    }

    @Test
    fun `un movimiento del mes en curso entra en la comparacion`() {
        val r = track(faltante + mov("2026-10-10", 200_000))

        assertEquals(5, r.evalMonth)
        assertPesos(7_439_525, r.plannedBalance)
        assertPesos(5_818_440, r.realBalance)
        assertPesos(200_000, r.rows[4].realContribution)
        assertPesos(5_818_440, r.rows[4].realBalance)
        // El mes en curso no entra en el promedio del ritmo (meses cerrados 2 a 4)
        assertPesos(200_000, r.pace)
        assertEquals(TrackingStatus.ATRASADA, r.status)
    }

    @Test
    fun `plan que empieza en el futuro no tiene meses reales`() {
        val r = track(emptyList(), today = "2026-05-10")
        assertEquals(0, r.currentMonth)
        assertEquals(0, r.evalMonth)
        assertTrue(r.rows.isEmpty())
        assertPesos(4_200_000, r.realBalance)
        assertPesos(4_200_000, r.plannedBalance)
        assertEquals(TrackingStatus.EN_CAMINO, r.status)
    }

    @Test
    fun `meta alcanzada no proyecta ni pide aporte`() {
        val r = track(emptyList(), params = plan.copy(initialBalance = pesos(20_000_000)))
        assertTrue(r.goalReached)
        assertEquals(TrackingStatus.ALCANZADA, r.status)
        assertNull(r.projectedTargetMonth)
        assertNull(r.requiredContribution)
    }

    @Test
    fun `el ritmo manual reemplaza al promedio`() {
        val r = track(faltante, pace = pesos(650_000))
        assertPesos(650_000, r.pace)
        assertEquals(17, r.projectedTargetMonth)
    }

    @Test
    fun `sin movimientos el ritmo es cero, no hay proyeccion y los meses cerrados cuentan cero`() {
        val r = track(emptyList())
        assertPesos(0, r.pace)
        assertNull(r.projectedTargetMonth)
        assertTrue(r.warnings.any { it.contains("cero") })
        assertPesos(745_378, r.requiredContribution)
        assertPesos(0, r.rows[0].realContribution)
        assertPesos(-600_000, r.rows[0].difference)
        assertNull(r.rows[4].realContribution)
    }

    @Test
    fun `los retiros restan del aporte del mes y del ritmo`() {
        val movs = alDia + mov("2026-09-20", -200_000)
        val r = track(movs)
        assertPesos(400_000, r.rows[3].realContribution)
        assertPesos(-200_000, r.rows[3].difference)
        assertPesos(533_333, r.pace) // (600.000 + 600.000 + 400.000) / 3
    }

    @Test
    fun `plan terminado cierra todos los meses y no pide aporte nuevo`() {
        val r = track(emptyList(), today = "2028-09-01")
        assertEquals(28, r.currentMonth)
        assertEquals(18, r.evalMonth)
        assertEquals(18, r.rows.size)
        assertTrue(r.rows.all { it.status == MonthStatus.CERRADO })
        assertPesos(4_861_901, r.realBalance)
        assertNull(r.requiredContribution)
        assertTrue(r.warnings.any { it.contains("venció") })
        assertEquals(TrackingStatus.ATRASADA, r.status)
    }

    @Test
    fun `el rendimiento ya causado aumenta la retencion al vencimiento`() {
        val base = SavingsParams(
            initialBalance = pesos(1_000_000), monthlyContribution = BigDecimal.ZERO,
            annualRateEA = bd("0.11"), months = 12,
            withholdingMode = WithholdingMode.AL_VENCIMIENTO, withholdingRateForMonth = { bd("0.07") }
        )
        val sin = SavingsEngine.simulate(base).summary.totalTax
        val con = SavingsEngine.simulate(base.copy(initialAccumulatedReturn = pesos(100_000))).summary.totalTax
        assertTrue(abs((con - sin).toDouble() - 7_000.0) <= 1.0, "la diferencia debería ser ≈ 7 % de 100.000")
    }
}
