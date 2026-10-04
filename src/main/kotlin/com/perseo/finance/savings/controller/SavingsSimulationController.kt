package com.perseo.finance.savings.controller

import com.perseo.finance.savings.dto.SavingsSimulationPreviewRequest
import com.perseo.finance.savings.dto.SavingsSimulationResponse
import com.perseo.finance.savings.service.SavingsSimulationService
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/simulations")
class SavingsSimulationController(
    private val simulationService: SavingsSimulationService
) {

    /** Recalcula en vivo mientras el usuario edita parámetros; no guarda nada. */
    @PostMapping("/preview")
    fun previsualizar(@Valid @RequestBody request: SavingsSimulationPreviewRequest): SavingsSimulationResponse =
        simulationService.previsualizar(request)
}
