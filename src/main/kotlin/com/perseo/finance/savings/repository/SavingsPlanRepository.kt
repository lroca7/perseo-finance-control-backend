package com.perseo.finance.savings.repository

import com.perseo.finance.savings.entity.SavingsPlanEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.UUID

interface SavingsPlanRepository : JpaRepository<SavingsPlanEntity, UUID> {
    fun findByGoalIdOrderByCreatedAtAsc(goalId: UUID): List<SavingsPlanEntity>
    fun findByGoalIdIn(goalIds: Collection<UUID>): List<SavingsPlanEntity>
    fun countByGoalId(goalId: UUID): Long

    /** Quita la marca de referencia a todos los planes de la meta (índice único parcial, RN-A.13). */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update SavingsPlanEntity p set p.esReferencia = false where p.goalId = :goalId")
    fun clearReferencia(@Param("goalId") goalId: UUID): Int
}
