-- V6__expenses_bloques.sql
-- HU-G.3: bloques mensuales de ingresos, facturas, ahorro y deudas (hojas Ene-Dic del Excel).
-- Las facturas tienen catálogo propio editable; ingresos/ahorro/deudas usan nombre libre
-- con sugerencia del mes anterior (en el Excel se escribían a mano cada mes).

-- Catálogo de facturas (columna D de la hoja Config).
CREATE TABLE expenses_bill_categories (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    nombre     VARCHAR(60) NOT NULL,
    activa     BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX uq_expenses_bill_categories_user_nombre
    ON expenses_bill_categories(user_id, nombre);
CREATE INDEX idx_expenses_bill_categories_user ON expenses_bill_categories(user_id);

-- Ingresos del mes (bloque B-E: categoría, día de pago, proyectado, recibido).
CREATE TABLE expenses_incomes (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    categoria   VARCHAR(60) NOT NULL,
    dia_pago    SMALLINT CHECK (dia_pago BETWEEN 1 AND 31),
    proyectado  BIGINT CHECK (proyectado >= 0),
    recibido    BIGINT CHECK (recibido >= 0),
    anio        INTEGER NOT NULL CHECK (anio BETWEEN 2000 AND 2100),
    mes         INTEGER NOT NULL CHECK (mes BETWEEN 1 AND 12),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_expenses_incomes UNIQUE (user_id, categoria, anio, mes)
);

CREATE INDEX idx_expenses_incomes_user_mes ON expenses_incomes(user_id, anio, mes);

-- Facturas del mes (bloque G-K: pagado, categoría, día de pago, valor, valor pagado).
CREATE TABLE expenses_bills (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id      UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    categoria_id UUID NOT NULL REFERENCES expenses_bill_categories(id) ON DELETE RESTRICT,
    dia_pago     SMALLINT CHECK (dia_pago BETWEEN 1 AND 31),
    valor        BIGINT CHECK (valor >= 0),
    pagado       BOOLEAN NOT NULL DEFAULT false,
    valor_pagado BIGINT CHECK (valor_pagado >= 0),
    anio         INTEGER NOT NULL CHECK (anio BETWEEN 2000 AND 2100),
    mes          INTEGER NOT NULL CHECK (mes BETWEEN 1 AND 12),
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_expenses_bills UNIQUE (user_id, categoria_id, anio, mes)
);

CREATE INDEX idx_expenses_bills_user_mes ON expenses_bills(user_id, anio, mes);

-- Ahorro del mes (bloque Q-S: nombre, meta, ahorrado). Flujo de caja mensual;
-- el detalle de metas vive en el módulo savings.
CREATE TABLE expenses_month_savings (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    nombre     VARCHAR(60) NOT NULL,
    meta       BIGINT CHECK (meta >= 0),
    ahorrado   BIGINT CHECK (ahorrado >= 0),
    anio       INTEGER NOT NULL CHECK (anio BETWEEN 2000 AND 2100),
    mes        INTEGER NOT NULL CHECK (mes BETWEEN 1 AND 12),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_expenses_month_savings UNIQUE (user_id, nombre, anio, mes)
);

CREATE INDEX idx_expenses_month_savings_user_mes ON expenses_month_savings(user_id, anio, mes);

-- Deudas del mes (bloque U-W: nombre, pendiente, pagado). Pagos mensuales;
-- el detalle de tarjetas vive en el módulo cards.
CREATE TABLE expenses_month_debts (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    nombre     VARCHAR(60) NOT NULL,
    pendiente  BIGINT CHECK (pendiente >= 0),
    pagado     BIGINT CHECK (pagado >= 0),
    anio       INTEGER NOT NULL CHECK (anio BETWEEN 2000 AND 2100),
    mes        INTEGER NOT NULL CHECK (mes BETWEEN 1 AND 12),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_expenses_month_debts UNIQUE (user_id, nombre, anio, mes)
);

CREATE INDEX idx_expenses_month_debts_user_mes ON expenses_month_debts(user_id, anio, mes);

-- Siembra el catálogo de facturas de la hoja Config para el usuario de desarrollo.
INSERT INTO expenses_bill_categories (id, user_id, nombre)
VALUES
    (gen_random_uuid(), '00000000-0000-0000-0000-000000000001', 'Luz'),
    (gen_random_uuid(), '00000000-0000-0000-0000-000000000001', 'Gas'),
    (gen_random_uuid(), '00000000-0000-0000-0000-000000000001', 'Agua'),
    (gen_random_uuid(), '00000000-0000-0000-0000-000000000001', 'Aportes en Linea'),
    (gen_random_uuid(), '00000000-0000-0000-0000-000000000001', 'Netflix'),
    (gen_random_uuid(), '00000000-0000-0000-0000-000000000001', 'Google Photos'),
    (gen_random_uuid(), '00000000-0000-0000-0000-000000000001', 'Movistar')
ON CONFLICT DO NOTHING;
