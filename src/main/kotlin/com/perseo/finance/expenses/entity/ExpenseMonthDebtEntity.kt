package com.perseo.finance.expenses.entity

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

/**
 * Deuda de un mes (bloque U-W del Excel): cuánto se debía y cuánto se pagó.
 * El detalle de tarjetas vive en el módulo cards; aquí solo el pago mensual.
 */
@Entity
@Table(name = "expenses_month_debts")
class ExpenseMonthDebtEntity(
    @Id
    @GeneratedValue
    val id: UUID? = null,

    @Column(name = "user_id", nullable = false)
    var userId: UUID = UUID.randomUUID(),

    @Column(nullable = false, length = 60)
    var nombre: String = "",

    @Column
    var pendiente: Long? = null,

    @Column
    var pagado: Long? = null,

    @Column(nullable = false)
    var anio: Int = 0,

    @Column(nullable = false)
    var mes: Int = 0,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()
)
