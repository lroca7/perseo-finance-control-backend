package com.perseo.finance.pdf

import java.math.BigDecimal
import java.time.LocalDate

/**
 * Candidato a línea de crédito detectado en el PDF (ej. un diferido tipo
 * REDIFE). Es solo una propuesta: el usuario decide si lo importa o no
 * (HU-2.2, HU-3.2 — no se auto-crea nada).
 */
data class ParsedCreditLineCandidate(
    val nombre: String,
    val cuotaActual: Int,
    val cuotasTotales: Int,
    val valorTransaccionOriginal: BigDecimal?,
    val valorAPagarEstaCuota: BigDecimal?,
    val saldoPendiente: BigDecimal?,
    val tasaEA: BigDecimal?
)

/**
 * Resultado de intentar leer un extracto en PDF. Ningún campo se asume:
 * si el parser no lo encontró, queda null y se reporta en [camposFaltantes]
 * para que el frontend se lo pida al usuario en vez de mandar un valor
 * inventado (criterio de aceptación de HU-2.2).
 */
data class ParsedStatementResponse(
    val periodoInicio: LocalDate?,
    val periodoFin: LocalDate?,
    val saldoTotal: BigDecimal?,
    val pagoMinimo: BigDecimal?,
    val fechaLimitePago: LocalDate?,
    val cuotaManejo: BigDecimal?,
    val otrosCargos: BigDecimal?,
    val cupoTotal: BigDecimal?,
    val candidatosLineasCredito: List<ParsedCreditLineCandidate>,
    val camposFaltantes: List<String>,
    /**
     * Texto crudo extraído del PDF (recortado a ~12.000 caracteres), solo
     * para diagnóstico mientras se ajustan las expresiones regulares a
     * distintos layouts. En extractos de varias páginas, la tabla de
     * movimientos suele estar más adelante — por eso el límite es generoso.
     * No pensado para mostrarse como funcionalidad final al usuario.
     */
    val textoDebug: String
)
