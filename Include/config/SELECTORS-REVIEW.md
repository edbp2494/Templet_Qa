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
