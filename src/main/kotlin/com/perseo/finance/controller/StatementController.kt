package com.perseo.finance.controller

import com.perseo.finance.dto.CreateStatementRequest
import com.perseo.finance.dto.StatementResponse
import com.perseo.finance.pdf.BankLayout
import com.perseo.finance.pdf.ParsedStatementResponse
import com.perseo.finance.pdf.PdfTextExtractor
import com.perseo.finance.pdf.StatementParserFactory
import com.perseo.finance.service.StatementService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.util.UUID

@RestController
class StatementController(
    private val statementService: StatementService,
    private val pdfTextExtractor: PdfTextExtractor
) {

    @GetMapping("/api/cards/{cardId}/statements")
    fun listar(@PathVariable cardId: UUID): List<StatementResponse> =
        statementService.listarPorTarjeta(cardId)

    @PostMapping("/api/cards/{cardId}/statements")
    @ResponseStatus(HttpStatus.CREATED)
    fun crear(
        @PathVariable cardId: UUID,
        @Valid @RequestBody request: CreateStatementRequest
    ): StatementResponse = statementService.crear(cardId, request)

    /**
     * HU-2.2: sube el PDF y devuelve una PREVIEW de lo que se pudo leer.
     * No persiste nada — el usuario revisa/corrige en el frontend y confirma
     * con el POST normal de arriba (y con POST /cards/{cardId}/lines para
     * los candidatos de línea de crédito que quiera importar).
     *
     * `banco` decide qué parser usar (cada banco compone su extracto
     * distinto — ver HU-F.5). Por defecto Davivienda, por compatibilidad.
     */
    @PostMapping("/api/cards/{cardId}/statements/parse-pdf", consumes = ["multipart/form-data"])
    fun parsearPdf(
        @PathVariable cardId: UUID,
        @RequestParam("file") file: MultipartFile,
        @RequestParam(defaultValue = "DAVIVIENDA") banco: BankLayout
    ): ParsedStatementResponse {
        val texto = pdfTextExtractor.extractText(file.inputStream)
        return StatementParserFactory.forBank(banco).parse(texto)
    }

    @DeleteMapping("/api/statements/{statementId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun eliminar(@PathVariable statementId: UUID) =
        statementService.eliminar(statementId)
}
