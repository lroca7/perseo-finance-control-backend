-- V1__create_schema.sql
-- Esquema inicial: usuarios, tarjetas, líneas de crédito y abonos extraordinarios.
-- Motor: PostgreSQL 15+

CREATE EXTENSION IF NOT EXISTS "pgcrypto"; -- para gen_random_uuid()

-- ─────────────────────────────────────────────
-- USERS
-- ─────────────────────────────────────────────
CREATE TABLE users (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email         VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ─────────────────────────────────────────────
-- CARDS
-- Una tarjeta física/producto del banco. El cupo y la tasa
-- son los "generales" de la tarjeta; una línea de crédito
-- puede sobreescribir su propia tasa si es distinta.
-- ─────────────────────────────────────────────
CREATE TABLE cards (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    banco       VARCHAR(100) NOT NULL,
    alias       VARCHAR(100) NOT NULL,          -- ej: "Visa Platinum ****7460"
    cupo_total  NUMERIC(14,2) NOT NULL CHECK (cupo_total >= 0),
    tasa_ea     NUMERIC(6,3)  NOT NULL CHECK (tasa_ea >= 0),  -- % efectivo anual
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_cards_user_id ON cards(user_id);

-- ─────────────────────────────────────────────
-- CREDIT_LINES
-- Cada diferido/producto financiado dentro de una tarjeta
-- (ej. "REDIFE CAPITAL"). Se simula de forma independiente.
-- tipo_amortizacion define qué motor de cálculo usar:
--   CAPITAL_FIJO -> capital constante, interés decreciente (caso REDIFE)
--   CUOTA_FIJA   -> cuota total constante tipo anualidad
-- ─────────────────────────────────────────────
CREATE TABLE credit_lines (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    card_id            UUID NOT NULL REFERENCES cards(id) ON DELETE CASCADE,
    nombre             VARCHAR(150) NOT NULL,               -- ej: "REDIFE CAPITAL"
    saldo_pendiente    NUMERIC(14,2) NOT NULL CHECK (saldo_pendiente >= 0),
    cuotas_restantes   INTEGER NOT NULL CHECK (cuotas_restantes > 0),
    tasa_ea            NUMERIC(6,3) CHECK (tasa_ea >= 0),    -- NULL = usa la de la tarjeta
    tipo_amortizacion  VARCHAR(20) NOT NULL
                       CHECK (tipo_amortizacion IN ('CAPITAL_FIJO', 'CUOTA_FIJA')),
    ignorada           BOOLEAN NOT NULL DEFAULT false,       -- ej: compras pequeñas a 1 cuota
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_credit_lines_card_id ON credit_lines(card_id);

-- ─────────────────────────────────────────────
-- EXTRA_PAYMENTS
-- Abonos extraordinarios asignados a una cuota específica
-- de una línea de crédito. Varios abonos pueden caer en la
-- misma cuota (se suman al simular).
-- ─────────────────────────────────────────────
CREATE TABLE extra_payments (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    credit_line_id  UUID NOT NULL REFERENCES credit_lines(id) ON DELETE CASCADE,
    numero_cuota    INTEGER NOT NULL CHECK (numero_cuota > 0),
    monto           NUMERIC(14,2) NOT NULL CHECK (monto > 0),
    estrategia      VARCHAR(20) NOT NULL DEFAULT 'REDUCIR_PLAZO'
                    CHECK (estrategia IN ('REDUCIR_PLAZO', 'REDUCIR_CUOTA')),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_extra_payments_line_id ON extra_payments(credit_line_id);
CREATE INDEX idx_extra_payments_line_cuota ON extra_payments(credit_line_id, numero_cuota);

-- ─────────────────────────────────────────────
-- STATEMENTS (opcional / HU-2.1)
-- Snapshot de un extracto mensual, útil para historial
-- y para futura extracción automática de PDF (HU-2.2).
-- ─────────────────────────────────────────────
CREATE TABLE statements (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    card_id           UUID NOT NULL REFERENCES cards(id) ON DELETE CASCADE,
    periodo_inicio    DATE NOT NULL,
    periodo_fin       DATE NOT NULL,
    saldo_total       NUMERIC(14,2) NOT NULL CHECK (saldo_total >= 0),
    pago_minimo       NUMERIC(14,2) NOT NULL CHECK (pago_minimo >= 0),
    fecha_limite_pago DATE NOT NULL,
    cuota_manejo      NUMERIC(14,2) NOT NULL DEFAULT 0 CHECK (cuota_manejo >= 0),
    otros_cargos      NUMERIC(14,2) NOT NULL DEFAULT 0 CHECK (otros_cargos >= 0),
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (card_id, periodo_fin)
);

CREATE INDEX idx_statements_card_id ON statements(card_id);
