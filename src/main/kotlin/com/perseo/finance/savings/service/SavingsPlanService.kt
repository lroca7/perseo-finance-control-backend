package com.perseo.finance.savings.service

import com.perseo.finance.common.badRequest
import com.perseo.finance.common.conflict
import com.perseo.finance.common.notFound
import com.perseo.finance.savings.dto.SavingsPlanRequest
import com.perseo.finance.savings.dto.SavingsPlanResponse
import com.perseo.finance.savings.entity.SavingsPlanEntity
import com.perseo.finance.savings.repository.SavingsGoalRepository
import com.perseo.finance.savings.repository.SavingsPlanRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@Service
class SavingsPlanService(
    private val goalRepository: SavingsGoalRepository,
    private val planRepository: SavingsPlanRepository
) {

    @Transactional(readOnly = true)
    fun listar(goalId: UUID): List<SavingsPlanResponse> {
        if (!goalRepository.existsById(goalId)) notFound("Meta no encontrada.")
        return planRepository.findByGoalIdOrderByCreatedAtAsc(goalId).map { it.toResponse() }
    }

    @Transactional(readOnly = true)
    fun obtener(planId: UUID): SavingsPlanResponse = obtenerEntidad(planId).toResponse()

    @Transactional
    fun crear(goalId: UUID, request: SavingsPlanRequest): SavingsPlanResponse {
        if (!goalRepository.existsById(goalId)) notFound("Meta no encontrada.")
        validar(request)

        val plan = SavingsPlanEntity(goalId = goalId)
        plan.aplicar(request)
        // El primer plan de una meta queda como referencia automáticamente (HU-A.26).
        plan.esReferencia = planRepository.countByGoalId(goalId) == 0L
        return planRepository.save(plan).toResponse()
    }

    @Transactional
    fun actualizar(planId: UUID, request: SavingsPlanRequest): SavingsPlanResponse {
        validar(request)
        val plan = obtenerEntidad(planId)
        plan.aplicar(request)
        return planRepository.save(plan).toResponse()
    }

    /**
     * Si se elimina el plan de referencia y quedan otros, hay que indicar el nuevo
     * con `nuevaReferenciaId` (HU-A.26 criterio 5).
     */
    @Transactional
    fun eliminar(planId: UUID, nuevaReferenciaId: UUID?) {
        val plan = obtenerEntidad(planId)

        if (plan.esReferencia) {
            val otros = planRepository.findByGoalIdOrderByCreatedAtAsc(plan.goalId).filter { it.id != plan.id }
            if (otros.isNotEmpty()) {
                val nuevaId = nuevaReferenciaId
                    ?: conflict("Este plan es el de referencia. Elige cuál será el nuevo plan de referencia.")
                val elegido = otros.firstOrNull { it.id == nuevaId }
                    ?: badRequest("El nuevo plan de referencia debe pertenecer a la misma meta.")

                planRepository.delete(plan)
                planRepository.flush()
                elegido.esReferencia = true
                elegido.updatedAt = Instant.now()
                planRepository.save(elegido)
                return
            }
        }
        planRepository.delete(plan)
    }

    @Transactional
    fun duplicar(planId: UUID): SavingsPlanResponse {
        val origen = obtenerEntidad(planId)
        val copia = SavingsPlanEntity(
            goalId = origen.goalId,
            nombre = "${origen.nombre} (copia)".take(80),
            tipoProducto = origen.tipoProducto,
            tasaEA = origen.tasaEA,
            comisionEA = origen.comisionEA,
            aporteMensual = origen.aporteMensual,
            momentoAporte = origen.momentoAporte,
            crecimientoAporteAnual = origen.crecimientoAporteAnual,
            plazoMeses = origen.plazoMeses,
            fechaInicio = origen.fechaInicio,
            inflacionAnual = origen.inflacionAnual,
            tratamientoRetencion = origen.tratamientoRetencion,
            retencionTarifa = origen.retencionTarifa,
            esReferencia = false
        )
        return planRepository.save(copia).toResponse()
    }

    /** Marca el plan como referencia de su meta; solo puede haber uno (RN-A.13). */
    @Transactional
    fun marcarReferencia(planId: UUID): SavingsPlanResponse {
        val goalId = obtenerEntidad(planId).goalId
        planRepository.clearReferencia(goalId) // limpia el contexto de persistencia
        val plan = obtenerEntidad(planId)
        plan.esReferencia = true
        plan.updatedAt = Instant.now()
        return planRepository.save(plan).toResponse()
    }

    fun obtenerEntidad(planId: UUID): SavingsPlanEntity =
        planRepository.findById(planId).orElse(null) ?: notFound("Plan no encontrado.")

    private fun validar(request: SavingsPlanRequest) {
        if (!request.tipoProducto.availableInMvp) {
            badRequest("El producto ${request.tipoProducto.label} aún no está disponible.")
        }
        if (!request.tipoProducto.allowsCommission && request.comisionEA.signum() > 0) {
            badRequest("El producto ${request.tipoProducto.label} no cobra comisión de administración.")
        }
    }

    private fun SavingsPlanEntity.aplicar(request: SavingsPlanRequest) {
        nombre = request.nombre.trim()
        tipoProducto = request.tipoProducto
        tasaEA = request.tasaEA
        comisionEA = request.comisionEA
        aporteMensual = request.aporteMensual
        momentoAporte = request.momentoAporte
        crecimientoAporteAnual = request.crecimientoAporteAnual
        plazoMeses = request.plazoMeses
        fechaInicio = request.fechaInicio ?: LocalDate.now().withDayOfMonth(1)
        inflacionAnual = request.inflacionAnual
        tratamientoRetencion = request.tratamientoRetencion ?: request.tipoProducto.defaultWithholding
        retencionTarifa = request.retencionTarifa
        updatedAt = Instant.now()
    }

    private fun SavingsPlanEntity.toResponse() = SavingsPlanResponse(
        id = id!!,
        goalId = goalId,
        nombre = nombre,
        tipoProducto = tipoProducto,
        tasaEA = tasaEA,
        comisionEA = comisionEA,
        aporteMensual = aporteMensual,
        momentoAporte = momentoAporte,
        crecimientoAporteAnual = crecimientoAporteAnual,
        plazoMeses = plazoMeses,
        fechaInicio = fechaInicio,
        inflacionAnual = inflacionAnual,
        tratamientoRetencion = tratamientoRetencion,
        retencionTarifa = retencionTarifa,
        esReferencia = esReferencia,
        createdAt = createdAt
    )
}
