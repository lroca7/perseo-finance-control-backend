package com.perseo.finance.expenses.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID

class ExpenseMonthSummaryTest {

    private val domicilioId = UUID.randomUUID()
    private val comprasId = UUID.randomUUID()

    @Test
    fun `suma el real por categoria y marca excedidas`() {
        val resumen = ExpenseMonthSummary.resumir(
            listOf(
                ExpenseMonthSummary.CategoriaInsumo(domicilioId, "Domicilios", true, 100000),
                ExpenseMonthSummary.CategoriaInsumo(comprasId, "Compras", true, 150000)
            ),
            listOf(
                ExpenseMonthSummary.GastoInsumo(domicilioId, 91000),
                ExpenseMonthSummary.GastoInsumo(comprasId, 143000),
                ExpenseMonthSummary.GastoInsumo(comprasId, 62000)
            )
        )

        val domicilios = resumen.filas.first { it.categoriaId == domicilioId }
        assertEquals(91000L, domicilios.real)
        assertEquals(9000L, domicilios.diferencia)
        assertFalse(domicilios.excedida)

        val compras = resumen.filas.first { it.categoriaId == comprasId }
        assertEquals(205000L, compras.real)
        assertEquals(-55000L, compras.diferencia)
        assertTrue(compras.excedida)

        assertEquals(250000L, resumen.totalProyectado)
        assertEquals(296000L, resumen.totalReal)
    }

    @Test
    fun `sin proyectado la diferencia es null y no se marca excedida`() {
        val resumen = ExpenseMonthSummary.resumir(
            listOf(ExpenseMonthSummary.CategoriaInsumo(domicilioId, "Domicilios", true, null)),
            listOf(ExpenseMonthSummary.GastoInsumo(domicilioId, 50000))
        )

        val fila = resumen.filas.single()
        assertEquals(50000L, fila.real)
        assertNull(fila.diferencia)
        assertFalse(fila.excedida)
        assertEquals(0L, resumen.totalProyectado)
        assertEquals(50000L, resumen.totalReal)
    }

    @Test
    fun `normaliza nombres ignorando mayusculas tildes y espacios`() {
        assertEquals(
            ExpenseMonthSummary.normalizarNombre("Peluquería"),
            ExpenseMonthSummary.normalizarNombre("  PELUQUERIA ")
        )
        assertEquals(
            ExpenseMonthSummary.normalizarNombre("Aseo  Personal"),
            ExpenseMonthSummary.normalizarNombre("aseo personal")
        )
    }
}
