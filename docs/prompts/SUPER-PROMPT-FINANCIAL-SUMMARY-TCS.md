# SUPER-PROMPT — TCs Financial Summary (Builder SaaS · E9 · US-03)

> Prompt autocontenido para crear/regenerar los test cases de la pantalla Financial Summary
> sin necesitar contexto de sesiones previas. Actualizado: 2026-07-16.
> Nota: la suite YA EXISTE (`Test Suites/Platforms/BuilderSaas/Financial-Summary.ts`).
> Usar este prompt para regenerarla, extenderla o replicar el patrón en otra pantalla.

---

## 1. Contexto

- Proyecto Katalon: `C:\Users\e2494\Katalon Studio\Templet` (`Sheets.prj`), lenguaje Groovy.
- Historia: **[E9 · US-03] Financial Summary Dashboard** — dueño: Alexander Maraima.
- Tarjeta Asana: https://app.asana.com/1/623607192935720/project/1214933743176424/task/1215803033478165
- Pantalla: `/financial-summary` de Builder SaaS (Next.js, testing deploy en Vercel).

## 2. Ambiente, URLs y roles

- URL base: GlobalVariable `BUILDER_SAAS_TEST_URL` (en `Profiles/global.glbl`),
  fallback `https://testing-templet-builder-saas.vercel.app`.
- **SIN SSO Microsoft en esta suite**: el rol se mockea por query param (no confundir con
  `Smoke.ts` de BuilderSaas, que sí usa `openBrowserAndLoginWithMicrosoft`).
- Switcher DEV en la app (esquina inferior derecha): chips `DEV | Admin | Owner | Spec`.
- Valores canónicos de `?role=`: `Admin` | `ContractOwner` | `Specialist`.
  ⚠️ `SuperAdmin` y `Project Specialist` NO existen como valores — se ignoran en silencio.
- El rol persiste en memoria durante la navegación client-side; reload sin param = `Admin`.
- Mock API: `GET /api/dashboards/financial-summary` → JSON tipado, sin auth en TESTING
  (verificada 2026-07-16). Keys top-level: totalSpend, budgetRemaining, budgetUsedPct,
  budgetTotal, spendByCategory[], spendByInitiative[], revenue{byInitiative[]},
  deliveryAccuracy{blueprintAccuracyPct, oneOffAccuracyPct}, pendingApprovals[].

## 3. Acceptance criteria de la tarjeta

1. AC1 — Página accesible desde Sidebar con `?role=ContractOwner`.
2. AC2 — Todos los datos vienen del mock route, sin hardcoding inline en la UI.
3. AC3 — Role guard: solo ContractOwner y SuperAdmin (=Admin en la app) pueden acceder.

## 4. Element-map (selectores)

- Archivo: `Include/config/element-maps/builder-saas-financial-summary.json`
  (40 selectores, **0 data-testid en toda la app** → todos FRAGILE, basados en texto/@href).
- XPaths clave:

| Elemento | XPath primario | Assert |
|---|---|---|
| Link sidebar | `//aside//a[@href='/financial-summary']` | HARD |
| Link activo | `//aside//a[@href='/financial-summary' and contains(@class,'bg-[#00FF7F]')]` | SOFT |
| Grupo sidebar | `//aside//p[normalize-space(.)='Track']` (el plan decía "Finance") | SOFT |
| Título | `//h1[normalize-space(.)='Financial Summary']` | HARD |
| Subtítulo | `//*[contains(normalize-space(.),'Budget & Revenue Overview')][not(*)]` (separador U+00B7 — usar contains) | SOFT |
| Badge rol | `//span[normalize-space(.)='Contract Owner']` (BUG: hardcodeado) | SOFT |
| KPIs (4) | `//p[normalize-space(.)='Total Spend']` · ídem 'Budget Remaining', 'Revenue Achieved', 'Pending Approvals' | HARD |
| Secciones | `//p[normalize-space(.)='Budget Usage']` · 'Spend by Category' · 'Spend by Initiative' · 'Revenue vs Target' · 'Delivery Accuracy' · `(//p[normalize-space(.)='Pending Approvals'])[last()]` | HARD/SOFT |
| Barras progreso | `//div[contains(@style,'width:') and contains(@style,'%')]` → count >= 18 | SOFT |
| Filas categoría | `//span[normalize-space(.)='Content Production']` · 'Design & Branding' · 'Events & Webinars' · 'Digital Campaigns' · 'Tools & Platforms' | SOFT |
| Iniciativas | `//span[normalize-space(.)='CST Agent Workshop']` etc. (aparecen x2: Spend by Initiative y Revenue vs Target) | SOFT |

## 5. Test cases (nomenclatura + flujo)

Carpetas: `Test Cases/BuilderSaas/financial-summary/<slug>.tc` + `Scripts/BuilderSaas/financial-summary/<slug>/Script.groovy`

1. **TC-BUILDERSAAS-FINSUMMARY-ACCESS-001** (`validate-access-contractowner`)
   Navegar `{base}/?role=ContractOwner` → verificar link sidebar (HARD) → click → URL termina
   en `/financial-summary` + H1 presente (HARD) + badge (SOFT) + grupo Track (SOFT, nota Finance).
2. **TC-BUILDERSAAS-FINSUMMARY-CONTENT-002** (`validate-dashboard-content`)
   Con la misma sesión: validar element-map completo
   (`ReposCoverageKeywords.validateScreenFromElementMap`) + chequeos aritméticos:
   284/400 = 71% used · 480/1200 = 40% achieved · pct por categoría ≈ spend/budget.
   AC2 se valida por consistencia aritmética entre bloques + (opcional) fetch a la mock API.
3. **TC-BUILDERSAAS-FINSUMMARY-GUARD-ALLOW-003** (`validate-guard-admin`)
   `{base}/financial-summary?role=Admin` → página accesible, H1 presente (HARD).
4. **TC-BUILDERSAAS-FINSUMMARY-GUARD-DENY-004** (`validate-guard-specialist-denied`)
   `{base}/financial-summary?role=Specialist` → esperar bloqueo (redirect/403/sin dashboard).
   🔴 **HOY FALLA POR DISEÑO**: RBAC no implementado (bug reportado en la tarjeta el 2026-07-16).
   Warnings adicionales que evidencia: sidebar no oculta el ítem para Specialist; badge hardcodeado.
   Cuando el guard se implemente y este TC pase: retirar la nota "falla por diseño" y fijar el
   mecanismo real de bloqueo (mismo criterio que TC-ARCHMATCH-AGENDAR-BUGCONFIRMAR-002).

## 6. Suite

`Test Suites/Platforms/BuilderSaas/Financial-Summary.ts` — 4 testCaseLink en orden 001→004,
todos `isReuseDriver=true` (una sola sesión de browser, sin SSO).

## 7. Convenciones obligatorias del proyecto

- Patrón failures/warnings + `CommonKeywords.logCaseSummary(caseId, failures, warnings)`;
  bloqueantes → `KeywordUtil.markFailedAndStop`.
- URL siempre vía `CustomKeywords.'CommonKeywords.getRequiredGlobal'('BUILDER_SAAS_TEST_URL', fallback)`.
- Selectores débiles: `FailureHandling.OPTIONAL` + fallback (`clickFirstPresent`).
- `.groovy` sin BOM (no usar `Set-Content` de PowerShell) · no regex estilo JS (`/\s+/g`).
- No git add/commit/push ni acciones Asana sin OK explícito del usuario.

## 8. Bugs conocidos (2026-07-16, reportados en la tarjeta)

1. RBAC no implementado — `/financial-summary` renderiza completo con `role=Specialist`.
2. Sidebar no oculta "Financial Summary" para Specialist.
3. Badge del header hardcodeado "Contract Owner" para cualquier rol.
4. Ítem bajo grupo "Track" (el scope decía "Finance") — pendiente confirmación de Alexander.

## 9. Drift de datos detectado (2026-07-16)

- Content Production pasó de `$96k / $130k` (element-map 07-14) a **$98k / $130k** (UI y API hoy)
  → el selector SOFT `cat_amount_sample` va a tirar warning hasta actualizar el map.
- La mock API `/api/dashboards/financial-summary` SÍ existe (el known_issue #5 del element-map
  quedó viejo): usable para validar consistencia UI ↔ API en CONTENT-002.

## 10. Resultado esperado al ejecutar HOY

| TC | Resultado |
|---|---|
| ACCESS-001 | PASSED (warning: grupo Track vs Finance) |
| CONTENT-002 | PASSED con warnings (drift $96k→$98k) |
| GUARD-ALLOW-003 | PASSED |
| GUARD-DENY-004 | **FAILED por diseño** (evidencia del bug AC3) |
