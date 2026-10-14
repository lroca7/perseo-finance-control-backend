package com.perseo.finance.expenses.repository

import com.perseo.finance.expenses.entity.ExpenseBillEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface ExpenseBillRepository : JpaRepository<ExpenseBillEntity, UUID> {
    fun findByUserIdAndAnioAndMes(userId: UUID, anio: Int, mes: Int): List<ExpenseBillEntity>
    fun countByCategoriaId(categoriaId: UUID): Long
}
