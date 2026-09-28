package com.perseo.finance.entity

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@Entity
@Table(
    name = "statements",
    uniqueConstraints = [UniqueConstraint(columnNames = ["card_id", "periodo_fin"])]
)
class StatementEntity(
    @Id
    @GeneratedValue
    val id: UUID? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "card_id", nullable = false)
    var card: CardEntity? = null,

    @Column(name = "periodo_inicio", nullable = false)
    var periodoInicio: LocalDate = LocalDate.now(),

    @Column(name = "periodo_fin", nullable = false)
    var periodoFin: LocalDate = LocalDate.now(),

    @Column(name = "saldo_total", nullable = false, precision = 14, scale = 2)
    var saldoTotal: BigDecimal = BigDecimal.ZERO,

    @Column(name = "pago_minimo", nullable = false, precision = 14, scale = 2)
    var pagoMinimo: BigDecimal = BigDecimal.ZERO,

    @Column(name = "fecha_limite_pago", nullable = false)
    var fechaLimitePago: LocalDate = LocalDate.now(),

    @Column(name = "cuota_manejo", nullable = false, precision = 14, scale = 2)
    var cuotaManejo: BigDecimal = BigDecimal.ZERO,

    @Column(name = "otros_cargos", nullable = false, precision = 14, scale = 2)
    var otrosCargos: BigDecimal = BigDecimal.ZERO,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now()
)
