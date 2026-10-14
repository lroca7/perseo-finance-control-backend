package com.perseo.finance.expenses.repository

import com.perseo.finance.expenses.entity.ExpenseMonthSavingEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface ExpenseMonthSavingRepository : JpaRepository<ExpenseMonthSavingEntity, UUID> {
    fun findByUserIdAndAnioAndMesOrderByNombreAsc(userId: UUID, anio: Int, mes: Int): List<ExpenseMonthSavingEntity>
    fun findByUserIdOrderByAnioDescMesDesc(userId: UUID): List<ExpenseMonthSavingEntity>
}
