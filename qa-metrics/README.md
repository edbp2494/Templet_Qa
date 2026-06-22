# QA Cycle Report (ciclos 24 → 24)

Sistema de métricas QA del proyecto Templet. Genera un **dashboard HTML** (amigable para PO/CEO) + **JSON** por cada ciclo mensual que va del **24 de un mes al 24 del siguiente**, con todo lo que arroja la operación de QA.

## Qué mide

- **Ejecuciones del ciclo:** corridas, casos ejecutados, Pass/Fail/Error, % de éxito, regresiones, tiempo automatizado.
- **Eficiencia:** tiempo automatizado vs. tiempo manual estimado y **horas ahorradas**.
- **Inventario de automatización:** total de casos/suites/scripts y por plataforma.
- **Trabajo del ciclo (git):** commits, autores, **casos nuevos**, **casos modificados**, **estabilizaciones/bloqueos resueltos** (fixes), líneas de código en Scripts/Keywords.
- **Tickets/errores** detectados (desde `Reports/asana_tickets`), por severidad.
- **Metas del ciclo:** tarjetas de "meta cumplida ✅ / pendiente ⚠️".

## Cómo se genera

El generador (`generate_cycle_report.py`, solo Python 3 estándar) lee:
- `Reports/Smoke-Summary/*.txt` (ejecuciones) y `Reports/asana_tickets/*.json` (tickets) — datos **locales**.
- `git log` del rango del ciclo (cambios) e inventario de `Test Cases/Test Suites/Scripts`.

Como `Reports/` está en `.gitignore`, cada corrida **acumula** lo nuevo en historiales versionados:
- `docs/qa-cycles/executions-history.json`
- `docs/qa-cycles/tickets-history.json`

Así el dashboard se puede recalcular igual en local y en CI (que solo ve lo versionado).

**Salida:** `docs/qa-cycles/<inicio>_a_<fin>/dashboard.html` + `data.json`, y `docs/qa-cycles/latest.html`.

## Formas de correrlo

1. **Manual desde Katalon:** ejecutar la suite `Test Suites/QA/Generate-Cycle-Report`
   (corre el Python y deja `docs/qa-cycles/latest.html`).
2. **Manual desde Windows:** doble clic a `qa-metrics/run-cycle-report.bat`
   (genera y abre el HTML). Con `run-cycle-report.bat push` además commitea y pushea.
3. **Línea de comandos:**
   ```
   python qa-metrics/generate_cycle_report.py [--at YYYY-MM-DD] [--manual-min 10]
   ```
   `--at` define el ciclo (por defecto hoy); `--manual-min` = minutos manuales por caso para estimar el ahorro.
4. **Automático (CI):** `.github/workflows/qa-cycle-report.yml` corre el **día 24 de cada mes**
   (y con botón manual), regenera el dashboard y hace **commit + push** del reporte
   (`docs/qa-cycles/`). Requiere permiso `contents: write` (ya declarado).

## Notas

- El push automático lo hace **solo el CI**. El run local NO pushea salvo que pases `push`.
- Para que las métricas de ejecuciones sean ricas, conviene correr este generador (o el `.bat`)
  después de las corridas de QA, para que ingiera los `Smoke-Summary` al historial versionado.
- `--manual-min` por defecto 10 min/caso; ajustá según tu baseline real de pruebas manuales.
