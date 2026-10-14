package com.perseo.finance.expenses.dto

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

// ---- Categorías ----

data class ExpenseCategoryRequest(
    @field:NotBlank(message = "Escribe un nombre para la categoría.")
    @field:Size(max = 60, message = "El nombre no puede superar 60 caracteres.")
    val nombre: String
)

data class UpdateExpenseCategoryRequest(
    @field:NotBlank(message = "Escribe un nombre para la categoría.")
    @field:Size(max = 60, message = "El nombre no puede superar 60 caracteres.")
    val nombre: String,

    @field:NotNull(message = "Indica si la categoría está activa.")
    val activa: Boolean
)

data class ExpenseCategoryResponse(
    val id: UUID,
    val nombre: String,
    val activa: Boolean,
    val usos: Long
)

// ---- Gastos ----

data class ExpenseRequest(
    @field:NotNull(message = "La fecha es obligatoria.")
    val fecha: LocalDate,

    @field:NotNull(message = "El valor es obligatorio.")
    @field:Min(value = 1, message = "El valor debe ser mayor a cero.")
    @field:Max(value = 999999999999, message = "El valor es demasiado grande.")
    val valor: Long,

    @field:NotNull(message = "La categoría es obligatoria.")
    val categoriaId: UUID,

    @field:Size(max = 200, message = "La descripción no puede superar 200 caracteres.")
    val descripcion: String = ""
)

data class ExpenseResponse(
    val id: UUID,
    val fecha: LocalDate,
    val valor: Long,
    val categoriaId: UUID,
    val categoriaNombre: String,
    val descripcion: String,
    val createdAt: Instant
)

// ---- Vista del mes ----

data class MonthCategoryRow(
    val categoriaId: UUID,
    val nombre: String,
    val activa: Boolean,
    /** null = sin proyectado este mes. */
    val proyectado: Long?,
    val real: Long
)

data class ExpenseMonthResponse(
    val anio: Int,
    val mes: Int,
    val categorias: List<MonthCategoryRow>,
    val gastos: List<ExpenseResponse>,
    val totalProyectado: Long,
    val totalReal: Long,
    /** Mes "aaaa-m" del que se puede heredar presupuesto (el último anterior con datos), o null. */
    val herenciaDesde: String?,
    val ingresos: IncomesBlock,
    val facturas: BillsBlock,
    val ahorro: MonthSavingsBlock,
    val deudas: MonthDebtsBlock,
    /** Nombres usados en meses anteriores (para sugerir al diligenciar). */
    val sugerenciasIngresos: List<String>,
    val sugerenciasAhorro: List<String>,
    val sugerenciasDeudas: List<String>,
    /** K52+O52+S52+W52 del Excel: facturas pagadas + gasto real + ahorrado + deudas pagadas. */
    val totalGastado: Long,
    /** B9−B13 del Excel: recibido − gastado. */
    val saldoRestante: Long
)

/** Mapa categoriaId -> proyectado. Ausente o null = quitar el proyectado de ese mes. */
data class UpdateBudgetsRequest(
    val presupuestos: Map<UUID, Long?> = emptyMap()
)

// ---- Importación desde el Excel ----

data class ImportExpenseItem(
    @field:NotNull(message = "La fecha es obligatoria.")
    val fecha: LocalDate,

    @field:NotNull(message = "El valor es obligatorio.")
    @field:Min(value = 1, message = "El valor debe ser mayor a cero.")
    val valor: Long,

    @field:NotBlank(message = "La categoría es obligatoria.")
    @field:Size(max = 60, message = "La categoría no puede superar 60 caracteres.")
    val categoria: String,

    @field:Size(max = 200, message = "La descripción no puede superar 200 caracteres.")
    val descripcion: String = ""
)

data class ImportMonthRequest(
    val gastos: List<ImportExpenseItem> = emptyList(),
    val ingresos: List<ImportIncomeItem> = emptyList(),
    val facturas: List<ImportBillItem> = emptyList(),
    val ahorro: List<ImportMonthSavingItem> = emptyList(),
    val deudas: List<ImportMonthDebtItem> = emptyList()
)

data class ImportMonthResponse(
    val importados: Int,
    val total: Long,
    val ingresos: Int,
    val facturas: Int,
    val ahorros: Int,
    val deudas: Int
)
