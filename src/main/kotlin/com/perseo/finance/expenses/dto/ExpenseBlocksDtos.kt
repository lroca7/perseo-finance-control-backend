package com.perseo.finance.expenses.dto

import jakarta.validation.Valid
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.LocalDate
import java.util.UUID

// ---- Ingresos del mes (bloque B-E) ----

data class IncomeItemRequest(
    @field:NotBlank(message = "La categoría del ingreso es obligatoria.")
    @field:Size(max = 60, message = "La categoría no puede superar 60 caracteres.")
    val categoria: String,

    /** Debe estar dentro del mes que se guarda. */
    val fecha: LocalDate? = null,

    @field:Min(value = 0, message = "El proyectado no puede ser negativo.")
    val proyectado: Long? = null,

    @field:Min(value = 0, message = "Lo recibido no puede ser negativo.")
    val recibido: Long? = null
)

data class UpdateIncomesRequest(
    @field:Valid
    val ingresos: List<IncomeItemRequest> = emptyList()
)

data class IncomeRow(
    val id: UUID,
    val categoria: String,
    val fecha: LocalDate?,
    val proyectado: Long?,
    val recibido: Long?
)

// ---- Facturas del mes (bloque G-K) ----

data class BillItemRequest(
    @field:NotNull(message = "La categoría de la factura es obligatoria.")
    val categoriaId: UUID,

    /** Debe estar dentro del mes que se guarda. */
    val fecha: LocalDate? = null,

    @field:Min(value = 0, message = "El valor no puede ser negativo.")
    val valor: Long? = null,

    val pagado: Boolean = false,

    @field:Min(value = 0, message = "El valor pagado no puede ser negativo.")
    val valorPagado: Long? = null
)

data class UpdateBillsRequest(
    @field:Valid
    val facturas: List<BillItemRequest> = emptyList()
)

data class BillRow(
    val id: UUID,
    val categoriaId: UUID,
    val categoriaNombre: String,
    val categoriaActiva: Boolean,
    val fecha: LocalDate?,
    val valor: Long?,
    val pagado: Boolean,
    val valorPagado: Long?
)

// ---- Ahorro del mes (bloque Q-S) ----

data class MonthSavingItemRequest(
    @field:NotBlank(message = "El nombre del ahorro es obligatorio.")
    @field:Size(max = 60, message = "El nombre no puede superar 60 caracteres.")
    val nombre: String,

    @field:Min(value = 0, message = "La meta no puede ser negativa.")
    val meta: Long? = null,

    @field:Min(value = 0, message = "Lo ahorrado no puede ser negativo.")
    val ahorrado: Long? = null
)

data class UpdateMonthSavingsRequest(
    @field:Valid
    val ahorros: List<MonthSavingItemRequest> = emptyList()
)

data class MonthSavingRow(
    val id: UUID,
    val nombre: String,
    val meta: Long?,
    val ahorrado: Long?
)

// ---- Deudas del mes (bloque U-W) ----

data class MonthDebtItemRequest(
    @field:NotBlank(message = "El nombre de la deuda es obligatorio.")
    @field:Size(max = 60, message = "El nombre no puede superar 60 caracteres.")
    val nombre: String,

    @field:Min(value = 0, message = "El pendiente no puede ser negativo.")
    val pendiente: Long? = null,

    @field:Min(value = 0, message = "Lo pagado no puede ser negativo.")
    val pagado: Long? = null
)

data class UpdateMonthDebtsRequest(
    @field:Valid
    val deudas: List<MonthDebtItemRequest> = emptyList()
)

data class MonthDebtRow(
    val id: UUID,
    val nombre: String,
    val pendiente: Long?,
    val pagado: Long?
)

// ---- Bloques agregados en la vista del mes ----

data class IncomesBlock(
    val filas: List<IncomeRow>,
    val totalProyectado: Long,
    val totalRecibido: Long
)

data class BillsBlock(
    val filas: List<BillRow>,
    val totalValor: Long,
    val totalPagado: Long
)

data class MonthSavingsBlock(
    val filas: List<MonthSavingRow>,
    val totalMeta: Long,
    val totalAhorrado: Long
)

data class MonthDebtsBlock(
    val filas: List<MonthDebtRow>,
    val totalPendiente: Long,
    val totalPagado: Long
)

// ---- Importación: se extiende con los 4 bloques ----

data class ImportIncomeItem(
    @field:NotBlank(message = "La categoría del ingreso es obligatoria.")
    val categoria: String,
    val fecha: LocalDate? = null,
    val proyectado: Long? = null,
    val recibido: Long? = null
)

data class ImportBillItem(
    @field:NotBlank(message = "La categoría de la factura es obligatoria.")
    val categoria: String,
    val fecha: LocalDate? = null,
    val valor: Long? = null,
    val pagado: Boolean = false,
    val valorPagado: Long? = null
)

data class ImportMonthSavingItem(
    @field:NotBlank(message = "El nombre del ahorro es obligatorio.")
    val nombre: String,
    val meta: Long? = null,
    val ahorrado: Long? = null
)

data class ImportMonthDebtItem(
    @field:NotBlank(message = "El nombre de la deuda es obligatorio.")
    val nombre: String,
    val pendiente: Long? = null,
    val pagado: Long? = null
)
