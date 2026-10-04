package com.perseo.finance.savings.repository

import com.perseo.finance.savings.domain.GoalStatus
import com.perseo.finance.savings.entity.SavingsGoalEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface SavingsGoalRepository : JpaRepository<SavingsGoalEntity, UUID> {
    fun findByUserIdOrderByCreatedAtDesc(userId: UUID): List<SavingsGoalEntity>
    fun findByUserIdAndEstadoNotOrderByCreatedAtDesc(userId: UUID, estado: GoalStatus): List<SavingsGoalEntity>
}
