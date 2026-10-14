package com.perseo.finance.expenses.repository

import com.perseo.finance.expenses.entity.ExpenseBillCategoryEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface ExpenseBillCategoryRepository : JpaRepository<ExpenseBillCategoryEntity, UUID> {
    fun findByUserIdOrderByNombreAsc(userId: UUID): List<ExpenseBillCategoryEntity>
}
