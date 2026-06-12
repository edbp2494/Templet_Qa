# 🎯 SELECTORS REVIEW — IDs Faltantes y Selectores Débiles

> **Proyecto**: Katalon QA - Templet Web Products  
> **Fecha**: 04/05/2026  
> **Acción requerida**: Solicitar al equipo de desarrollo agregar `id` o `data-testid` a los elementos listados.

---

## 📋 Resumen

Los test cases actuales dependen de **XPaths basados en texto** y **clases CSS genéricas** que son frágiles ante cambios de UI (reordenamientos, traducciones, refactors de CSS). A continuación se documentan los selectores débiles y los IDs que el equipo dev debería agregar.

---

## 🔴 CRÍTICO — Selectores que se romperán fácilmente

### 1. Botón de Login con Microsoft (Todas las plataformas)

| Selector actual | Problema |
|---|---|
| `//a[contains(normalize-space(.), 'Log in with Microsoft') or contains(@href,'saml/login.php')]` | Depende del texto visible. Si cambia a "Sign in with Microsoft" o se traduce, falla. |
| `a.btn-log[href*='saml/login.php']` | Clase `.btn-log` es genérica y puede cambiar. |

**✅ ID sugerido**: `id="btn-login-microsoft"` o `data-testid="login-microsoft"`

---

### 2. Dashboard H4 (Sheets, Decks, Email)

| Selector actual | Problema |
|---|---|
| `//h4[contains(normalize-space(.),'Dashboard')]` | Frágil si cambian el tag (h3, h2) o el texto ("Panel", "Home"). |

**✅ ID sugerido**: `id="page-title"` o `data-testid="dashboard-title"`

---

### 3. Botones Create Document / Create Email / Create Initiative

| Selector actual | Problema |
|---|---|
| `//a[contains(normalize-space(.),'Create Document')]` | Texto-dependiente. Un cambio a "New Document" lo rompe. |
| `//a[contains(normalize-space(.),'Create Email')]` | Idem |
| `//a[contains(normalize-space(.),'Create Initiative')]` | Idem |

**✅ IDs sugeridos**:
- `id="btn-create-document"` o `data-testid="create-document"`
- `id="btn-create-email"` o `data-testid="create-email"`
- `id="btn-create-initiative"` o `data-testid="create-initiative"`

---

### 4. Log Out link

| Selector actual | Problema |
|---|---|
| `//a[contains(normalize-space(.),'Log Out')]` | Texto-dependiente. Puede cambiar a "Logout", "Sign out", "Cerrar sesión". |

**✅ ID sugerido**: `id="btn-logout"` o `data-testid="logout"`

---

### 5. Labels de filtros (Client, Initiative, Sort)

| Selector actual | Problema |
|---|---|
| `//label[contains(normalize-space(.),'Client')]` | Depende del texto del label. |
| `//label[contains(normalize-space(.),'Initiative')]` | Idem |
| `//label[contains(normalize-space(.),'Sort')]` | Idem |

**✅ IDs sugeridos**:
- Label: `for="filter-client"` → Select: `id="filter-client"`
- Label: `for="filter-initiative"` → Select: `id="filter-initiative"`
- Label: `for="filter-sort"` → Select: `id="filter-sort"`

---

## 🟡 MEDIO — Selectores parcialmente estables

### 6. Navbar

| Selector actual | Problema |
|---|---|
| `//*[@id='navbarsExample07']` | ✅ Tiene ID. Estable. Pero el nombre "Example07" sugiere que es un placeholder. |
| `//*[contains(@class,'navbar') and not(contains(@class,'navbar-toggler'))]` | Clase genérica Bootstrap, podría matchear múltiples elementos. |

**✅ ID sugerido**: Renombrar a `id="main-navbar"` o `data-testid="navbar"`

---

### 7. Selects de filtrado

| Selector actual | Estado |
|---|---|
| `//*[@id='inputGroupSelect02']` | ✅ Tiene ID pero nombre genérico Bootstrap. |
| `//*[@id='sortField-alpha']` | ✅ Tiene ID semántico. Estable. |

**✅ Recomendación**: Renombrar `inputGroupSelect02` → `id="select-client"` (semántico)

---

### 8. Placeholders de texto

| Selector actual | Problema |
|---|---|
| `//*[contains(normalize-space(.),'Select Client')]` | Depende del placeholder text. |
| `//*[contains(normalize-space(.),'Select a client first')]` | Idem |
| `//*[contains(normalize-space(.),'Newest')]` | Depende del valor por defecto del sort. |

**✅ Recomendación**: Agregar `data-testid` a los contenedores de estos selects.

---

### 9. Footer

| Selector actual | Problema |
|---|---|
| `//*[contains(normalize-space(.),'All Rights Reserved') and contains(normalize-space(.),'Terms')]` | Frágil ante cambios legales en el texto. |

**✅ ID sugerido**: `id="main-footer"` o `data-testid="footer"`

---

### 10. Brand/Logo por plataforma

| Selector actual | Problema |
|---|---|
| `//*[contains(normalize-space(.),'decks.templet')]` | Texto-dependiente |
| `//*[contains(normalize-space(.),'email.templet')]` | Idem |

**✅ ID sugerido**: `id="brand-logo"` o `data-testid="platform-brand"`

---

## 🟢 ESTABLES — Selectores que ya son buenos

| Selector | Motivo |
|---|---|
| `//input[@name='loginfmt' or @id='i0116']` | Microsoft Login (controlado por MS, estable) |
| `//*[@id='sortField-alpha']` | ID semántico |
| `//input[@id='idSIButton9']` | Microsoft controlled, estable |

---

## 📊 Resumen por Plataforma

| Plataforma | Selectores débiles | IDs faltantes | Prioridad |
|---|---|---|---|
| **Sheets** | 8 | 6 | Alta |
| **Decks** | 9 | 7 | Alta |
| **Email** | 9 | 7 | Alta |
| **Common (Login)** | 2 | 1 | Crítica |

---

## 🛠️ Acción para Desarrollo

### Template de implementación (HTML):

```html
<!-- ANTES (frágil) -->
<a href="/admin/create.php">Create Document</a>

<!-- DESPUÉS (robusto para QA) -->
<a href="/admin/create.php" id="btn-create-document" data-testid="create-document">Create Document</a>
```

### Convención sugerida para `data-testid`:

```
data-testid="[accion]-[componente]"

Ejemplos:
- data-testid="login-microsoft"
- data-testid="create-document"
- data-testid="filter-client"
- data-testid="nav-logout"
- data-testid="platform-brand"
- data-testid="page-title"
```

---

## ⏳ Impacto

- **Sin IDs**: Cada cambio de texto o estructura HTML puede romper los tests.
- **Con IDs**: Los tests sobreviven refactors de UI, cambios de texto y redesigns.
- **Esfuerzo dev estimado**: ~2-3 horas para agregar `data-testid` a todos los elementos listados.
- **Beneficio**: Tests 90% más estables, menos mantenimiento QA.

---

## 📝 Notas

- Los selectores actuales funcionan con `FailureHandling.OPTIONAL` para no bloquear la ejecución.
- Se usa `normalize-space()` para tolerar espacios/saltos de línea extra.
- El flujo Microsoft SSO es estable porque usa IDs controlados por Microsoft (`idSIButton9`, `i0116`, etc.)


---

## Pedido consolidado de data-testid (Fase 2, 2026-06-12)
Generado desde Include/config/element-maps/*.json. Un data-testid por elemento estabiliza el selector primario y elimina los fallbacks por texto.

### repos/templet-builders
| Pantalla | Elemento | Archivo fuente |
|---|---|---|
| Brand List | `heading_active_brands` | `app/(logged)/brand/page.tsx:112` |
| Brand List | `btn_new_brand` | `app/(logged)/brand/page.tsx:132` |
| Convert PPTX | `input_pptx_file` | `app/(logged)/convert/page.tsx:176` |
| Current Spend | `heading_current_spend` | `components/layouts/track/current-spend.tsx:222` |
| Current Spend | `input_search` | `components/layouts/track/current-spend.tsx:396` |
| Layout - Create Options | `heading_create_layout` | `app/(logged)/layout/page.tsx:139` |
| Layout - Create Options | `option_buttons` | `app/(logged)/layout/page.tsx:133` |
| Project Schedule | `heading_project_schedule` | `app/(logged)/project-schedule/page.tsx` |
| Project Schedule | `timeline_grid` | `app/(logged)/project-schedule/page.tsx` |
| Task Creation - Content | `tab_new_content` | `app/(logged)/task-creation/content/page.tsx` |
| Task Creation - Content | `tab_new_edit` | `app/(logged)/task-creation/content/page.tsx` |
| Task Creation - Non Standard | `heading_nonstandard_tasks` | `app/(logged)/task-creation/non-standard/page.tsx` |
| Task Creation - Non Standard | `btn_new_sub_task` | `app/(logged)/task-creation/non-standard/page.tsx` |
| Task Creation - Non Standard | `btn_set_it_up` | `app/(logged)/task-creation/non-standard/page.tsx` |
| Task Creation - Non Standard | `select_client` | `app/(logged)/task-creation/non-standard/page.tsx` |
| Template List | `heading_active_templates` | `app/(logged)/template/page.tsx` |
| Template List | `btn_new_template` | `app/(logged)/template/page.tsx` |
| Work in Progress | `heading_wip` | `app/(logged)/work-in-progress/page.tsx` |
| Work in Progress | `col_collateral` | `app/(logged)/work-in-progress/page.tsx` |
| Work in Progress | `col_status` | `app/(logged)/work-in-progress/page.tsx` |

### repos/templet-schedulers
| Pantalla | Elemento | Archivo fuente |
|---|---|---|
| Daily Pulse | `heading_daily_pulse` | `components/daily-pulse/DailyPulseDashboard.tsx:71` |
| Initiatives | `heading_initiatives` | `app/initiatives/page.tsx:231` |
| Initiatives | `btn_save` | `app/initiatives/page.tsx:239` |
| Metrics Dashboard | `heading_metrics` | `components/metrics/MetricsPage.tsx:172` |
| Metrics Dashboard | `btn_load_data` | `components/metrics/MetricsPage.tsx:241` |
| Requests | `heading_requests` | `components/requests/Requests.tsx:674` |
| Requests | `input_search` | `components/requests/FilterOptions.tsx` |
| Resources | `heading_resources` | `components/resources/Resources.tsx:426` |
| Resources | `team_section` | `components/resources/Resources.tsx:466` |
| Summary (Task creation log) | `heading_summary` | `app/summary/page.tsx:52` |
| Summary (Task creation log) | `input_search` | `components/summary/SummarySection.tsx:257` |
| Summary (Task creation log) | `col_type_sortable` | `components/summary/SummarySection.tsx:330` |
| Task Creation - Content | `tab_new_content` | `app/task-creation/content/page.tsx:353` |
| Task Creation - Content | `tab_new_edit` | `app/task-creation/content/page.tsx:356` |
| Task Creation - Edits | `select_client` | `app/task-creation/edits/page.tsx:536` |
| Task Creation - Edits | `select_project` | `app/task-creation/edits/page.tsx:553` |
| Task Creation - Edits | `btn_set_it_up` | `app/task-creation/edits/page.tsx:760` |
| Task Creation - Non Standard | `heading_nonstandard` | `app/task-creation/non-standard/page.tsx:532` |
| Task Creation - Non Standard | `btn_new_sub_task` | `app/task-creation/non-standard/page.tsx:685` |
| Task Creation - Standard | `select_client` | `app/task-creation/standard/page.tsx:627` |
| Task Creation - Standard | `select_project` | `app/task-creation/standard/page.tsx:651` |
| Task Creation - Standard | `btn_set_it_up` | `app/task-creation/standard/page.tsx:932` |
| Execution Tracking | `heading_execution_tracking` | `components/tracking/TrackingPage.tsx:158` |
| Execution Tracking | `select_month` | `components/tracking/TrackingPage.tsx:164` |
| Execution Tracking | `btn_load_data` | `components/tracking/TrackingPage.tsx:198` |
