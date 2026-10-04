package com.perseo.finance.common

import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.server.ResponseStatusException

/**
 * Los errores de negocio (notFound / badRequest / conflict de ApiErrors.kt) se lanzan como
 * ResponseStatusException. Por defecto Spring oculta su mensaje; aquí se devuelve con la misma
 * forma que usan las validaciones, { "errors": [{ "field": "", "message": "..." }] }, que es la
 * que el cliente del frontend ya sabe leer. Va primero en el orden para ganarle a un handler genérico.
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
class ResponseStatusAdvice {

    @ExceptionHandler(ResponseStatusException::class)
    fun handle(ex: ResponseStatusException): ResponseEntity<Map<String, Any>> {
        val message = ex.reason ?: ex.statusCode.toString()
        val body = mapOf("errors" to listOf(mapOf("field" to "", "message" to message)))
        return ResponseEntity.status(ex.statusCode).body(body)
    }
}
