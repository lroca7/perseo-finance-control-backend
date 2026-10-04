package com.perseo.finance.savings.domain

enum class ProductType(
    val label: String,
    val defaultWithholding: WithholdingMode,
    val allowsCommission: Boolean
) {
    CUENTA_AHORROS("Cuenta de ahorros", WithholdingMode.MENSUAL, false),
    CDT("CDT", WithholdingMode.AL_VENCIMIENTO, false),
    FIC("Fondo de inversión colectiva (FIC)", WithholdingMode.MENSUAL, true),
    FPV_AFC("Pensión voluntaria / AFC", WithholdingMode.MENSUAL, false),
    MANUAL("Manual", WithholdingMode.MENSUAL, true);

    /** FPV_AFC se habilita con la HU-A.12 (fase 3). */
    val availableInMvp: Boolean get() = this != FPV_AFC
}

enum class GoalStatus { ACTIVA, ALCANZADA, ARCHIVADA }

enum class TaxConcept { RETENCION_RENDIMIENTOS, GMF }
