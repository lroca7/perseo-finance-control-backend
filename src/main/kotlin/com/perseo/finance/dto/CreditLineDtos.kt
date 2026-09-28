package com.perseo.finance.dto

import com.perseo.finance.domain.AmortizationType
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import java.math.BigDecimal
import java.util.UUID

data class CreateCreditLineRequest(
    @field:NotBlank(message = "El nombre es obligatorio")
    val nombre: String,

    @field:DecimalMin(value = "0.0", message = "El saldo no puede ser negativo")
    val saldoPendiente: BigDecimal,

    @field:Min(value = 1, message = "Las cuotas restantes deben ser al menos 1")
    val cuotasRestantes: Int,

    /** Si es null, se usa la tasa de la tarjeta. */
    val tasaEA: BigDecimal? = null,

    val tipoAmortizacion: AmortizationType = AmortizationType.CAPITAL_FIJO,

    val ignorada: Boolean = false
)

typealias UpdateCreditLineRequest = CreateCreditLineRequest

data class CreditLineResponse(
    val id: UUID,
    val cardId: UUID,
    val nombre: String,
    val saldoPendiente: BigDecimal,
    val cuotasRestantes: Int,
    val tasaEA: BigDecimal,
    val tipoAmortizacion: AmortizationType,
    val ignorada: Boolean,
    /** Capital fijo mensual calculado (solo informativo si tipo = CAPITAL_FIJO). */
    val capitalFijoMensual: BigDecimal
)
