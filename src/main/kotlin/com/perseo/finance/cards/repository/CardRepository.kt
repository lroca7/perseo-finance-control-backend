package com.perseo.finance.cards.repository

import com.perseo.finance.cards.entity.CardEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface CardRepository : JpaRepository<CardEntity, UUID> {
    fun findAllByUserId(userId: UUID): List<CardEntity>
}
