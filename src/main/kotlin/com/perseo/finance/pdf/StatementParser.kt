package com.perseo.finance.pdf

/** Bancos con parser propio de extracto (HU-2.2 / HU-F.5). */
enum class BankLayout {
    DAVIVIENDA,
    BANCOLOMBIA
}

/** Contrato común: cada banco compone su extracto distinto, así que cada uno implementa el suyo. */
interface StatementParser {
    fun parse(text: String): ParsedStatementResponse
}

object StatementParserFactory {
    fun forBank(bank: BankLayout): StatementParser = when (bank) {
        BankLayout.DAVIVIENDA -> DaviviendaStatementParser
        BankLayout.BANCOLOMBIA -> BancolombiaStatementParser
    }
}
