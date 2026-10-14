package com.perseo.finance.expenses.repository

import com.perseo.finance.expenses.entity.ExpenseBudgetEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface ExpenseBudgetRepository : JpaRepository<ExpenseBudgetEntity, UUID> {
    fun findByUserIdAndAnioAndMes(userId: UUID, anio: Int, mes: Int): List<ExpenseBudgetEntity>
    fun findByUserIdOrderByAnioDescMesDesc(userId: UUID): List<ExpenseBudgetEntity>
}
