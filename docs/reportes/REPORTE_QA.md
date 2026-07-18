# REPORTE_QA — Auditoría y Optimización del Proyecto Templet (Katalon)

> Fecha: 2026-06-12 | Ejecutado por: Claude (QA Lead session) | Alcance: Fases 1, 3 y 4. Fase 2 bloqueada (ver Gaps).

---

## 1. RESUMEN EJECUTIVO

| Métrica | Antes | Después |
|---|---|---|
| Test cases | 44 | 44 (sin TCs nuevos — ver Gaps Fase 2) |
| Test suites | 31 | 43 (+12 suites QA) |
| Bloques de login SSO duplicados inline | 6 scripts (~38 líneas c/u, ~228 líneas) | 0 — centralizados en `openBrowserAndLoginWithMicrosoft` |
| Bugs de runtime en keywords | 1 (FilenameFilter crash) | 0 |
| Manejo de credenciales en scripts | 6 scripts resolvían MS_USER/MS_PASS localmente | Solo 3 (objects-test-prod, legítimo: las pasan a `collectPlatformState`) |

Hallazgo crítico corregido hoy: `TC-BUILDERS-TRACKING-BLUEPRINT-001` fallaba en runtime por `MissingMethodException` en la purga de chart-snapshots (`TempletPortalKeywords.groovy:3262`) — el closure casteado a `java.io.FilenameFilter` tenía 1 argumento en vez de 2 (`File dir, String name`).

---

## 2. CAMBIOS APLICADOS

### Archivos modificados

| Archivo | Cambio |
|---|---|
| `Keywords/TempletPortalKeywords.groovy` (L3262) | Fix `FilenameFilter`: closure ahora con firma `(File dir, String name)`. Corrige el ERROR de la suite Tracking-Full-Flow del 2026-06-12. |
| `Scripts/Sheets/filters/client-initiative-sort/Script.groovy` | Bloque SSO manual (~38 líneas) → `openBrowserAndLoginWithMicrosoft(sheetsTestUrl)`. Elimina resolución local de credenciales. |
| `Scripts/Sheets/filters/initiative-content-validation/Script.groovy` | Ídem. |
| `Scripts/Decks/filters/client-initiative-sort/Script1778459631903.groovy` | Ídem (con `decksTestUrl`). También corrige indentación rota del bloque original. |
| `Scripts/Decks/filters/initiative-content-validation/Script1778459631910.groovy` | Ídem. |
| `Scripts/Email/filters/client-initiative-sort/Script1778459587704.groovy` | Ídem (con `emailTestUrl`). |
| `Scripts/Email/filters/initiative-content-validation/Script1778459587713.groovy` | Ídem. |

Razón: el patrón ya estaba probado en producción en los 10 scripts de `objects/` y `validation/` (mismos portales manager.php). Ahora los 19 scripts autenticados usan la misma keyword con retry 2x — un único punto de mantenimiento para el flujo SSO.

### Archivos nuevos — Test Suites (`Test Suites/QA/`)

| Suite | Contenido | isReuseDriver |
|---|---|---|
| `TS_Smoke.ts` | 8 TCs Critical: smoke + functional-smoke de las 5 plataformas. Fail-fast por TC. | false |
| `TS_Full_Regression.ts` | Los 44 TCs ordenados por módulo (Builders → Sheets → Decks → Email → Schedulers). | false global; true solo en bloque Tracking |
| `TS_Integration_Builders.ts` | Tracking ×4 (1 login) + direct-redirect. | true/false |
| `TS_Integration_Sheets.ts` | filters ×2 + validation ×2. | true |
| `TS_Integration_Decks.ts` | filters ×2 + validation ×2. | true |
| `TS_Integration_Email.ts` | filters ×2 + validation ×2. | true |
| `TS_Integration_Schedulers.ts` | functional-smoke (único E2E disponible). | false |
| `TS_Visual_Builders.ts` | objects ×2. | true |
| `TS_Visual_Sheets.ts` | objects ×4. | true |
| `TS_Visual_Decks.ts` | objects ×4. | true |
| `TS_Visual_Email.ts` | objects ×4. | true |
| `TS_Visual_Schedulers.ts` | objects ×2. | true |

---

## 3. GAPS DE COBERTURA RESTANTES

1. **Fase 2 completa (cobertura desde repos frontend/backend): NO EJECUTADA.** Bloqueante técnico: solo la carpeta del proyecto Katalon está montada en esta sesión; no hay acceso a los repositorios de frontend/backend/APIs. Para ejecutarla: montar esos repos y correr el skill `katalon-element-mapper` por pantalla → `katalon-testcase-creator`.
2. **Renombrado masivo a `TC_[Modulo]_[Accion]_[Resultado]`: NO APLICADO (decisión justificada).** Los IDs actuales (`TC-PLATAFORMA-AREA-NNN`) están referenciados en snapshots históricos, `BuildersTrackingSuiteListener`, tickets Asana JSON y CI/CD. Renombrar rompería trazabilidad y las 31 suites existentes sin beneficio funcional.
3. **Helper `validateSelectItems` duplicado** en los scripts de filters (3 copias). No extraído en esta pasada: usa closures locales (`snap`) acoplados al script. Candidato a `CommonKeywords.validateSelectOptions(Map config)` en próxima iteración con plan de validación propio.
4. **Lógica de purga de history** (`TempletPortalKeywords.groovy:3265-3269`): cuando hay >4 archivos borra todos, no conserva los 4 más recientes. Funcional pero subóptimo.
5. **Warnings activos de la app**: chips de email-filter no reflejan filtros "June"/"2026" en tabs Task Creation y Login (warnings recurrentes en la suite Tracking). Posible bug de UI a confirmar con el equipo dev.
6. **data-testid** pendiente en app Builders (ver `Include/config/SELECTORS-REVIEW.md`) — los fallbacks por texto siguen siendo el plan B.

---

## 4. PRÓXIMOS PASOS RECOMENDADOS

**Prioridad alta**
1. Re-ejecutar `Tracking-Full-Flow.ts` para confirmar el fix del FilenameFilter (blueprint-tab debe quedar PASSED).
2. Ejecutar `TS_Integration_Sheets.ts`, `TS_Integration_Decks.ts` y `TS_Integration_Email.ts` para validar la migración del login a keyword (los 6 scripts de filters).
3. Confirmar con dev si los chips de email-filter en Task Creation/Login son comportamiento esperado; si no, abrir ticket.

**Mejoras de infraestructura**
4. Montar repos frontend/backend y ejecutar Fase 2 (element maps + TCs nuevos por pantalla no cubierta).
5. Extraer `validateSelectItems` a CommonKeywords (gap #3).
6. Activar schedule nocturno en `builders-tracking-regression.yml` y configurar secrets de CI (requiere autorización).
7. Integrar `asana_tickets_*.json` con la API real de Asana.

---

## Plan de validación de esta sesión

1. Katalon Studio → abrir proyecto → verificar que las 12 suites de `Test Suites/QA/` cargan sin error de parseo.
2. Ejecutar `TS_Smoke.ts` (esperado: 8/8, ~10 min).
3. Ejecutar `Tracking-Full-Flow.ts` (esperado: 4/4 PASSED, sin MissingMethodException).
4. Revisar `Reports/Tracking/chart-snapshots/blueprint/history/` tras la corrida: debe rotar PNGs sin crash.

## Riesgos de regresión

- Migración de login en filters: si el botón "Log in with Microsoft" de los portales PHP difiere del selector interno de la keyword, el login fallaría — mitigado porque los scripts de objects/validation de los mismos portales ya usan esta keyword en suites verdes.
- Las suites nuevas son aditivas: no modifican las 31 existentes ni ningún listener.
- Sin cambios en `openBrowserAndLoginWithMicrosoft`, `isReuseDriver` ni `extractTrackingMetrics`.
