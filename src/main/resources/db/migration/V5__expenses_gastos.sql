-- V5__expenses_gastos.sql
-- Módulo de gastos diarios (HU-G.1): categorías editables por usuario,
-- presupuesto mensual por categoría y registro de gastos.
-- Montos en pesos enteros (COP sin decimales), como el Excel de origen.

-- Categorías de gasto (catálogo de la hoja Config del Excel).
CREATE TABLE expenses_categories (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    nombre     VARCHAR(60) NOT NULL,
    activa     BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Nombre único por usuario, insensible a mayúsculas/tildes a nivel de app
-- (la comparación normalizada vive en el servicio); esta restricción cubre
-- el caso exacto.
CREATE UNIQUE INDEX uq_expenses_categories_user_nombre
    ON expenses_categories(user_id, nombre);
CREATE INDEX idx_expenses_categories_user ON expenses_categories(user_id);

-- Presupuesto proyectado por categoría y mes (celdas N22:N51 del Excel).
CREATE TABLE expenses_budgets (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id      UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    categoria_id UUID NOT NULL REFERENCES expenses_categories(id) ON DELETE CASCADE,
    anio         INTEGER NOT NULL CHECK (anio BETWEEN 2000 AND 2100),
    mes          INTEGER NOT NULL CHECK (mes BETWEEN 1 AND 12),
    proyectado   BIGINT NOT NULL CHECK (proyectado >= 0),
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_expenses_budgets UNIQUE (user_id, categoria_id, anio, mes)
);

CREATE INDEX idx_expenses_budgets_user_mes ON expenses_budgets(user_id, anio, mes);

-- Registro de gastos del mes (filas 56+ del Excel: fecha, valor, categoría, descripción).
CREATE TABLE expenses_expenses (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id      UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    categoria_id UUID NOT NULL REFERENCES expenses_categories(id) ON DELETE RESTRICT,
    fecha        DATE NOT NULL,
    valor        BIGINT NOT NULL CHECK (valor > 0),
    descripcion  VARCHAR(200) NOT NULL DEFAULT '',
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_expenses_expenses_user_fecha ON expenses_expenses(user_id, fecha);
CREATE INDEX idx_expenses_expenses_categoria ON expenses_expenses(categoria_id);

-- Siembra el catálogo de la hoja Config para el usuario de desarrollo.
INSERT INTO expenses_categories (id, user_id, nombre)
VALUES
    (gen_random_uuid(), '00000000-0000-0000-0000-000000000001', 'Supermercado'),
    (gen_random_uuid(), '00000000-0000-0000-0000-000000000001', 'Domicilios'),
    (gen_random_uuid(), '00000000-0000-0000-0000-000000000001', 'Arriendo'),
    (gen_random_uuid(), '00000000-0000-0000-0000-000000000001', 'Transporte'),
    (gen_random_uuid(), '00000000-0000-0000-0000-000000000001', 'Medicinas'),
    (gen_random_uuid(), '00000000-0000-0000-0000-000000000001', 'Casa'),
    (gen_random_uuid(), '00000000-0000-0000-0000-000000000001', 'Lucy'),
    (gen_random_uuid(), '00000000-0000-0000-0000-000000000001', 'Compras'),
    (gen_random_uuid(), '00000000-0000-0000-0000-000000000001', 'Entretenimiento'),
    (gen_random_uuid(), '00000000-0000-0000-0000-000000000001', 'Regalos'),
    (gen_random_uuid(), '00000000-0000-0000-0000-000000000001', 'Viajes'),
    (gen_random_uuid(), '00000000-0000-0000-0000-000000000001', 'Peluqueria'),
    (gen_random_uuid(), '00000000-0000-0000-0000-000000000001', 'Mascotas'),
    (gen_random_uuid(), '00000000-0000-0000-0000-000000000001', 'Veterinaria'),
    (gen_random_uuid(), '00000000-0000-0000-0000-000000000001', 'Otros'),
    (gen_random_uuid(), '00000000-0000-0000-0000-000000000001', 'Aseo Personal'),
    (gen_random_uuid(), '00000000-0000-0000-0000-000000000001', 'Paseos'),
    (gen_random_uuid(), '00000000-0000-0000-0000-000000000001', 'Donaciones'),
    (gen_random_uuid(), '00000000-0000-0000-0000-000000000001', 'Prestamos'),
    (gen_random_uuid(), '00000000-0000-0000-0000-000000000001', 'Tienda'),
    (gen_random_uuid(), '00000000-0000-0000-0000-000000000001', 'Bancarios'),
    (gen_random_uuid(), '00000000-0000-0000-0000-000000000001', 'Tarjetazo'),
    (gen_random_uuid(), '00000000-0000-0000-0000-000000000001', 'Emprendimientos')
ON CONFLICT DO NOTHING;
