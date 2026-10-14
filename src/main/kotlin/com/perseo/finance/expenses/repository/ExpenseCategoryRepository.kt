package com.perseo.finance.expenses.repository

import com.perseo.finance.expenses.entity.ExpenseCategoryEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface ExpenseCategoryRepository : JpaRepository<ExpenseCategoryEntity, UUID> {
    fun findByUserIdOrderByNombreAsc(userId: UUID): List<ExpenseCategoryEntity>
}
