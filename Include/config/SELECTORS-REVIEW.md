# 🎯 SELECTORS REVIEW — IDs Faltantes y Selectores Débiles

> **Proyecto**: Katalon QA - Templet Web Products  
> **Fecha**: 04/05/2026  
> **Acción requerida**: Solicitar al equipo de desarrollo agregar `id` o `data-testid` a los elementos listados.

---

## 📋 Resumen

Los test cases actuales dependen de **XPaths basados en texto** y **clases CSS genéricas** que son frágiles ante cambios de UI. Se documentan los selectores débiles y los IDs que el equipo dev debería agregar.

## 🔴 CRÍTICO

| Elemento | Selector actual | ID sugerido |
|---|---|---|
| Login Microsoft | `//a[contains(.,'Log in with Microsoft')]` | `id="btn-login-microsoft"` |
| Dashboard H4 | `//h4[contains(.,'Dashboard')]` | `data-testid="dashboard-title"` |
| Create Document | `//a[contains(.,'Create Document')]` | `id="btn-create-document"` |
| Create Email | `//a[contains(.,'Create Email')]` | `id="btn-create-email"` |
| Create Initiative | `//a[contains(.,'Create Initiative')]` | `id="btn-create-initiative"` |
| Log Out | `//a[contains(.,'Log Out')]` | `id="btn-logout"` |

## 🟡 MEDIO

| Elemento | Selector actual | Recomendación |
|---|---|---|
| Navbar | `id='navbarsExample07'` | Renombrar → `id="main-navbar"` |
| Select Cliente | `id='inputGroupSelect02'` | Renombrar → `id="select-client"` |
| Footer | `contains(.,'All Rights Reserved')` | `id="main-footer"` |
| Brand logo | `contains(.,'decks.templet')` | `data-testid="platform-brand"` |

## 🟢 ESTABLES

| Selector | Motivo |
|---|---|
| `//input[@id='i0116']` | Microsoft Login — controlado por MS |
| `//input[@id='idSIButton9']` | Microsoft controlled |
| `//*[@id='sortField-alpha']` | ID semántico |

## 🛠️ Template para Dev

```html
<!-- ANTES -->
<a href="/admin/create.php">Create Document</a>

<!-- DESPUÉS -->
<a href="/admin/create.php" id="btn-create-document" data-testid="create-document">Create Document</a>
```
