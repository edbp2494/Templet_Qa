# Proyecto QA Katalon - Estructura Canonica (Mayo 2026)

## Estado actual

- Proyecto activo: C:/Users/e2494/Katalon Studio/Templet/Editores/Sheet
- Archivo de proyecto: Sheets.prj
- Enfoque: suites por plataforma + colecciones cross-platform.
- Regla de mantenimiento: mantener una sola suite/coleccion canonica por objetivo.

## Plataformas y alcance real

- Sheets (admin): smoke, objetos sidebar/list view, filtros, test-vs-prod.
- Decks (admin): smoke funcional, objetos sidebar/list view, filtros, test-vs-prod.
- Email (admin): smoke funcional, objetos sidebar/list view, filtros, test-vs-prod.
- Builders (landing TEST): smoke y validacion de sidebar visible/click.
- Schedulers (landing TEST): smoke y validacion de sidebar visible/click.

## Estructura simple recomendada

```text
Sheet/
|-- Keywords/
|   |-- CommonKeywords.groovy
|   |-- TempletPortalKeywords.groovy
|   `-- ObjectCaptureKeywords.groovy
|-- Object Repository/
|   |-- Sheets/
|   |-- Decks/
|   `-- Email/
|-- Test Cases/
|   |-- Sheets/
|   |-- Decks/
|   |-- Email/
|   |-- Builders/
|   `-- Schedulers/
`-- Test Suites/
    |-- Platforms/
    `-- Collection-*.tsc
```

## Suites canonicas (mantener)

### Por plataforma

- Test Suites/Platforms/Sheets/Sheets-Full-Regression-ReuseDriver.ts
- Test Suites/Platforms/Decks/Decks-Full-Regression-ReuseDriver.ts
- Test Suites/Platforms/Email/Email-Full-Regression-ReuseDriver.ts
- Test Suites/Platforms/Sheets/Objects/Visible-Clicks.ts
- Test Suites/Platforms/Decks/Objects/Visible-Clicks.ts
- Test Suites/Platforms/Email/Objects/Visible-Clicks.ts
- Test Suites/Platforms/Builders/Landing/Smoke.ts
- Test Suites/Platforms/Schedulers/Landing/Smoke.ts

### Colecciones maestras (siempre 3)

- Test Suites/Collection-Builder.tsc
- Test Suites/Collection-Schedulers.tsc
- Test Suites/Collection-Email-Sheets-Decks.tsc

## Convenciones de escalabilidad

1. No crear .ts y .tsc para la misma coleccion. Usar solo .tsc.
2. No guardar archivos boilerplate .groovy dentro de Test Suites.
3. Mantener scripts por caso en Scripts/<Plataforma>/... y metadatos en Test Cases/<Plataforma>/... .
4. Evitar artefactos de grabacion (por ejemplo, carpetas Page_* o Screenshots/Targets) en repositorio.
5. Si una suite es "DEPRECATED", eliminarla en el mismo cambio para no dejar ruido.

## Aprendizajes y prevencion (Mayo 2026)

1. En Katalon, no crear colecciones funcionales como scripts `.ts` Groovy; las colecciones deben ser `.tsc` (XML).
2. Evitar duplicados `.ts` y `.tsc` con el mismo objetivo. Mantener solo la variante canonica.
3. Alinear siempre `testCaseId` de suites con el archivo real existente en `Test Cases/`.
4. Alinear el `<name>` interno del `.tc` con su nombre de archivo para evitar resoluciones ambiguas del planner.
5. Si aparece `Failed to plan execution ... test case not found`, validar primero referencias en suites y luego refrescar proyecto.
6. Tracking pertenece a Builders; no modelarlo como plataforma separada en colecciones maestras.

Checklist rapido antes de ejecutar una coleccion:
1. Verificar que todos los `testSuiteEntity` existan.
2. Verificar que los `testCaseId` de cada suite existan.
3. Confirmar que no haya pares duplicados `.ts`/`.tsc` para la misma coleccion.
4. Ejecutar Refresh del proyecto en Katalon antes de la corrida final.

## Ejecucion recomendada

El flujo principal se mantiene manual desde Katalon Studio:

1. Abrir CrossPlatform-Full-Regression-ReuseDriver.tsc.
2. Ejecutar con profile ENV_TEST.
3. Revisar reportes en Reports/.

Opcional: run-regression.ps1 para ejecucion por script local.

## Mantenimiento de reportes

- Script local: `run-report-retention.ps1`
- Politica por defecto:
  - evidencia transitoria en Reports/: 45 dias
  - evidencia importante en Reports/Important, Reports/Tracking/important, Reports/asana_tickets y Reports/Visual/Baselines: 180 dias
  - snapshots latest de Tracking y discovery JSON: protegidos
- Limpieza inicial de ruido transitorio:
  - `./run-report-retention.ps1 -AggressiveTransientCleanup`
- Limpieza periodica:
  - `./run-report-retention.ps1`

## Instrucciones de Buenas Noches

1. Ejecutar la optimizacion de buenas noches.
2. Luego de la optimizacion de buenas noches, agregar la regresion a la carpeta de estabilizacion y avisar para ejecutar.
3. Ejecutar la coleccion: Test Suites/Estabilizacion/Regresion-Estabilizacion.tsc con profile ENV_TEST.

## Estabilizacion

- La coleccion de estabilizacion debe mantenerse simplificada: un solo suite por bloque funcional unico, sin duplicar TCs cubiertos por otros suites.
- Prioridad: cross-platform smoke, smoke por plataforma, objects visible-clicks, filtros y acciones listadas una sola vez.
- Coleccion full recomendada para corrida final: `Test Suites/Estabilizacion/Collection-Full-Validation-v2.tsc`.

## Notas

- Este README prioriza estructura y gobernanza. Para detalle tecnico de cada caso, revisar el script de cada test case en Scripts/.
2. Inspecccionar elemento
3. Copiar XPath → Actualizar Object Repository XML
4. Guardar y re-run

---

# Builders Tracking Suite (Katalon)

## Plataformas revisadas
- **Builders Tracking** (`/tracking`)
  - Tabs: All, Blueprint, Task Creation, Login

## ¿Qué valida la suite?
- Título y subtítulo dinámico
- Switch Production (apagado por defecto)
- Botón Load Data
- Cards principales (títulos, métricas)
- Secciones: Daily Executions, Executions by Trace, Error Rate by Trace, Top Active Users
- Headers de tabla tras scroll
- Color y clase del tab activo/inactivo
- Chips y elementos clave
- Comparación visual con baseline (snapshots JSON)
- Descubrimiento DOM automático (export a JSON)
- Scroll automático para forzar render de elementos
- Reutilización de navegador (suite integrada, 1 login)

## Estructura del proyecto
- **Keywords/**: Custom Keywords reutilizables
- **Test Cases/**: Casos de prueba por tab
- **Test Suites/**: Suite integrada `Tracking-Full-Flow.ts` (All → Blueprint → Task Creation → Login)
- **Reports/Tracking/discovery/**: Ejemplo de descubrimiento DOM (no subir datos reales)
- **Reports/Tracking/snapshots/**: Snapshots visuales (no subir datos reales)

## ¿Qué deben revisar los devs?
- Que los elementos clave sean visibles en DOM aunque no estén en viewport
- Que los tabs tengan clases/atributos únicos para distinguir activo/inactivo
- Que los textos de cards y headers sean consistentes
- Si hay lazy loading/render condicional, que se pueda forzar render con scroll
- Sugerir selectores robustos (clases, data-attributes, roles)

## ¿Qué archivos NO subir?
- Reportes reales, screenshots, credenciales, archivos temporales
- Ver `.gitignore` para reglas

## Patrón de suite integrada
- Documentado en `katalon-integrated-suite-pattern.md`
- 1 login por suite, reutiliza navegador

---

**¿Dudas o mejoras?**
- QA: Revisar archivos de descubrimiento y proponer selectores
- Dev: Sugerir hooks o atributos para automatización

