package com.perseo.finance.domain

import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * Motor de simulación de amortización — HU-T.5.
 *
 * Sin dependencias de Spring/JPA a propósito: debe poder testearse
 * de forma completamente aislada y reutilizarse desde cualquier capa.
 */
object AmortizationEngine {

    private const val DEFAULT_MAX_MONTHS = 600

    /** Tasa efectiva mensual a partir de la tasa efectiva anual (%). */
    fun tasaMensual(tasaEA: Double): Double =
        (1.0 + tasaEA / 100.0).pow(1.0 / 12.0) - 1.0

    /**
     * Cuota de una anualidad (cuota total constante) para un saldo P,
     * tasa mensual r y n cuotas. Usado solo para AmortizationType.CUOTA_FIJA.
     */
    fun cuotaAnualidad(saldo: Double, tasaMensual: Double, cuotas: Int): Double =
        if (tasaMensual == 0.0) saldo / cuotas
        else saldo * tasaMensual / (1 - (1 + tasaMensual).pow(-cuotas.toDouble()))

    /**
     * Simula una línea de crédito completa hasta que el saldo llega a 0
     * (o hasta [maxMonths] como límite de seguridad).
     *
     * @param saldo saldo pendiente actual.
     * @param cuotasRestantes cuotas que le quedan a la línea (define el capital fijo si aplica).
     * @param tasaEA tasa efectiva anual, en porcentaje (ej. 28.77).
     * @param tipo mecanismo de amortización (ver [AmortizationType]).
     * @param abonos mapa de "# de cuota" -> monto extra abonado ese mes (puede estar vacío).
     * @param estrategia qué hacer con el resto del plazo/cuota tras un abono extra.
     */
    fun simular(
        saldo: Double,
        cuotasRestantes: Int,
        tasaEA: Double,
        tipo: AmortizationType,
        abonos: Map<Int, Double> = emptyMap(),
        estrategia: PaymentStrategy = PaymentStrategy.REDUCIR_PLAZO,
        maxMonths: Int = DEFAULT_MAX_MONTHS
    ): SimulationResult {
        val r = tasaMensual(tasaEA)
        var balance = saldo

        // Parámetro variable según el tipo: capital fijo (lineal) o cuota fija (anualidad).
        var capitalFijo = saldo / cuotasRestantes
        var cuotaFija = cuotaAnualidad(saldo, r, cuotasRestantes)

        val filas = mutableListOf<ScheduleRow>()
        var mes = 0
        var totalInteres = 0.0
        var totalCapital = 0.0

        while (balance > 1.0 && mes < maxMonths) {
            mes++
            val interes = balance * r
            val extra = abonos[mes] ?: 0.0

            val capitalCalculado = when (tipo) {
                AmortizationType.CAPITAL_FIJO -> min(capitalFijo + extra, balance)
                AmortizationType.CUOTA_FIJA -> {
                    val principalBase = max(cuotaFija - interes, 0.0)
                    min(principalBase + extra, balance)
                }
            }

            balance -= capitalCalculado
            totalInteres += interes
            totalCapital += capitalCalculado

            filas.add(
                ScheduleRow(
                    numeroCuota = mes,
                    interes = interes,
                    capital = capitalCalculado,
                    cuotaTotal = interes + capitalCalculado,
                    saldoFinal = balance,
                    abonoExtra = extra
                )
            )

            // Si hubo abono extra y la estrategia es "reducir cuota", se recalcula
            // el parámetro fijo para el resto del plazo original.
            if (extra > 0.0 && estrategia == PaymentStrategy.REDUCIR_CUOTA && balance > 1.0) {
                val restante = cuotasRestantes - mes
                if (restante > 0) {
                    when (tipo) {
                        AmortizationType.CAPITAL_FIJO -> capitalFijo = balance / restante
                        AmortizationType.CUOTA_FIJA -> cuotaFija = cuotaAnualidad(balance, r, restante)
                    }
                }
            }
        }

        return SimulationResult(
            filas = filas,
            meses = mes,
            totalInteres = totalInteres,
            totalCapital = totalCapital,
            totalPagado = totalInteres + totalCapital,
            nuncaSePagaEnPlazo = mes >= maxMonths
        )
    }

    /**
     * Compara el escenario sin abonos contra el escenario con los abonos
     * indicados, y calcula el ahorro. Es lo que expone el endpoint de
     * simulación (HU-4.2 / HU-T.4).
     */
    fun comparar(
        saldo: Double,
        cuotasRestantes: Int,
        tasaEA: Double,
        tipo: AmortizationType,
        abonos: Map<Int, Double>,
        estrategia: PaymentStrategy
    ): ComparisonResult {
        val sinAbonos = simular(saldo, cuotasRestantes, tasaEA, tipo)
        val conAbonos = if (abonos.isEmpty()) {
            sinAbonos
        } else {
            simular(saldo, cuotasRestantes, tasaEA, tipo, abonos, estrategia)
        }

        val ahorroIntereses = if (sinAbonos.nuncaSePagaEnPlazo || conAbonos.nuncaSePagaEnPlazo) {
            0.0
        } else {
            max(sinAbonos.totalInteres - conAbonos.totalInteres, 0.0)
        }
        val mesesAhorrados = if (sinAbonos.nuncaSePagaEnPlazo || conAbonos.nuncaSePagaEnPlazo) {
            0
        } else {
            max(sinAbonos.meses - conAbonos.meses, 0)
        }

        return ComparisonResult(
            sinAbonos = sinAbonos,
            conAbonos = conAbonos,
            ahorroIntereses = ahorroIntereses,
            mesesAhorrados = mesesAhorrados
        )
    }
}
