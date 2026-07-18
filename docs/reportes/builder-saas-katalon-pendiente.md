# Builder SaaS — Cobertura Katalon (estado y pendientes)

Plataforma: `https://testing-templet-builder-saas.vercel.app` (TEST). Login Microsoft SSO
(`openBrowserAndLoginWithMicrosoft`). Middleware exige sesión MS: sin sesión los `/api/*`
devuelven `401 {"error":"Unauthorized"}` y las páginas server-side fallan el fetch.

## Cobertura creada el 10 jul 2026

**Fase 1 — Smoke funcional + sanity (COMPLETADA):**

- 3 TCs nuevos en `Test Cases/BuilderSaas/smoke/` (planos) + scripts en `Scripts/BuilderSaas/smoke/<slug>/Script.groovy`:
  - `TC-BUILDERSAAS-SMOKE-PAGES-001` (validate-smoke-pages) — abre la sesión de la suite; valida
    Home, `/brand-properties`, `/templates`, `/blueprints`, `/layout` (Brand assets), `/qa`
    sin "Failed to load" ni 404; heurística de contadores en Home. **Detecta la regresión del
    middleware** (hoy FAILED = correcto).
  - `TC-BUILDERSAAS-API-SANITY-002` (validate-api-sanity) — reusa sesión; fetch same-origin
    (`credentials:'include'`) a `/api/brands`, `/api/templates`, `/api/layouts`; valida 200 y
    counts sanos. Referencia = snapshot previo si existe, si no seeds `[brands:62, templates:215,
    layouts:1779]` (solo referencia, NO aserción). Warning si desvío >20%.
  - `TC-BUILDERSAAS-AUTH-003` (validate-auth-unauthorized) — sin navegador; `clearSession()` +
    GET anónimo a los 3 endpoints, espera 401.
- Suite `Test Suites/Platforms/BuilderSaas/Smoke.ts` — orden pages → api-sanity → auth, todos
  `isReuseDriver=true` (1 solo login SSO).
- Snapshots en `Reports/BuilderSaas/snapshots/`: `builder_saas_smoke_latest.json` (evidencia,
  siempre) y `builder_saas_api_latest.json` (baseline, solo si el run no tuvo failures) + `history/`.

**Habilitante — sesión MS para HttpClient:**

- `Keywords/ApiKeywords.groovy`: `defaultHeaders` inyectado en `callJson`; `useBrowserSession()`
  adopta cookies del navegador (incluye HttpOnly) en HttpClient; `clearSession()` las remueve.
- Fix `Scripts/Builders/api-regression/*` (los 4): login defensivo + `useBrowserSession()` tras
  calcular `base`. Ya no fallan por 401. Las expectativas 200/201/400/404/409 quedan igual →
  seguirán rojos hasta que dev arregle US-04/07/08/09, pero ya no por falta de sesión.
- Suite `API-Regression-BuilderSaaS.ts`: `isReuseDriver` false→true en los 4 links.
- `Profiles/global.glbl`: nueva `BUILDER_SAAS_TEST_URL`.

Resultado esperado HOY (10 jul 2026): TC-001 FAILED (detecta regresión), TC-002 PASSED,
TC-003 PASSED.

## Fase 2 — PENDIENTE

- `TC-BUILDERSAAS-VISUAL-004` con `VisualKeywords`/`captureVisualSnapshot`, a crear **cuando dev
  arregle el middleware**. Hoy NO se crea: con las páginas rotas el baseline visual sería basura.
