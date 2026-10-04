package com.perseo.finance.savings.repository

import com.perseo.finance.savings.entity.SavingsBalanceCheckEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface SavingsBalanceCheckRepository : JpaRepository<SavingsBalanceCheckEntity, UUID> {
    fun findByGoalIdOrderByFechaDescCreatedAtDesc(goalId: UUID): List<SavingsBalanceCheckEntity>
    fun findByGoalIdIn(goalIds: Collection<UUID>): List<SavingsBalanceCheckEntity>
}
