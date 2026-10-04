package com.perseo.finance.savings.dto

import com.perseo.finance.savings.domain.ContributionTiming
import com.perseo.finance.savings.domain.ProductType
import com.perseo.finance.savings.domain.RateOrigin
import com.perseo.finance.savings.domain.WithholdingMode
import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import java.math.BigDecimal
import java.time.LocalDate

/** Simulación en vivo, sin guardar nada (HU-A.2). Tasas como FRACCIÓN. */
data class SavingsSimulationPreviewRequest(
    @field:DecimalMin(value = "0", message = "El saldo inicial no puede ser negativo.")
    val saldoInicial: BigDecimal = BigDecimal.ZERO,

    /** Opcional: si viene, el resultado incluye avance y mes en que se alcanza la meta. */
    @field:DecimalMin(value = "0.01", message = "El monto objetivo debe ser mayor a cero.")
    val montoObjetivo: BigDecimal? = null,

    val tipoProducto: ProductType,

    @field:DecimalMin(value = "0.000001", message = "La tasa debe ser mayor a cero.")
    @field:DecimalMax(value = "1", message = "La tasa no puede superar 100 %.")
    val tasaEA: BigDecimal,

    @field:DecimalMin(value = "0", message = "La comisión no puede ser negativa.")
    @field:DecimalMax(value = "0.99", message = "La comisión debe ser menor a 100 %.")
    val comisionEA: BigDecimal = BigDecimal.ZERO,

    @field:DecimalMin(value = "0", message = "El aporte mensual no puede ser negativo.")
    val aporteMensual: BigDecimal = BigDecimal.ZERO,

    val momentoAporte: ContributionTiming = ContributionTiming.FIN,

    @field:DecimalMin(value = "0", message = "El crecimiento del aporte no puede ser negativo.")
    @field:DecimalMax(value = "0.5", message = "El crecimiento del aporte no puede superar 50 %.")
    val crecimientoAporteAnual: BigDecimal = BigDecimal.ZERO,

    @field:Min(value = 1, message = "El plazo mínimo es 1 mes.")
    @field:Max(value = 600, message = "El plazo máximo es 600 meses.")
    val plazoMeses: Int,

    val fechaInicio: LocalDate? = null,

    @field:DecimalMin(value = "0", message = "La inflación no puede ser negativa.")
    @field:DecimalMax(value = "0.5", message = "La inflación no puede superar 50 %.")
    val inflacionAnual: BigDecimal = BigDecimal.ZERO,

    val tratamientoRetencion: WithholdingMode? = null,

    @field:DecimalMin(value = "0", message = "La retención no puede ser negativa.")
    @field:DecimalMax(value = "1", message = "La retención no puede superar 100 %.")
    val retencionTarifa: BigDecimal? = null
)

data class SavingsSimulationMonthResponse(
    val mes: Int,
    /** Primer día del mes calendario correspondiente (RN-A.7). */
    val fecha: LocalDate,
    val saldoInicial: BigDecimal,
    val aporte: BigDecimal,
    val rendimientoBruto: BigDecimal,
    val comision: BigDecimal,
    val impuesto: BigDecimal,
    val saldoFinal: BigDecimal,
    /** Retención causada y aún no cobrada (solo AL_VENCIMIENTO). */
    val impuestoPendiente: BigDecimal,
    /** Lo que quedaría en mano si se retirara ese mes. */
    val saldoNeto: BigDecimal,
    /** true en el mes en que el saldo neto alcanza la meta por primera vez. */
    val metaAlcanzada: Boolean
)

data class SavingsSimulationSummaryResponse(
    val totalAportado: BigDecimal,
    val rendimientoBruto: BigDecimal,
    val comisiones: BigDecimal,
    val impuestos: BigDecimal,
    val saldoNetoFinal: BigDecimal,
    /** Saldo neto final en pesos de hoy (RN-A.5). Igual al nominal si la inflación es 0. */
    val valorRealSaldoNeto: BigDecimal,
    /** saldoNetoFinal / meta, como fracción (1.10 = 110 %). null si no hay meta. */
    val avanceMeta: BigDecimal?,
    /** 0 = ya alcanzada con el saldo inicial; null = no se alcanza en el plazo (o sin meta). */
    val mesMetaAlcanzada: Int?
)

data class TaxParameterUsedResponse(
    val tarifa: BigDecimal,
    val vigenteDesde: LocalDate?,
    val vigenteHasta: LocalDate?,
    val origen: RateOrigin
)

data class SavingsSimulationResponse(
    val meses: List<SavingsSimulationMonthResponse>,
    val resumen: SavingsSimulationSummaryResponse,
    /** Tarifas de retención usadas, para que el resultado sea auditable (HU-A.2). */
    val parametrosTributarios: List<TaxParameterUsedResponse>,
    val advertencias: List<String>,
    val aviso: String
)
