package com.perseo.finance.savings.controller

import com.perseo.finance.savings.domain.ProductType
import com.perseo.finance.savings.dto.SavingsProductResponse
import com.perseo.finance.savings.dto.TaxParameterResponse
import com.perseo.finance.savings.service.TaxParameterService
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.*
import java.time.LocalDate

@RestController
class SavingsCatalogController(
    private val taxParameterService: TaxParameterService
) {

    /** Parámetros tributarios vigentes en `fecha` (por defecto hoy). */
    @GetMapping("/api/tax-parameters")
    fun parametros(
        @RequestParam(required = false) tipo: ProductType?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) fecha: LocalDate?
    ): List<TaxParameterResponse> =
        taxParameterService.vigentes(tipo, fecha ?: LocalDate.now())

    /** Productos de ahorro con sus reglas fiscales precargadas (HU-A.5). */
    @GetMapping("/api/savings/products")
    fun productos(): List<SavingsProductResponse> =
        taxParameterService.productos()
}
