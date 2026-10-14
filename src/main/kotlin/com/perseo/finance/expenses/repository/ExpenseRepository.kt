package com.perseo.finance.expenses.repository

import com.perseo.finance.expenses.entity.ExpenseEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.time.LocalDate
import java.util.UUID

interface ExpenseRepository : JpaRepository<ExpenseEntity, UUID> {
    fun findByUserIdAndFechaBetweenOrderByFechaDescCreatedAtDesc(
        userId: UUID,
        desde: LocalDate,
        hasta: LocalDate
    ): List<ExpenseEntity>

    fun countByCategoriaId(categoriaId: UUID): Long
    fun existsByUserIdAndFechaBetween(userId: UUID, desde: LocalDate, hasta: LocalDate): Boolean

    @Query("select e.categoriaId as categoriaId, count(e) as total from ExpenseEntity e where e.userId = :userId group by e.categoriaId")
    fun contarPorCategoria(userId: UUID): List<ConteoPorCategoria>

    interface ConteoPorCategoria {
        val categoriaId: UUID
        val total: Long
    }
}
