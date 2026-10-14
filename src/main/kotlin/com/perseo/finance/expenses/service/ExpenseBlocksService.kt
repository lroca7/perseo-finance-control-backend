package com.perseo.finance.expenses.service

import com.perseo.finance.common.badRequest
import com.perseo.finance.common.conflict
import com.perseo.finance.common.notFound
import com.perseo.finance.expenses.domain.ExpenseMonthSummary
import com.perseo.finance.expenses.dto.BillItemRequest
import com.perseo.finance.expenses.dto.BillRow
import com.perseo.finance.expenses.dto.BillsBlock
import com.perseo.finance.expenses.dto.ImportBillItem
import com.perseo.finance.expenses.dto.ImportMonthDebtItem
import com.perseo.finance.expenses.dto.ImportMonthSavingItem
import com.perseo.finance.expenses.dto.ImportIncomeItem
import com.perseo.finance.expenses.dto.IncomeItemRequest
import com.perseo.finance.expenses.dto.IncomeRow
import com.perseo.finance.expenses.dto.IncomesBlock
import com.perseo.finance.expenses.dto.MonthDebtItemRequest
import com.perseo.finance.expenses.dto.MonthDebtRow
import com.perseo.finance.expenses.dto.MonthDebtsBlock
import com.perseo.finance.expenses.dto.MonthSavingItemRequest
import com.perseo.finance.expenses.dto.MonthSavingRow
import com.perseo.finance.expenses.dto.MonthSavingsBlock
import com.perseo.finance.expenses.entity.ExpenseBillCategoryEntity
import com.perseo.finance.expenses.entity.ExpenseBillEntity
import com.perseo.finance.expenses.entity.ExpenseIncomeEntity
import com.perseo.finance.expenses.entity.ExpenseMonthDebtEntity
import com.perseo.finance.expenses.entity.ExpenseMonthSavingEntity
import com.perseo.finance.expenses.repository.ExpenseBillCategoryRepository
import com.perseo.finance.expenses.repository.ExpenseBillRepository
import com.perseo.finance.expenses.repository.ExpenseIncomeRepository
import com.perseo.finance.expenses.repository.ExpenseMonthDebtRepository
import com.perseo.finance.expenses.repository.ExpenseMonthSavingRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

/**
 * Bloques mensuales de ingresos, facturas, ahorro y deudas (HU-G.3).
 * Cada PUT reemplaza el bloque completo del mes (como editar la hoja del Excel).
 */
@Service
class ExpenseBlocksService(
    private val incomeRepository: ExpenseIncomeRepository,
    private val billRepository: ExpenseBillRepository,
    private val billCategoryRepository: ExpenseBillCategoryRepository,
    private val savingRepository: ExpenseMonthSavingRepository,
    private val debtRepository: ExpenseMonthDebtRepository
) {

    data class Bloques(
        val ingresos: IncomesBlock,
        val facturas: BillsBlock,
        val ahorro: MonthSavingsBlock,
        val deudas: MonthDebtsBlock,
        val sugerenciasIngresos: List<String>,
        val sugerenciasAhorro: List<String>,
        val sugerenciasDeudas: List<String>
    )

    @Transactional(readOnly = true)
    fun leer(userId: UUID, anio: Int, mes: Int): Bloques {
        val ingresos = incomeRepository.findByUserIdAndAnioAndMesOrderByCategoriaAsc(userId, anio, mes)
        val facturas = billRepository.findByUserIdAndAnioAndMes(userId, anio, mes)
        val nombresBill = billCategoryRepository.findByUserIdOrderByNombreAsc(userId)
            .associate { it.id!! to it }
        val ahorros = savingRepository.findByUserIdAndAnioAndMesOrderByNombreAsc(userId, anio, mes)
        val deudas = debtRepository.findByUserIdAndAnioAndMesOrderByNombreAsc(userId, anio, mes)

        return Bloques(
            ingresos = IncomesBlock(
                filas = ingresos.map {
                    IncomeRow(it.id!!, it.categoria, it.fecha, it.proyectado, it.recibido)
                },
                totalProyectado = ingresos.sumOf { it.proyectado ?: 0L },
                totalRecibido = ingresos.sumOf { it.recibido ?: 0L }
            ),
            facturas = BillsBlock(
                filas = facturas.map {
                    val cat = nombresBill[it.categoriaId]
                    BillRow(
                        it.id!!, it.categoriaId, cat?.nombre ?: "(eliminada)",
                        cat?.activa ?: false, it.fecha, it.valor, it.pagado, it.valorPagado
                    )
                },
                totalValor = facturas.sumOf { it.valor ?: 0L },
                totalPagado = facturas.sumOf { it.valorPagado ?: 0L }
            ),
            ahorro = MonthSavingsBlock(
                filas = ahorros.map { MonthSavingRow(it.id!!, it.nombre, it.meta, it.ahorrado) },
                totalMeta = ahorros.sumOf { it.meta ?: 0L },
                totalAhorrado = ahorros.sumOf { it.ahorrado ?: 0L }
            ),
            deudas = MonthDebtsBlock(
                filas = deudas.map { MonthDebtRow(it.id!!, it.nombre, it.pendiente, it.pagado) },
                totalPendiente = deudas.sumOf { it.pendiente ?: 0L },
                totalPagado = deudas.sumOf { it.pagado ?: 0L }
            ),
            sugerenciasIngresos = sugerencias(
                incomeRepository.findByUserIdOrderByAnioDescMesDesc(userId)
                    .filter { it.anio * 12 + it.mes < anio * 12 + mes }.map { it.categoria }
            ),
            sugerenciasAhorro = sugerencias(
                savingRepository.findByUserIdOrderByAnioDescMesDesc(userId)
                    .filter { it.anio * 12 + it.mes < anio * 12 + mes }.map { it.nombre }
            ),
            sugerenciasDeudas = sugerencias(
                debtRepository.findByUserIdOrderByAnioDescMesDesc(userId)
                    .filter { it.anio * 12 + it.mes < anio * 12 + mes }.map { it.nombre }
            )
        )
    }

    // ---------------- Guardado (reemplazo completo del mes) ----------------

    @Transactional
    fun guardarIngresos(userId: UUID, anio: Int, mes: Int, items: List<IncomeItemRequest>) {
        verificarNombresUnicos(items.map { it.categoria }, "ingreso")
        items.forEach { validarFechaEnMes(it.fecha, anio, mes, "ingreso") }
        incomeRepository.deleteAll(
            incomeRepository.findByUserIdAndAnioAndMesOrderByCategoriaAsc(userId, anio, mes)
        )
        items.forEach {
            incomeRepository.save(
                ExpenseIncomeEntity(
                    userId = userId, categoria = it.categoria.trim(), fecha = it.fecha,
                    proyectado = it.proyectado, recibido = it.recibido, anio = anio, mes = mes
                )
            )
        }
    }

    @Transactional
    fun guardarFacturas(userId: UUID, anio: Int, mes: Int, items: List<BillItemRequest>) {
        if (items.map { it.categoriaId }.toSet().size != items.size) {
            conflict("Hay una factura repetida en el mes.")
        }
        items.forEach { validarFechaEnMes(it.fecha, anio, mes, "factura") }
        val categorias = billCategoryRepository.findByUserIdOrderByNombreAsc(userId)
            .associateBy { it.id }
        items.forEach {
            val cat = categorias[it.categoriaId] ?: notFound("Categoría de factura no encontrada.")
            if (!cat.activa) badRequest("La categoría '${cat.nombre}' está inactiva.")
        }
        billRepository.deleteAll(billRepository.findByUserIdAndAnioAndMes(userId, anio, mes))
        items.forEach {
            billRepository.save(
                ExpenseBillEntity(
                    userId = userId, categoriaId = it.categoriaId, fecha = it.fecha,
                    valor = it.valor, pagado = it.pagado, valorPagado = it.valorPagado,
                    anio = anio, mes = mes
                )
            )
        }
    }

    @Transactional
    fun guardarAhorros(userId: UUID, anio: Int, mes: Int, items: List<MonthSavingItemRequest>) {
        verificarNombresUnicos(items.map { it.nombre }, "ahorro")
        savingRepository.deleteAll(
            savingRepository.findByUserIdAndAnioAndMesOrderByNombreAsc(userId, anio, mes)
        )
        items.forEach {
            savingRepository.save(
                ExpenseMonthSavingEntity(
                    userId = userId, nombre = it.nombre.trim(),
                    meta = it.meta, ahorrado = it.ahorrado, anio = anio, mes = mes
                )
            )
        }
    }

    @Transactional
    fun guardarDeudas(userId: UUID, anio: Int, mes: Int, items: List<MonthDebtItemRequest>) {
        verificarNombresUnicos(items.map { it.nombre }, "deuda")
        debtRepository.deleteAll(
            debtRepository.findByUserIdAndAnioAndMesOrderByNombreAsc(userId, anio, mes)
        )
        items.forEach {
            debtRepository.save(
                ExpenseMonthDebtEntity(
                    userId = userId, nombre = it.nombre.trim(),
                    pendiente = it.pendiente, pagado = it.pagado, anio = anio, mes = mes
                )
            )
        }
    }

    // ---------------- Importación ----------------

    /** Falla con 409 si algún bloque del mes ya tiene datos y no se fuerza (verificación previa). */
    @Transactional(readOnly = true)
    fun verificarImportacion(userId: UUID, anio: Int, mes: Int, forzar: Boolean) {
        if (forzar) return
        val hayDatos = incomeRepository.findByUserIdAndAnioAndMesOrderByCategoriaAsc(userId, anio, mes).isNotEmpty() ||
            billRepository.findByUserIdAndAnioAndMes(userId, anio, mes).isNotEmpty() ||
            savingRepository.findByUserIdAndAnioAndMesOrderByNombreAsc(userId, anio, mes).isNotEmpty() ||
            debtRepository.findByUserIdAndAnioAndMesOrderByNombreAsc(userId, anio, mes).isNotEmpty()
        if (hayDatos) conflict("El mes ya tiene ingresos, facturas, ahorros o deudas. Reintenta con forzar=true para reemplazarlos.")
    }

    @Transactional
    fun importarBloques(
        userId: UUID,
        anio: Int,
        mes: Int,
        ingresos: List<ImportIncomeItem>,
        facturas: List<ImportBillItem>,
        ahorros: List<ImportMonthSavingItem>,
        deudas: List<ImportMonthDebtItem>
    ): Map<String, Int> {
        guardarIngresos(userId, anio, mes, ingresos.map {
            IncomeItemRequest(it.categoria, it.fecha, it.proyectado, it.recibido)
        })
        val porNombre = billCategoryRepository.findByUserIdOrderByNombreAsc(userId)
            .associateBy { ExpenseMonthSummary.normalizarNombre(it.nombre) }.toMutableMap()
        guardarFacturas(userId, anio, mes, facturas.map {
            val clave = ExpenseMonthSummary.normalizarNombre(it.categoria)
            val cat = porNombre.getOrPut(clave) {
                billCategoryRepository.save(ExpenseBillCategoryEntity(userId = userId, nombre = it.categoria.trim()))
            }
            if (!cat.activa) badRequest("La categoría '${cat.nombre}' está inactiva.")
            BillItemRequest(cat.id!!, it.fecha, it.valor, it.pagado, it.valorPagado)
        })
        guardarAhorros(userId, anio, mes, ahorros.map {
            MonthSavingItemRequest(it.nombre, it.meta, it.ahorrado)
        })
        guardarDeudas(userId, anio, mes, deudas.map {
            MonthDebtItemRequest(it.nombre, it.pendiente, it.pagado)
        })
        return mapOf(
            "ingresos" to ingresos.size, "facturas" to facturas.size,
            "ahorros" to ahorros.size, "deudas" to deudas.size
        )
    }

    // ---------------- Validaciones ----------------

    private fun verificarNombresUnicos(nombres: List<String>, que: String) {
        val normalizados = nombres.map { ExpenseMonthSummary.normalizarNombre(it) }
        if (normalizados.toSet().size != normalizados.size) {
            conflict("Hay un $que repetido en el mes.")
        }
    }

    private fun validarFechaEnMes(fecha: LocalDate?, anio: Int, mes: Int, que: String) {
        if (fecha != null && (fecha.year != anio || fecha.monthValue != mes)) {
            badRequest("La fecha del $que debe estar dentro del mes $anio-$mes.")
        }
    }

    private fun sugerencias(nombres: List<String>): List<String> {
        // Nombres usados en meses anteriores, sin repetir, los más recientes primero.
        return nombres.distinct().take(20)
    }
}
