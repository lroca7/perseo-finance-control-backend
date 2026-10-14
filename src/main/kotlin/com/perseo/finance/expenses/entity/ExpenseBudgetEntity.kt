package com.perseo.finance.expenses.entity

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

/** Presupuesto proyectado de una categoría en un mes (celdas N22:N51 del Excel). */
@Entity
@Table(name = "expenses_budgets")
class ExpenseBudgetEntity(
    @Id
    @GeneratedValue
    val id: UUID? = null,

    @Column(name = "user_id", nullable = false)
    var userId: UUID = UUID.randomUUID(),

    @Column(name = "categoria_id", nullable = false)
    var categoriaId: UUID = UUID.randomUUID(),

    @Column(nullable = false)
    var anio: Int = 0,

    @Column(nullable = false)
    var mes: Int = 0,

    /** Pesos enteros (COP sin decimales). */
    @Column(nullable = false)
    var proyectado: Long = 0,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()
)
