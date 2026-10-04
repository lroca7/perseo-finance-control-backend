package com.perseo.finance.cards.repository

import com.perseo.finance.cards.entity.CreditLineEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface CreditLineRepository : JpaRepository<CreditLineEntity, UUID> {
    fun findAllByCardId(cardId: UUID): List<CreditLineEntity>
}
