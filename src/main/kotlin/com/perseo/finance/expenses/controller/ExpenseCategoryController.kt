package com.perseo.finance.expenses.controller

import com.perseo.finance.expenses.dto.ExpenseCategoryRequest
import com.perseo.finance.expenses.dto.ExpenseCategoryResponse
import com.perseo.finance.expenses.dto.UpdateExpenseCategoryRequest
import com.perseo.finance.expenses.service.ExpenseCategoryService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
class ExpenseCategoryController(
    private val categoryService: ExpenseCategoryService
) {

    @GetMapping("/api/expenses/categories")
    fun listar(@RequestParam userId: UUID): List<ExpenseCategoryResponse> =
        categoryService.listar(userId)

    @PostMapping("/api/expenses/categories")
    @ResponseStatus(HttpStatus.CREATED)
    fun crear(
        @RequestParam userId: UUID,
        @Valid @RequestBody request: ExpenseCategoryRequest
    ): ExpenseCategoryResponse =
        categoryService.crear(userId, request)

    @PutMapping("/api/expenses/categories/{id}")
    fun actualizar(
        @PathVariable id: UUID,
        @RequestParam userId: UUID,
        @Valid @RequestBody request: UpdateExpenseCategoryRequest
    ): ExpenseCategoryResponse =
        categoryService.actualizar(id, userId, request)

    @DeleteMapping("/api/expenses/categories/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun eliminar(@PathVariable id: UUID, @RequestParam userId: UUID) =
        categoryService.eliminar(id, userId)
}
