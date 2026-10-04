package com.perseo.finance.savings.service

import com.perseo.finance.common.badRequest
import com.perseo.finance.common.conflict
import com.perseo.finance.common.notFound
import com.perseo.finance.savings.domain.MovementType
import com.perseo.finance.savings.dto.SavingsBalanceCheckRequest
import com.perseo.finance.savings.dto.SavingsBalanceCheckResponse
import com.perseo.finance.savings.dto.SavingsMovementRequest
import com.perseo.finance.savings.dto.SavingsMovementResponse
import com.perseo.finance.savings.entity.SavingsBalanceCheckEntity
import com.perseo.finance.savings.entity.SavingsContributionEntity
import com.perseo.finance.savings.repository.SavingsBalanceCheckRepository
import com.perseo.finance.savings.repository.SavingsContributionRepository
import com.perseo.finance.savings.repository.SavingsGoalRepository
import com.perseo.finance.savings.repository.SavingsPlanRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.UUID

/** Aportes y retiros reales (A.11, A.15, A.22) y conciliaciones de saldo (A.14). */
@Service
class SavingsMovementService(
    private val goalRepository: SavingsGoalRepository,
    private val planRepository: SavingsPlanRepository,
    private val contributionRepository: SavingsContributionRepository,
    private val balanceCheckRepository: SavingsBalanceCheckRepository,
    private val progressService: SavingsProgressService
) {

    // ---------------- Movimientos ----------------

    @Transactional(readOnly = true)
    fun listarMovimientos(goalId: UUID): List<SavingsMovementResponse> {
        verificarMeta(goalId)
        return contributionRepository.findByGoalIdOrderByFechaDescCreatedAtDesc(goalId).map { it.toResponse() }
    }

    @Transactional
    fun crearMovimiento(goalId: UUID, request: SavingsMovementRequest): SavingsMovementResponse {
        verificarMeta(goalId)
        validarFecha(goalId, request.fecha)
        validarRetiro(goalId, request)

        val guardado = contributionRepository.save(
            SavingsContributionEntity(
                goalId = goalId,
                fecha = request.fecha,
                monto = request.monto,
                tipo = request.tipo,
                nota = request.nota?.trim()?.ifBlank { null }
            )
        )
        progressService.sincronizarEstado(goalId)
        return guardado.toResponse()
    }

    @Transactional
    fun actualizarMovimiento(id: UUID, request: SavingsMovementRequest): SavingsMovementResponse {
        val movimiento = contributionRepository.findById(id).orElse(null) ?: notFound("Movimiento no encontrado.")
        validarFecha(movimiento.goalId, request.fecha)
        validarRetiro(movimiento.goalId, request)

        movimiento.fecha = request.fecha
        movimiento.monto = request.monto
        movimiento.tipo = request.tipo
        movimiento.nota = request.nota?.trim()?.ifBlank { null }
        val guardado = contributionRepository.save(movimiento)
        progressService.sincronizarEstado(movimiento.goalId)
        return guardado.toResponse()
    }

    @Transactional
    fun eliminarMovimiento(id: UUID) {
        val movimiento = contributionRepository.findById(id).orElse(null) ?: notFound("Movimiento no encontrado.")
        contributionRepository.delete(movimiento)
        contributionRepository.flush()
        progressService.sincronizarEstado(movimiento.goalId)
    }

    // ---------------- Conciliaciones ----------------

    @Transactional(readOnly = true)
    fun listarConciliaciones(goalId: UUID): List<SavingsBalanceCheckResponse> {
        verificarMeta(goalId)
        return balanceCheckRepository.findByGoalIdOrderByFechaDescCreatedAtDesc(goalId).map { it.toResponse() }
    }

    @Transactional
    fun crearConciliacion(goalId: UUID, request: SavingsBalanceCheckRequest): SavingsBalanceCheckResponse {
        verificarMeta(goalId)
        validarFecha(goalId, request.fecha)

        val guardada = balanceCheckRepository.save(
            SavingsBalanceCheckEntity(
                goalId = goalId,
                fecha = request.fecha,
                saldoReal = request.saldoReal,
                nota = request.nota?.trim()?.ifBlank { null }
            )
        )
        progressService.sincronizarEstado(goalId)
        return guardada.toResponse()
    }

    @Transactional
    fun actualizarConciliacion(id: UUID, request: SavingsBalanceCheckRequest): SavingsBalanceCheckResponse {
        val conciliacion = balanceCheckRepository.findById(id).orElse(null) ?: notFound("Conciliación no encontrada.")
        validarFecha(conciliacion.goalId, request.fecha)

        conciliacion.fecha = request.fecha
        conciliacion.saldoReal = request.saldoReal
        conciliacion.nota = request.nota?.trim()?.ifBlank { null }
        val guardada = balanceCheckRepository.save(conciliacion)
        progressService.sincronizarEstado(conciliacion.goalId)
        return guardada.toResponse()
    }

    @Transactional
    fun eliminarConciliacion(id: UUID) {
        val conciliacion = balanceCheckRepository.findById(id).orElse(null) ?: notFound("Conciliación no encontrada.")
        balanceCheckRepository.delete(conciliacion)
        balanceCheckRepository.flush()
        progressService.sincronizarEstado(conciliacion.goalId)
    }

    // ---------------- Validaciones ----------------

    private fun verificarMeta(goalId: UUID) {
        if (!goalRepository.existsById(goalId)) notFound("Meta no encontrada.")
    }

    /** No futura, y no anterior al inicio del plan de referencia (si existe). */
    private fun validarFecha(goalId: UUID, fecha: LocalDate) {
        if (fecha.isAfter(LocalDate.now())) badRequest("La fecha no puede ser futura.")
        val plan = planRepository.findByGoalIdOrderByCreatedAtAsc(goalId).firstOrNull { it.esReferencia }
        if (plan != null) {
            val inicio = plan.fechaInicio.withDayOfMonth(1)
            if (fecha.isBefore(inicio)) badRequest("La fecha no puede ser anterior al inicio del plan ($inicio).")
        }
    }

    /** Decisión D6: un retiro mayor al saldo estimado se advierte y requiere confirmación. */
    private fun validarRetiro(goalId: UUID, request: SavingsMovementRequest) {
        if (request.tipo != MovementType.RETIRO || request.confirmarExceso) return
        val saldo = progressService.obtener(goalId, null).saldoRealEstimado
        if (request.monto > saldo) {
            conflict(
                "El retiro (${request.monto.toPlainString()}) supera el saldo estimado (${saldo.toPlainString()}). " +
                    "Si es correcto, envíalo con confirmarExceso=true."
            )
        }
    }

    private fun SavingsContributionEntity.toResponse() = SavingsMovementResponse(
        id = id!!, goalId = goalId, fecha = fecha, monto = monto, tipo = tipo, nota = nota, createdAt = createdAt
    )

    private fun SavingsBalanceCheckEntity.toResponse() = SavingsBalanceCheckResponse(
        id = id!!, goalId = goalId, fecha = fecha, saldoReal = saldoReal, nota = nota, createdAt = createdAt
    )
}
