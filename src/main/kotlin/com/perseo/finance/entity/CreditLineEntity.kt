package com.perseo.finance.entity

import com.perseo.finance.domain.AmortizationType
import jakarta.persistence.*
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "credit_lines")
class CreditLineEntity(
    @Id
    @GeneratedValue
    val id: UUID? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "card_id", nullable = false)
    var card: CardEntity? = null,

    @Column(nullable = false, length = 150)
    var nombre: String = "",

    @Column(name = "saldo_pendiente", nullable = false, precision = 14, scale = 2)
    var saldoPendiente: BigDecimal = BigDecimal.ZERO,

    @Column(name = "cuotas_restantes", nullable = false)
    var cuotasRestantes: Int = 1,

    /** Si es null, el servicio debe usar la tasa de la tarjeta. */
    @Column(name = "tasa_ea", precision = 6, scale = 3)
    var tasaEA: BigDecimal? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_amortizacion", nullable = false, length = 20)
    var tipoAmortizacion: AmortizationType = AmortizationType.CAPITAL_FIJO,

    @Column(nullable = false)
    var ignorada: Boolean = false,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now(),

    @OneToMany(mappedBy = "creditLine", cascade = [CascadeType.ALL], orphanRemoval = true)
    val extraPayments: MutableList<ExtraPaymentEntity> = mutableListOf()
)
