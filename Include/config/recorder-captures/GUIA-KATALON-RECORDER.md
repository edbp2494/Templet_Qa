# Guía — Configurar Katalon Recorder igual al proyecto Templet QA

> Objetivo: usar **Katalon Recorder** (extensión de navegador) solo como **capturador de elementos / grabador de flujos**.
> El Recorder NO ejecuta Groovy ni los keywords del proyecto. Lo que hace es darte selectores y un script crudo que
> luego entra al pipeline de skills: `katalon-element-mapper` → `katalon-testcase-creator` → `katalon-optimizer`.

---

## 0. Cómo encaja en el proyecto (el flujo completo)

```
[1] Katalon Recorder  ──graba el flujo en la app Templet──►  exporta archivo crudo
        │
        ▼
[2] Guardas el export en:  Include/config/recorder-captures/<plataforma>/<pantalla>.html
        │
        ▼
[3] Skill katalon-element-mapper  ──lee XPaths del export──►  Include/config/element-maps/<plataforma>-<pantalla>.json
        │
        ▼
[4] Skill katalon-testcase-creator  ──lee el JSON──►  Scripts/<Plataforma>/<area>/Script.groovy  + Test Case XML
        │
        ▼
[5] Skill katalon-optimizer  ──refactoriza──►  extrae lógica a TempletPortalKeywords.groovy, refuerza selectores
```

Tú solo haces los pasos **[1] y [2]** a mano. Los pasos [3]–[5] me los pides en el chat y yo los corro con las skills.

---

## 1. Instalar y abrir

1. Instala la extensión **Katalon Recorder** (Chrome Web Store o Firefox Add-ons).
2. Ábrela con el icono de la barra o `Ctrl/Cmd + Shift + sí` según tu navegador.
3. Verás 3 columnas: **Command / Target / Value** y abajo las pestañas **Log / Screenshots / Variables / Reference / Self-healing**.

---

## 2. Configurar el Recorder "igual al proyecto"

### 2.1 Estrategia de selectores → priorizar XPath (clave)

El proyecto Templet usa **XPath con esta jerarquía**: `@data-testid` > `@id` > `@aria-label` > estructura semántica con `normalize-space(.)`. Por defecto el Recorder prefiere `id`/`css`, así que hay que reordenar.

1. Menú **More options → Settings** (o el icono ⚙ / los tres puntos).
2. Busca la sección de **Locator Builders** / orden de targets.
3. Arrastra hacia arriba, en este orden:
   1. `xpath:attributes`  (captura `data-testid`, `name`, etc.)
   2. `xpath:idRelative`
   3. `id`
   4. `name`
   5. `xpath:position` (último recurso — frágil)
4. Esto hace que, al grabar, el **Target** que elija por defecto sea un XPath, que es lo que consume el mapper.

> Tip: aunque el Target por defecto no sea XPath, cada celda **Target** tiene un desplegable ▾ que muestra TODAS las alternativas (`xpath=...`, `css=...`, `id=...`). Siempre puedes elegir la versión XPath ahí.

### 2.2 Self-healing

Déjalo activado (pestaña **Self-healing**): si un selector cambia, el Recorder propone uno nuevo y lo verás. Útil para detectar selectores frágiles ANTES de meterlos al proyecto.

### 2.3 Variables de ambiente (las URLs del proyecto)

El proyecto maneja las URLs como GlobalVariables. En el Recorder puedes replicarlas con comandos `store` al inicio del test, para no escribir URLs a mano:

| Command | Target | Value |
|---|---|---|
| `store` | `https://testing-templet-builders.vercel.app/` | `BUILDERS_TEST_URL` |
| `store` | `https://sheets-test.templet.io/admin/manager.php` | `SHEETS_TEST_URL` |
| `store` | `https://decks-test.templet.io/admin/manager.php` | `DECKS_TEST_URL` |
| `store` | `https://emails-test.templet.io/admin/manager.php` | `EMAIL_TEST_URL` |
| `store` | `https://testing-templet-schedulers.vercel.app/` | `SCHEDULERS_TEST_URL` |

Luego usas `open` con `${BUILDERS_TEST_URL}`. Las verás en la pestaña **Variables** durante la ejecución.

> Las URLs de la tabla salen de `CLAUDE.md` (sección GlobalVariables). Si cambian, actualízalas ahí también.

---

## 3. Grabar un flujo

1. Abre la app de la plataforma que vas a testear (ej. el dashboard de Tracking de Builders).
2. Pulsa **Record** (círculo rojo).
3. Haz las acciones reales: navegar al tab, abrir un panel, etc.
   - **OJO con acciones destructivas:** no hagas clic en botones que crean datos reales (`Set it up!`, submits de tareas/emails). Solo navega y abre. El proyecto los marca como `verify_present`, no `click`.
4. Pulsa **Record** otra vez para parar.
5. Revisa la tabla **Command / Target / Value**: cada fila es una acción con su selector capturado.

### Login Microsoft SSO

El login MS tiene IDs estables que el proyecto ya conoce — no necesitas grabarlos con cuidado, pero si los grabas, el Target ideal es:
- Usuario: `//input[@name='loginfmt' or @id='i0116']`
- Botón Next/Sign in: `//input[@id='idSIButton9']`

---

## 4. Exportar el script a la carpeta ideal

### 4.1 Formato recomendado

En el panel izquierdo: **Export**. Elige el formato:

- **Katalon Recorder (.html)** ← **recomendado**. Es el formato nativo, conserva todos los selectores y es el más fácil de leer para el mapper.
- Alternativa: **JUnit/Java (Selenium WebDriver)** si prefieres ver XPaths embebidos en código.

> No necesitas que el Recorder genere Groovy/Katalon Studio directamente — ese paso lo hace el pipeline de skills, que produce código que respeta los patrones del proyecto (`TempletPortalKeywords`, failures vs warnings, etc.).

### 4.2 Dónde guardarlo (carpeta ideal)

Guarda el export en la carpeta nueva creada para esto, **una subcarpeta por plataforma**:

```
Include/config/recorder-captures/
├── builders/        ← capturas de Builders
├── sheets/
├── decks/
├── email/
├── schedulers/
└── GUIA-KATALON-RECORDER.md   ← este archivo
```

**Convención de nombre** (igual que los element-maps existentes, kebab-case):

```
<plataforma>-<pantalla>.html
```

Ejemplos:
- `builders/builders-tracking-all.html`
- `builders/builders-task-creation-non-standard.html`
- `sheets/sheets-admin-surface.html`

Así el archivo crudo del Recorder queda emparejado con el JSON que generará el mapper en `Include/config/element-maps/<mismo-nombre>.json`.

---

## 5. Pasar la captura al pipeline (me lo pides en el chat)

Una vez tengas el export guardado, escríbeme algo como:

> "Mapea los elementos de `recorder-captures/builders/builders-tracking-all.html`"

Yo entonces:
1. Corro **katalon-element-mapper** → genera `Include/config/element-maps/builders-tracking-all.json` (con `xpath_primary`, `xpath_fallback`, `selector_strength`, `assertion_type`, y la lista `needs_data_testid`).
2. Si me dices "crea el test case", corro **katalon-testcase-creator** → `Scripts/Builders/.../Script.groovy` + Test Case XML + entrada en el `.ts` suite.
3. Si hay código repetido o selectores débiles, corro **katalon-optimizer** → extrae a `TempletPortalKeywords.groovy` y refuerza con fallbacks.

---

## 6. Checklist rápido

- [ ] Locator Builders reordenado para priorizar `xpath:attributes` / `xpath:idRelative`
- [ ] Variables de URL cargadas con `store` (opcional pero recomendado)
- [ ] Flujo grabado SIN acciones destructivas (no submits reales)
- [ ] Export en formato `.html` nativo
- [ ] Guardado en `Include/config/recorder-captures/<plataforma>/<plataforma>-<pantalla>.html`
- [ ] Pedirme en el chat: "mapea los elementos de <archivo>"

---

## Notas

- **Git:** la carpeta `recorder-captures/` NO está en `.gitignore` todavía. Si no quieres versionar exports crudos, dime y te propongo la línea para agregar (recuerda: yo no hago `git add/commit/push` sin tu autorización — regla del proyecto).
- **Recorder ≠ Studio:** el Recorder no puede ejecutar las suites `.ts` ni los keywords. Su único rol aquí es capturar selectores rápido. La ejecución real sigue en Katalon Studio.
