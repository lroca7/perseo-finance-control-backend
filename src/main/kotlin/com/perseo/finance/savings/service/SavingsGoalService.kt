package com.perseo.finance.savings.service

import com.perseo.finance.common.badRequest
import com.perseo.finance.common.notFound
import com.perseo.finance.savings.domain.GoalStatus
import com.perseo.finance.savings.dto.CreateGoalRequest
import com.perseo.finance.savings.dto.SavingsGoalResponse
import com.perseo.finance.savings.dto.SavingsProgressResponse
import com.perseo.finance.savings.dto.UpdateGoalRequest
import com.perseo.finance.savings.entity.SavingsGoalEntity
import com.perseo.finance.savings.entity.SavingsPlanEntity
import com.perseo.finance.savings.repository.SavingsBalanceCheckRepository
import com.perseo.finance.savings.repository.SavingsContributionRepository
import com.perseo.finance.savings.repository.SavingsGoalRepository
import com.perseo.finance.savings.repository.SavingsPlanRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Service
class SavingsGoalService(
    private val goalRepository: SavingsGoalRepository,
    private val planRepository: SavingsPlanRepository,
    private val contributionRepository: SavingsContributionRepository,
    private val balanceCheckRepository: SavingsBalanceCheckRepository,
    private val progressService: SavingsProgressService
) {

    @Transactional(readOnly = true)
    fun listar(userId: UUID, incluirArchivadas: Boolean): List<SavingsGoalResponse> {
        val metas = if (incluirArchivadas) {
            goalRepository.findByUserIdOrderByCreatedAtDesc(userId)
        } else {
            goalRepository.findByUserIdAndEstadoNotOrderByCreatedAtDesc(userId, GoalStatus.ARCHIVADA)
        }
        if (metas.isEmpty()) return emptyList()

        val ids = metas.map { it.id!! }
        val planes = planRepository.findByGoalIdIn(ids).groupBy { it.goalId }
        val movimientos = contributionRepository.findByGoalIdIn(ids).groupBy { it.goalId }
        val conciliaciones = balanceCheckRepository.findByGoalIdIn(ids).groupBy { it.goalId }

        return metas.map { meta ->
            val id = meta.id!!
            val susPlanes = planes[id] ?: emptyList()
            val progreso = progressService.calcular(
                meta, susPlanes, movimientos[id] ?: emptyList(), conciliaciones[id] ?: emptyList(), null
            )
            meta.toResponse(susPlanes, progreso)
        }
    }

    @Transactional(readOnly = true)
    fun obtener(goalId: UUID): SavingsGoalResponse = respuesta(obtenerEntidad(goalId))

    @Transactional
    fun crear(userId: UUID, request: CreateGoalRequest): SavingsGoalResponse {
        val meta = SavingsGoalEntity(
            userId = userId,
            nombre = request.nombre.trim(),
            icono = request.icono,
            montoObjetivo = request.montoObjetivo,
            saldoInicial = request.saldoInicial,
            fechaObjetivo = request.fechaObjetivo
        )
        return respuesta(goalRepository.save(meta))
    }

    @Transactional
    fun actualizar(goalId: UUID, request: UpdateGoalRequest): SavingsGoalResponse {
        val meta = obtenerEntidad(goalId)
        meta.nombre = request.nombre.trim()
        meta.icono = request.icono
        meta.montoObjetivo = request.montoObjetivo
        meta.saldoInicial = request.saldoInicial
        meta.fechaObjetivo = request.fechaObjetivo

        val nuevoEstado = request.estado
        if (nuevoEstado == GoalStatus.ALCANZADA) badRequest("El estado ALCANZADA lo asigna el sistema.")
        if (nuevoEstado != null) meta.estado = nuevoEstado
        meta.updatedAt = Instant.now()

        goalRepository.save(meta)
        // Cambiar el objetivo o el saldo inicial puede alcanzar o des-alcanzar la meta.
        progressService.sincronizarEstado(goalId)
        return respuesta(obtenerEntidad(goalId))
    }

    /** Planes, movimientos y conciliaciones se borran en cascada desde la BD. */
    @Transactional
    fun eliminar(goalId: UUID) {
        if (!goalRepository.existsById(goalId)) notFound("Meta no encontrada.")
        goalRepository.deleteById(goalId)
    }

    private fun obtenerEntidad(goalId: UUID): SavingsGoalEntity =
        goalRepository.findById(goalId).orElse(null) ?: notFound("Meta no encontrada.")

    private fun respuesta(meta: SavingsGoalEntity): SavingsGoalResponse {
        val id = meta.id!!
        val planes = planRepository.findByGoalIdOrderByCreatedAtAsc(id)
        val progreso = progressService.calcular(
            meta,
            planes,
            contributionRepository.findByGoalIdOrderByFechaDescCreatedAtDesc(id),
            balanceCheckRepository.findByGoalIdOrderByFechaDescCreatedAtDesc(id),
            null
        )
        return meta.toResponse(planes, progreso)
    }

    private fun SavingsGoalEntity.toResponse(planes: List<SavingsPlanEntity>, progreso: SavingsProgressResponse) =
        SavingsGoalResponse(
            id = id!!,
            nombre = nombre,
            icono = icono,
            montoObjetivo = montoObjetivo,
            saldoInicial = saldoInicial,
            fechaObjetivo = fechaObjetivo,
            estado = estado,
            planReferenciaId = planes.firstOrNull { it.esReferencia }?.id,
            cantidadPlanes = planes.size,
            saldoRealEstimado = progreso.saldoRealEstimado,
            avanceMeta = progreso.avanceMeta,
            estadoSeguimiento = progreso.estado,
            mesesParaMeta = progreso.mesesParaMeta,
            aporteNecesario = progreso.aporteNecesario,
            ritmoActual = progreso.ritmoActual,
            createdAt = createdAt
        )
}
