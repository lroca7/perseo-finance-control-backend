package com.perseo.finance.expenses.entity

import jakarta.persistence.*
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

/** Un gasto del registro diario (filas 56+ del Excel: fecha, valor, categoría, descripción). */
@Entity
@Table(name = "expenses_expenses")
class ExpenseEntity(
    @Id
    @GeneratedValue
    val id: UUID? = null,

    @Column(name = "user_id", nullable = false)
    var userId: UUID = UUID.randomUUID(),

    @Column(name = "categoria_id", nullable = false)
    var categoriaId: UUID = UUID.randomUUID(),

    @Column(nullable = false)
    var fecha: LocalDate = LocalDate.now(),

    /** Pesos enteros (COP sin decimales), siempre > 0. */
    @Column(nullable = false)
    var valor: Long = 0,

    @Column(nullable = false, length = 200)
    var descripcion: String = "",

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()
)
