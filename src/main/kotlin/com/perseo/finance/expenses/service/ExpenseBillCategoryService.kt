package com.perseo.finance.expenses.service

import com.perseo.finance.common.badRequest
import com.perseo.finance.common.conflict
import com.perseo.finance.common.notFound
import com.perseo.finance.expenses.domain.ExpenseMonthSummary
import com.perseo.finance.expenses.dto.ExpenseCategoryRequest
import com.perseo.finance.expenses.dto.ExpenseCategoryResponse
import com.perseo.finance.expenses.dto.UpdateExpenseCategoryRequest
import com.perseo.finance.expenses.entity.ExpenseBillCategoryEntity
import com.perseo.finance.expenses.repository.ExpenseBillCategoryRepository
import com.perseo.finance.expenses.repository.ExpenseBillRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

/** Catálogo editable de facturas (HU-G.3). Mismas reglas que las categorías de gasto. */
@Service
class ExpenseBillCategoryService(
    private val categoryRepository: ExpenseBillCategoryRepository,
    private val billRepository: ExpenseBillRepository
) {

    @Transactional(readOnly = true)
    fun listar(userId: UUID): List<ExpenseCategoryResponse> {
        return categoryRepository.findByUserIdOrderByNombreAsc(userId).map {
            it.toResponse(billRepository.countByCategoriaId(it.id!!))
        }
    }

    @Transactional
    fun crear(userId: UUID, request: ExpenseCategoryRequest): ExpenseCategoryResponse {
        val nombre = request.nombre.trim()
        if (nombre.isEmpty()) badRequest("Escribe un nombre para la categoría.")
        verificarUnico(userId, null, nombre)
        val guardada = categoryRepository.save(
            ExpenseBillCategoryEntity(userId = userId, nombre = nombre)
        )
        return guardada.toResponse(0L)
    }

    @Transactional
    fun actualizar(id: UUID, userId: UUID, request: UpdateExpenseCategoryRequest): ExpenseCategoryResponse {
        val categoria = obtener(id, userId)
        val nombre = request.nombre.trim()
        if (nombre.isEmpty()) badRequest("Escribe un nombre para la categoría.")
        verificarUnico(userId, id, nombre)
        categoria.nombre = nombre
        categoria.activa = request.activa
        categoria.updatedAt = Instant.now()
        val guardada = categoryRepository.save(categoria)
        return guardada.toResponse(billRepository.countByCategoriaId(id))
    }

    @Transactional
    fun eliminar(id: UUID, userId: UUID) {
        val categoria = obtener(id, userId)
        if (billRepository.countByCategoriaId(id) > 0) {
            conflict("La categoría tiene facturas asociadas: desactívala en vez de eliminarla.")
        }
        categoryRepository.delete(categoria)
        categoryRepository.flush()
    }

    private fun obtener(id: UUID, userId: UUID): ExpenseBillCategoryEntity {
        val categoria = categoryRepository.findById(id).orElse(null) ?: notFound("Categoría no encontrada.")
        if (categoria.userId != userId) notFound("Categoría no encontrada.")
        return categoria
    }

    private fun verificarUnico(userId: UUID, exceptoId: UUID?, nombre: String) {
        val normalizado = ExpenseMonthSummary.normalizarNombre(nombre)
        val duplicada = categoryRepository.findByUserIdOrderByNombreAsc(userId).any {
            (exceptoId == null || it.id != exceptoId) &&
                ExpenseMonthSummary.normalizarNombre(it.nombre) == normalizado
        }
        if (duplicada) conflict("Ya existe una categoría con ese nombre.")
    }

    private fun ExpenseBillCategoryEntity.toResponse(usos: Long) = ExpenseCategoryResponse(
        id = id!!, nombre = nombre, activa = activa, usos = usos
    )
}
