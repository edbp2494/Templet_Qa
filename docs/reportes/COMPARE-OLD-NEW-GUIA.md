# Comparación Legacy vs Migración TS — Builder (Guía de ejecución)

Compara **legacy** `https://builder.templet.io/` contra **testing migración TS**
`https://testing-templet-builder-saas.vercel.app/` para detectar discrepancias
de estilo/diseño/comportamiento no contempladas como cambio intencional.

## Piezas

| Pieza | Ruta | Rol |
|---|---|---|
| Suite | `Test Suites/Platforms/Builders/Compare-Old-New.ts` | Corre el TC de captura |
| Test Case | `Test Cases/Builders/compare-old-new-app` | Login + navegación + screenshots pareados |
| Script captura | `Scripts/Builders/compare-old-new-app/Script.groovy` | Lógica de captura (21 módulos, 7 BCs) |
| Analizador | `Scripts/compare_screenshots.py` | Diff visual → heatmaps + JSON + HTML |
| Salida capturas | `Reports/Builders/Compare/` | `vieja-XX.png` / `nueva-XX.png` |
| Salida reporte | `Reports/Builders/Compare/analysis/` | `COMPARISON-REPORT.html`, `comparison-results.json`, heatmaps |

## Cobertura por BC (módulo → ruta, igual en ambas apps)

> La migración **renombró rutas** (singular → plural). El script usa rutas por-app (`oldPath`/`newPath`).

| BC | Módulos | Ruta legacy | Ruta nueva (deployada) |
|---|---|---|---|
| BC-01 Brand Properties | 02 lista, 03 detalle, 04-06 tabs | `/brand` | `/brand-properties` (lista) · `/brand/{id}` (detalle) |
| BC-02 Brand Layouts | 07 lista, 08 detalle* | `/layout` | `/layout` (igual) |
| BC-03 Templates | 09 lista, 10 detalle | `/template` | `/templates` |
| BC-04 Task Creation | 11 One-Off, 12 Non-Standard | `/task-creation/content` · `/task-creation/non-standard` | igual |
| BC-08 Layout Generation | 13 create, 14 upload | `/layout/create` · `/layout/upload` | igual |
| BC-09 Initiative Mgmt | 15 lista, 16 detalle, 17 requests | `/blueprint/manager/power-user` | `/blueprints` (Initiatives se gestionan dentro de Blueprints) |
| BC-10 File Delivery | 18 convert | `/convert` | ⚠️ `/convert` da 404 — ruta real a confirmar |
| Contexto | 01 home, 19-21 track | iguales | iguales |

\* El módulo 08 (Layout detail) sólo se captura si seteás `LAYOUT_ID` en el script.

## Cómo ejecutar

1. **Katalon Studio** (ejecuta desde `C:\Users\e2494\Katalon Studio\Templet`):
   abrir `Test Suites/Platforms/Builders/Compare-Old-New.ts` → Run (Chrome).
   Hace login MS SSO en cada app y guarda `vieja-*` / `nueva-*` en `Reports/Builders/Compare/`.
   Las capturas del run anterior se archivan solas en `Reports/Builders/Compare/_prev_<timestamp>/`.
2. **Regenerar el reporte visual**:
   ```
   pip install opencv-python numpy
   python Scripts/compare_screenshots.py
   ```
   Abrir `Reports/Builders/Compare/analysis/COMPARISON-REPORT.html`.

## Configuración

- **Fixtures de detalle** (en `Script.groovy`, deben ser válidos en AMBAS apps): `BRAND_ID`, `TEMPLATE_ID`, `BLUEPRINT_ID` ya cargados; `LAYOUT_ID` vacío → setealo para incluir Layout detail.
- **Sidebar excluido del diff**: `IGNORE_LEFT_PX = 260` en `compare_screenshots.py` descarta la franja izquierda (nav rediseñado = cambio intencional) para no inflar el diff%. Poner `0` para comparar la imagen completa.
- **Severidad**: >15% CRÍTICO, >5% MEDIO, resto BAJO. Archivo `<20KB` = posible 404/loading.

## Qué se arregló respecto al run del 04-jul

- Rutas de la app nueva estaban inventadas (`brand-properties`, `templates`, `blueprints`, `one-off-request`, `financial-summary`) → **404**. Ahora usan las rutas reales del código (`/brand`, `/template`, `/layout`, `/blueprint`, `/task-creation/*`, `/current-spend`).
- Tabs de Brand (Colors/Samples/Techtionary) salían idénticos → ahora se **clickean** (no dependen de `?tab=`).
- Se agregaron los 3 BCs faltantes: **Layout Generation, Initiatives (vía Blueprint) y File Delivery**.
- El diff ya no se infla por el rediseño del sidebar.

## Caveats a verificar en el primer run

- Rutas nuevas de la app **legacy** para features posiblemente nuevas (`layout/create`, `layout/upload`, `convert`, `blueprint/{id}/requests`): si no existen en legacy, el módulo avisa (warning) y captura sólo el lado nuevo — eso ya es un hallazgo (feature nueva de la migración).
- Los IDs de detalle asumen mismo backend/ID en ambas apps; si el detalle da 404, ajustar el fixture.

## Resultado 1ª corrida (08-jul-2026)

Suite **PASSED**: 40 capturas (20 vieja + 20 nueva), 0 failures. El análisis mostró que 7 rutas de la app nueva devolvían **404 ("This page could not be found")** porque el test usaba rutas legacy; el dashboard y el nav de la nueva muestran esas features vivas y con datos (Brands 62, Blueprints 41, Initiatives 973), así que **son renombres de ruta, no regresiones**. Las rutas ya se corrigieron arriba (`oldPath`/`newPath`); re-ejecutar para capturar los 7 BCs completos. Pendiente confirmar la ruta real de File Delivery (`/convert` → 404).

## Cierre (pendiente, requiere autorización)

Tras correr y revisar el reporte, si no hay discrepancias bloqueantes: comentar el resumen por módulo en la tarea **"Setup & scaffolding del proyecto"** y marcar OK para Manuel/Randy. No se ejecuta sin autorización explícita (regla CLAUDE.md).
