package com.perseo.finance.expenses.entity

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

/** Categoría de factura editable por el usuario (columna D de la hoja Config). */
@Entity
@Table(name = "expenses_bill_categories")
class ExpenseBillCategoryEntity(
    @Id
    @GeneratedValue
    val id: UUID? = null,

    @Column(name = "user_id", nullable = false)
    var userId: UUID = UUID.randomUUID(),

    @Column(nullable = false, length = 60)
    var nombre: String = "",

    @Column(nullable = false)
    var activa: Boolean = true,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()
)
