package com.perseo.finance.service

import com.perseo.finance.dto.CardResponse
import com.perseo.finance.dto.CreateCardRequest
import com.perseo.finance.dto.UpdateCardRequest
import com.perseo.finance.entity.CardEntity
import com.perseo.finance.repository.CardRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@Service
class CardService(
    private val cardRepository: CardRepository
) {

    @Transactional(readOnly = true)
    fun listarPorUsuario(userId: UUID): List<CardResponse> =
        cardRepository.findAllByUserId(userId).map { it.toResponse() }

    @Transactional(readOnly = true)
    fun obtener(cardId: UUID): CardResponse =
        buscarEntidad(cardId).toResponse()

    @Transactional
    fun crear(userId: UUID, request: CreateCardRequest): CardResponse {
        val card = CardEntity(
            userId = userId,
            banco = request.banco,
            alias = request.alias,
            cupoTotal = request.cupoTotal,
            tasaEA = request.tasaEA
        )
        return cardRepository.save(card).toResponse()
    }

    @Transactional
    fun actualizar(cardId: UUID, request: UpdateCardRequest): CardResponse {
        val card = buscarEntidad(cardId)
        card.banco = request.banco
        card.alias = request.alias
        card.cupoTotal = request.cupoTotal
        card.tasaEA = request.tasaEA
        card.updatedAt = Instant.now()
        return cardRepository.save(card).toResponse()
    }

    @Transactional
    fun eliminar(cardId: UUID) {
        if (!cardRepository.existsById(cardId)) {
            throw ResourceNotFoundException("Tarjeta $cardId no encontrada")
        }
        cardRepository.deleteById(cardId)
    }

    fun buscarEntidad(cardId: UUID): CardEntity =
        cardRepository.findById(cardId)
            .orElseThrow { ResourceNotFoundException("Tarjeta $cardId no encontrada") }

    private fun CardEntity.toResponse() = CardResponse(
        id = id!!,
        banco = banco,
        alias = alias,
        cupoTotal = cupoTotal,
        tasaEA = tasaEA,
        saldoTotalLineas = creditLines
            .filterNot { it.ignorada }
            .fold(BigDecimal.ZERO) { acc, line -> acc + line.saldoPendiente }
    )
}
