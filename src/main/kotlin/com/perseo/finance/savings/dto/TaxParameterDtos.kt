package com.perseo.finance.savings.dto

import com.perseo.finance.savings.domain.ProductType
import com.perseo.finance.savings.domain.TaxConcept
import com.perseo.finance.savings.domain.WithholdingMode
import java.math.BigDecimal
import java.time.LocalDate

data class TaxParameterResponse(
    val tipoProducto: ProductType,
    val concepto: TaxConcept,
    val tarifa: BigDecimal,
    val vigenteDesde: LocalDate,
    val vigenteHasta: LocalDate?,
    val notas: String?
)

/** Productos disponibles con sus reglas precargadas (HU-A.5). */
data class SavingsProductResponse(
    val tipo: ProductType,
    val nombre: String,
    val tratamientoRetencion: WithholdingMode,
    val permiteComision: Boolean,
    /** Tarifa de retención vigente hoy como fracción; null si no hay parámetros. */
    val retencionVigente: BigDecimal?
)
