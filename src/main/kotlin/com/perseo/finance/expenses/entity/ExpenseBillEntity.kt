package com.perseo.finance.expenses.entity

import jakarta.persistence.*
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

/** Factura de un mes (bloque G-K del Excel). null = celda vacía. */
@Entity
@Table(name = "expenses_bills")
class ExpenseBillEntity(
    @Id
    @GeneratedValue
    val id: UUID? = null,

    @Column(name = "user_id", nullable = false)
    var userId: UUID = UUID.randomUUID(),

    @Column(name = "categoria_id", nullable = false)
    var categoriaId: UUID = UUID.randomUUID(),

    /** Fecha dentro del mes (selector de fecha en la UI; antes: día de pago). */
    @Column
    var fecha: LocalDate? = null,

    @Column
    var valor: Long? = null,

    @Column(nullable = false)
    var pagado: Boolean = false,

    @Column(name = "valor_pagado")
    var valorPagado: Long? = null,

    @Column(nullable = false)
    var anio: Int = 0,

    @Column(nullable = false)
    var mes: Int = 0,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()
)
