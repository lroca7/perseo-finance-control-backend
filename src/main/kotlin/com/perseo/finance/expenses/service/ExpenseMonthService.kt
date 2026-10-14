package com.perseo.finance.expenses.service

import com.perseo.finance.common.badRequest
import com.perseo.finance.common.conflict
import com.perseo.finance.common.notFound
import com.perseo.finance.expenses.domain.ExpenseMonthSummary
import com.perseo.finance.expenses.dto.ExpenseMonthResponse
import com.perseo.finance.expenses.dto.ExpenseRequest
import com.perseo.finance.expenses.dto.ExpenseResponse
import com.perseo.finance.expenses.dto.ImportMonthRequest
import com.perseo.finance.expenses.dto.ImportMonthResponse
import com.perseo.finance.expenses.dto.MonthCategoryRow
import com.perseo.finance.expenses.dto.UpdateBudgetsRequest
import com.perseo.finance.expenses.entity.ExpenseBudgetEntity
import com.perseo.finance.expenses.entity.ExpenseCategoryEntity
import com.perseo.finance.expenses.entity.ExpenseEntity
import com.perseo.finance.expenses.repository.ExpenseBudgetRepository
import com.perseo.finance.expenses.repository.ExpenseCategoryRepository
import com.perseo.finance.expenses.repository.ExpenseRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

/**
 * Vista del mes, presupuestos, registro de gastos e importación desde el Excel (HU-G.1).
 * Cada mes es independiente; el frontend hereda el proyectado del mes anterior si lo desea.
 */
@Service
class ExpenseMonthService(
    private val categoryRepository: ExpenseCategoryRepository,
    private val budgetRepository: ExpenseBudgetRepository,
    private val expenseRepository: ExpenseRepository,
    private val blocksService: ExpenseBlocksService
) {

    // ---------------- Vista del mes ----------------

    @Transactional(readOnly = true)
    fun obtenerMes(userId: UUID, anio: Int, mes: Int): ExpenseMonthResponse {
        validarMes(anio, mes)
        val rango = YearMonth.of(anio, mes)
        val categorias = categoryRepository.findByUserIdOrderByNombreAsc(userId)
        val presupuestos = budgetRepository.findByUserIdAndAnioAndMes(userId, anio, mes)
            .associate { it.categoriaId to it.proyectado }
        val gastos = expenseRepository.findByUserIdAndFechaBetweenOrderByFechaDescCreatedAtDesc(
            userId, rango.atDay(1), rango.atEndOfMonth()
        )
        val nombres = categorias.associate { it.id!! to it.nombre }

        val resumen = ExpenseMonthSummary.resumir(
            categorias.map {
                ExpenseMonthSummary.CategoriaInsumo(it.id!!, it.nombre, it.activa, presupuestos[it.id])
            },
            gastos.map { ExpenseMonthSummary.GastoInsumo(it.categoriaId, it.valor) }
        )
        val bloques = blocksService.leer(userId, anio, mes)

        // B13 del Excel: facturas pagadas + gasto real + ahorrado + deudas pagadas.
        val totalGastado = bloques.facturas.totalPagado + resumen.totalReal +
            bloques.ahorro.totalAhorrado + bloques.deudas.totalPagado

        return ExpenseMonthResponse(
            anio = anio,
            mes = mes,
            categorias = resumen.filas.map {
                MonthCategoryRow(it.categoriaId, it.nombre, it.activa, it.proyectado, it.real)
            },
            gastos = gastos.map { it.toResponse(nombres[it.categoriaId] ?: "(eliminada)") },
            totalProyectado = resumen.totalProyectado,
            totalReal = resumen.totalReal,
            herenciaDesde = ultimoMesConPresupuesto(userId, anio, mes),
            ingresos = bloques.ingresos,
            facturas = bloques.facturas,
            ahorro = bloques.ahorro,
            deudas = bloques.deudas,
            sugerenciasIngresos = bloques.sugerenciasIngresos,
            sugerenciasAhorro = bloques.sugerenciasAhorro,
            sugerenciasDeudas = bloques.sugerenciasDeudas,
            totalGastado = totalGastado,
            // B17 del Excel: recibido − gastado.
            saldoRestante = bloques.ingresos.totalRecibido - totalGastado
        )
    }

    // ---------------- Presupuestos ----------------

    @Transactional
    fun guardarPresupuestos(userId: UUID, anio: Int, mes: Int, request: UpdateBudgetsRequest): ExpenseMonthResponse {
        validarMes(anio, mes)
        val existentes = budgetRepository.findByUserIdAndAnioAndMes(userId, anio, mes)
            .associateBy { it.categoriaId }
        for ((categoriaId, proyectado) in request.presupuestos) {
            // Solo categorías propias; el valor null o negativo quita el proyectado.
            val categoria = categoryRepository.findById(categoriaId).orElse(null)
            if (categoria == null || categoria.userId != userId) notFound("Categoría no encontrada.")
            val actual = existentes[categoriaId]
            if (proyectado == null || proyectado < 0) {
                if (actual != null) budgetRepository.delete(actual)
            } else {
                if (actual == null) {
                    budgetRepository.save(
                        ExpenseBudgetEntity(
                            userId = userId, categoriaId = categoriaId,
                            anio = anio, mes = mes, proyectado = proyectado
                        )
                    )
                } else {
                    actual.proyectado = proyectado
                    actual.updatedAt = Instant.now()
                    budgetRepository.save(actual)
                }
            }
        }
        budgetRepository.flush()
        return obtenerMes(userId, anio, mes)
    }

    // ---------------- Gastos ----------------

    @Transactional
    fun crearGasto(userId: UUID, request: ExpenseRequest): ExpenseResponse {
        val categoria = categoriaActiva(userId, request.categoriaId)
        validarFecha(request.fecha)
        val guardado = expenseRepository.save(
            ExpenseEntity(
                userId = userId,
                categoriaId = categoria.id!!,
                fecha = request.fecha,
                valor = request.valor,
                descripcion = request.descripcion.trim()
            )
        )
        return guardado.toResponse(categoria.nombre)
    }

    @Transactional
    fun actualizarGasto(id: UUID, userId: UUID, request: ExpenseRequest): ExpenseResponse {
        val gasto = obtenerGasto(id, userId)
        val categoria = categoriaActiva(userId, request.categoriaId)
        validarFecha(request.fecha)
        gasto.fecha = request.fecha
        gasto.valor = request.valor
        gasto.categoriaId = categoria.id!!
        gasto.descripcion = request.descripcion.trim()
        gasto.updatedAt = Instant.now()
        val guardado = expenseRepository.save(gasto)
        return guardado.toResponse(categoria.nombre)
    }

    @Transactional
    fun eliminarGasto(id: UUID, userId: UUID) {
        val gasto = obtenerGasto(id, userId)
        expenseRepository.delete(gasto)
        expenseRepository.flush()
    }

    // ---------------- Importación desde el Excel ----------------

    @Transactional
    fun importarMes(userId: UUID, anio: Int, mes: Int, request: ImportMonthRequest, forzar: Boolean): ImportMonthResponse {
        validarMes(anio, mes)
        val rango = YearMonth.of(anio, mes)
        if (!forzar && expenseRepository.existsByUserIdAndFechaBetween(userId, rango.atDay(1), rango.atEndOfMonth())) {
            conflict("El mes ya tiene gastos registrados. Reintenta con forzar=true para reemplazarlos.")
        }
        blocksService.verificarImportacion(userId, anio, mes, forzar)
        // Reemplazo idempotente: borra lo que haya en el rango antes de importar.
        val previos = expenseRepository.findByUserIdAndFechaBetweenOrderByFechaDescCreatedAtDesc(
            userId, rango.atDay(1), rango.atEndOfMonth()
        )
        expenseRepository.deleteAll(previos)

        val porNombre = categoryRepository.findByUserIdOrderByNombreAsc(userId)
            .associateBy { ExpenseMonthSummary.normalizarNombre(it.nombre) }.toMutableMap()
        var total = 0L
        var n = 0
        for (item in request.gastos) {
            validarFecha(item.fecha)
            if (item.fecha.year != anio || item.fecha.monthValue != mes) {
                badRequest("El gasto ${item.fecha} no pertenece al mes $anio-$mes.")
            }
            val clave = ExpenseMonthSummary.normalizarNombre(item.categoria)
            val categoria = porNombre.getOrPut(clave) {
                categoryRepository.save(ExpenseCategoryEntity(userId = userId, nombre = item.categoria.trim()))
            }
            if (!categoria.activa) badRequest("La categoría '${categoria.nombre}' está inactiva.")
            expenseRepository.save(
                ExpenseEntity(
                    userId = userId,
                    categoriaId = categoria.id!!,
                    fecha = item.fecha,
                    valor = item.valor,
                    descripcion = item.descripcion.trim()
                )
            )
            total += item.valor
            n++
        }
        val conteo = blocksService.importarBloques(
            userId, anio, mes, request.ingresos, request.facturas, request.ahorro, request.deudas
        )
        return ImportMonthResponse(
            importados = n, total = total,
            ingresos = conteo["ingresos"] ?: 0, facturas = conteo["facturas"] ?: 0,
            ahorros = conteo["ahorros"] ?: 0, deudas = conteo["deudas"] ?: 0
        )
    }

    // ---------------- Validaciones ----------------

    private fun validarMes(anio: Int, mes: Int) {
        if (anio !in 2000..2100 || mes !in 1..12) badRequest("Mes inválido: $anio-$mes.")
    }

    private fun validarFecha(fecha: LocalDate) {
        if (fecha.isAfter(LocalDate.now())) badRequest("La fecha no puede ser futura.")
    }

    private fun obtenerGasto(id: UUID, userId: UUID): ExpenseEntity {
        val gasto = expenseRepository.findById(id).orElse(null) ?: notFound("Gasto no encontrado.")
        if (gasto.userId != userId) notFound("Gasto no encontrado.")
        return gasto
    }

    private fun categoriaActiva(userId: UUID, categoriaId: UUID): ExpenseCategoryEntity {
        val categoria = categoryRepository.findById(categoriaId).orElse(null)
            ?: notFound("Categoría no encontrada.")
        if (categoria.userId != userId) notFound("Categoría no encontrada.")
        if (!categoria.activa) badRequest("La categoría '${categoria.nombre}' está inactiva.")
        return categoria
    }

    /** Último mes anterior con presupuesto, en formato "anio-mes" (para heredar en el frontend). */
    private fun ultimoMesConPresupuesto(userId: UUID, anio: Int, mes: Int): String? {
        val clave = anio * 12 + mes
        return budgetRepository.findByUserIdOrderByAnioDescMesDesc(userId)
            .firstOrNull { it.anio * 12 + it.mes < clave }
            ?.let { "${it.anio}-${it.mes}" }
    }

    private fun ExpenseEntity.toResponse(nombre: String) = ExpenseResponse(
        id = id!!, fecha = fecha, valor = valor, categoriaId = categoriaId,
        categoriaNombre = nombre, descripcion = descripcion, createdAt = createdAt
    )
}
