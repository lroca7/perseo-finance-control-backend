package com.perseo.finance.cards.repository

import com.perseo.finance.cards.entity.StatementEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface StatementRepository : JpaRepository<StatementEntity, UUID> {
    fun findAllByCardIdOrderByPeriodoFinDesc(cardId: UUID): List<StatementEntity>
}
