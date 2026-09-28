package com.perseo.finance.controller

import com.perseo.finance.dto.CreateCreditLineRequest
import com.perseo.finance.dto.CreditLineResponse
import com.perseo.finance.dto.UpdateCreditLineRequest
import com.perseo.finance.service.CreditLineService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
class CreditLineController(
    private val creditLineService: CreditLineService
) {

    @GetMapping("/api/cards/{cardId}/lines")
    fun listar(@PathVariable cardId: UUID): List<CreditLineResponse> =
        creditLineService.listarPorTarjeta(cardId)

    @GetMapping("/api/lines/{lineId}")
    fun obtener(@PathVariable lineId: UUID): CreditLineResponse =
        creditLineService.obtener(lineId)

    @PostMapping("/api/cards/{cardId}/lines")
    @ResponseStatus(HttpStatus.CREATED)
    fun crear(
        @PathVariable cardId: UUID,
        @Valid @RequestBody request: CreateCreditLineRequest
    ): CreditLineResponse = creditLineService.crear(cardId, request)

    @PutMapping("/api/lines/{lineId}")
    fun actualizar(
        @PathVariable lineId: UUID,
        @Valid @RequestBody request: UpdateCreditLineRequest
    ): CreditLineResponse = creditLineService.actualizar(lineId, request)

    @DeleteMapping("/api/lines/{lineId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun eliminar(@PathVariable lineId: UUID) =
        creditLineService.eliminar(lineId)
}
