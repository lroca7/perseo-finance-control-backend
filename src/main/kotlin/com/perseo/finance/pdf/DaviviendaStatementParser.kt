package com.perseo.finance.pdf

import java.math.BigDecimal
import java.time.LocalDate

/**
 * Parser específico para el layout de extracto de Davivienda (HU-2.2).
 *
 * Es deliberadamente conservador: si una expresión regular no encuentra el
 * dato, el campo queda en null en vez de adivinar. No hay "modo genérico"
 * para otros bancos todavía — cada banco compone su extracto distinto
 * (ver HU-F.5 en el backlog), así que este parser solo debe usarse como
 * punto de partida, con el usuario revisando el resultado antes de guardar.
 */
object DaviviendaStatementParser : StatementParser {

    private val mesesEs = mapOf(
        "Ene" to 1, "Feb" to 2, "Mar" to 3, "Abr" to 4, "May" to 5, "Jun" to 6,
        "Jul" to 7, "Ago" to 8, "Sep" to 9, "Oct" to 10, "Nov" to 11, "Dic" to 12
    )

    private fun parseMoney(raw: String?): BigDecimal? =
        raw?.replace(".", "")?.replace(",", "")?.trim()?.toBigDecimalOrNull()

    private fun parseFechaDdMmmAaaa(raw: String?): LocalDate? {
        if (raw == null) return null
        val m = Regex("""(\d{1,2})/(\w{3})/(\d{4})""").find(raw) ?: return null
        val (dia, mesTxt, anio) = m.destructured
        val mes = mesesEs[mesTxt.replaceFirstChar { it.uppercase() }] ?: return null
        return runCatching { LocalDate.of(anio.toInt(), mes, dia.toInt()) }.getOrNull()
    }

    /**
     * @param text texto plano ya extraído del PDF (ver [PdfTextExtractor]).
     */
    override fun parse(text: String): ParsedStatementResponse {
        val faltantes = mutableListOf<String>()

        val periodo = Regex(
            """Periodo de facturaci[oó]n:\s*(\d{1,2}/\w{3}/\d{4})\s*-\s*(\d{1,2}/\w{3}/\d{4})"""
        ).find(text)
        val periodoInicio = parseFechaDdMmmAaaa(periodo?.groupValues?.getOrNull(1))
        val periodoFin = parseFechaDdMmmAaaa(periodo?.groupValues?.getOrNull(2))
        if (periodoInicio == null) faltantes += "periodoInicio"
        if (periodoFin == null) faltantes += "periodoFin"

        val fechaLimite = parseFechaDdMmmAaaa(
            Regex("""Fecha l[ií]mite de pago\s*(\d{1,2}/\w{3}/\d{4})""").find(text)?.groupValues?.getOrNull(1)
        )
        if (fechaLimite == null) faltantes += "fechaLimitePago"

        val pagoMinimo = parseMoney(
            Regex("""Pago m[ií]nimo\s*\$?\s*([\d.,]+)""").find(text)?.groupValues?.getOrNull(1)
        )
        if (pagoMinimo == null) faltantes += "pagoMinimo"

        val pagoTotal = parseMoney(
            Regex("""Pago total\s*\$?\s*([\d.,]+)""").find(text)?.groupValues?.getOrNull(1)
        )
        if (pagoTotal == null) faltantes += "saldoTotal"

        val cuotaManejo = parseMoney(
            Regex("""Cuota de manejo\s*\$?\s*([\d.,]+)""").find(text)?.groupValues?.getOrNull(1)
        )
        if (cuotaManejo == null) faltantes += "cuotaManejo"

        val otrosCargos = parseMoney(
            Regex("""Otros cargos\s*\$?\s*([\d.,]+)""").find(text)?.groupValues?.getOrNull(1)
        )
        // otrosCargos es opcional (puede ser legítimamente $0), no se marca como faltante forzoso.

        val cupoTotal = parseMoney(
            Regex("""Cupo total\s*\$?\s*([\d.,]+)""").find(text)?.groupValues?.getOrNull(1)
        )
        if (cupoTotal == null) faltantes += "cupoTotal"

        val candidatos = extraerCandidatosLineasCredito(text)

        return ParsedStatementResponse(
            periodoInicio = periodoInicio,
            periodoFin = periodoFin,
            saldoTotal = pagoTotal,
            pagoMinimo = pagoMinimo,
            fechaLimitePago = fechaLimite,
            cuotaManejo = cuotaManejo,
            otrosCargos = otrosCargos,
            cupoTotal = cupoTotal,
            candidatosLineasCredito = candidatos,
            camposFaltantes = faltantes,
            textoDebug = text.take(12000)
        )
    }

    /**
     * Busca filas de la tabla de movimientos con forma:
     * "NOMBRE $valor N de M $intereses $valorAPagar $saldoPendiente lifemiles numTx tasa"
     * Solo se proponen como candidato las que tienen más de 1 cuota total
     * (HU-3.2: las compras a 1 sola cuota se ignoran) y saldo pendiente > 0
     * (se excluyen diferidos ya terminados).
     */
    private fun extraerCandidatosLineasCredito(text: String): List<ParsedCreditLineCandidate> {
        val fila = Regex(
            """([A-ZÁÉÍÓÚÑ][A-Za-zÁÉÍÓÚÑ0-9 \*.]{2,40}?)\s+\$?([\d,]+)\s+(\d+)\s+de\s+(\d+)\s+\$?([\d,]+)\s+\$?([\d,]+)\s+\$?([\d,]+)\s+\d+\s+\d+\s+([\d.]+)"""
        )

        return fila.findAll(text).mapNotNull { m ->
            val (nombre, valorTx, cuotaActual, cuotasTotales, _intereses, valorAPagar, saldoPendiente, tasa) =
                m.destructured

            val totales = cuotasTotales.toIntOrNull() ?: return@mapNotNull null
            val saldo = parseMoney(saldoPendiente) ?: BigDecimal.ZERO

            // Filtro: ignorar compras a 1 cuota y diferidos ya saldados.
            if (totales <= 1 || saldo <= BigDecimal.ZERO) return@mapNotNull null

            ParsedCreditLineCandidate(
                nombre = nombre.trim(),
                cuotaActual = cuotaActual.toIntOrNull() ?: 1,
                cuotasTotales = totales,
                valorTransaccionOriginal = parseMoney(valorTx),
                valorAPagarEstaCuota = parseMoney(valorAPagar),
                saldoPendiente = saldo,
                tasaEA = tasa.toBigDecimalOrNull()
            )
        }.distinctBy { it.nombre to it.saldoPendiente }.toList()
    }
}
