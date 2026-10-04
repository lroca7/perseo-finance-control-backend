package com.perseo.finance.savings.dto

import com.perseo.finance.savings.domain.GoalStatus
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Future
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class CreateGoalRequest(
    @field:NotBlank(message = "Escribe un nombre para la meta.")
    @field:Size(max = 80, message = "El nombre no puede superar 80 caracteres.")
    val nombre: String,

    @field:Size(max = 30, message = "El ícono no puede superar 30 caracteres.")
    val icono: String = "piggy-bank",

    @field:DecimalMin(value = "0.01", message = "El monto objetivo debe ser mayor a cero.")
    val montoObjetivo: BigDecimal,

    @field:DecimalMin(value = "0", message = "El saldo inicial no puede ser negativo.")
    val saldoInicial: BigDecimal = BigDecimal.ZERO,

    @field:Future(message = "La fecha objetivo debe ser posterior a hoy.")
    val fechaObjetivo: LocalDate? = null
)

data class UpdateGoalRequest(
    @field:NotBlank(message = "Escribe un nombre para la meta.")
    @field:Size(max = 80, message = "El nombre no puede superar 80 caracteres.")
    val nombre: String,

    @field:Size(max = 30, message = "El ícono no puede superar 30 caracteres.")
    val icono: String = "piggy-bank",

    @field:DecimalMin(value = "0.01", message = "El monto objetivo debe ser mayor a cero.")
    val montoObjetivo: BigDecimal,

    @field:DecimalMin(value = "0", message = "El saldo inicial no puede ser negativo.")
    val saldoInicial: BigDecimal = BigDecimal.ZERO,

    @field:Future(message = "La fecha objetivo debe ser posterior a hoy.")
    val fechaObjetivo: LocalDate? = null,

    /** null = no cambia. Solo se aceptan ACTIVA y ARCHIVADA; ALCANZADA la asigna el sistema. */
    val estado: GoalStatus? = null
)

data class SavingsGoalResponse(
    val id: UUID,
    val nombre: String,
    val icono: String,
    val montoObjetivo: BigDecimal,
    val saldoInicial: BigDecimal,
    val fechaObjetivo: LocalDate?,
    val estado: GoalStatus,
    val planReferenciaId: UUID?,
    val cantidadPlanes: Int,
    val saldoRealEstimado: BigDecimal,
    /** saldoRealEstimado / monto objetivo como fracción (0.28 = 28 %). */
    val avanceMeta: BigDecimal,
    val estadoSeguimiento: GoalTrackingStatus,
    val mesesParaMeta: Int?,
    val aporteNecesario: BigDecimal?,
    val ritmoActual: BigDecimal?,
    val createdAt: Instant
)
