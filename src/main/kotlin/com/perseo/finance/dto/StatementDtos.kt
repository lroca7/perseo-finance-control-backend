package com.perseo.finance.dto

import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotNull
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

data class CreateStatementRequest(
    @field:NotNull(message = "El inicio del período es obligatorio")
    val periodoInicio: LocalDate,

    @field:NotNull(message = "El fin del período es obligatorio")
    val periodoFin: LocalDate,

    @field:DecimalMin(value = "0.0", message = "El saldo total no puede ser negativo")
    val saldoTotal: BigDecimal,

    @field:DecimalMin(value = "0.0", message = "El pago mínimo no puede ser negativo")
    val pagoMinimo: BigDecimal,

    @field:NotNull(message = "La fecha límite de pago es obligatoria")
    val fechaLimitePago: LocalDate,

    @field:DecimalMin(value = "0.0", message = "La cuota de manejo no puede ser negativa")
    val cuotaManejo: BigDecimal = BigDecimal.ZERO,

    @field:DecimalMin(value = "0.0", message = "Los otros cargos no pueden ser negativos")
    val otrosCargos: BigDecimal = BigDecimal.ZERO
)

data class StatementResponse(
    val id: UUID,
    val cardId: UUID,
    val periodoInicio: LocalDate,
    val periodoFin: LocalDate,
    val saldoTotal: BigDecimal,
    val pagoMinimo: BigDecimal,
    val fechaLimitePago: LocalDate,
    val cuotaManejo: BigDecimal,
    val otrosCargos: BigDecimal
)
