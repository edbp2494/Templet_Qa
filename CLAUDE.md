# Claude Super-Agente — Proyecto Templet QA (Katalon)

> Actualizado: 2026-06-06 | Estado del proyecto: Builders Tracking completo al 100%

---

## REGLAS OPERATIVAS CRÍTICAS (leer primero)

1. **Nunca editar scripts en el worktree.** El CWD del agente puede ser `Editores/Sheet.worktrees/agents-*` pero Katalon ejecuta desde `C:\Users\e2494\Katalon Studio\Templet`. Siempre editar en la ruta principal.
2. **Nunca usar PowerShell `Set-Content` para archivos `.groovy`.** Agrega BOM (EF BB BF) que Groovy no soporta. Usar siempre:
   ```powershell
   [System.IO.File]::WriteAllText($path, $content, [System.Text.UTF8Encoding]::new($false))
   ```
3. **No hacer git add, commit, push** sin autorizacion explicita del usuario.
4. **No crear tickets Asana, no disparar CI/CD** sin autorización explícita del usuario.
5. **Leer el archivo completo antes de proponer cambios.** Sin refactors grandes no pedidos.
6. **Si algo es riesgoso o destructivo**, detenerse y pedir confirmación.

---

## Contexto del Proyecto

- **Proyecto Katalon:** `Sheets.prj` (raíz: `C:\Users\e2494\Katalon Studio\Templet`)
- **Lenguaje:** Groovy (Katalon DSL)
- **Plataformas testeadas:** Builders, Sheets, Decks, Email, Schedulers
- **Foco activo:** Builders Tracking — suite completa, lista para producción

### URLs de Ambientes (GlobalVariables)
| Variable | URL |
|---|---|
| `BUILDERS_TEST_URL` | `https://testing-templet-builders.vercel.app/` |
| `SHEETS_TEST_URL` | `https://sheets-test.templet.io/admin/manager.php` |
| `DECKS_TEST_URL` | `https://decks-test.templet.io/admin/manager.php` |
| `EMAIL_TEST_URL` | `https://emails-test.templet.io/admin/manager.php` |
| `SCHEDULERS_TEST_URL` | `https://testing-templet-schedulers.vercel.app/` |

### Credenciales (nunca exponer)
- Se leen desde `Include/config/templet-credentials.properties` (fuera de Git)
- Variables: `MS_USER`, `MS_PASS`
- Fallback: env vars `TEMPLET_MS_USER`, `TEMPLET_MS_PASS`
- Auth: Microsoft SSO (`login.microsoftonline.com`) — IDs estables controlados por MS

---

## Mapa de Archivos Clave

```
C:\Users\e2494\Katalon Studio\Templet\
├── CLAUDE.md                                          ← este archivo
├── BUILDERS_TRACKING_COMPLETION.md                    ← doc completa del estado
├── AGENTE-CLAUDE-SHEET.md                             ← prompt de inicio de sesión
├── .github/
│   ├── instructions/learnings.instructions.md         ← aprendizajes críticos
│   └── workflows/builders-tracking-regression.yml     ← CI/CD preparado (manual)
├── Keywords/
│   ├── TempletPortalKeywords.groovy                   ← ~3100 líneas, keyword principal
│   ├── CommonKeywords.groovy                          ← utils: logSummary, getRequiredGlobal
│   ├── AsanaErrorTicketGenerator.groovy               ← genera tickets JSON de errores
│   ├── AsanaErrorTicketGeneratorKeyword.groovy        ← ejecutado post-suite
│   ├── SheetsKeywords.groovy                          ← keywords específicas Sheets
│   ├── VisualKeywords.groovy                          ← comparación visual
│   └── ObjectCaptureKeywords.groovy                   ← captura de objetos
├── Test Cases/Builders/tracking/
│   ├── validate-all-dashboard/                        ← TC-BUILDERS-TRACKING-ALL-001
│   ├── validate-blueprint-tab/                        ← TC-BUILDERS-TRACKING-BLUEPRINT-001
│   ├── validate-task-creation-tab/                    ← TC-BUILDERS-TRACKING-TASK-001
│   └── validate-login-tab/                            ← TC-BUILDERS-TRACKING-LOGIN-001
├── Scripts/Builders/tracking/
│   ├── validate-all-dashboard/Script.groovy
│   ├── validate-blueprint-tab/Script.groovy
│   ├── validate-task-creation-tab/Script.groovy
│   └── validate-login-tab/Script.groovy
├── Test Suites/Platforms/Builders/Tracking/
│   └── Tracking-Full-Flow.ts                          ← isReuseDriver=true, 4 TCs
├── Test Listeners/
│   ├── BuildersTrackingSuiteListener.groovy           ← @AfterTestSuite → genera tickets
│   └── SmokeTestListener.groovy
├── Include/config/
│   ├── templet-credentials.properties                 ← NO en Git
│   ├── templet-credentials.properties.example
│   ├── log.properties
│   └── SELECTORS-REVIEW.md                            ← selectores débiles documentados
├── Reports/
│   ├── Tracking/snapshots/                            ← JSONs baseline por TC
│   │   ├── tracking_all_latest.json
│   │   ├── tracking_blueprint_latest.json
│   │   ├── tracking_task_creation_latest.json
│   │   ├── tracking_login_latest.json
│   │   └── history/
│   └── asana_tickets/                                 ← generado post-suite
└── Object Repository/Builders/Tracking/All/           ← objetos del tracking dashboard
```

---

## API de Keywords — TempletPortalKeywords.groovy

### Autenticación y Sesión
| Método | Descripción |
|---|---|
| `openBrowserAndLoginWithMicrosoft(String targetUrl)` | Abre browser, hace SSO Microsoft, reintenta 2x si falla |
| `resolveCredential(String primaryName, String secondaryName)` | Lee credencial desde GlobalVar / env / .properties |
| `isValidAppSession()` | Verifica que la URL no sea de Microsoft (sesión válida) |
| `currentUrlSafe()` | URL actual sin lanzar excepción |
| `closeExtraTabsKeepCurrent(String reason)` | Cierra tabs SSO extra, mantiene la actual |
| `isBrowserSessionAlive()` | Verifica que el WebDriver tenga sesión activa |
| `safeCloseBrowser()` | Cierra browser sin lanzar excepción |
| `logoutAndVerify(String protectedUrl)` | Hace logout y verifica que no hay sesión |

### Builders Tracking (foco principal)
| Método | Descripción |
|---|---|
| `validateBuildersTrackingAllDashboard(Map config)` | Valida dashboard "All": SSO, título dinámico, switch Production, botón Load Data, snapshot JSON |
| `validateBuildersTrackingTabDashboard(Map config)` | Valida tab específico (Blueprint/Task Creation/Login): tab visible, tema color, prefix Daily Executions, card titles, snapshot |
| `extractTrackingMetrics()` | Extrae métricas del DOM (Total Executions, Active Users, Error Rate) |
| `discoverTrackingTabStructure(String tabLabel, String outputPath)` | Descubre estructura DOM de un tab y exporta JSON |
| `collectTrackingTabState(String tabLabel)` | Captura estado completo de un tab como Map |
| `compareTrackingVisuals(Map current, Map previous, ...)` | Compara snapshots visuales entre runs |
| `verifyXPathPresent(String name, String xpath, int timeout)` | Verifica elemento por XPath, retorna boolean |
| `verifyXPathText(String name, String xpath, String expected, int timeout)` | Verifica texto exacto de elemento |
| `clickXPathAndKeepValidSession(String name, String xpath, int timeout)` | Click con verificación de sesión activa |

### Parámetros de validateBuildersTrackingAllDashboard
```groovy
CustomKeywords.'TempletPortalKeywords.validateBuildersTrackingAllDashboard'([
  caseId: 'TC-BUILDERS-TRACKING-ALL-001',
  platformLabel: 'Builders TEST - Tracking All',
  urlVariableName: 'BUILDERS_TEST_URL',
  fallbackUrl: 'https://testing-templet-builders.vercel.app/',
  directUrl: 'https://testing-templet-builders.vercel.app/tracking',
  snapshotLatestPath: System.getProperty('user.dir') + '/Reports/Tracking/snapshots/tracking_all_latest.json',
  snapshotHistoryDir: System.getProperty('user.dir') + '/Reports/Tracking/snapshots/history',
  positionTolerancePx: 48
])
```

### Parámetros de validateBuildersTrackingTabDashboard
```groovy
CustomKeywords.'TempletPortalKeywords.validateBuildersTrackingTabDashboard'([
  caseId: 'TC-BUILDERS-TRACKING-BLUEPRINT-001',
  platformLabel: 'Builders TEST - Tracking Blueprint',
  tabLabel: 'Blueprint',                      // 'Blueprint' | 'Task Creation' | 'Login'
  expectedTheme: 'blueprint',                  // 'blueprint' | 'task' | 'login'
  expectedDailyPrefix: 'blueprint operations per day',
  requiredCardTitles: [
    'Blueprint Work Plan',
    'Blueprint Creation Admin',
    'Blueprint Creation Poweruser',
    'Blueprint Ai Draft Generation',
    'Blueprint Task Creation Flow'
  ],
  urlVariableName: 'BUILDERS_TEST_URL',
  fallbackUrl: buildersTestUrl,
  directUrl: trackingUrl,
  snapshotLatestPath: System.getProperty('user.dir') + '/Reports/Tracking/snapshots/tracking_blueprint_latest.json',
  snapshotHistoryDir: System.getProperty('user.dir') + '/Reports/Tracking/snapshots/history'
])
```

### Cards requeridas por tab (estado actual)
| Tab | requiredCardTitles |
|---|---|
| Blueprint | Blueprint Work Plan, Blueprint Creation Admin, Blueprint Creation Poweruser, Blueprint Ai Draft Generation, Blueprint Task Creation Flow |
| Task Creation | Task Creation Non Standard Request, Task Creation One Off Request |
| Login | Login |

### Otras plataformas (keywords reutilizables)
| Método | Descripción |
|---|---|
| `runPublicLandingSmoke(Map config)` | Smoke test de landing pública |
| `runPublicSignInSmoke(Map config)` | Smoke test de login público |
| `verifyLandingVisibleObjects(Map config)` | Verifica objetos visibles en landing |
| `clickLandingObjectsAndReturnHome(Map config)` | Click en objetos y vuelve a home |
| `verifyAuthenticatedVisibleObjects(Map config)` | Objetos visibles post-login |
| `clickAuthenticatedVisibleObjectsAndReturnHome(Map config)` | Click en objetos autenticados |
| `runAuthenticatedSidebarSequence(Map config)` | Secuencia completa de sidebar autenticado |
| `verifyAuthenticatedSidebarOrderOnly(Map config)` | Solo verifica orden del sidebar |
| `validateDirectRedirect(Map config)` | Valida redirect directo a una URL |
| `validateDirectRedirectBatch(Map config)` | Batch de redirects |
| `collectPlatformState(...)` | Captura estado completo de plataforma |
| `comparePlatformStates(...)` | Compara estados entre runs |

### Utilidades DOM
| Método | Descripción |
|---|---|
| `findElementQuiet(TestObject obj, int timeout)` | Busca elemento sin lanzar excepción |
| `clickIfPresent(TestObject obj, int timeout)` | Click si presente, false si no |
| `clickFirstPresent(List candidates, int timeout)` | Click en primer elemento encontrado |
| `isPresentQuiet(TestObject obj, int timeout)` | Verifica presencia sin excepción |
| `discoverVisibleSafeClickables(List excludeTokens, int max)` | Descubre elementos clickeables en el DOM |
| `clickHamburgerIfPresent()` | Abre hamburger menu si existe |
| `sidebarItemsState(List expectedItems)` | Estado de items del sidebar |
| `discoverSidebarItems(int max, List excludeTokens)` | Descubre items del sidebar |
| `isSidebarItemVisible(String itemText)` | Verifica visibilidad de item del sidebar |
| `ensureSidebarOpenForItem(String itemText)` | Asegura sidebar abierto para item |
| `clickSidebarItemByText(String itemText)` | Click en item del sidebar por texto |

### Snapshots y Evidencia
| Método | Descripción |
|---|---|
| `captureVisualSnapshot(String alias, String objectPath, int timeout)` | Captura snapshot visual de un objeto |
| `captureCaseScreenshot(String caseName, String label)` | Screenshot con timestamp en Reports/Screenshots/ |
| `readJsonIfExists(String filePath)` | Lee JSON de snapshot, retorna Map vacío si no existe |
| `writeJsonSnapshot(String filePath, Map data)` | Escribe snapshot JSON con mkdirs automático |
| `snapshotWindowState()` | Captura estado de ventana actual |

---

## API de Keywords — CommonKeywords.groovy

```groovy
// Obtener GlobalVariable con fallback seguro
Object val = CustomKeywords.'CommonKeywords.getRequiredGlobal'('BUILDERS_TEST_URL', 'https://default.url/')

// Loguear resumen de un test case con failures y warnings
CustomKeywords.'CommonKeywords.logCaseSummary'('TC-001', failures, warnings)
```

---

## API de Keywords — AsanaErrorTicketGenerator.groovy / AsanaErrorTicketGeneratorKeyword.groovy

```groovy
// Ejecutado automáticamente por el BuildersTrackingSuiteListener
// Lee snapshots de errors en Reports/Tracking/snapshots/
// Genera Reports/asana_tickets/asana_tickets_{timestamp}.json

// Para invocar manualmente:
CustomKeywords.'AsanaErrorTicketGeneratorKeyword.processBuildersTrackingErrors'()
```

**Severidades generadas:**
- `HIGH` → failures bloqueantes (tab no encontrado, card requerida faltante)
- `MEDIUM` → warnings (elemento no crítico, screenshot fallido)
- `LOW` → informativos

---

## Test Suites Existentes

### Builders
| Suite | Path | Descripción |
|---|---|---|
| `Tracking-Full-Flow.ts` | `Test Suites/Platforms/Builders/Tracking/` | 4 TCs, isReuseDriver=true, 1 login |
| `Direct-Redirect.ts` | `Test Suites/Platforms/Builders/Redirects/` | Valida redirects directos |
| `Visible-Clicks.ts` | `Test Suites/Platforms/Builders/Objects/` | Objetos visibles y clicks |
| `Smoke.ts` | `Test Suites/Platforms/Builders/Landing/` | Smoke landing |

### Otras Plataformas
| Plataforma | Suites disponibles |
|---|---|
| Sheets | Full-Regression, Objects-Validation, List-Actions-Response, Filters, Smoke |
| Decks | Full-Regression, Objects-Validation, List-Actions-Response, Filters, Smoke |
| Email | Full-Regression, Objects-Validation, List-Actions-Response, Filters, Smoke |
| Schedulers | Smoke, Objects/Visible-Clicks |
| Cross-Platform | `CrossPlatform-Compare-Test-Prod.ts`, `CrossPlatform-Functional-Smoke.ts` |
| Full | `Full-Validation-All-Platforms.ts` |

---

## Patrones Obligatorios

### Suite integrada con reutilización de driver
```xml
<!-- En .ts suite XML -->
<isReuseDriver>true</isReuseDriver>
<!-- 1 login inicial en TC 1, los siguientes TC usan la misma sesión -->
```

### Script de test case típico (Tracking)
```groovy
// Patrón estándar en Scripts/Builders/tracking/*/Script.groovy
String buildersTestUrl = CustomKeywords.'CommonKeywords.getRequiredGlobal'('BUILDERS_TEST_URL', 'https://testing-templet-builders.vercel.app/')
String trackingUrl = buildersTestUrl.endsWith('/') ? buildersTestUrl + 'tracking' : buildersTestUrl + '/tracking'

CustomKeywords.'TempletPortalKeywords.validateBuildersTrackingTabDashboard'([...])
```

### Evitar regex JS en Groovy
```groovy
// MAL — /\s+/g no es Groovy válido
text.replaceAll(/\s+/g, ' ')

// BIEN — usar Java regex o comparación por charCode
text.replaceAll('\\s+', ' ')
// O comparar carácter a carácter con .codePointAt()
```

### Nomenclatura de test cases
```
TC-{PLATAFORMA}-{AREA}-{SUBAREA}-{NNN}
TC-BUILDERS-TRACKING-ALL-001
TC-BUILDERS-TRACKING-BLUEPRINT-001
TC-BUILDERS-TRACKING-TASK-001
TC-BUILDERS-TRACKING-LOGIN-001
```

### Failures vs Warnings
```groovy
List failures = []   // Bloqueantes → marcan TC como FAILED
List warnings = []   // Informativos → TC queda PASSED con warning

// Al final del TC:
if (failures) {
    CustomKeywords.'CommonKeywords.logCaseSummary'(caseId, failures, warnings)
    KeywordUtil.markFailedAndStop("[${caseId}] Failures: ${failures}")
} else {
    CustomKeywords.'CommonKeywords.logCaseSummary'(caseId, failures, warnings)
    KeywordUtil.logInfo("[${caseId}] PASSED" + (warnings ? " con ${warnings.size()} warnings" : ""))
}
```

---

## Selectores — Estado y Notas

Ver `Include/config/SELECTORS-REVIEW.md` para lista completa. Resumen:

**Estables (IDs de Microsoft — no tocar):**
- `//input[@name='loginfmt' or @id='i0116']` → campo usuario MS
- `//input[@id='idSIButton9']` → botón "Next/Sign in" MS

**Débiles (usar `FailureHandling.OPTIONAL` + fallback):**
- Botón login Microsoft propio: `//a[contains(normalize-space(.), 'Log in with Microsoft')]`
- Botón Create Document/Email/Initiative: texto-dependientes
- Dashboard H4: tag-dependiente

**Patrón de fallback recomendado:**
```groovy
TestObject primary = xpathObject('primary', '//button[@data-testid="btn-load-data"]')
TestObject fallback = xpathObject('fallback', '//button[normalize-space(.)="Load Data"]')
boolean found = clickFirstPresent([primary, fallback], 10)
if (!found) warnings.add('[SELECTOR] Load Data button not found')
```

---

## CI/CD — GitHub Actions

**Archivo:** `.github/workflows/builders-tracking-regression.yml`

**Estado actual:** Solo `workflow_dispatch` activo (manual). Push y schedule están comentados.

**Para activar ejecución automática** (no hacer sin autorización):
```yaml
on:
  push:
    branches: [ main, develop ]
  schedule:
    - cron: '0 2 * * *'  # Daily 2 AM UTC
```

**Secrets necesarios para CI (no configurados aún):**
- `ASANA_API_KEY`, `ASANA_PROJECT_GID`
- `TEMPLET_MS_USER`, `TEMPLET_MS_PASS`

---

## Flujo de Trabajo al Recibir una Tarea

1. **Leer objetivo y alcance exacto.**
2. **Inspeccionar archivos afectados** antes de proponer nada:
   - Para Tracking: `Keywords/TempletPortalKeywords.groovy` líneas 2394+ y 2707+
   - Para nuevo TC: script correspondiente en `Scripts/[Plataforma]/[area]/`
   - Para suite: `.ts` en `Test Suites/Platforms/[Plataforma]/`
3. **Identificar riesgos de regresión:**
   - ¿El cambio afecta `openBrowserAndLoginWithMicrosoft`? → impacta TODAS las suites
   - ¿El cambio afecta `isReuseDriver`? → puede forzar re-login innecesario
   - ¿El cambio modifica `extractTrackingMetrics`? → impacta snapshots históricos
4. **Proponer solución mínima** con diff concreto (archivo + snippet exacto).
5. **Indicar plan de validación:** qué suite ejecutar, qué logs revisar, qué snapshot comparar.

---

## Próximos Pasos Conocidos (Backlog)

1. **Activar CI/CD** — descomentar triggers en `builders-tracking-regression.yml`
2. **Integrar Asana API** — conectar `asana_tickets_*.json` con la API real de Asana
3. **Agregar data-testid** al app Builders (pedido al equipo dev, ver SELECTORS-REVIEW.md)
4. **Expandir Tracking** — agregar más tabs si se crean nuevas secciones en la app
5. **Replicar patrón Tracking** en otras plataformas (Sheets, Decks, Email tienen suites básicas)
6. **Activar schedule** diario para regression automática nocturna

---

## Formato de Respuesta Obligatorio

```
A) Resumen (máx 5 líneas)
B) Hallazgos: CRÍTICO / MEDIO / BAJO
C) Cambios propuestos: archivo + snippet listo para copiar
D) Plan de validación: pasos reproducibles
E) Riesgos de regresión
```
