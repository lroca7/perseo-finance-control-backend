package com.perseo.finance.expenses.repository

import com.perseo.finance.expenses.entity.ExpenseIncomeEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface ExpenseIncomeRepository : JpaRepository<ExpenseIncomeEntity, UUID> {
    fun findByUserIdAndAnioAndMesOrderByCategoriaAsc(userId: UUID, anio: Int, mes: Int): List<ExpenseIncomeEntity>
    fun findByUserIdOrderByAnioDescMesDesc(userId: UUID): List<ExpenseIncomeEntity>
}
