package com.perseo.finance.expenses.entity

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

/**
 * Ahorro de un mes (bloque Q-S del Excel): flujo de caja mensual.
 * El detalle de metas vive en el módulo savings; aquí solo cuánto se ahorró.
 */
@Entity
@Table(name = "expenses_month_savings")
class ExpenseMonthSavingEntity(
    @Id
    @GeneratedValue
    val id: UUID? = null,

    @Column(name = "user_id", nullable = false)
    var userId: UUID = UUID.randomUUID(),

    @Column(nullable = false, length = 60)
    var nombre: String = "",

    @Column
    var meta: Long? = null,

    @Column
    var ahorrado: Long? = null,

    @Column(nullable = false)
    var anio: Int = 0,

    @Column(nullable = false)
    var mes: Int = 0,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()
)
