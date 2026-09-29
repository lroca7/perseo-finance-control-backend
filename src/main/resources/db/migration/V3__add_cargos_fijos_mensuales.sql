-- V3__add_cargos_fijos_mensuales.sql
-- Campo agregado para cuota de manejo + seguro + otros cargos fijos
-- recurrentes de la tarjeta (no ligados a ninguna línea de crédito puntual).
-- Se usa para calcular el "pago aproximado del mes en curso":
-- suma de la cuota #1 de cada línea activa + este valor.
ALTER TABLE cards
    ADD COLUMN cargos_fijos_mensuales NUMERIC(14,2) NOT NULL DEFAULT 0
        CHECK (cargos_fijos_mensuales >= 0);
