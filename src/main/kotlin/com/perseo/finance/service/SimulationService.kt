package com.perseo.finance.service

import com.perseo.finance.domain.AmortizationEngine
import com.perseo.finance.domain.ComparisonResult
import com.perseo.finance.domain.PaymentStrategy
import com.perseo.finance.domain.SimulationResult
import com.perseo.finance.dto.*
import com.perseo.finance.entity.ExtraPaymentEntity
import com.perseo.finance.repository.ExtraPaymentRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.UUID

@Service
class SimulationService(
    private val creditLineService: CreditLineService,
    private val extraPaymentRepository: ExtraPaymentRepository
) {

    /**
     * Endpoint principal de simulación (HU-4.2 / HU-T.4): compara el
     * escenario sin abonos contra el escenario con los abonos ya guardados
     * para esta línea (HU-T.7).
     */
    @Transactional(readOnly = true)
    fun compararLinea(lineId: UUID): ComparisonResponse {
        val line = creditLineService.buscarEntidad(lineId)
        val tasa = creditLineService.tasaEfectiva(line)

        val abonosGuardados = extraPaymentRepository.findAllByCreditLineId(lineId)
        val estrategia = abonosGuardados.firstOrNull()?.estrategia ?: PaymentStrategy.REDUCIR_PLAZO
        val mapaAbonos = sumarAbonosPorCuota(abonosGuardados)

        val resultado = AmortizationEngine.comparar(
            saldo = line.saldoPendiente.toDouble(),
            cuotasRestantes = line.cuotasRestantes,
            tasaEA = tasa.toDouble(),
            tipo = line.tipoAmortizacion,
            abonos = mapaAbonos,
            estrategia = estrategia
        )

        return resultado.toResponse()
    }

    @Transactional(readOnly = true)
    fun listarAbonos(lineId: UUID): List<ExtraPaymentResponse> =
        extraPaymentRepository.findAllByCreditLineId(lineId).map {
            ExtraPaymentResponse(
                id = it.id!!,
                numeroCuota = it.numeroCuota,
                monto = it.monto,
                estrategia = it.estrategia
            )
        }

    @Transactional
    fun agregarAbono(lineId: UUID, request: CreateExtraPaymentRequest): CreditLineResponse {
        val line = creditLineService.buscarEntidad(lineId)
        extraPaymentRepository.save(
            ExtraPaymentEntity(
                creditLine = line,
                numeroCuota = request.numeroCuota,
                monto = request.monto,
                estrategia = request.estrategia
            )
        )
        return creditLineService.obtener(lineId)
    }

    @Transactional
    fun eliminarAbono(lineId: UUID, extraPaymentId: UUID) {
        extraPaymentRepository.deleteByIdAndCreditLineId(extraPaymentId, lineId)
    }

    private fun sumarAbonosPorCuota(abonos: List<ExtraPaymentEntity>): Map<Int, Double> =
        abonos.groupBy { it.numeroCuota }
            .mapValues { (_, lista) -> lista.sumOf { it.monto.toDouble() } }

    private fun ComparisonResult.toResponse() = ComparisonResponse(
        sinAbonos = sinAbonos.toResponse(),
        conAbonos = conAbonos.toResponse(),
        ahorroIntereses = ahorroIntereses.toMoney(),
        mesesAhorrados = mesesAhorrados
    )

    private fun SimulationResult.toResponse() = SimulationResponse(
        filas = filas.map {
            ScheduleRowResponse(
                numeroCuota = it.numeroCuota,
                interes = it.interes.toMoney(),
                capital = it.capital.toMoney(),
                cuotaTotal = it.cuotaTotal.toMoney(),
                saldoFinal = it.saldoFinal.toMoney(),
                abonoExtra = it.abonoExtra.toMoney()
            )
        },
        meses = meses,
        totalInteres = totalInteres.toMoney(),
        totalCapital = totalCapital.toMoney(),
        totalPagado = totalPagado.toMoney(),
        nuncaSePagaEnPlazo = nuncaSePagaEnPlazo
    )

    private fun Double.toMoney(): BigDecimal =
        BigDecimal(this).setScale(2, RoundingMode.HALF_UP)
}
