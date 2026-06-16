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
String targetUrl = buildersTestUrl.endsWith('/')
	? buildersTestUrl + 'blueprint/manager/power-user'
	: buildersTestUrl + '/blueprint/manager/power-user'

// ── Reusar sesion (TC2 con isReuseDriver=true) ────────────────────────────────
boolean sessionAlive = CustomKeywords.'TempletPortalKeywords.isBrowserSessionAlive'()
if (!sessionAlive) {
	// Fallback: ejecutado como TC standalone
	CustomKeywords.'TempletPortalKeywords.openBrowserAndLoginWithMicrosoft'(targetUrl)
} else {
	WebUI.navigateToUrl(targetUrl)
}
WebUI.waitForPageLoad(20)

// Verificar sesion valida (no en MS login)
boolean validSession = CustomKeywords.'TempletPortalKeywords.isValidAppSession'()
if (!validSession) {
	failures.add('[AUTH] Sesion no valida — URL apunta a Microsoft login')
	CustomKeywords.'CommonKeywords.logCaseSummary'(caseId, failures, warnings)
	KeywordUtil.markFailedAndStop("[${caseId}] Failures: ${failures}")
}

// ── 1. Heading principal ──────────────────────────────────────────────────────
boolean headingOk = CustomKeywords.'TempletPortalKeywords.verifyXPathPresent'(
	'heading_blueprint_manager',
	"//h1[normalize-space(.)='Blueprint manager']",
	10
)
if (!headingOk) {
	headingOk = CustomKeywords.'TempletPortalKeywords.verifyXPathPresent'(
		'heading_blueprint_manager_fallback',
		"//h1[contains(normalize-space(.),'Blueprint')]",
		5
	)
	if (!headingOk) {
		failures.add('[HEADING] "Blueprint manager" no encontrado en /blueprint/manager/power-user')
	} else {
		warnings.add('[SELECTOR] heading via fallback — pedir data-testid')
	}
}

// ── 2. Boton New Blueprint ────────────────────────────────────────────────────
boolean btnNewBlueprint = CustomKeywords.'TempletPortalKeywords.verifyXPathPresent'(
	'btn_new_blueprint',
	"//button[normalize-space(.)='New Blueprint']",
	10
)
if (!btnNewBlueprint) {
	btnNewBlueprint = CustomKeywords.'TempletPortalKeywords.verifyXPathPresent'(
		'btn_new_blueprint_fallback',
		"//button[contains(normalize-space(.),'New Blueprint')]",
		5
	)
	if (!btnNewBlueprint) {
		failures.add('[BTN] "New Blueprint" button no encontrado')
	} else {
		warnings.add('[SELECTOR] btn_new_blueprint via fallback')
	}
}

// ── 3. Boton Filter by brand ──────────────────────────────────────────────────
boolean btnFilter = CustomKeywords.'TempletPortalKeywords.verifyXPathPresent'(
	'btn_filter_by_brand',
	"//button[normalize-space(.)='Filter by brand']",
	10
)
if (!btnFilter) {
	btnFilter = CustomKeywords.'TempletPortalKeywords.verifyXPathPresent'(
		'btn_filter_by_brand_fallback',
		"//button[contains(normalize-space(.),'Filter by brand')]",
		5
	)
	if (!btnFilter) {
		failures.add('[BTN] "Filter by brand" button no encontrado')
	} else {
		warnings.add('[SELECTOR] btn_filter_by_brand via fallback')
	}
}

// ── 4. Grid de blueprints ─────────────────────────────────────────────────────
boolean gridOk = CustomKeywords.'TempletPortalKeywords.verifyXPathPresent'(
	'grid_blueprints',
	"//div[contains(@class,'grid-cols-1') and contains(@class,'gap-8')]",
	10
)
if (!gridOk) {
	gridOk = CustomKeywords.'TempletPortalKeywords.verifyXPathPresent'(
		'grid_blueprints_fallback',
		"//div[contains(@class,'gap-8')][.//h3]",
		5
	)
	if (!gridOk) {
		failures.add('[GRID] Contenedor de blueprint cards no encontrado')
	} else {
		warnings.add('[SELECTOR] grid_blueprints via fallback')
	}
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
