package com.perseo.finance.cards.repository

import com.perseo.finance.cards.entity.ExtraPaymentEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface ExtraPaymentRepository : JpaRepository<ExtraPaymentEntity, UUID> {
    fun findAllByCreditLineId(creditLineId: UUID): List<ExtraPaymentEntity>
    fun deleteByIdAndCreditLineId(id: UUID, creditLineId: UUID)
}
