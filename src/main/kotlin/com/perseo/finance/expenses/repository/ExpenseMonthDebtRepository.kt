package com.perseo.finance.expenses.repository

import com.perseo.finance.expenses.entity.ExpenseMonthDebtEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface ExpenseMonthDebtRepository : JpaRepository<ExpenseMonthDebtEntity, UUID> {
    fun findByUserIdAndAnioAndMesOrderByNombreAsc(userId: UUID, anio: Int, mes: Int): List<ExpenseMonthDebtEntity>
    fun findByUserIdOrderByAnioDescMesDesc(userId: UUID): List<ExpenseMonthDebtEntity>
}
