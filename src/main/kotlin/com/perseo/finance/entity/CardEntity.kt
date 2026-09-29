package com.perseo.finance.entity

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "cards")
class CardEntity(
    @Id
    @GeneratedValue
    val id: UUID? = null,

    @Column(name = "user_id", nullable = false)
    var userId: UUID = UUID.randomUUID(),

    @Column(nullable = false, length = 100)
    var banco: String = "",

    @Column(nullable = false, length = 100)
    var alias: String = "",

    @Column(name = "cupo_total", nullable = false, precision = 14, scale = 2)
    var cupoTotal: BigDecimal = BigDecimal.ZERO,

    @Column(name = "tasa_ea", nullable = false, precision = 6, scale = 3)
    var tasaEA: BigDecimal = BigDecimal.ZERO,

    /**
     * Cuota de manejo + seguro + otros cargos fijos recurrentes, agregados
     * en un solo valor editable por el usuario. No ligado a ninguna línea
     * de crédito puntual — se usa para el "pago aproximado del mes en curso".
     */
    @Column(name = "cargos_fijos_mensuales", nullable = false, precision = 14, scale = 2)
    var cargosFijosMensuales: BigDecimal = BigDecimal.ZERO,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),

    @OneToMany(mappedBy = "card", cascade = [CascadeType.ALL], orphanRemoval = true)
    val creditLines: MutableList<CreditLineEntity> = mutableListOf()
)
