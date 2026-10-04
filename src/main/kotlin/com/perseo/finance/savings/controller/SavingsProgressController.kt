package com.perseo.finance.savings.controller

import com.perseo.finance.savings.dto.SavingsProgressResponse
import com.perseo.finance.savings.service.SavingsProgressService
import org.springframework.web.bind.annotation.*
import java.math.BigDecimal
import java.util.UUID

@RestController
class SavingsProgressController(
    private val progressService: SavingsProgressService
) {

    /**
     * Plan vs. realidad y proyección al ritmo actual. `ritmo` (opcional) reemplaza el promedio
     * de los últimos 3 meses cerrados, para responder "¿y si ahorro X al mes?".
     */
    @GetMapping("/api/goals/{goalId}/progress")
    fun progreso(
        @PathVariable goalId: UUID,
        @RequestParam(required = false) ritmo: BigDecimal?
    ): SavingsProgressResponse =
        progressService.obtener(goalId, ritmo)
}
