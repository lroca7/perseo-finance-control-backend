package com.perseo.finance.savings.repository

import com.perseo.finance.savings.entity.SavingsContributionEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface SavingsContributionRepository : JpaRepository<SavingsContributionEntity, UUID> {
    fun findByGoalIdOrderByFechaDescCreatedAtDesc(goalId: UUID): List<SavingsContributionEntity>
    fun findByGoalIdIn(goalIds: Collection<UUID>): List<SavingsContributionEntity>
}
