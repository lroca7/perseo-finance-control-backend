package com.perseo.finance.service

import com.perseo.finance.dto.CreateCreditLineRequest
import com.perseo.finance.dto.CreditLineResponse
import com.perseo.finance.dto.UpdateCreditLineRequest
import com.perseo.finance.entity.CreditLineEntity
import com.perseo.finance.repository.CreditLineRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.util.UUID

@Service
class CreditLineService(
    private val creditLineRepository: CreditLineRepository,
    private val cardService: CardService
) {

    @Transactional(readOnly = true)
    fun listarPorTarjeta(cardId: UUID): List<CreditLineResponse> =
        creditLineRepository.findAllByCardId(cardId).map { it.toResponse() }

    @Transactional(readOnly = true)
    fun obtener(lineId: UUID): CreditLineResponse =
        buscarEntidad(lineId).toResponse()

    @Transactional
    fun crear(cardId: UUID, request: CreateCreditLineRequest): CreditLineResponse {
        val card = cardService.buscarEntidad(cardId)
        val line = CreditLineEntity(
            card = card,
            nombre = request.nombre,
            saldoPendiente = request.saldoPendiente,
            cuotasRestantes = request.cuotasRestantes,
            tasaEA = request.tasaEA,
            tipoAmortizacion = request.tipoAmortizacion,
            ignorada = request.ignorada
        )
        return creditLineRepository.save(line).toResponse()
    }

    @Transactional
    fun actualizar(lineId: UUID, request: UpdateCreditLineRequest): CreditLineResponse {
        val line = buscarEntidad(lineId)
        line.nombre = request.nombre
        line.saldoPendiente = request.saldoPendiente
        line.cuotasRestantes = request.cuotasRestantes
        line.tasaEA = request.tasaEA
        line.tipoAmortizacion = request.tipoAmortizacion
        line.ignorada = request.ignorada
        line.updatedAt = Instant.now()
        return creditLineRepository.save(line).toResponse()
    }

    @Transactional
    fun eliminar(lineId: UUID) {
        if (!creditLineRepository.existsById(lineId)) {
            throw ResourceNotFoundException("Línea de crédito $lineId no encontrada")
        }
        creditLineRepository.deleteById(lineId)
    }

    fun buscarEntidad(lineId: UUID): CreditLineEntity =
        creditLineRepository.findById(lineId)
            .orElseThrow { ResourceNotFoundException("Línea de crédito $lineId no encontrada") }

    /** Tasa efectiva a usar: la propia de la línea, o si no tiene, la de su tarjeta. */
    fun tasaEfectiva(line: CreditLineEntity): BigDecimal =
        line.tasaEA ?: line.card?.tasaEA ?: BigDecimal.ZERO

    private fun CreditLineEntity.toResponse(): CreditLineResponse {
        val tasa = tasaEfectiva(this)
        val capitalFijo = if (cuotasRestantes > 0) {
            saldoPendiente.divide(BigDecimal(cuotasRestantes), 2, RoundingMode.HALF_UP)
        } else {
            BigDecimal.ZERO
        }
        return CreditLineResponse(
            id = id!!,
            cardId = card?.id!!,
            nombre = nombre,
            saldoPendiente = saldoPendiente,
            cuotasRestantes = cuotasRestantes,
            tasaEA = tasa,
            tipoAmortizacion = tipoAmortizacion,
            ignorada = ignorada,
            capitalFijoMensual = capitalFijo
        )
    }
}
