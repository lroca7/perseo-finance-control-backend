-- V4__ahorro_metas.sql
-- Módulo de ahorro: metas, planes, aportes/retiros, conciliaciones y parámetros tributarios.
--
-- NOTA DE RECONSTRUCCIÓN (12-oct-2026): el archivo original de esta migración se perdió
-- del repo cuando la BD local ya la tenía aplicada (versión 4, "ahorro metas"). Este archivo
-- se reconstruyó desde el esquema vivo (pg_dump) para que una BD nueva quede idéntica y la
-- validación de Flyway vuelva a pasar. Todo es IF NOT EXISTS / inserciones condicionales:
-- en la BD actual no cambia nada; en una BD nueva crea el esquema completo.
-- Motor: PostgreSQL 15+

-- savings_goals: una meta (viaje, fondo de emergencia...). El saldo inicial vive aquí (D4).
CREATE TABLE IF NOT EXISTS savings_goals (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL REFERENCES users(id),
    nombre          VARCHAR(80) NOT NULL,
    icono           VARCHAR(30) NOT NULL DEFAULT 'piggy-bank',
    monto_objetivo  NUMERIC(18,2) NOT NULL CHECK (monto_objetivo > 0),
    saldo_inicial   NUMERIC(18,2) NOT NULL DEFAULT 0 CHECK (saldo_inicial >= 0),
    fecha_objetivo  DATE,
    estado          VARCHAR(20) NOT NULL DEFAULT 'ACTIVA'
                    CHECK (estado IN ('ACTIVA', 'ALCANZADA', 'ARCHIVADA')),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_savings_goals_user ON savings_goals(user_id);

-- savings_plans: hipótesis de simulación de una meta. Solo uno es referencia (índice parcial).
CREATE TABLE IF NOT EXISTS savings_plans (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    goal_id                 UUID NOT NULL REFERENCES savings_goals(id) ON DELETE CASCADE,
    nombre                  VARCHAR(80) NOT NULL,
    tipo_producto           VARCHAR(20) NOT NULL
                            CHECK (tipo_producto IN ('CUENTA_AHORROS', 'CDT', 'FIC', 'FPV_AFC', 'MANUAL')),
    tasa_ea                 NUMERIC(9,6) NOT NULL CHECK (tasa_ea > 0 AND tasa_ea <= 1),
    comision_ea             NUMERIC(9,6) NOT NULL DEFAULT 0 CHECK (comision_ea >= 0 AND comision_ea < 1),
    aporte_mensual          NUMERIC(18,2) NOT NULL DEFAULT 0 CHECK (aporte_mensual >= 0),
    momento_aporte          VARCHAR(10) NOT NULL DEFAULT 'FIN' CHECK (momento_aporte IN ('INICIO', 'FIN')),
    crecimiento_aporte_anual NUMERIC(9,6) NOT NULL DEFAULT 0
                            CHECK (crecimiento_aporte_anual >= 0 AND crecimiento_aporte_anual <= 0.5),
    plazo_meses             INTEGER NOT NULL CHECK (plazo_meses >= 1 AND plazo_meses <= 600),
    fecha_inicio            DATE NOT NULL,
    inflacion_anual         NUMERIC(9,6) NOT NULL DEFAULT 0
                            CHECK (inflacion_anual >= 0 AND inflacion_anual <= 0.5),
    tratamiento_retencion   VARCHAR(20) NOT NULL DEFAULT 'MENSUAL'
                            CHECK (tratamiento_retencion IN ('MENSUAL', 'AL_VENCIMIENTO')),
    retencion_tarifa        NUMERIC(9,6) CHECK (retencion_tarifa >= 0 AND retencion_tarifa <= 1),
    es_referencia           BOOLEAN NOT NULL DEFAULT false,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_savings_plans_goal ON savings_plans(goal_id);
CREATE UNIQUE INDEX IF NOT EXISTS uq_savings_plans_referencia ON savings_plans(goal_id) WHERE es_referencia;

-- savings_contributions: aportes y retiros reales (el signo lo da tipo).
CREATE TABLE IF NOT EXISTS savings_contributions (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    goal_id     UUID NOT NULL REFERENCES savings_goals(id) ON DELETE CASCADE,
    fecha       DATE NOT NULL,
    monto       NUMERIC(18,2) NOT NULL CHECK (monto > 0),
    tipo        VARCHAR(10) NOT NULL DEFAULT 'APORTE' CHECK (tipo IN ('APORTE', 'RETIRO')),
    nota        VARCHAR(200),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_savings_contributions_goal_fecha ON savings_contributions(goal_id, fecha);

-- savings_balance_checks: conciliaciones de saldo real.
CREATE TABLE IF NOT EXISTS savings_balance_checks (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    goal_id     UUID NOT NULL REFERENCES savings_goals(id) ON DELETE CASCADE,
    fecha       DATE NOT NULL,
    saldo_real  NUMERIC(18,2) NOT NULL CHECK (saldo_real >= 0),
    nota        VARCHAR(200),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_savings_balance_checks_goal_fecha ON savings_balance_checks(goal_id, fecha);

-- tax_parameters: tarifas fuera del código, con vigencia (el motor usa la vigente cada mes).
CREATE TABLE IF NOT EXISTS tax_parameters (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tipo_producto   VARCHAR(20) NOT NULL,
    concepto        VARCHAR(40) NOT NULL,
    tarifa          NUMERIC(9,6) NOT NULL CHECK (tarifa >= 0 AND tarifa <= 1),
    vigente_desde   DATE NOT NULL,
    vigente_hasta   DATE,
    notas           VARCHAR(200),
    CONSTRAINT tax_parameters_check CHECK (vigente_hasta IS NULL OR vigente_hasta >= vigente_desde)
);
CREATE INDEX IF NOT EXISTS idx_tax_parameters_lookup ON tax_parameters(tipo_producto, concepto, vigente_desde);

-- Parámetros iniciales (verificar con normativa o contador). Solo si la tabla está vacía.
INSERT INTO tax_parameters (tipo_producto, concepto, tarifa, vigente_desde, notas)
SELECT * FROM (VALUES
    ('CUENTA_AHORROS', 'RETENCION_RENDIMIENTOS', 0.07, DATE '2020-01-01', 'Valor inicial. Verificar.'),
    ('CDT',            'RETENCION_RENDIMIENTOS', 0.07, DATE '2020-01-01', 'Valor inicial. Verificar.'),
    ('FIC',            'RETENCION_RENDIMIENTOS', 0.07, DATE '2020-01-01', 'Provisional, igual al general. Verificar con la ficha del fondo.'),
    ('MANUAL',         'RETENCION_RENDIMIENTOS', 0.07, DATE '2020-01-01', 'Valor inicial. El usuario puede personalizarlo.'),
    ('CUENTA_AHORROS', 'GMF', 0.004, DATE '2020-01-01', '4x1000. Exenciones por verificar.'),
    ('CDT',            'GMF', 0.004, DATE '2020-01-01', '4x1000. Exenciones por verificar.'),
    ('FIC',            'GMF', 0.004, DATE '2020-01-01', '4x1000. Exenciones por verificar.'),
    ('MANUAL',         'GMF', 0.004, DATE '2020-01-01', '4x1000. Exenciones por verificar.')
) AS v(tipo_producto, concepto, tarifa, vigente_desde, notas)
WHERE NOT EXISTS (SELECT 1 FROM tax_parameters);
