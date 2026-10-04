package com.perseo.finance.savings.service

import com.perseo.finance.common.badRequest
import com.perseo.finance.common.notFound
import com.perseo.finance.savings.domain.BalanceCheck
import com.perseo.finance.savings.domain.Movement
import com.perseo.finance.savings.domain.MovementType
import com.perseo.finance.savings.domain.SavingsEngine
import com.perseo.finance.savings.domain.SavingsParams
import com.perseo.finance.savings.domain.SavingsTracker
import com.perseo.finance.savings.domain.TrackingInput
import com.perseo.finance.savings.domain.GoalStatus
import com.perseo.finance.savings.dto.GoalTrackingStatus
import com.perseo.finance.savings.dto.SavingsProgressResponse
import com.perseo.finance.savings.dto.SavingsTrackingMonthResponse
import com.perseo.finance.savings.entity.SavingsBalanceCheckEntity
import com.perseo.finance.savings.entity.SavingsContributionEntity
import com.perseo.finance.savings.entity.SavingsGoalEntity
import com.perseo.finance.savings.entity.SavingsPlanEntity
import com.perseo.finance.savings.repository.SavingsBalanceCheckRepository
import com.perseo.finance.savings.repository.SavingsContributionRepository
import com.perseo.finance.savings.repository.SavingsGoalRepository
import com.perseo.finance.savings.repository.SavingsPlanRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@Service
class SavingsProgressService(
    private val goalRepository: SavingsGoalRepository,
    private val planRepository: SavingsPlanRepository,
    private val contributionRepository: SavingsContributionRepository,
    private val balanceCheckRepository: SavingsBalanceCheckRepository,
    private val taxParameterService: TaxParameterService
) {

    companion object {
        const val AVISO =
            "Estimación: el saldo real se calcula con la tasa del plan; concilia con tu banco para corregir desvíos."
    }

    /** Plan vs. realidad de una meta (HU-A.16). `ritmo` reemplaza el promedio de los últimos 3 meses. */
    @Transactional(readOnly = true)
    fun obtener(goalId: UUID, ritmo: BigDecimal?): SavingsProgressResponse {
        val meta = goalRepository.findById(goalId).orElse(null) ?: notFound("Meta no encontrada.")
        return calcular(
            meta,
            planRepository.findByGoalIdOrderByCreatedAtAsc(goalId),
            contributionRepository.findByGoalIdOrderByFechaDescCreatedAtDesc(goalId),
            balanceCheckRepository.findByGoalIdOrderByFechaDescCreatedAtDesc(goalId),
            ritmo
        )
    }

    /**
     * Pasa la meta a ALCANZADA cuando el saldo real neto llega al objetivo, y la devuelve a ACTIVA si
     * después deja de cumplirlo (por ejemplo, tras un retiro). Las archivadas no se tocan.
     */
    @Transactional
    fun sincronizarEstado(goalId: UUID) {
        val meta = goalRepository.findById(goalId).orElse(null) ?: return
        val alcanzada = obtener(goalId, null).estado == GoalTrackingStatus.ALCANZADA
        val nuevo = when {
            alcanzada && meta.estado == GoalStatus.ACTIVA -> GoalStatus.ALCANZADA
            !alcanzada && meta.estado == GoalStatus.ALCANZADA -> GoalStatus.ACTIVA
            else -> return
        }
        meta.estado = nuevo
        meta.updatedAt = Instant.now()
        goalRepository.save(meta)
    }

    fun calcular(
        meta: SavingsGoalEntity,
        planes: List<SavingsPlanEntity>,
        movimientos: List<SavingsContributionEntity>,
        conciliaciones: List<SavingsBalanceCheckEntity>,
        ritmo: BigDecimal?,
        hoy: LocalDate = LocalDate.now()
    ): SavingsProgressResponse {
        val objetivo = meta.montoObjetivo
        val plan = planes.firstOrNull { it.esReferencia }

        if (plan == null) {
            val saldo = saldoSinPlan(meta, movimientos, conciliaciones)
            return SavingsProgressResponse(
                goalId = meta.id!!,
                planReferenciaId = null,
                estado = if (saldo >= objetivo) GoalTrackingStatus.ALCANZADA else GoalTrackingStatus.SIN_PLAN,
                montoObjetivo = objetivo,
                saldoRealEstimado = saldo,
                avanceMeta = ratio(saldo, objetivo),
                advertencias = listOf("Crea un plan para estimar rendimientos y proyectar la meta."),
                aviso = AVISO
            )
        }

        val tarifas = taxParameterService.resolverRetencion(
            plan.tipoProducto, plan.fechaInicio, plan.plazoMeses + SavingsEngine.MAX_MONTHS, plan.retencionTarifa
        )
        val params = SavingsParams(
            initialBalance = meta.saldoInicial,
            monthlyContribution = plan.aporteMensual,
            annualRateEA = plan.tasaEA,
            months = plan.plazoMeses,
            commissionEA = plan.comisionEA,
            timing = plan.momentoAporte,
            withholdingMode = plan.tratamientoRetencion,
            withholdingRateForMonth = { m -> tarifas.forMonth(m) },
            annualContributionGrowth = plan.crecimientoAporteAnual
        )

        val inicio = plan.fechaInicio.withDayOfMonth(1)
        val mesLimite = meta.fechaObjetivo
            ?.let { SavingsTracker.monthIndex(inicio, it) }
            ?.takeIf { it >= 1 }

        val resultado = try {
            SavingsTracker.track(
                TrackingInput(
                    plan = params,
                    startDate = plan.fechaInicio,
                    target = objetivo,
                    movements = movimientos.map {
                        Movement(it.fecha, if (it.tipo == MovementType.RETIRO) it.monto.negate() else it.monto)
                    },
                    checks = conciliaciones.map { BalanceCheck(it.fecha, it.saldoReal) },
                    today = hoy,
                    paceOverride = ritmo,
                    deadlineMonth = mesLimite
                )
            )
        } catch (e: IllegalArgumentException) {
            badRequest(e.message ?: "No se pudo calcular el seguimiento.")
        }

        return SavingsProgressResponse(
            goalId = meta.id!!,
            planReferenciaId = plan.id,
            estado = GoalTrackingStatus.valueOf(resultado.status.name),
            montoObjetivo = objetivo,
            saldoRealEstimado = resultado.realBalance,
            saldoRealNeto = resultado.realNetBalance,
            avanceMeta = ratio(resultado.realBalance, objetivo),
            mesActual = resultado.currentMonth,
            mesEvaluado = resultado.evalMonth,
            saldoPlaneado = resultado.plannedBalance,
            diferencia = resultado.difference,
            ritmoActual = resultado.pace,
            mesMetaPlan = resultado.planTargetMonth,
            mesMetaProyectado = resultado.projectedTargetMonth,
            fechaMetaProyectada = resultado.projectedTargetMonth?.let { inicio.plusMonths((it - 1).toLong()) },
            mesesDeRetraso = resultado.delayMonths,
            mesesParaMeta = resultado.projectedTargetMonth?.let { it - resultado.evalMonth },
            aporteNecesario = resultado.requiredContribution,
            mesesParaAporteNecesario = resultado.requiredForMonths,
            meses = resultado.rows.map { r ->
                SavingsTrackingMonthResponse(
                    mes = r.month,
                    fecha = r.date,
                    estado = r.status,
                    aportePlaneado = r.plannedContribution,
                    aporteReal = r.realContribution,
                    diferencia = r.difference,
                    saldoPlaneado = r.plannedBalance,
                    saldoReal = r.realBalance,
                    conciliado = r.reconciled
                )
            },
            advertencias = tarifas.warnings + resultado.warnings,
            aviso = AVISO
        )
    }

    /** Sin plan no hay tasa: saldo = último saldo conciliado (o el inicial) + movimientos posteriores. */
    private fun saldoSinPlan(
        meta: SavingsGoalEntity,
        movimientos: List<SavingsContributionEntity>,
        conciliaciones: List<SavingsBalanceCheckEntity>
    ): BigDecimal {
        val ultima = conciliaciones.maxByOrNull { it.fecha }
        val base = ultima?.saldoReal ?: meta.saldoInicial
        return movimientos
            .filter { ultima == null || it.fecha.isAfter(ultima.fecha) }
            .fold(base) { acc, m -> if (m.tipo == MovementType.RETIRO) acc - m.monto else acc + m.monto }
    }

    private fun ratio(valor: BigDecimal, objetivo: BigDecimal): BigDecimal =
        valor.divide(objetivo, 4, RoundingMode.HALF_UP)
}
