package com.perseo.finance.domain

/**
 * Mecanismo de amortización de una línea de crédito.
 *
 * CAPITAL_FIJO: capital constante (saldo ÷ cuotas restantes), interés decreciente.
 *               Es el mecanismo real validado con el extracto (caso REDIFE).
 * CUOTA_FIJA:   cuota total constante, tipo anualidad. Se deja disponible por si
 *               algún otro producto/banco sí amortiza así.
 */
enum class AmortizationType {
    CAPITAL_FIJO,
    CUOTA_FIJA
}

/** Qué hacer con el resto del plazo cuando cae un abono extraordinario. */
enum class PaymentStrategy {
    REDUCIR_PLAZO,
    REDUCIR_CUOTA
}
