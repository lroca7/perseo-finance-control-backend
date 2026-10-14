package com.perseo.finance.expenses.entity

import jakarta.persistence.*
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

/** Ingreso de un mes (bloque B-E del Excel). null = celda vacía. */
@Entity
@Table(name = "expenses_incomes")
class ExpenseIncomeEntity(
    @Id
    @GeneratedValue
    val id: UUID? = null,

    @Column(name = "user_id", nullable = false)
    var userId: UUID = UUID.randomUUID(),

    @Column(nullable = false, length = 60)
    var categoria: String = "",

    /** Fecha dentro del mes (selector de fecha en la UI; antes: día de pago). */
    @Column
    var fecha: LocalDate? = null,

    @Column
    var proyectado: Long? = null,

    @Column
    var recibido: Long? = null,

    @Column(nullable = false)
    var anio: Int = 0,

    @Column(nullable = false)
    var mes: Int = 0,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()
)
