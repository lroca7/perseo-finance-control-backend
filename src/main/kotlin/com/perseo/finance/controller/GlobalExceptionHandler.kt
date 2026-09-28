package com.perseo.finance.controller

import com.perseo.finance.service.DuplicateResourceException
import com.perseo.finance.service.ResourceNotFoundException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

data class FieldErrorItem(val field: String, val message: String)
data class ErrorResponse(val errors: List<FieldErrorItem>)

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(ex: MethodArgumentNotValidException): ResponseEntity<ErrorResponse> {
        val errores = ex.bindingResult.fieldErrors.map {
            FieldErrorItem(it.field, it.defaultMessage ?: "Valor inválido")
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ErrorResponse(errores))
    }

    @ExceptionHandler(ResourceNotFoundException::class)
    fun handleNotFound(ex: ResourceNotFoundException): ResponseEntity<ErrorResponse> =
        ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(ErrorResponse(listOf(FieldErrorItem("id", ex.message ?: "No encontrado"))))

    @ExceptionHandler(DuplicateResourceException::class)
    fun handleDuplicate(ex: DuplicateResourceException): ResponseEntity<ErrorResponse> =
        ResponseEntity.status(HttpStatus.CONFLICT)
            .body(ErrorResponse(listOf(FieldErrorItem("periodoFin", ex.message ?: "Ya existe"))))
}
