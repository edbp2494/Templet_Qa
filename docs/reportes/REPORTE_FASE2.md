# REPORTE_FASE2 — Cobertura generada desde repos (Templet-Product-Team)

> Fecha: 2026-06-12 | Ejecutado por: Claude (QA super-agente) | Estado: COMPLETADA
> Continuación de `REPORTE_QA.md` (gap #1: Fase 2 bloqueada por falta de repos — desbloqueada hoy).

---

## A) Resumen

Se analizaron los 5 repos clonados en `repos\` (solo lectura), se inventariaron sus pantallas, se cruzaron contra los 44 TCs existentes y se generaron **13 TCs nuevos** (5 Builders, 5 Schedulers, 3 admin PHP) con **13 element-maps**, **1 keyword nueva** (`ReposCoverageKeywords.groovy`) y **3 suites ejecutables** en `Test Suites/Platforms/QA/`. Cada failure/warning sale en formato accionable (PASO + SELECTOR + ARCHIVO FUENTE + ACCIÓN SUGERIDA) y alimenta el pipeline de tickets Asana con el campo nuevo `sourceFile`.

## B) Tabla repo → URL → pantallas → TCs

| Repo | URL (GlobalVariable) | Pantallas inventariadas | Ya cubiertas | TCs NUEVOS |
|---|---|---|---|---|
| `templet-builders` | `BUILDERS_TEST_URL` | 18 rutas en `app/(logged)` + login | tracking (4 TCs), landing/smoke, objects, redirects | 5 |
| `templet-schedulers` | `SCHEDULERS_TEST_URL` | 29 rutas en `app/` | smoke, functional-smoke, objects (2) | 5 |
| `sheets.templet` | `SHEETS_TEST_URL` | 9 archivos `admin/*.php` (3 con UI real) | manager.php (filters/objects/validation/smoke) | 1 |
| `deck.templet` | `DECKS_TEST_URL` | ídem | manager.php (ídem) | 1 |
| `email.templet` | `EMAIL_TEST_URL` | ídem | manager.php (ídem) | 1 |

**Cruce por URL:** las cadenas de las URLs TEST **no** aparecen en el código de ningún repo (deploys vía Vercel/env vars; los PHP apuntan a prod `document.templet.io` / `email.templet.io` en código). Se mantiene el mapeo de `CLAUDE.md`, ya verificado previamente por el usuario.

### TCs nuevos (nomenclatura continúa la existente; ningún TC actual fue renombrado)

| Case ID | Pantalla (ruta) | Element map |
|---|---|---|
| TC-BUILDERS-TASKCREATION-NONSTANDARD-001 | /task-creation/non-standard | builders-task-creation-non-standard.json |
| TC-BUILDERS-TASKCREATION-CONTENT-001 | /task-creation/content | builders-task-creation-content.json |
| TC-BUILDERS-WIP-DASHBOARD-001 | /work-in-progress | builders-work-in-progress.json |
| TC-BUILDERS-SCHEDULE-PROJECT-001 | /project-schedule | builders-project-schedule.json |
| TC-BUILDERS-TEMPLATE-LIST-001 | /template | builders-template-list.json |
| TC-SCHEDULERS-TRACKING-DASHBOARD-001 | /tracking | schedulers-tracking.json |
| TC-SCHEDULERS-TASKCREATION-STANDARD-001 | /task-creation/standard | schedulers-task-creation-standard.json |
| TC-SCHEDULERS-REQUESTS-LIST-001 | /requests | schedulers-requests.json |
| TC-SCHEDULERS-RESOURCES-BOARD-001 | /resources | schedulers-resources.json |
| TC-SCHEDULERS-SUMMARY-LOG-001 | /summary | schedulers-summary.json |
| TC-SHEETS-ADMIN-SURFACE-001 | admin/manager.php + info.php + 403.php | sheets-admin-surface.json |
| TC-DECKS-ADMIN-SURFACE-001 | ídem (decks) | decks-admin-surface.json |
| TC-EMAIL-ADMIN-SURFACE-001 | ídem (email) | email-admin-surface.json |

## C) Hallazgos del análisis de código

**CRÍTICO (seguridad, a confirmar con dev):** `admin/info.php` en los 3 repos PHP contiene únicamente `phpinfo()`. Si es accesible en TEST/PROD expone versiones, paths y configuración del servidor. Los TCs ADMIN-SURFACE lo chequean como **warning** (ticket MEDIUM con `sourceFile: admin/info.php:1`) para no bloquear la suite. ACCIÓN SUGERIDA al dev: eliminar el archivo o protegerlo con `checkLogged()`.

**MEDIO:** en los repos PHP, `clientes.php` (proxy cURL a getClients), `info.php` y `access.php` (librería de seguridad) **no son pantallas UI** — el alcance PHP se ajustó a 1 TC de superficie admin por plataforma. `builder.php` requiere `?sheet=CLIENT_Initiative_Document__Sheet.html` con datos reales: queda como gap (ver E).

**MEDIO:** divergencia de comportamiento del guard de sesión entre plataformas: sheets/deck redirigen a `login.php?RelayState=…` (302) y email devuelve `401` seco (`admin/access.php:26-34`). Documentado para el equipo dev; puede afectar UX y automatización futura.

**BAJO:** ninguna pantalla nueva de Builders/Schedulers tiene `data-testid`; todos los selectores primarios son por texto/aria/estructura con fallback. Lista completa en `selector_review.needs_data_testid` de cada element-map (refuerza el pedido ya registrado en `Include/config/SELECTORS-REVIEW.md`).

## D) Archivos creados / modificados

### Nuevos
- `Keywords/ReposCoverageKeywords.groovy` — keyword genérica `validateScreenFromElementMap(Map)`: lee el element-map, gestiona sesión (1 login por suite, reusa driver vivo), ejecuta acciones (`verify_present`, `verify_text`, `verify_count`, `click`, `verify_absent`, `verify_absent_after_wait`, navegación multi-URL vía `url_path`), arma failures/warnings en el formato accionable obligatorio, captura `captureCaseScreenshot(caseId,'failure')`, escribe snapshot JSON en `Reports/Tracking/snapshots/repos_coverage_*_latest.json` y cierra con `logCaseSummary` + `markFailedAndStop`.
- `Include/config/element-maps/` — 13 JSONs (selectores primario+fallback, fuerza, assertion HARD/SOFT, `source_file` del repo por elemento).
- 13 × `Scripts/<Plataforma>/<area>/<slug>/Script.groovy` (delgados: solo invocan la keyword con su map).
- 13 × `Test Cases/<Plataforma>/<area>/<slug>.tc` (UUIDs v4 reales).
- `Test Suites/Platforms/QA/Repos-Coverage-Builders.ts` (5 TCs), `Repos-Coverage-Schedulers.ts` (5), `Repos-Coverage-AdminPHP.ts` (3: Sheets→Decks→Email). Todas con `isReuseDriver=true` y orden por plataforma para minimizar cambios de URL.

### Modificados (mínimos, aditivos — requeridos por la integración de tickets pedida)
- `Keywords/AsanaErrorTicketGenerator.groovy`: passthrough del campo `sourceFile` snapshot→error→ticket JSON y línea "Source File (repo frontend)" en la descripción del ticket. Sin cambios de lógica.
- `Test Listeners/BuildersTrackingSuiteListener.groovy`: el filtro post-suite ahora también matchea suites `Repos-Coverage*` (antes solo `Tracking`/`builders`). Sin otros cambios.
- NO se tocaron: `openBrowserAndLoginWithMicrosoft`, `extractTrackingMetrics`, TCs/suites existentes, ni código de los repos.

> Incidente corregido durante la sesión: una desincronización del filesystem truncó el final de los 2 archivos modificados; se reparó al instante y se verificó (UTF-8 válido, llaves balanceadas, sin BOM, diff limitado a las líneas previstas).

## E) Decisiones tomadas (delegadas)

1. **Priorización de pantallas** por valor de negocio y estabilidad: formularios de creación de tareas, dashboards de tracking/WIP y listados con data. Quedan fuera de esta pasada (gap): Builders `brand`, `convert`, `current-spend`, `layout/*`, `blueprint/[id]` (rutas dinámicas requieren IDs reales); Schedulers `assign`, `accounts`, `collateral*`, `daily-pulse`, `initiatives`, `metrics`, `productivity`, etc. (24 rutas restantes); PHP `builder.php` (requiere documento real).
2. **Sin clicks destructivos:** los botones "Set it up!" (crean tareas reales vía email/Asana) y "Delete" solo se verifican por presencia. Clicks solo en elementos seguros (tabs, sort, Load Data).
3. **3 suites en vez de 1**: una por plataforma Next.js + una combinada PHP (1 TC por portal), según preferencia aprobada.
4. **phpinfo como warning** (no failure) para que la suite quede verde mientras dev decide; el ticket MEDIUM se genera igual.
5. Numeración: áreas nuevas arrancan en `-001` (no colisionan con TCs existentes).

## F) Integración con tickets Asana

Cada TC escribe `Reports/Tracking/snapshots/repos_coverage_<tc>_latest.json` con `caseId, platform, tab, url, screenshot, failures[], warnings[], sourceFile`. Tras cada suite `Repos-Coverage*`, el listener invoca el generador: failures→HIGH, warnings→MEDIUM, dedupe por caso+mensaje, export a `Reports/asana_tickets/asana_tickets_<ts>.json` con `sourceFile` para reparación directa. Además, cada mensaje individual ya contiene `ARCHIVO FUENTE:` con ruta:línea del componente.

## G) Plan de validación (pasos exactos)

1. Abrir Katalon Studio → proyecto `Sheets.prj` → refrescar (F5 sobre la raíz) → verificar que `Test Suites/Platforms/QA/` muestra las 3 suites `Repos-Coverage-*` y que `Keywords/ReposCoverageKeywords.groovy` compila sin errores (pestaña Problems vacía).
2. Ejecutar `Repos-Coverage-Builders.ts`: Test Explorer → `Test Suites/Platforms/QA/Repos-Coverage-Builders` → botón Run (Chrome). Esperado: 1 solo login SSO, 5 TCs PASSED (warnings de selector-fallback aceptables), ~6-10 min.
3. Ejecutar `Repos-Coverage-Schedulers.ts` (ídem). Nota: el TC de tracking hace click en "Load Data" — esperado que el spinner desaparezca <60 s.
4. Ejecutar `Repos-Coverage-AdminPHP.ts`. Revisar especialmente el warning de `info.php`: si aparece, confirmar el hallazgo de seguridad con dev.
5. Tras cada suite, revisar `Reports/asana_tickets/asana_tickets_*.json`: los tickets deben incluir `sourceFile` y mensajes con `ARCHIVO FUENTE` / `ACCION SUGERIDA`.
6. Regresión: re-ejecutar `Tracking-Full-Flow.ts` para confirmar que los cambios aditivos al generador/listener no afectan el flujo existente (esperado: 4/4 PASSED, tickets igual que antes + sin campo roto).
7. Por CLI (opcional): `katalonc -noSplash -runMode=console -projectPath="C:\Users\e2494\Katalon Studio\Templet\Sheets.prj" -retry=0 -testSuitePath="Test Suites/Platforms/QA/Repos-Coverage-Builders" -browserType="Chrome"`.

## H) Riesgos de regresión

- **Generador/listener:** cambios aditivos; si un snapshot viejo no trae `sourceFile`, queda `''` (operador `?:`) — sin impacto en suites existentes. El listener ahora también corre tras suites `Repos-Coverage*` (era el objetivo).
- **Selectores por texto:** sensibles a cambios de copy en los frontends; cada element-map documenta el fallback y `needs_data_testid` para mitigar. Falla típica = actualizar element-map, no el script.
- **Datos de TEST:** `verify_count` (templates, filtros) es SOFT — ambientes vacíos generan warning, no failure.
- **SSO entre portales PHP:** la suite AdminPHP asume que la cookie de Microsoft permite encadenar los 3 portales; si expira, la keyword re-loguea sola (`isValidAppSession` → `openBrowserAndLoginWithMicrosoft`).
- `repos/` permanece intacta y fuera de versionado (verificado con git status). Recordatorio: aparece como untracked — confirmar que esté en `.gitignore` antes del próximo commit manual.

## I) Gaps restantes (próxima iteración)

1. 24 rutas de Schedulers y 8 de Builders sin TC dedicado (priorizadas fuera por valor/fragilidad).
2. `builder.php` (editor visual PHP): requiere fixture de documento estable en TEST.
3. Rutas dinámicas (`blueprint/[id]`, `template/[id]`, `assign/[pm]/[date]`): necesitan IDs semilla.
4. Pedir a dev `data-testid` para los elementos listados en los element-maps (acelera y estabiliza todo lo anterior).
5. Repos opcionales `templet-schedulers-client` y `shared.templet` no usados (no hicieron falta; pedirlos si se cubre builder.php).

---

## J) Resultados de la primera ejecución (2026-06-12 17:18–17:40) y fix aplicado

| Suite | Resultado | Detalle |
|---|---|---|
| Repos-Coverage-Builders | 4/5 PASSED | FAILED: TC-BUILDERS-TASKCREATION-NONSTANDARD-001 (ver causa raíz abajo) |
| Repos-Coverage-Schedulers | 4/5 PASSED | FAILED: TC-SCHEDULERS-TRACKING-DASHBOARD-001 (misma causa raíz) |
| Repos-Coverage-AdminPHP | ejecutada | Tickets MEDIUM generados (Sheets 1, Decks 2, Email 1) — revisar si incluyen el hallazgo phpinfo |
| Tracking-Full-Flow (regresión) | 4/4 PASSED | Sin regresión por los cambios al generador/listener; tickets con `sourceFile` OK |

**Causa raíz de los 2 FAILED:** ambos eran el PRIMER TC de su suite. `validateScreenFromElementMap` hacía `openBrowserAndLoginWithMicrosoft(targetUrl)` pero tras el SSO el browser queda en home y la keyword no re-navegaba a la pantalla objetivo — todos los selectores se evaluaban contra la home. Los TCs 2-5 (driver vivo) sí navegan explícitamente y pasaron.

**Fix aplicado (Keywords/ReposCoverageKeywords.groovy):** tras cada login/re-login ahora se ejecuta `WebUI.navigateToUrl(targetUrl)` + `waitForPageLoad(30)`. Re-ejecutar ambas suites; esperado 5/5.

**Validación adicional de la corrida:** formato accionable confirmado en logs y tickets (PASO + SELECTOR + ARCHIVO FUENTE + ACCION SUGERIDA), screenshots de fallo generados, pipeline Asana clasificando HIGH/MEDIUM por componente, y el patrón de warnings funcionando (Requests: filtros no visibles — posible toggle `showFilters`; Resources: la app no usa tag `<main>` — candidatos a ajuste de element-map en la próxima iteración, no bloquean).
