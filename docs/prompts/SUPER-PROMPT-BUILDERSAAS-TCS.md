# SUPER-PROMPT — TCs Katalon Builder SAAS (plan aprobado 10 jul 2026, listo para ejecutar)

> Uso: en un chat nuevo con la carpeta `C:\Users\e2494\Katalon Studio\Templet` conectada, pegar este
> archivo completo o decir: **"Lee SUPER-PROMPT-BUILDERSAAS-TCS.md y ejecútalo tal cual"**.
> El plan YA fue aprobado por Ed con ajustes — NO volver a preguntar; ejecutar directo.

## Contexto

Plataforma nueva **BuilderSaas**: `https://testing-templet-builder-saas.vercel.app`, login Microsoft SSO
(mismas credenciales del proyecto, `openBrowserAndLoginWithMicrosoft`). El 10 jul 2026 una regresión del
middleware de Manuel rompió el fetch server-side sin sesión: `/brand-properties`, `/templates`,
`/blueprints` muestran "Failed to load …" y el Home queda con contadores en 0; `/qa` sí funciona.
Sin sesión, `/api/*` devuelve `401 {"error":"Unauthorized"}`. Referencias API del 10 jul:
`/api/brands` 62 registros, `/api/templates` 215, `/api/layouts` 1779.

Resultado esperado HOY al correr la suite nueva: TC-001 **FAILED** (detecta la regresión — correcto),
TC-002 PASSED, TC-003 PASSED.

## Descubrimientos ya verificados (NO re-explorar)

- Perfil global: `Profiles/global.glbl` (único, `defaultProfile=true`). No existe `default.glbl`.
- Estructura real: los `.tc` van PLANOS en la carpeta de área (ej.
  `Test Cases/Builders/tracking/validate-all-dashboard.tc`) + script en
  `Scripts/<Plataforma>/<area>/<slug>/Script.groovy`. (El árbol de CLAUDE.md está desactualizado en esto.)
- `Keywords/ApiKeywords.groovy` (73 líneas): `callJson`/`assertStatus`/`cleanupResource` con Apache
  HttpClient **sin cookies ni sesión** → un login de browser NO autentica a HttpClient por sí solo.
- Ya existen `Test Suites/API-Regression-BuilderSaaS.ts` + 4 TCs en `Test Cases/Builders/api-regression/`
  (validate-brands-api-bugs, validate-templates-api-bugs, validate-initiatives-api-bugs,
  validate-layouts-api-smoke) que esperan 200/201 SIN sesión → el middleware los rompe con 401.
  **Ninguno prueba acceso anónimo a propósito** (verificado leyendo los 4 scripts) → no hay
  expectativas que cambiar a 401; solo agregar login + adopción de cookies.
- Suite modelo: `Test Suites/Platforms/Builders/Tracking/Tracking-Full-Flow.ts`
  (`isReuseDriver=true` en TODOS los links).
- Firmas confirmadas — `TempletPortalKeywords.groovy`: `openBrowserAndLoginWithMicrosoft(String)` L382,
  `currentUrlSafe()` L271, `isValidAppSession()` L285, `captureCaseScreenshot(caseName,label)` L294,
  `writeJsonSnapshot(path,Map)` L2343, `readJsonIfExists` cerca, `verifyXPathPresent(name,xpath,timeout)`
  L2571, `isBrowserSessionAlive()` existe. `CommonKeywords`: `getRequiredGlobal(name,fallback)`,
  `logCaseSummary(caseId,failures,warnings)`.

## Tareas — 14 archivos (crear/editar en la ruta principal)

### 1. `Profiles/global.glbl` (EDIT)
Insertar tras el bloque de `BUILDERS_BRAND_FIXTURE_URL`:
```xml
   <variable>
      <defaultValue>'https://testing-templet-builder-saas.vercel.app'</defaultValue>
      <description>URL del Builder SAAS en TEST (middleware con sesion MS). Sin slash final.</description>
      <id>var-builder-saas-test-url-global</id>
      <name>BUILDER_SAAS_TEST_URL</name>
   </variable>
```

### 2. `Keywords/ApiKeywords.groovy` (EDIT aditivo — cambio habilitante, firma de callJson intacta)
- Campo `static Map defaultHeaders = [:]`; en `callJson`, tras el `Content-Type`:
  `defaultHeaders.each { k, v -> req.setHeader(k.toString(), v.toString()) }`.
- Nuevo `@Keyword static String useBrowserSession()`: toma cookies de
  `com.kms.katalon.core.webui.driver.DriverFactory.getWebDriver().manage().getCookies()`
  (incluye HttpOnly; solo del dominio actual → llamar estando EN la app tras login), arma
  `"name=value; ..."` y lo pone en `defaultHeaders['Cookie']`. Log con cantidad de cookies.
- Nuevo `@Keyword static void clearSession()`: remueve `Cookie` de defaultHeaders.

### 3-5. Scripts nuevos en `Scripts/BuilderSaas/smoke/<slug>/Script.groovy`

**`validate-smoke-pages` — TC-BUILDERSAAS-SMOKE-PAGES-001** (primer TC: abre la sesión de la suite)
- `base` via `getRequiredGlobal('BUILDER_SAAS_TEST_URL', fallback)` + `replaceAll('/+$','')`.
- `openBrowserAndLoginWithMicrosoft(base)` → páginas: `''` (Home), `/brand-properties`, `/templates`,
  `/blueprints`, `/layout` (Brand assets, ruta verificada del sidebar), `/qa`.
- Por página: `WebUI.navigateToUrl` + `waitForPageLoad(20)` + `delay(2)` (hidratación Next.js);
  failure si `!isValidAppSession()`; failure si el body contiene "Failed to load" (case-insensitive,
  capturar el texto exacto); failure si markers 404 ("This page could not be found" o "404: NOT_FOUND");
  solo en Home: heurística de contadores (elementos hoja dentro de `main` con texto numérico puro
  ≤10 chars) → failure si existen y TODOS = 0; warning si no se encuentra ninguno (selector débil,
  sin data-testid). Screenshot por página (`captureCaseScreenshot`, label `page_<slug>`).
- Snapshot: `Reports/BuilderSaas/snapshots/builder_saas_smoke_latest.json` + copia en
  `history/builder_saas_smoke_<yyyyMMdd_HHmmss>.json` (`writeJsonSnapshot`). Latest siempre (es evidencia,
  no baseline).

**`validate-api-sanity` — TC-BUILDERSAAS-API-SANITY-002** (reusa sesión)
- Login defensivo: si `!isBrowserSessionAlive()` o `!currentUrlSafe().startsWith(base)` →
  `openBrowserAndLoginWithMicrosoft(base)` (leer antes la implementación L381-460 para confirmar que
  tolera/omite browser ya abierto y ajustar el patrón si hace falta).
- Por endpoint (`/api/brands`, `/api/templates`, `/api/layouts`): fetch same-origin con
  `credentials:'include'` vía `WebUI.executeJavaScript` — patrón asíncrono en `window.__qaApiResult`
  + polling cada 1s hasta 30s. Count calculado en el browser:
  `Array.isArray(j) ? j.length : (j.data?.length ?? j.total ?? -1)`.
- **AJUSTE APROBADO — sin hardcodeo como aserción:** seeds `[brands:62, templates:215, layouts:1779]`
  SOLO como referencia inicial. `previous = readJsonIfExists(latest)`; referencia =
  `previous.counts[key]` si es número > 0, si no el seed. Failure: timeout, status≠200, error o
  count<=0. Warning: desvío >20% vs referencia (indicar fuente: "snapshot previo" | "seed inicial").
- Snapshot `builder_saas_api_latest.json`: history SIEMPRE; **latest SOLO si el run no tuvo failures**
  (no envenenar la baseline con counts rotos).
- Groovy gotcha: en el JS embebido no usar `$` de GString sin control; interpolar solo la URL.

**`validate-auth-unauthorized` — TC-BUILDERSAAS-AUTH-003** (sin browser)
- Primero `CustomKeywords.'ApiKeywords.clearSession'()` (garantiza anonimato aunque otro TC haya
  adoptado cookies en la misma JVM).
- GET a los 3 endpoints con `callJson`: failure si status≠401 — mensaje distinto si 200
  ("DATOS EXPUESTOS SIN AUTH") vs otro status ("middleware inesperado"), incluir `raw.take(120)`.
  Warning si el body del 401 no contiene "unauthorized".

### 6-8. Descriptores en `Test Cases/BuilderSaas/smoke/` (planos)
`validate-smoke-pages.tc`, `validate-api-sanity.tc`, `validate-auth-unauthorized.tc` — formato del
proyecto: description, `name` = slug (igual al filename), tag (`buildersaas,smoke,...`), comment,
`<origin>MANUAL</origin>`, `<recordOption>OTHER</recordOption>`, `testCaseGuid` = UUID v4 real (uuidgen).

### 9. `Test Suites/Platforms/BuilderSaas/Smoke.ts` (NUEVO)
Espejo de Tracking-Full-Flow.ts: pageLoadTimeout 30, testSuiteGuid UUID, 3 `testCaseLink` en orden
pages → api-sanity → auth, todos `isReuseDriver=true`, `isRun=true`,
`usingDataBindingAtTestSuiteLevel=false`, guid UUID cada uno,
`testCaseId` = `Test Cases/BuilderSaas/smoke/<slug>`.

### 10-13. Los 4 scripts de `Scripts/Builders/api-regression/*/Script.groovy` (EDIT mínimo)
Tras calcular `base`, insertar:
```groovy
// 07/2026: el middleware exige sesion MS — login + adoptar cookies del browser para HttpClient
if (!CustomKeywords.'TempletPortalKeywords.isBrowserSessionAlive'() ||
    !CustomKeywords.'TempletPortalKeywords.currentUrlSafe'().startsWith(base)) {
    CustomKeywords.'TempletPortalKeywords.openBrowserAndLoginWithMicrosoft'(base)
}
CustomKeywords.'ApiKeywords.useBrowserSession'()
```
Nada más cambia (las expectativas 200/201/400/404/409 quedan igual; seguirán rojos hasta que dev
arregle los bugs US-04/07/08/09, pero ya no por 401).

### 14. `Test Suites/API-Regression-BuilderSaaS.ts` (EDIT)
`isReuseDriver` false → **true** en los 4 links (1 solo login SSO gracias al login defensivo).
Mejora flagged y coherente con el patrón aprobado.

### Al terminar: actualizar memoria
`builder-saas-katalon-pendiente.md`: cobertura creada el 10 jul (3 TCs + suite + fix api-regression);
fase 2 pendiente = **TC-BUILDERSAAS-VISUAL-004** con `VisualKeywords`/`captureVisualSnapshot` cuando
arreglen el middleware (hoy el baseline visual sería basura — NO crearlo aún).

## Reglas duras (aprobadas por Ed — no negociar)

1. UTF-8 **sin BOM** en todo `.groovy` (usar la herramienta Write nativa; JAMÁS PowerShell `Set-Content`).
2. Ruta principal `C:\Users\e2494\Katalon Studio\Templet` — nunca worktree.
3. **Sin git** (add/commit/push), **sin ejecutar suites**, sin tickets Asana, sin CI/CD.
4. NO crear el TC visual (es fase 2).
5. Groovy: nada de regex estilo JS (`/\s+/g`) → usar `'\\s+'`; escapar backslashes en JS embebido.
6. Solo cambios del alcance aprobado — cero refactors extra.

## Pasos de ejecución sugeridos

1. Leer `openBrowserAndLoginWithMicrosoft` (TempletPortalKeywords L381-460) y
   `writeJsonSnapshot`/`readJsonIfExists` (~L2320-2360) para confirmar comportamiento exacto.
2. Generar UUIDs (`uuidgen` en el sandbox, ~8).
3. Escribir los 14 archivos (Write/Edit con rutas Windows).
4. Verificar: `hexdump -C <groovy> | head -1` sin `ef bb bf`; `xmllint --noout` en .tc/.ts/.glbl;
   llaves balanceadas en los .groovy.
5. Actualizar memoria y cerrar con el formato obligatorio A-E de CLAUDE.md.
