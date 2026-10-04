package com.perseo.finance.savings.repository

import com.perseo.finance.savings.domain.ProductType
import com.perseo.finance.savings.domain.TaxConcept
import com.perseo.finance.savings.entity.TaxParameterEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface TaxParameterRepository : JpaRepository<TaxParameterEntity, UUID> {
    fun findByTipoProductoAndConceptoOrderByVigenteDesdeDesc(
        tipoProducto: ProductType,
        concepto: TaxConcept
    ): List<TaxParameterEntity>

    fun findByTipoProductoOrderByConceptoAscVigenteDesdeDesc(tipoProducto: ProductType): List<TaxParameterEntity>

    fun findAllByOrderByTipoProductoAscConceptoAscVigenteDesdeDesc(): List<TaxParameterEntity>
}
