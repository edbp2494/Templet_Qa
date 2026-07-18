# 🚨 REGLAS CRÍTICAS — LEE ESTO PRIMERO

**Última actualización:** 2026-07-04  
**Estado:** Obligatorio antes de cualquier cambio  

---

## ⚠️ REGLAS NO NEGOCIABLES

### 1. NUNCA editar en worktrees
```
❌ NO editar en:  C:\Users\e2494\Katalon Studio\Templet\Editores\Sheet.worktrees\*
✅ SIEMPRE editar en: C:\Users\e2494\Katalon Studio\Templet\
```
**Por qué:** El CWD del agente puede estar en worktrees, pero Katalon ejecuta desde la raíz. Cambios en worktrees se pierden.

---

### 2. NUNCA usar `Set-Content` para `.groovy`
```powershell
❌ MALO:
Set-Content -Path $file -Value $content -Encoding UTF8

✅ BUENO:
[System.IO.File]::WriteAllText($path, $content, [System.Text.UTF8Encoding]::new($false))
```
**Por qué:** PowerShell agrega BOM (EF BB BF) que Groovy no soporta → `Unexpected character` en línea 1.

---

### 3. NUNCA hacer git add/commit/push sin autorización explícita
- No subir nada a Git sin que lo pida el usuario
- No cambiar .gitignore sin consultar

---

### 4. SIEMPRE validar antes de ejecutar
**Antes de ejecutar cualquier suite en Katalon:**
1. ✅ Leer el script completo (`Script.groovy`)
2. ✅ Verificar imports (¿están todos?)
3. ✅ Verificar métodos WebUI (¿existen en Katalon 11.0.1?)
4. ✅ Verificar rutas y URLs
5. ✅ Verificar try-catch-finally
6. ✅ Probar con un caso simple ANTES de 16 módulos

---

## 📋 ESTRUCTURA DEL PROYECTO

```
C:\Users\e2494\Katalon Studio\Templet\
├── Keywords/
│   ├── TempletPortalKeywords.groovy        ← API principal (~3100 líneas)
│   ├── CommonKeywords.groovy               ← Utilidades: logSummary, getRequiredGlobal
│   └── [otras keywords específicas]
├── Test Cases/
│   ├── Builders/                           ← Test cases de Builders
│   └── [otras plataformas]
├── Scripts/
│   ├── Builders/                           ← Scripts Groovy de test cases
│   └── [otras plataformas]
├── Test Suites/
│   ├── Platforms/Builders/                 ← Suites de Builders
│   └── [otras plataformas]
├── Include/config/
│   ├── templet-credentials.properties      ← 🔐 NO EN GIT (credenciales)
│   ├── templet-credentials.properties.example
│   └── SELECTORS-REVIEW.md                 ← Selectores débiles documentados
├── Reports/
│   ├── Builders/Compare/                   ← Screenshots pareados
│   ├── Tracking/snapshots/                 ← Baselines de tracking
│   └── asana_tickets/                      ← Tickets generados
├── CLAUDE.md                               ← Panorama completo del proyecto
├── AGENTE-CLAUDE-SHEET.md                  ← Prompt de inicio de sesión
└── 📍 REGLAS-CRITICAS-ANTES-DE-EMPEZAR.md ← TÚ ESTÁS AQUÍ
```

---

## 🔑 KEYWORDS CLAVE

### Login / Sesión (TempletPortalKeywords)
```groovy
CustomKeywords.'TempletPortalKeywords.openBrowserAndLoginWithMicrosoft'(url)
CustomKeywords.'TempletPortalKeywords.currentUrlSafe'()
CustomKeywords.'TempletPortalKeywords.isValidAppSession'()
```

### Builders Tracking
```groovy
CustomKeywords.'TempletPortalKeywords.validateBuildersTrackingAllDashboard'(config)
CustomKeywords.'TempletPortalKeywords.validateBuildersTrackingTabDashboard'(config)
```

### Utilidades (CommonKeywords)
```groovy
CustomKeywords.'CommonKeywords.getRequiredGlobal'('VAR_NAME', 'fallback')
CustomKeywords.'CommonKeywords.logCaseSummary'(caseId, failures, warnings)
```

---

## ✅ PATRÓN CORRECTO PARA SCRIPTS

```groovy
import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import com.kms.katalon.core.model.FailureHandling
import internal.GlobalVariable

String caseId = 'TC-BUILDERS-XXX-001'
List failures = []
List warnings = []

try {
    // 1. Login (si es TC1 de suite con isReuseDriver=true)
    CustomKeywords.'TempletPortalKeywords.openBrowserAndLoginWithMicrosoft'(targetUrl)
    WebUI.waitForPageLoad(15)
    
    // 2. Validaciones
    // ... tu lógica aquí ...
    
    // 3. Screenshot si aplica
    WebUI.takeScreenshot(filePath)
    
    // 4. Reportar
    if (failures) {
        CustomKeywords.'CommonKeywords.logCaseSummary'(caseId, failures, warnings)
        KeywordUtil.markFailedAndStop("[${caseId}] FAILED")
    } else {
        CustomKeywords.'CommonKeywords.logCaseSummary'(caseId, failures, warnings)
        KeywordUtil.logInfo("[${caseId}] PASSED")
    }
    
} catch (Exception e) {
    KeywordUtil.markFailedAndStop("[${caseId}] Error crítico: ${e.message}")
} finally {
    try {
        WebUI.closeBrowser()
    } catch (Exception ex) {
        KeywordUtil.logInfo("[${caseId}] No se pudo cerrar navegador")
    }
}
```

---

## 🎯 ERRORES FRECUENTES QUE YA COMETÍ (NO REPETIR)

| Error | Causado por | Solución |
|-------|-----------|----------|
| `MissingPropertyException: KeywordUtil` | Import faltante | Agregar `import com.kms.katalon.core.util.KeywordUtil` |
| `No signature of method openBrowser()` | No existe sin parámetros | Usar `CustomKeywords.'TempletPortalKeywords.openBrowserAndLoginWithMicrosoft'(url)` |
| `NoSuchSessionException: invalid session id` | Navegar entre apps en 1 sesión | **Usar 2 sesiones separadas: FASE 1 (app vieja) + FASE 2 (app nueva)** |
| Screenshots vacíos/grises | Falta espera de carga | Agregar `WebUI.waitForPageLoad(15)` después de navigate |
| URLs malformadas `//` | Construcción de rutas inconsistente | Verificar trailing slashes y rutas vacías |
| BOM en archivos `.groovy` | `Set-Content` con UTF8 | Usar `[System.IO.File]::WriteAllText()` con `$false` |

---

## 🔐 CREDENCIALES

**Ubicación:** `Include/config/templet-credentials.properties` (NO EN GIT)

**Variables disponibles:**
- `MS_USER` — usuario Microsoft
- `MS_PASS` — contraseña Microsoft

**Fallback:** Variables de entorno `TEMPLET_MS_USER`, `TEMPLET_MS_PASS`

**Método de lectura:**
```groovy
String user = CustomKeywords.'CommonKeywords.getRequiredGlobal'('MS_USER', 'user_fallback')
String pass = CustomKeywords.'CommonKeywords.getRequiredGlobal'('MS_PASS', 'pass_fallback')
```

---

## 📌 CHECKLIST ANTES DE CADA CAMBIO

Antes de tocar CUALQUIER cosa en el proyecto:

- [ ] ✅ Leo este archivo (`REGLAS-CRITICAS-ANTES-DE-EMPEZAR.md`)
- [ ] ✅ Leo `CLAUDE.md` para el panorama completo
- [ ] ✅ Verifiqué que edito en `C:\Users\e2494\Katalon Studio\Templet\` (NO worktrees)
- [ ] ✅ Si edito `.groovy`, lo haré con script PowerShell correcto (sin BOM)
- [ ] ✅ Si uso WebUI, verifiqué la firma del método en Katalon 11.0.1
- [ ] ✅ Valido el código ANTES de ejecutar (no después)
- [ ] ✅ NO haré git add/commit/push sin autorización
- [ ] ✅ Documentaré cambios si es algo que va a permanencia

---

## 📞 ESCALADAS

**Si algo no está claro:**
1. Pregunta explícitamente (no asumas)
2. Valida con casos reales del proyecto
3. No inventes métodos — consulta la documentación de Katalon

---

**Última revisión:** 2026-07-04 17:30  
**Creado por:** Copilot (después de primer fallo masivo 😅)
