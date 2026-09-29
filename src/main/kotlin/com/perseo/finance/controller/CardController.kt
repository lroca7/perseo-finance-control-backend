package com.perseo.finance.controller

import com.perseo.finance.dto.CardResponse
import com.perseo.finance.dto.CreateCardRequest
import com.perseo.finance.dto.EstimatedMonthlyPaymentResponse
import com.perseo.finance.dto.UpdateCardRequest
import com.perseo.finance.service.CardService
import com.perseo.finance.service.EstimatedPaymentService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import java.util.UUID

/**
 * NOTA: mientras no exista autenticación (HU-T.8), el userId se recibe
 * como parámetro fijo/temporal. Cuando se agregue JWT, este parámetro
 * se reemplaza por el usuario resuelto del token.
 */
@RestController
@RequestMapping("/api/cards")
class CardController(
    private val cardService: CardService,
    private val estimatedPaymentService: EstimatedPaymentService
) {

    @GetMapping
    fun listar(@RequestParam userId: UUID): List<CardResponse> =
        cardService.listarPorUsuario(userId)

    @GetMapping("/{cardId}")
    fun obtener(@PathVariable cardId: UUID): CardResponse =
        cardService.obtener(cardId)

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun crear(@RequestParam userId: UUID, @Valid @RequestBody request: CreateCardRequest): CardResponse =
        cardService.crear(userId, request)

    @PutMapping("/{cardId}")
    fun actualizar(@PathVariable cardId: UUID, @Valid @RequestBody request: UpdateCardRequest): CardResponse =
        cardService.actualizar(cardId, request)

    @DeleteMapping("/{cardId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun eliminar(@PathVariable cardId: UUID) =
        cardService.eliminar(cardId)

    /** Pago aproximado del mes en curso (cuota #1 de cada línea activa + cargos fijos). */
    @GetMapping("/{cardId}/pago-estimado-mes")
    fun pagoEstimadoMes(@PathVariable cardId: UUID): EstimatedMonthlyPaymentResponse =
        estimatedPaymentService.estimarMesActual(cardId)
}
