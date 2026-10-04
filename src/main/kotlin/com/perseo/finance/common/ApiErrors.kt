package com.perseo.finance.common

import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException

/*
 * Errores HTTP compartidos por todos los módulos. Si tu GlobalExceptionHandler ya mapea
 * una excepción propia para "no encontrado", cambia SOLO estas tres funciones y todos
 * los módulos que las usan se adaptan.
 */

fun notFound(message: String): Nothing =
    throw ResponseStatusException(HttpStatus.NOT_FOUND, message)

fun badRequest(message: String): Nothing =
    throw ResponseStatusException(HttpStatus.BAD_REQUEST, message)

fun conflict(message: String): Nothing =
    throw ResponseStatusException(HttpStatus.CONFLICT, message)
