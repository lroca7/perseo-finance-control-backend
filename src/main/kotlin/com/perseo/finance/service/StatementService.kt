package com.perseo.finance.service

import com.perseo.finance.dto.CreateStatementRequest
import com.perseo.finance.dto.StatementResponse
import com.perseo.finance.entity.StatementEntity
import com.perseo.finance.repository.StatementRepository
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class StatementService(
    private val statementRepository: StatementRepository,
    private val cardService: CardService
) {

    @Transactional(readOnly = true)
    fun listarPorTarjeta(cardId: UUID): List<StatementResponse> =
        statementRepository.findAllByCardIdOrderByPeriodoFinDesc(cardId).map { it.toResponse() }

    /**
     * HU-2.1: carga manual de un extracto. Un extracto por tarjeta y período
     * (mismo `periodoFin`) — si ya existe, se rechaza en vez de duplicar.
     */
    @Transactional
    fun crear(cardId: UUID, request: CreateStatementRequest): StatementResponse {
        val card = cardService.buscarEntidad(cardId)
        val statement = StatementEntity(
            card = card,
            periodoInicio = request.periodoInicio,
            periodoFin = request.periodoFin,
            saldoTotal = request.saldoTotal,
            pagoMinimo = request.pagoMinimo,
            fechaLimitePago = request.fechaLimitePago,
            cuotaManejo = request.cuotaManejo,
            otrosCargos = request.otrosCargos
        )
        return try {
            statementRepository.save(statement).toResponse()
        } catch (ex: DataIntegrityViolationException) {
            throw DuplicateResourceException(
                "Ya existe un extracto cargado para esta tarjeta con fecha fin ${request.periodoFin}"
            )
        }
    }

    @Transactional
    fun eliminar(statementId: UUID) {
        if (!statementRepository.existsById(statementId)) {
            throw ResourceNotFoundException("Extracto $statementId no encontrado")
        }
        statementRepository.deleteById(statementId)
    }

    private fun StatementEntity.toResponse() = StatementResponse(
        id = id!!,
        cardId = card?.id!!,
        periodoInicio = periodoInicio,
        periodoFin = periodoFin,
        saldoTotal = saldoTotal,
        pagoMinimo = pagoMinimo,
        fechaLimitePago = fechaLimitePago,
        cuotaManejo = cuotaManejo,
        otrosCargos = otrosCargos
    )
}
