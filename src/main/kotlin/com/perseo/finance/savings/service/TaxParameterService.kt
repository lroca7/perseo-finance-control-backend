package com.perseo.finance.savings.service

import com.perseo.finance.savings.domain.ProductType
import com.perseo.finance.savings.domain.ResolvedRates
import com.perseo.finance.savings.domain.TaxConcept
import com.perseo.finance.savings.domain.TaxRateEntry
import com.perseo.finance.savings.domain.TaxRateResolver
import com.perseo.finance.savings.dto.SavingsProductResponse
import com.perseo.finance.savings.dto.TaxParameterResponse
import com.perseo.finance.savings.repository.TaxParameterRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate

@Service
class TaxParameterService(
    private val repository: TaxParameterRepository
) {

    /** Parámetros vigentes en `fecha` (HU-A.20). Opcionalmente filtrados por producto. */
    @Transactional(readOnly = true)
    fun vigentes(tipo: ProductType?, fecha: LocalDate): List<TaxParameterResponse> {
        val todos = if (tipo != null) {
            repository.findByTipoProductoOrderByConceptoAscVigenteDesdeDesc(tipo)
        } else {
            repository.findAllByOrderByTipoProductoAscConceptoAscVigenteDesdeDesc()
        }
        return todos
            .filter { p ->
                val hasta = p.vigenteHasta
                !p.vigenteDesde.isAfter(fecha) && (hasta == null || !hasta.isBefore(fecha))
            }
            .map { p ->
                TaxParameterResponse(
                    tipoProducto = p.tipoProducto,
                    concepto = p.concepto,
                    tarifa = p.tarifa,
                    vigenteDesde = p.vigenteDesde,
                    vigenteHasta = p.vigenteHasta,
                    notas = p.notas
                )
            }
    }

    /** Tarifa de retención de cada mes del plan, buscada por la fecha de ese mes (RN-A.10). */
    @Transactional(readOnly = true)
    fun resolverRetencion(
        tipo: ProductType,
        fechaInicio: LocalDate,
        meses: Int,
        personalizada: BigDecimal?
    ): ResolvedRates = TaxRateResolver.resolve(entradasRetencion(tipo), fechaInicio, meses, personalizada)

    /** Productos ofrecidos en el MVP con sus reglas precargadas (HU-A.5). */
    @Transactional(readOnly = true)
    fun productos(): List<SavingsProductResponse> {
        val hoy = LocalDate.now()
        return ProductType.values().filter { it.availableInMvp }.map { tipo ->
            val entradas = entradasRetencion(tipo)
            val tarifa = if (entradas.isEmpty()) null else TaxRateResolver.resolve(entradas, hoy, 1).forMonth(1)
            SavingsProductResponse(
                tipo = tipo,
                nombre = tipo.label,
                tratamientoRetencion = tipo.defaultWithholding,
                permiteComision = tipo.allowsCommission,
                retencionVigente = tarifa
            )
        }
    }

    private fun entradasRetencion(tipo: ProductType): List<TaxRateEntry> =
        repository.findByTipoProductoAndConceptoOrderByVigenteDesdeDesc(tipo, TaxConcept.RETENCION_RENDIMIENTOS)
            .map { TaxRateEntry(it.tarifa, it.vigenteDesde, it.vigenteHasta) }
}
