package com.perseo.finance.repository

import com.perseo.finance.entity.CardEntity
import com.perseo.finance.entity.CreditLineEntity
import com.perseo.finance.entity.ExtraPaymentEntity
import com.perseo.finance.entity.StatementEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface CardRepository : JpaRepository<CardEntity, UUID> {
    fun findAllByUserId(userId: UUID): List<CardEntity>
}

interface CreditLineRepository : JpaRepository<CreditLineEntity, UUID> {
    fun findAllByCardId(cardId: UUID): List<CreditLineEntity>
}

interface ExtraPaymentRepository : JpaRepository<ExtraPaymentEntity, UUID> {
    fun findAllByCreditLineId(creditLineId: UUID): List<ExtraPaymentEntity>
    fun deleteByIdAndCreditLineId(id: UUID, creditLineId: UUID)
}

interface StatementRepository : JpaRepository<StatementEntity, UUID> {
    fun findAllByCardIdOrderByPeriodoFinDesc(cardId: UUID): List<StatementEntity>
}
