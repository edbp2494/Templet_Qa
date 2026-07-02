import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import com.kms.katalon.core.model.FailureHandling as FailureHandling

String caseId = 'TC-BUILDERS-BLUEPRINT-MANAGER-001'
List failures = []
List warnings = []

// ── URL ──────────────────────────────────────────────────────────────────────
String buildersTestUrl = CustomKeywords.'CommonKeywords.getRequiredGlobal'(
	'BUILDERS_TEST_URL',
	'https://testing-templet-builders.vercel.app/'
)
String targetUrl = buildersTestUrl.replaceAll('/+$', '') + '/blueprint/manager/power-user'

// ── Reusar sesion (TC2 con isReuseDriver=true) ────────────────────────────────
if (!CustomKeywords.'TempletPortalKeywords.ensureAuthenticatedSession'(targetUrl, 20)) {
	failures.add('[AUTH] Sesion no valida — URL apunta a Microsoft login')
	CustomKeywords.'CommonKeywords.logCaseSummary'(caseId, failures, warnings)
	KeywordUtil.markFailedAndStop("[${caseId}] Failures: ${failures}")
}

// ── 1. Heading principal ──────────────────────────────────────────────────────
Map headingOk = CustomKeywords.'TempletPortalKeywords.verifyXPathPresentWithFallback'(
	'heading_blueprint_manager',
	"//h1[normalize-space(.)='Blueprint manager']",
	"//h1[contains(normalize-space(.),'Blueprint')]",
	10, 5
)
if (!headingOk.found) {
	failures.add('[HEADING] "Blueprint manager" no encontrado en /blueprint/manager/power-user')
} else if (headingOk.usedFallback) {
	warnings.add('[SELECTOR] heading via fallback — pedir data-testid')
}

// ── 2. Boton New Blueprint ────────────────────────────────────────────────────
Map btnNewBlueprint = CustomKeywords.'TempletPortalKeywords.verifyXPathPresentWithFallback'(
	'btn_new_blueprint',
	"//button[normalize-space(.)='New Blueprint']",
	"//button[contains(normalize-space(.),'New Blueprint')]",
	10, 5
)
if (!btnNewBlueprint.found) {
	failures.add('[BTN] "New Blueprint" button no encontrado')
} else if (btnNewBlueprint.usedFallback) {
	warnings.add('[SELECTOR] btn_new_blueprint via fallback')
}

// ── 3. Boton Filter by brand ──────────────────────────────────────────────────
Map btnFilter = CustomKeywords.'TempletPortalKeywords.verifyXPathPresentWithFallback'(
	'btn_filter_by_brand',
	"//button[normalize-space(.)='Filter by brand']",
	"//button[contains(normalize-space(.),'Filter by brand')]",
	10, 5
)
if (!btnFilter.found) {
	failures.add('[BTN] "Filter by brand" button no encontrado')
} else if (btnFilter.usedFallback) {
	warnings.add('[SELECTOR] btn_filter_by_brand via fallback')
}

// ── 4. Grid de blueprints ─────────────────────────────────────────────────────
Map gridOk = CustomKeywords.'TempletPortalKeywords.verifyXPathPresentWithFallback'(
	'grid_blueprints',
	"//div[contains(@class,'grid-cols-1') and contains(@class,'gap-8')]",
	"//div[contains(@class,'gap-8')][.//h3]",
	10, 5
)
if (!gridOk.found) {
	failures.add('[GRID] Contenedor de blueprint cards no encontrado')
} else if (gridOk.usedFallback) {
	warnings.add('[SELECTOR] grid_blueprints via fallback')
}

// ── 5. Contar blueprint cards via JS (>=1 requerido) ─────────────────────────
Long cardCount = (Long) WebUI.executeJavaScript(
	"return document.querySelectorAll('.gap-8 h3, [class*=\"gap-8\"] h3').length;", null
)
int cards = (cardCount ?: 0L).intValue()
if (cards > 0) {
	KeywordUtil.logInfo("[${caseId}] Blueprint cards encontradas: ${cards}")
} else {
	Long cardsFallback = (Long) WebUI.executeJavaScript(
		"return document.querySelectorAll('main h3').length;", null
	)
	int cardsFb = (cardsFallback ?: 0L).intValue()
	if (cardsFb > 0) {
		warnings.add("[CARDS] ${cardsFb} blueprint cards via main//h3 (grid selector fragil)")
	} else {
		failures.add('[CARDS] No se encontraron blueprint cards — grid vacio o no cargo')
	}
}

// ── Captura y resumen ─────────────────────────────────────────────────────────
CustomKeywords.'TempletPortalKeywords.captureCaseScreenshot'(caseId, 'blueprint_manager_final')

if (failures) {
	CustomKeywords.'CommonKeywords.logCaseSummary'(caseId, failures, warnings)
	KeywordUtil.markFailedAndStop("[${caseId}] Failures: ${failures}")
} else {
	CustomKeywords.'CommonKeywords.logCaseSummary'(caseId, failures, warnings)
	KeywordUtil.logInfo("[${caseId}] PASSED" + (warnings ? " con ${warnings.size()} warnings" : ""))
}
