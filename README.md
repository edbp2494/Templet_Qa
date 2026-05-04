# Katalon QA — Templet Web Products

Proyecto de automatización QA en **Katalon Studio (Groovy)** para los portales web de Templet (templet.io).

## 🎯 Plataformas cubiertas

| Plataforma | TEST URL | PROD URL | Estado |
|---|---|---|---|
| **Sheets** | `sheets-test.templet.io/admin/manager.php` | `sheets.templet.io/admin/manager.php` | ✅ Activo |
| **Decks** | `decks-test.templet.io/admin/manager.php` | `deck.templet.io/admin/manager.php` | ✅ Activo |
| **Email** | `emails-test.templet.io/admin/manager.php` | `email.templet.io/admin/manager.php` | ✅ Activo |
| **Media** | `media-test.templet.io/admin/manager.php` | `media.templet.io/admin/manager.php` | ⚠️ TEST no deployado — omitido automáticamente |

## 🚀 Quick Start — Suite recomendada para uso diario

Abre Katalon Studio y ejecuta:
```
Test Suites/Collection-Smoke-All-Platforms
```
Ejecuta los 4 smokes en secuencia (Chrome). Si un caso falla, continúa con los siguientes.

## 📊 Suites disponibles

| Suite | Contenido | Uso |
|---|---|---|
| `Collection-Smoke-All-Platforms` | 4 smokes en secuencia | **Validación diaria** ⭐ |
| `Smoke-Sheets` | TC-000, TC-010, TC-003 | Solo Sheets |
| `Smoke-Decks` | TC-011, TC-004 | Solo Decks |
| `Smoke-Email` | TC-012, TC-005 | Solo Email |
| `Smoke-Media` | TC-013, TC-006 | Solo Media (omite si TEST no existe) |
| `Suite-Sheets-Phase1-MVP` | TC-000, TC-001, TC-002, TC-003, TC-010 | Regression Sheets completo |
| `Suite-CrossPlatform-Compare-TEST-PROD` | TC-003, TC-004, TC-005, TC-006 | Comparación TEST vs PROD |
| `Suite-CrossPlatform-Functional-Smoke-Fase2` | TC-010, TC-011, TC-012, TC-013 | Smoke funcional multi-plataforma |
| `Collection-Full-Regression` | Phase1 + Compare + Smoke Fase2 | Regresión completa |

## 📝 Test Cases

| ID | Nombre | Plataforma | Tipo |
|---|---|---|---|
| TC-000 | TC-SHEETS-SMOKE-000 | Sheets | Smoke apertura (sin login) |
| TC-001 | TC-CROSS-URLS-OBJECTS-001 | Todas | Verificar URLs + elementos login |
| TC-002 | TC-CROSS-URLS-LOGIN-002 | Todas | Login Microsoft en todas las URLs |
| TC-003 | TC-SHEETS-TEST-PROD-OBJECTS-003 | Sheets | Comparar UI TEST vs PROD |
| TC-004 | TC-DECKS-TEST-PROD-OBJECTS-004 | Decks | Comparar UI TEST vs PROD |
| TC-005 | TC-EMAIL-TEST-PROD-OBJECTS-005 | Email | Comparar UI TEST vs PROD |
| TC-006 | TC-MEDIA-TEST-PROD-OBJECTS-006 | Media | Comparar UI TEST vs PROD (omite si no hay TEST) |
| TC-010 | TC-SHEETS-FUNCTIONAL-SMOKE-010 | Sheets | Login, dashboard, crear, logout |
| TC-011 | TC-DECKS-FUNCTIONAL-SMOKE-011 | Decks | Login, dashboard, crear, logout |
| TC-012 | TC-EMAIL-FUNCTIONAL-SMOKE-012 | Email | Login, dashboard, crear, logout |
| TC-013 | TC-MEDIA-FUNCTIONAL-SMOKE-013 | Media | Login, elementos, logout (omite si no hay TEST) |

## 🔐 Configuración de credenciales

Copia el archivo ejemplo y completa con las credenciales reales:

```bash
cp Include/config/templet-credentials.properties.example Include/config/templet-credentials.properties
```

Edita `templet-credentials.properties`:
```properties
MS_USER=usuario@dominio.com
MS_PASS=tu_password
```

> ⚠️ `templet-credentials.properties` está en `.gitignore` y nunca se sube al repo.

Alternativamente, define las variables de entorno:
```
TEMPLET_MS_USER=usuario@dominio.com
TEMPLET_MS_PASS=tu_password
```

## 🐛 Media TEST no deployado

Hasta que `media-test.templet.io` sea deployado, TC-013 y TC-006 se omiten automáticamente con status PASSED + aviso en el log:
```
TC-MEDIA-FUNCTIONAL-SMOKE-013 OMITIDO — Media TEST no deployado aún.
```

Cuando Media TEST esté disponible, elimina el bloque DNS pre-check de los scripts correspondientes.

## 📁 Estructura del proyecto

```
Sheets.prj
Keywords/
  TempletPortalKeywords.groovy   ← SSO login, credential resolution, comparePlatformStates
  CommonKeywords.groovy
Scripts/Sheets/
  TC-SHEETS-SMOKE-000/
  TC-CROSS-URLS-OBJECTS-001/
  TC-CROSS-URLS-LOGIN-002/
  TC-SHEETS-TEST-PROD-OBJECTS-003/
  TC-DECKS-TEST-PROD-OBJECTS-004/
  TC-EMAIL-TEST-PROD-OBJECTS-005/
  TC-MEDIA-TEST-PROD-OBJECTS-006/
  TC-SHEETS-FUNCTIONAL-SMOKE-010/
  TC-DECKS-FUNCTIONAL-SMOKE-011/
  TC-EMAIL-FUNCTIONAL-SMOKE-012/
  TC-MEDIA-FUNCTIONAL-SMOKE-013/
Test Cases/Sheets/              ← archivos .tc (metadata)
Test Suites/                    ← suites individuales + collections
Test Listeners/
  SmokeTestListener.groovy       ← resumen por consola + smoke_summary_*.txt
Include/config/
  SELECTORS-REVIEW.md            ← selectores débiles + IDs recomendados para dev
  templet-credentials.properties.example
```

## 🛠️ Selectores débiles (para equipo de dev)

Ver [`Include/config/SELECTORS-REVIEW.md`](Include/config/SELECTORS-REVIEW.md) para la lista de 10 selectores frágiles y los `data-testid` recomendados.
