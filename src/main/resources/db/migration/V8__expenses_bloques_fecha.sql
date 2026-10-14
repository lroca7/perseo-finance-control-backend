-- V8__expenses_bloques_fecha.sql
-- HU-G.3 (ajuste UX): en ingresos y facturas se reemplaza el día de pago (número)
-- por un selector de fecha, como el registro de gastos. Se conserva el dato:
-- dia_pago se convierte a fecha dentro del mismo mes (tope: último día del mes).
ALTER TABLE expenses_incomes ADD COLUMN fecha DATE;
UPDATE expenses_incomes
SET fecha = make_date(
    anio,
    mes,
    LEAST(
        dia_pago,
        EXTRACT(DAY FROM (make_date(anio, mes, 1) + INTERVAL '1 month - 1 day'))::int
    )
)
WHERE dia_pago IS NOT NULL;
ALTER TABLE expenses_incomes DROP COLUMN dia_pago;

ALTER TABLE expenses_bills ADD COLUMN fecha DATE;
UPDATE expenses_bills
SET fecha = make_date(
    anio,
    mes,
    LEAST(
        dia_pago,
        EXTRACT(DAY FROM (make_date(anio, mes, 1) + INTERVAL '1 month - 1 day'))::int
    )
)
WHERE dia_pago IS NOT NULL;
ALTER TABLE expenses_bills DROP COLUMN dia_pago;
