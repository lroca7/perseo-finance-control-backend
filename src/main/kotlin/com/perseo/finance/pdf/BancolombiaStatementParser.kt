package com.perseo.finance.pdf

import java.math.BigDecimal
import java.time.LocalDate

/**
 * Parser específico para el layout de extracto de Bancolombia (HU-2.2).
 *
 * Diferencias clave frente a Davivienda que obligan a un parser aparte:
 * - Notación numérica invertida: "." como separador de miles, "," como
 *   separador decimal (ej. "316.124,00" = 316124.00), al revés de Davivienda.
 * - Fechas en formato "17 ago - 15 sep. 2026" y "oct. 05, 2026" (mes en
 *   texto, no numérico como en Davivienda).
 * - Notación de cuotas "12/24" en vez de "2 de 60".
 * - No separa "cuota de manejo" como concepto propio: viene mezclado dentro
 *   de "Otros Cargos" (comisión, cuota de manejo, CMF, cobros de remplazo).
 *   Por eso aquí cuotaManejo siempre queda en 0 y el valor real cae en
 *   otrosCargos — no es un campo faltante, es una decisión del banco.
 */
object BancolombiaStatementParser : StatementParser {

    private val mesesEs = mapOf(
        "ene" to 1, "feb" to 2, "mar" to 3, "abr" to 4, "may" to 5, "jun" to 6,
        "jul" to 7, "ago" to 8, "sep" to 9, "oct" to 10, "nov" to 11, "dic" to 12
    )

    /** "316.124,00" -> 316124.00 (punto de miles, coma decimal). */
    private fun parseMoney(raw: String?): BigDecimal? {
        if (raw == null) return null
        val limpio = raw.trim().replace(".", "").replace(",", ".")
        return limpio.toBigDecimalOrNull()
    }

    private fun mes(abbr: String): Int? = mesesEs[abbr.lowercase().take(3)]

    override fun parse(text: String): ParsedStatementResponse {
        val faltantes = mutableListOf<String>()

        // "Periodo facturado 17 ago - 15 sep. 2026"
        val periodo = Regex(
            """Periodo facturado\s*(\d{1,2})\s*(\w{3})\.?\s*-\s*(\d{1,2})\s*(\w{3})\.?\s*(\d{4})"""
        ).find(text)
        val periodoInicio = periodo?.let { m ->
            val (d1, mes1, _, _, anio) = m.destructured
            mes(mes1)?.let { runCatching { LocalDate.of(anio.toInt(), it, d1.toInt()) }.getOrNull() }
        }
        val periodoFin = periodo?.let { m ->
            val (_, _, d2, mes2, anio) = m.destructured
            mes(mes2)?.let { runCatching { LocalDate.of(anio.toInt(), it, d2.toInt()) }.getOrNull() }
        }
        if (periodoInicio == null) faltantes += "periodoInicio"
        if (periodoFin == null) faltantes += "periodoFin"

        // "Pagar antes de: oct. 05, 2026"
        val fechaLimite = Regex(
            """Pagar antes de:\s*(\w{3})\.?\s*(\d{1,2}),\s*(\d{4})"""
        ).find(text)?.let { m ->
            val (mesTxt, dia, anio) = m.destructured
            mes(mesTxt)?.let { runCatching { LocalDate.of(anio.toInt(), it, dia.toInt()) }.getOrNull() }
        }
        if (fechaLimite == null) faltantes += "fechaLimitePago"

        val pagoMinimo = parseMoney(
            Regex("""Pago m[ií]nimo:?\s*\$?\s*([\d.,]+)""").find(text)?.groupValues?.getOrNull(1)
        )
        if (pagoMinimo == null) faltantes += "pagoMinimo"

        val pagoTotal = parseMoney(
            Regex("""Pago Total:?\s*\$?\s*([\d.,]+)""").find(text)?.groupValues?.getOrNull(1)
        )
        if (pagoTotal == null) faltantes += "saldoTotal"

        val cupoTotal = parseMoney(
            Regex("""Cupo total:?\s*\$?\s*([\d.,]+)""").find(text)?.groupValues?.getOrNull(1)
        )
        if (cupoTotal == null) faltantes += "cupoTotal"

        // Bancolombia no separa cuota de manejo: viene dentro de "Otros Cargos".
        val otrosCargos = parseMoney(
            Regex("""Otros Cargos\s*\$?\s*([\d.,]+)""").find(text)?.groupValues?.getOrNull(1)
        )

        val candidatos = extraerCandidatosLineasCredito(text)

        return ParsedStatementResponse(
            periodoInicio = periodoInicio,
            periodoFin = periodoFin,
            saldoTotal = pagoTotal,
            pagoMinimo = pagoMinimo,
            fechaLimitePago = fechaLimite,
            cuotaManejo = BigDecimal.ZERO,
            otrosCargos = otrosCargos,
            cupoTotal = cupoTotal,
            candidatosLineasCredito = candidatos,
            camposFaltantes = faltantes,
            textoDebug = text.take(12000)
        )
    }

    /**
     * Filas de movimientos con forma real (verificada con texto extraído):
     * "NOMBRE" queda pegado, sin espacio, justo antes del número de
     * autorización — ej. "AVANCE SUCURSAL VIRTUAL329792 10/10/2025 $ ...".
     * Luego: fecha, valor, cuotaActual/cuotasTotales, valorCuota,
     * interesMensual%, interesAnual%, saldoPendiente.
     * Solo candidatas las de más de 1 cuota total y saldo pendiente > 0
     * (mismo criterio que Davivienda, HU-3.2).
     */
    private fun extraerCandidatosLineasCredito(text: String): List<ParsedCreditLineCandidate> {
        val fila = Regex(
            """([A-ZÁÉÍÓÚÑ][A-Za-zÁÉÍÓÚÑ ]{2,40}?)(\d+)\s+(\d{2}/\d{2}/\d{4})\s+\$?\s*(-?[\d.,]+)\s+(\d+)/(\d+)\s+\$?\s*(-?[\d.,]+)\s+([\d.,]+)\s*%\s+([\d.,]+)\s*%\s+\$?\s*([\d.,]+)"""
        )

        return fila.findAll(text).mapNotNull { m ->
            val (nombre, _numAutorizacion, _fecha, valorMovimiento, cuotaActual, cuotasTotales, valorCuota, _interesMensual, interesAnual, saldoPendiente) =
                m.destructured

            val totales = cuotasTotales.toIntOrNull() ?: return@mapNotNull null
            val saldo = parseMoney(saldoPendiente) ?: BigDecimal.ZERO

            if (totales <= 1 || saldo <= BigDecimal.ZERO) return@mapNotNull null

            ParsedCreditLineCandidate(
                nombre = nombre.trim(),
                cuotaActual = cuotaActual.toIntOrNull() ?: 1,
                cuotasTotales = totales,
                valorTransaccionOriginal = parseMoney(valorMovimiento),
                valorAPagarEstaCuota = parseMoney(valorCuota),
                saldoPendiente = saldo,
                tasaEA = parseMoney(interesAnual)
            )
        }.distinctBy { it.nombre to it.saldoPendiente }.toList()
    }
}
