package com.perseo.finance.service

import com.perseo.finance.domain.AmortizationEngine
import com.perseo.finance.dto.EstimatedLinePayment
import com.perseo.finance.dto.EstimatedMonthlyPaymentResponse
import com.perseo.finance.entity.CreditLineEntity
import com.perseo.finance.entity.ExtraPaymentEntity
import com.perseo.finance.repository.CreditLineRepository
import com.perseo.finance.repository.ExtraPaymentRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.UUID

@Service
class EstimatedPaymentService(
    private val cardService: CardService,
    private val creditLineRepository: CreditLineRepository,
    private val creditLineService: CreditLineService,
    private val extraPaymentRepository: ExtraPaymentRepository
) {

    /**
     * "Pago aproximado del mes en curso" — se recalcula cada vez que se pide,
     * nunca se guarda. Es un estimado del usuario, no el pago mínimo oficial
     * del banco (ver nota en EstimatedMonthlyPaymentResponse).
     */
    @Transactional(readOnly = true)
    fun estimarMesActual(cardId: UUID): EstimatedMonthlyPaymentResponse {
        val card = cardService.buscarEntidad(cardId)
        val lineasActivas = creditLineRepository.findAllByCardId(cardId).filterNot { it.ignorada }

        val porLinea = lineasActivas.map { line ->
            EstimatedLinePayment(
                creditLineId = line.id!!,
                nombre = line.nombre,
                cuotaMes = cuotaDelMesActual(line)
            )
        }

        val cargosFijos = card.cargosFijosMensuales
        val total = porLinea.fold(cargosFijos) { acc, l -> acc + l.cuotaMes }

        return EstimatedMonthlyPaymentResponse(
            porLinea = porLinea,
            cargosFijos = cargosFijos,
            total = total
        )
    }

    /** Cuota total de la primera cuota simulada, respetando abonos ya cargados en esa línea. */
    private fun cuotaDelMesActual(line: CreditLineEntity): BigDecimal {
        if (line.saldoPendiente <= BigDecimal.ZERO) return BigDecimal.ZERO

        val abonos = extraPaymentRepository.findAllByCreditLineId(line.id!!)
        val estrategia = abonos.firstOrNull()?.estrategia
            ?: com.perseo.finance.domain.PaymentStrategy.REDUCIR_PLAZO
        val mapaAbonos = sumarAbonosPorCuota(abonos)
        val tasa = creditLineService.tasaEfectiva(line)

        val resultado = AmortizationEngine.simular(
            saldo = line.saldoPendiente.toDouble(),
            cuotasRestantes = line.cuotasRestantes,
            tasaEA = tasa.toDouble(),
            tipo = line.tipoAmortizacion,
            abonos = mapaAbonos,
            estrategia = estrategia
        )

        val cuota = resultado.filas.firstOrNull()?.cuotaTotal ?: 0.0
        return BigDecimal(cuota).setScale(2, RoundingMode.HALF_UP)
    }

    private fun sumarAbonosPorCuota(abonos: List<ExtraPaymentEntity>): Map<Int, Double> =
        abonos.groupBy { it.numeroCuota }
            .mapValues { (_, lista) -> lista.sumOf { it.monto.toDouble() } }
}
