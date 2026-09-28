package com.perseo.finance.controller

import com.perseo.finance.dto.ComparisonResponse
import com.perseo.finance.dto.CreateExtraPaymentRequest
import com.perseo.finance.dto.CreditLineResponse
import com.perseo.finance.dto.ExtraPaymentResponse
import com.perseo.finance.service.SimulationService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/lines/{lineId}")
class SimulationController(
    private val simulationService: SimulationService
) {

    /** HU-4.2 / HU-T.4: compara sin abonos vs. con los abonos guardados de la línea. */
    @GetMapping("/simulation")
    fun comparar(@PathVariable lineId: UUID): ComparisonResponse =
        simulationService.compararLinea(lineId)

    @GetMapping("/extra-payments")
    fun listarAbonos(@PathVariable lineId: UUID): List<ExtraPaymentResponse> =
        simulationService.listarAbonos(lineId)

    /** HU-4.3 / HU-T.7: agrega un abono extraordinario a una cuota específica. */
    @PostMapping("/extra-payments")
    @ResponseStatus(HttpStatus.CREATED)
    fun agregarAbono(
        @PathVariable lineId: UUID,
        @Valid @RequestBody request: CreateExtraPaymentRequest
    ): CreditLineResponse = simulationService.agregarAbono(lineId, request)

    @DeleteMapping("/extra-payments/{extraPaymentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun eliminarAbono(@PathVariable lineId: UUID, @PathVariable extraPaymentId: UUID) =
        simulationService.eliminarAbono(lineId, extraPaymentId)
}
