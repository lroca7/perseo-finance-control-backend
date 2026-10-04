package com.perseo.finance.savings.controller

import com.perseo.finance.savings.dto.SavingsBalanceCheckRequest
import com.perseo.finance.savings.dto.SavingsBalanceCheckResponse
import com.perseo.finance.savings.dto.SavingsMovementRequest
import com.perseo.finance.savings.dto.SavingsMovementResponse
import com.perseo.finance.savings.service.SavingsMovementService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
class SavingsMovementController(
    private val movementService: SavingsMovementService
) {

    // ---- Aportes y retiros reales ----

    @GetMapping("/api/goals/{goalId}/contributions")
    fun listarMovimientos(@PathVariable goalId: UUID): List<SavingsMovementResponse> =
        movementService.listarMovimientos(goalId)

    @PostMapping("/api/goals/{goalId}/contributions")
    @ResponseStatus(HttpStatus.CREATED)
    fun crearMovimiento(
        @PathVariable goalId: UUID,
        @Valid @RequestBody request: SavingsMovementRequest
    ): SavingsMovementResponse =
        movementService.crearMovimiento(goalId, request)

    @PutMapping("/api/contributions/{id}")
    fun actualizarMovimiento(
        @PathVariable id: UUID,
        @Valid @RequestBody request: SavingsMovementRequest
    ): SavingsMovementResponse =
        movementService.actualizarMovimiento(id, request)

    @DeleteMapping("/api/contributions/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun eliminarMovimiento(@PathVariable id: UUID) =
        movementService.eliminarMovimiento(id)

    // ---- Conciliaciones de saldo ----

    @GetMapping("/api/goals/{goalId}/balance-checks")
    fun listarConciliaciones(@PathVariable goalId: UUID): List<SavingsBalanceCheckResponse> =
        movementService.listarConciliaciones(goalId)

    @PostMapping("/api/goals/{goalId}/balance-checks")
    @ResponseStatus(HttpStatus.CREATED)
    fun crearConciliacion(
        @PathVariable goalId: UUID,
        @Valid @RequestBody request: SavingsBalanceCheckRequest
    ): SavingsBalanceCheckResponse =
        movementService.crearConciliacion(goalId, request)

    @PutMapping("/api/balance-checks/{id}")
    fun actualizarConciliacion(
        @PathVariable id: UUID,
        @Valid @RequestBody request: SavingsBalanceCheckRequest
    ): SavingsBalanceCheckResponse =
        movementService.actualizarConciliacion(id, request)

    @DeleteMapping("/api/balance-checks/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun eliminarConciliacion(@PathVariable id: UUID) =
        movementService.eliminarConciliacion(id)
}
