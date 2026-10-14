package com.perseo.finance.expenses.controller

import com.perseo.finance.expenses.dto.ExpenseMonthResponse
import com.perseo.finance.expenses.dto.ExpenseRequest
import com.perseo.finance.expenses.dto.ExpenseResponse
import com.perseo.finance.expenses.dto.ImportMonthRequest
import com.perseo.finance.expenses.dto.ImportMonthResponse
import com.perseo.finance.expenses.dto.UpdateBillsRequest
import com.perseo.finance.expenses.dto.UpdateBudgetsRequest
import com.perseo.finance.expenses.dto.UpdateIncomesRequest
import com.perseo.finance.expenses.dto.UpdateMonthDebtsRequest
import com.perseo.finance.expenses.dto.UpdateMonthSavingsRequest
import com.perseo.finance.expenses.service.ExpenseBlocksService
import com.perseo.finance.expenses.service.ExpenseMonthService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
class ExpenseMonthController(
    private val monthService: ExpenseMonthService,
    private val blocksService: ExpenseBlocksService
) {

    @GetMapping("/api/expenses/months/{anio}/{mes}")
    fun obtenerMes(
        @PathVariable anio: Int,
        @PathVariable mes: Int,
        @RequestParam userId: UUID
    ): ExpenseMonthResponse =
        monthService.obtenerMes(userId, anio, mes)

    @PutMapping("/api/expenses/months/{anio}/{mes}/budgets")
    fun guardarPresupuestos(
        @PathVariable anio: Int,
        @PathVariable mes: Int,
        @RequestParam userId: UUID,
        @Valid @RequestBody request: UpdateBudgetsRequest
    ): ExpenseMonthResponse =
        monthService.guardarPresupuestos(userId, anio, mes, request)

    @PostMapping("/api/expenses/months/{anio}/{mes}/expenses")
    @ResponseStatus(HttpStatus.CREATED)
    fun crearGasto(
        @PathVariable anio: Int,
        @PathVariable mes: Int,
        @RequestParam userId: UUID,
        @Valid @RequestBody request: ExpenseRequest
    ): ExpenseResponse =
        monthService.crearGasto(userId, request)

    @PutMapping("/api/expenses/expenses/{id}")
    fun actualizarGasto(
        @PathVariable id: UUID,
        @RequestParam userId: UUID,
        @Valid @RequestBody request: ExpenseRequest
    ): ExpenseResponse =
        monthService.actualizarGasto(id, userId, request)

    @DeleteMapping("/api/expenses/expenses/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun eliminarGasto(@PathVariable id: UUID, @RequestParam userId: UUID) =
        monthService.eliminarGasto(id, userId)

    @PostMapping("/api/expenses/months/{anio}/{mes}/import")
    @ResponseStatus(HttpStatus.CREATED)
    fun importarMes(
        @PathVariable anio: Int,
        @PathVariable mes: Int,
        @RequestParam userId: UUID,
        @RequestParam(defaultValue = "false") forzar: Boolean,
        @Valid @RequestBody request: ImportMonthRequest
    ): ImportMonthResponse =
        monthService.importarMes(userId, anio, mes, request, forzar)

    // ---- Bloques mensuales (HU-G.3): cada PUT reemplaza el bloque del mes ----

    @PutMapping("/api/expenses/months/{anio}/{mes}/incomes")
    fun guardarIngresos(
        @PathVariable anio: Int,
        @PathVariable mes: Int,
        @RequestParam userId: UUID,
        @Valid @RequestBody request: UpdateIncomesRequest
    ): ExpenseMonthResponse {
        blocksService.guardarIngresos(userId, anio, mes, request.ingresos)
        return monthService.obtenerMes(userId, anio, mes)
    }

    @PutMapping("/api/expenses/months/{anio}/{mes}/bills")
    fun guardarFacturas(
        @PathVariable anio: Int,
        @PathVariable mes: Int,
        @RequestParam userId: UUID,
        @Valid @RequestBody request: UpdateBillsRequest
    ): ExpenseMonthResponse {
        blocksService.guardarFacturas(userId, anio, mes, request.facturas)
        return monthService.obtenerMes(userId, anio, mes)
    }

    @PutMapping("/api/expenses/months/{anio}/{mes}/month-savings")
    fun guardarAhorros(
        @PathVariable anio: Int,
        @PathVariable mes: Int,
        @RequestParam userId: UUID,
        @Valid @RequestBody request: UpdateMonthSavingsRequest
    ): ExpenseMonthResponse {
        blocksService.guardarAhorros(userId, anio, mes, request.ahorros)
        return monthService.obtenerMes(userId, anio, mes)
    }

    @PutMapping("/api/expenses/months/{anio}/{mes}/month-debts")
    fun guardarDeudas(
        @PathVariable anio: Int,
        @PathVariable mes: Int,
        @RequestParam userId: UUID,
        @Valid @RequestBody request: UpdateMonthDebtsRequest
    ): ExpenseMonthResponse {
        blocksService.guardarDeudas(userId, anio, mes, request.deudas)
        return monthService.obtenerMes(userId, anio, mes)
    }
}
