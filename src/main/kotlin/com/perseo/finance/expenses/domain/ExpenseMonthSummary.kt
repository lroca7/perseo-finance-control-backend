package com.perseo.finance.expenses.domain

import java.util.UUID

/**
 * Cálculo puro del resumen del mes (equivalente al SUMIF por categoría del Excel).
 * Sin Spring ni JPA: se prueba con [ExpenseMonthSummaryTest].
 */
object ExpenseMonthSummary {

    data class CategoriaInsumo(
        val categoriaId: UUID,
        val nombre: String,
        val activa: Boolean,
        /** null = sin proyectado este mes. */
        val proyectado: Long?
    )

    data class GastoInsumo(
        val categoriaId: UUID,
        val valor: Long
    )

    data class FilaResumen(
        val categoriaId: UUID,
        val nombre: String,
        val activa: Boolean,
        val proyectado: Long?,
        val real: Long,
        /** null cuando no hay proyectado. */
        val diferencia: Long?,
        val excedida: Boolean
    )

    data class Resumen(
        val filas: List<FilaResumen>,
        val totalProyectado: Long,
        val totalReal: Long
    )

    fun resumir(categorias: List<CategoriaInsumo>, gastos: List<GastoInsumo>): Resumen {
        val realPorCategoria = gastos.groupingBy { it.categoriaId }.fold(0L) { acc, g -> acc + g.valor }
        val filas = categorias.map { c ->
            val real = realPorCategoria[c.categoriaId] ?: 0L
            FilaResumen(
                categoriaId = c.categoriaId,
                nombre = c.nombre,
                activa = c.activa,
                proyectado = c.proyectado,
                real = real,
                diferencia = c.proyectado?.minus(real),
                excedida = c.proyectado != null && real > c.proyectado
            )
        }
        return Resumen(
            filas = filas,
            totalProyectado = categorias.sumOf { it.proyectado ?: 0L },
            totalReal = gastos.sumOf { it.valor }
        )
    }

    /** Normaliza un nombre para comparar categorías (ignora mayúsculas, tildes y espacios extra). */
    fun normalizarNombre(nombre: String): String =
        java.text.Normalizer.normalize(nombre.trim().lowercase(), java.text.Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")
            .replace(Regex("\\s+"), " ")
}
