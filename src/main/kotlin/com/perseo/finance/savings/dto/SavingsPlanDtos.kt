package com.perseo.finance.savings.dto

import com.perseo.finance.savings.domain.ContributionTiming
import com.perseo.finance.savings.domain.ProductType
import com.perseo.finance.savings.domain.WithholdingMode
import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

/**
 * Se usa para crear y para actualizar un plan.
 * Tasas y porcentajes como FRACCIÓN (0.11 = 11 %).
 */
data class SavingsPlanRequest(
    @field:NotBlank(message = "Escribe un nombre para el plan.")
    @field:Size(max = 80, message = "El nombre no puede superar 80 caracteres.")
    val nombre: String,

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

    /** null = primer día del mes actual. */
    val fechaInicio: LocalDate? = null,

    @field:DecimalMin(value = "0", message = "La inflación no puede ser negativa.")
    @field:DecimalMax(value = "0.5", message = "La inflación no puede superar 50 %.")
    val inflacionAnual: BigDecimal = BigDecimal.ZERO,

    /** null = el que corresponde al producto (cuenta: mensual, CDT: al vencimiento). */
    val tratamientoRetencion: WithholdingMode? = null,

    /** null = usar la tarifa vigente de los parámetros tributarios. */
    @field:DecimalMin(value = "0", message = "La retención no puede ser negativa.")
    @field:DecimalMax(value = "1", message = "La retención no puede superar 100 %.")
    val retencionTarifa: BigDecimal? = null
)

data class SavingsPlanResponse(
    val id: UUID,
    val goalId: UUID,
    val nombre: String,
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
    val retencionTarifa: BigDecimal?,
    val esReferencia: Boolean,
    val createdAt: Instant
)
