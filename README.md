# Perseo Finance — Backend

Backend en Kotlin + Spring Boot para el módulo de tarjetas de crédito.
Implementa las HU técnicas de la Épica 6 (ver `docs/historias-de-usuario-tarjetas.md`).

## Requisitos
- JDK 21
- Docker (para Postgres local)

## Cómo correrlo

1. Levantar Postgres:
   ```bash
   docker compose up -d
   ```

2. Correr la app (Flyway aplica las migraciones automáticamente al arrancar):
   ```bash
   ./gradlew bootRun
   ```

3. La API queda en `http://localhost:8080`.
   Documentación interactiva (Swagger): `http://localhost:8080/swagger-ui.html`

## Correr los tests

```bash
./gradlew test
```

Incluye las pruebas unitarias del motor de amortización (`AmortizationEngineTest`),
que cubren los casos límite definidos en HU-T.5: tasa 0%, abono que cancela la
deuda, abono mayor al saldo, y cuota que no cubre el interés.

## Estructura

```
src/main/kotlin/com/perseo/finance/
  domain/       -> motor de amortización puro, sin dependencias de Spring (HU-T.5)
  entity/       -> entidades JPA (cards, credit_lines, extra_payments, users)
  repository/   -> repositorios Spring Data
  dto/          -> request/response de la API
  service/      -> lógica de aplicación (CRUD + orquestación de la simulación)
  controller/   -> endpoints REST

src/main/resources/db/migration/
  V1__create_schema.sql -> migración Flyway inicial
```

## Endpoints principales

| Método | Ruta                                   | Descripción |
|--------|-----------------------------------------|-------------|
| GET    | `/api/cards?userId=...`                 | Listar tarjetas de un usuario |
| POST   | `/api/cards?userId=...`                 | Crear tarjeta |
| GET    | `/api/cards/{cardId}/lines`             | Listar líneas de crédito de una tarjeta |
| POST   | `/api/cards/{cardId}/lines`             | Crear línea de crédito |
| GET    | `/api/lines/{lineId}/simulation`        | Comparar escenario sin abonos vs. con abonos |
| POST   | `/api/lines/{lineId}/extra-payments`    | Agregar abono extraordinario a una cuota |

## Pendiente (no bloqueante para el MVP)
- Autenticación JWT (HU-T.8) — hoy `userId` se pasa como query param.
- Migrar el motor de amortización de `Double` a `BigDecimal` con precisión
  explícita antes de manejar dinero real en producción (ver nota en `Schedule.kt`).
- Endpoint de `statements` (HU-2.1) y carga automática de PDF (HU-2.2).
