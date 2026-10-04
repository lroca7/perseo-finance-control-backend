package com.perseo.finance.savings.service

import com.perseo.finance.common.badRequest
import com.perseo.finance.common.notFound
import com.perseo.finance.savings.domain.ContributionTiming
import com.perseo.finance.savings.domain.ProductType
import com.perseo.finance.savings.domain.SavingsEngine
import com.perseo.finance.savings.domain.SavingsParams
import com.perseo.finance.savings.domain.SavingsRates
import com.perseo.finance.savings.domain.WithholdingMode
import com.perseo.finance.savings.dto.SavingsSimulationMonthResponse
import com.perseo.finance.savings.dto.SavingsSimulationPreviewRequest
import com.perseo.finance.savings.dto.SavingsSimulationResponse
import com.perseo.finance.savings.dto.SavingsSimulationSummaryResponse
import com.perseo.finance.savings.dto.TaxParameterUsedResponse
import com.perseo.finance.savings.repository.SavingsGoalRepository
import com.perseo.finance.savings.repository.SavingsPlanRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.util.UUID

/** Entrada común: sale de un plan guardado + su meta, o de una simulación en vivo. */
data class SavingsSimulationInput(
    val saldoInicial: BigDecimal,
    val montoObjetivo: BigDecimal?,
    val tipoProducto: ProductType,
    val tasaEA: BigDecimal,
    val comisionEA: BigDecimal,
    val aporteMensual: BigDecimal,
    val momentoAporte: ContributionTiming,
    val crecimientoAporteAnual: BigDecimal,
    val plazoMeses: Int,
    val fechaInicio: LocalDate,
    val inflacionAnual: BigDecimal,
    val tratamientoRetencion: WithholdingMode,
    val retencionTarifa: BigDecimal?
)

@Service
class SavingsSimulationService(
    private val planRepository: SavingsPlanRepository,
    private val goalRepository: SavingsGoalRepository,
    private val taxParameterService: TaxParameterService
) {

    companion object {
        const val AVISO =
            "Estimación: la liquidación real depende de tu entidad y de tu situación tributaria."
    }

    /** Simula un plan guardado usando el saldo inicial y el objetivo de su meta. */
    @Transactional(readOnly = true)
    fun simularPlan(planId: UUID): SavingsSimulationResponse {
        val plan = planRepository.findById(planId).orElse(null) ?: notFound("Plan no encontrado.")
        val meta = goalRepository.findById(plan.goalId).orElse(null) ?: notFound("Meta no encontrada.")
        return ejecutar(
            SavingsSimulationInput(
                saldoInicial = meta.saldoInicial,
                montoObjetivo = meta.montoObjetivo,
                tipoProducto = plan.tipoProducto,
                tasaEA = plan.tasaEA,
                comisionEA = plan.comisionEA,
                aporteMensual = plan.aporteMensual,
                momentoAporte = plan.momentoAporte,
                crecimientoAporteAnual = plan.crecimientoAporteAnual,
                plazoMeses = plan.plazoMeses,
                fechaInicio = plan.fechaInicio,
                inflacionAnual = plan.inflacionAnual,
                tratamientoRetencion = plan.tratamientoRetencion,
                retencionTarifa = plan.retencionTarifa
            )
        )
    }

    /** Simulación en vivo: no guarda nada (HU-A.2 criterio 2). */
    @Transactional(readOnly = true)
    fun previsualizar(request: SavingsSimulationPreviewRequest): SavingsSimulationResponse {
        if (!request.tipoProducto.availableInMvp) {
            badRequest("El producto ${request.tipoProducto.label} aún no está disponible.")
        }
        return ejecutar(
            SavingsSimulationInput(
                saldoInicial = request.saldoInicial,
                montoObjetivo = request.montoObjetivo,
                tipoProducto = request.tipoProducto,
                tasaEA = request.tasaEA,
                comisionEA = request.comisionEA,
                aporteMensual = request.aporteMensual,
                momentoAporte = request.momentoAporte,
                crecimientoAporteAnual = request.crecimientoAporteAnual,
                plazoMeses = request.plazoMeses,
                fechaInicio = request.fechaInicio ?: LocalDate.now().withDayOfMonth(1),
                inflacionAnual = request.inflacionAnual,
                tratamientoRetencion = request.tratamientoRetencion ?: request.tipoProducto.defaultWithholding,
                retencionTarifa = request.retencionTarifa
            )
        )
    }

    fun ejecutar(input: SavingsSimulationInput): SavingsSimulationResponse {
        val tarifas = taxParameterService.resolverRetencion(
            input.tipoProducto, input.fechaInicio, input.plazoMeses, input.retencionTarifa
        )

        val params = SavingsParams(
            initialBalance = input.saldoInicial,
            monthlyContribution = input.aporteMensual,
            annualRateEA = input.tasaEA,
            months = input.plazoMeses,
            commissionEA = input.comisionEA,
            timing = input.momentoAporte,
            withholdingMode = input.tratamientoRetencion,
            withholdingRateForMonth = { m -> tarifas.forMonth(m) },
            annualContributionGrowth = input.crecimientoAporteAnual
        )

        val resultado = try {
            SavingsEngine.simulate(params)
        } catch (e: IllegalArgumentException) {
            badRequest(e.message ?: "Parámetros de simulación inválidos.")
        }

        val objetivo = input.montoObjetivo
        val mesMeta: Int? = when {
            objetivo == null -> null
            input.saldoInicial >= objetivo -> 0
            else -> resultado.rows.firstOrNull { it.netBalance >= objetivo }?.month
        }

        val primerDia = input.fechaInicio.withDayOfMonth(1)
        val meses = resultado.rows.map { r ->
            SavingsSimulationMonthResponse(
                mes = r.month,
                fecha = primerDia.plusMonths((r.month - 1).toLong()),
                saldoInicial = r.openingBalance,
                aporte = r.contribution,
                rendimientoBruto = r.grossReturn,
                comision = r.commission,
                impuesto = r.tax,
                saldoFinal = r.closingBalance,
                impuestoPendiente = r.pendingTax,
                saldoNeto = r.netBalance,
                metaAlcanzada = mesMeta != null && mesMeta == r.month
            )
        }

        val s = resultado.summary
        val resumen = SavingsSimulationSummaryResponse(
            totalAportado = s.totalContributed,
            rendimientoBruto = s.totalGrossReturn,
            comisiones = s.totalCommission,
            impuestos = s.totalTax,
            saldoNetoFinal = s.finalNetBalance,
            valorRealSaldoNeto = SavingsRates.realValue(s.finalNetBalance, input.inflacionAnual, input.plazoMeses),
            avanceMeta = objetivo?.let { s.finalNetBalance.divide(it, 4, RoundingMode.HALF_UP) },
            mesMetaAlcanzada = mesMeta
        )

        return SavingsSimulationResponse(
            meses = meses,
            resumen = resumen,
            parametrosTributarios = tarifas.used.map { u ->
                TaxParameterUsedResponse(
                    tarifa = u.tarifa,
                    vigenteDesde = u.entry?.vigenteDesde,
                    vigenteHasta = u.entry?.vigenteHasta,
                    origen = u.origin
                )
            },
            advertencias = tarifas.warnings,
            aviso = AVISO
        )
    }
}
