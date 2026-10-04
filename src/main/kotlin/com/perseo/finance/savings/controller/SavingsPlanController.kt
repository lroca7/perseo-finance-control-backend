package com.perseo.finance.savings.controller

import com.perseo.finance.savings.dto.SavingsPlanRequest
import com.perseo.finance.savings.dto.SavingsPlanResponse
import com.perseo.finance.savings.dto.SavingsSimulationResponse
import com.perseo.finance.savings.service.SavingsPlanService
import com.perseo.finance.savings.service.SavingsSimulationService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
class SavingsPlanController(
    private val planService: SavingsPlanService,
    private val simulationService: SavingsSimulationService
) {

    @GetMapping("/api/goals/{goalId}/plans")
    fun listar(@PathVariable goalId: UUID): List<SavingsPlanResponse> =
        planService.listar(goalId)

    @PostMapping("/api/goals/{goalId}/plans")
    @ResponseStatus(HttpStatus.CREATED)
    fun crear(@PathVariable goalId: UUID, @Valid @RequestBody request: SavingsPlanRequest): SavingsPlanResponse =
        planService.crear(goalId, request)

    @GetMapping("/api/plans/{planId}")
    fun obtener(@PathVariable planId: UUID): SavingsPlanResponse =
        planService.obtener(planId)

    @PutMapping("/api/plans/{planId}")
    fun actualizar(@PathVariable planId: UUID, @Valid @RequestBody request: SavingsPlanRequest): SavingsPlanResponse =
        planService.actualizar(planId, request)

    /** Si es el plan de referencia y hay otros, indicar `nuevaReferenciaId`. */
    @DeleteMapping("/api/plans/{planId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun eliminar(@PathVariable planId: UUID, @RequestParam(required = false) nuevaReferenciaId: UUID?) =
        planService.eliminar(planId, nuevaReferenciaId)

    @PostMapping("/api/plans/{planId}/set-reference")
    fun marcarReferencia(@PathVariable planId: UUID): SavingsPlanResponse =
        planService.marcarReferencia(planId)

    @PostMapping("/api/plans/{planId}/duplicate")
    @ResponseStatus(HttpStatus.CREATED)
    fun duplicar(@PathVariable planId: UUID): SavingsPlanResponse =
        planService.duplicar(planId)

    /** Simulación de un plan guardado (usa el saldo inicial y el objetivo de su meta). */
    @GetMapping("/api/plans/{planId}/simulation")
    fun simular(@PathVariable planId: UUID): SavingsSimulationResponse =
        simulationService.simularPlan(planId)
}
