package com.perseo.finance.dto

import java.math.BigDecimal
import java.util.UUID

data class EstimatedLinePayment(
    val creditLineId: UUID,
    val nombre: String,
    val cuotaMes: BigDecimal
)

/**
 * "Pago aproximado del mes en curso" (a diferencia del cronograma, esto
 * es un cálculo bajo demanda, nunca se guarda): suma de la cuota #1 de
 * cada línea activa (respetando abonos ya cargados en esa cuota) más los
 * cargos fijos mensuales de la tarjeta (manejo, seguro, etc.).
 *
 * Es un ESTIMADO del usuario, no el pago mínimo oficial que exige el
 * banco — el frontend debe dejar esto claro.
 */
data class EstimatedMonthlyPaymentResponse(
    val porLinea: List<EstimatedLinePayment>,
    val cargosFijos: BigDecimal,
    val total: BigDecimal
)
