package com.perseo.finance.savings.entity

import com.perseo.finance.savings.domain.ContributionTiming
import com.perseo.finance.savings.domain.ProductType
import com.perseo.finance.savings.domain.WithholdingMode
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

/**
 * Plan = hipótesis de simulación de una meta. Tasas y porcentajes como FRACCIÓN (0.11 = 11 %).
 */
@Entity
@Table(name = "savings_plans")
class SavingsPlanEntity(
    @Id
    @GeneratedValue
    val id: UUID? = null,

    @Column(name = "goal_id", nullable = false)
    var goalId: UUID = UUID.randomUUID(),

    @Column(nullable = false, length = 80)
    var nombre: String = "",

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_producto", nullable = false, length = 20)
    var tipoProducto: ProductType = ProductType.CUENTA_AHORROS,

    @Column(name = "tasa_ea", nullable = false, precision = 9, scale = 6)
    var tasaEA: BigDecimal = BigDecimal.ZERO,

    @Column(name = "comision_ea", nullable = false, precision = 9, scale = 6)
    var comisionEA: BigDecimal = BigDecimal.ZERO,

    @Column(name = "aporte_mensual", nullable = false, precision = 18, scale = 2)
    var aporteMensual: BigDecimal = BigDecimal.ZERO,

    @Enumerated(EnumType.STRING)
    @Column(name = "momento_aporte", nullable = false, length = 10)
    var momentoAporte: ContributionTiming = ContributionTiming.FIN,

    @Column(name = "crecimiento_aporte_anual", nullable = false, precision = 9, scale = 6)
    var crecimientoAporteAnual: BigDecimal = BigDecimal.ZERO,

    @Column(name = "plazo_meses", nullable = false)
    var plazoMeses: Int = 12,

    @Column(name = "fecha_inicio", nullable = false)
    var fechaInicio: LocalDate = LocalDate.now().withDayOfMonth(1),

    @Column(name = "inflacion_anual", nullable = false, precision = 9, scale = 6)
    var inflacionAnual: BigDecimal = BigDecimal.ZERO,

    @Enumerated(EnumType.STRING)
    @Column(name = "tratamiento_retencion", nullable = false, length = 20)
    var tratamientoRetencion: WithholdingMode = WithholdingMode.MENSUAL,

    /** null = usar la tarifa vigente de `tax_parameters`. */
    @Column(name = "retencion_tarifa", precision = 9, scale = 6)
    var retencionTarifa: BigDecimal? = null,

    @Column(name = "es_referencia", nullable = false)
    var esReferencia: Boolean = false,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()
)
