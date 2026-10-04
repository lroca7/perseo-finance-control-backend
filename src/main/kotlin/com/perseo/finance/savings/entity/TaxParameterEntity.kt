package com.perseo.finance.savings.entity

import com.perseo.finance.savings.domain.ProductType
import com.perseo.finance.savings.domain.TaxConcept
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

@Entity
@Table(name = "tax_parameters")
class TaxParameterEntity(
    @Id
    @GeneratedValue
    val id: UUID? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_producto", nullable = false, length = 20)
    var tipoProducto: ProductType = ProductType.MANUAL,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    var concepto: TaxConcept = TaxConcept.RETENCION_RENDIMIENTOS,

    @Column(nullable = false, precision = 9, scale = 6)
    var tarifa: BigDecimal = BigDecimal.ZERO,

    @Column(name = "vigente_desde", nullable = false)
    var vigenteDesde: LocalDate = LocalDate.now(),

    @Column(name = "vigente_hasta")
    var vigenteHasta: LocalDate? = null,

    @Column(length = 200)
    var notas: String? = null
)
