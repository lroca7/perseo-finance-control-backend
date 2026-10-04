package com.perseo.finance.savings.controller

import com.perseo.finance.savings.dto.CreateGoalRequest
import com.perseo.finance.savings.dto.SavingsGoalResponse
import com.perseo.finance.savings.dto.UpdateGoalRequest
import com.perseo.finance.savings.service.SavingsGoalService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import java.util.UUID

/**
 * NOTA: igual que CardController, mientras no exista autenticación (HU-T.8)
 * el userId se recibe como parámetro.
 */
@RestController
@RequestMapping("/api/goals")
class SavingsGoalController(
    private val goalService: SavingsGoalService
) {

    @GetMapping
    fun listar(
        @RequestParam userId: UUID,
        @RequestParam(defaultValue = "false") incluirArchivadas: Boolean
    ): List<SavingsGoalResponse> =
        goalService.listar(userId, incluirArchivadas)

    @GetMapping("/{goalId}")
    fun obtener(@PathVariable goalId: UUID): SavingsGoalResponse =
        goalService.obtener(goalId)

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun crear(@RequestParam userId: UUID, @Valid @RequestBody request: CreateGoalRequest): SavingsGoalResponse =
        goalService.crear(userId, request)

    @PutMapping("/{goalId}")
    fun actualizar(@PathVariable goalId: UUID, @Valid @RequestBody request: UpdateGoalRequest): SavingsGoalResponse =
        goalService.actualizar(goalId, request)

    @DeleteMapping("/{goalId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun eliminar(@PathVariable goalId: UUID) =
        goalService.eliminar(goalId)
}
