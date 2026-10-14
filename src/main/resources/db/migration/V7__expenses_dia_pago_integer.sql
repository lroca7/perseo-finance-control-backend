-- V7__expenses_dia_pago_integer.sql
-- Hibernate mapea Int de Kotlin a INTEGER; SMALLINT del V6 no valida. Se unifica a INTEGER.
ALTER TABLE expenses_incomes ALTER COLUMN dia_pago TYPE INTEGER;
ALTER TABLE expenses_bills ALTER COLUMN dia_pago TYPE INTEGER;
