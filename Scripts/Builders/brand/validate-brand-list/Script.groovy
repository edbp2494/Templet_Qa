// ─────────────────────────────────────────────────────────────────────────────
// TC: TC-BUILDERS-BRAND-001
// Plataforma: Builders | Área: brand
// Descripción: Valida la pantalla Brand Properties (/brand): heading Active Brands, boton New Brand, columnas de tabla (Name, Client, # of Initiatives, # of Requests) y presencia de filas de datos.
// Suites: Platforms/Builders/BrandBlueprint/Brand-Blueprint-Validation, Platforms/QA/Repos-Coverage-Builders
// Precondiciones: credenciales MS en Include/config/templet-credentials.properties
// ─────────────────────────────────────────────────────────────────────────────
import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import com.kms.katalon.core.model.FailureHandling as FailureHandling

String caseId = 'TC-BUILDERS-BRAND-001'
List failures = []
List warnings = []

// ── URL ──────────────────────────────────────────────────────────────────────
String buildersTestUrl = CustomKeywords.'CommonKeywords.getRequiredGlobal'(
	'BUILDERS_TEST_URL',
	'https://testing-templet-builders.vercel.app/'
)
String targetUrl = buildersTestUrl.replaceAll('/+$', '') + '/brand'

// ── Login (TC1 de la suite — abre browser y hace SSO) ────────────────────────
CustomKeywords.'TempletPortalKeywords.openBrowserAndLoginWithMicrosoft'(targetUrl)
WebUI.waitForPageLoad(15)

// Asegurarse de estar en /brand tras el login
String currentUrl = CustomKeywords.'TempletPortalKeywords.currentUrlSafe'()
if (!currentUrl.contains('/brand')) {
	WebUI.navigateToUrl(targetUrl)
	WebUI.waitForPageLoad(15)
}

// ── 1. Heading principal ──────────────────────────────────────────────────────
Map headingOk = CustomKeywords.'TempletPortalKeywords.verifyXPathPresentWithFallback'(
	'heading_active_brands',
	"//h1[normalize-space(.)='Active Brands']",
	"//h1[contains(normalize-space(.),'Active Brands')]",
	10, 5
)
if (!headingOk.found) {
	failures.add('[HEADING] "Active Brands" no encontrado en /brand')
} else if (headingOk.usedFallback) {
	warnings.add('[SELECTOR] heading_active_brands via fallback — pedir data-testid')
}

// ── 2. Boton New Brand ────────────────────────────────────────────────────────
Map btnNewBrand = CustomKeywords.'TempletPortalKeywords.verifyXPathPresentWithFallback'(
	'btn_new_brand',
	"//button[normalize-space(.)='New Brand']",
	"//button[contains(normalize-space(.),'New Brand')]",
	10, 5
)
if (!btnNewBrand.found) {
	failures.add('[BTN] "New Brand" button no encontrado')
} else if (btnNewBrand.usedFallback) {
	warnings.add('[SELECTOR] btn_new_brand via fallback')
}

// ── 3. Columnas de la tabla ───────────────────────────────────────────────────
[
	[id: 'col_name',        xpath: "//button[normalize-space(.)='Name']",             label: 'Name'],
	[id: 'col_client',      xpath: "//button[normalize-space(.)='Client']",           label: 'Client'],
	[id: 'col_initiatives', xpath: "//button[normalize-space(.)='# of Initiatives']", label: '# of Initiatives'],
	[id: 'col_requests',    xpath: "//button[normalize-space(.)='# of Requests']",    label: '# of Requests']
].each { col ->
	boolean ok = CustomKeywords.'TempletPortalKeywords.verifyXPathPresent'(col.id, col.xpath, 8)
	if (!ok) {
		failures.add("[COL] Columna \"${col.label}\" no encontrada")
	}
}

// ── 4. Filas de datos (SOFT — estructura UNKNOWN) ─────────────────────────────
Long rowsTr  = (Long) WebUI.executeJavaScript("return document.querySelectorAll('tbody tr').length;", null)
Long rowsTd  = (Long) WebUI.executeJavaScript("return document.querySelectorAll('tr td').length;",   null)
int rowCount = Math.max((rowsTr ?: 0L).intValue(), (rowsTd ?: 0L).intValue())

if (rowCount > 0) {
	KeywordUtil.logInfo("[${caseId}] Filas de tabla encontradas: ${rowCount}")
} else {
	Long rowsAlt = (Long) WebUI.executeJavaScript(
		"return document.querySelectorAll('[class*=\"row\"], [class*=\"brand-item\"]').length;", null
	)
	if ((rowsAlt ?: 0L) > 0L) {
		warnings.add("[TABLE] ${rowsAlt} filas via selector alternativo — confirmar estructura DOM")
	} else {
		warnings.add('[TABLE] No se detectaron filas — estructura DOM desconocida, verificar manualmente')
	}
}

// ── Captura y resumen ─────────────────────────────────────────────────────────
CustomKeywords.'TempletPortalKeywords.captureCaseScreenshot'(caseId, 'brand_list_final')

if (failures) {
	CustomKeywords.'CommonKeywords.logCaseSummary'(caseId, failures, warnings)
	KeywordUtil.markFailedAndStop("[${caseId}] Failures: ${failures}")
} else {
	CustomKeywords.'CommonKeywords.logCaseSummary'(caseId, failures, warnings)
	KeywordUtil.logInfo("[${caseId}] PASSED" + (warnings ? " con ${warnings.size()} warnings" : ""))
}
