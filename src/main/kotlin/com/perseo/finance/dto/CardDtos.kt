package com.perseo.finance.dto

import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotBlank
import java.math.BigDecimal
import java.util.UUID

data class CreateCardRequest(
    @field:NotBlank(message = "El banco es obligatorio")
    val banco: String,

    @field:NotBlank(message = "El alias es obligatorio")
    val alias: String,

    @field:DecimalMin(value = "0.0", message = "El cupo no puede ser negativo")
    val cupoTotal: BigDecimal,

    @field:DecimalMin(value = "0.0", message = "La tasa no puede ser negativa")
    val tasaEA: BigDecimal
)

typealias UpdateCardRequest = CreateCardRequest

data class CardResponse(
    val id: UUID,
    val banco: String,
    val alias: String,
    val cupoTotal: BigDecimal,
    val tasaEA: BigDecimal,
    val saldoTotalLineas: BigDecimal
)
