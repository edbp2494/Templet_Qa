import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import com.kms.katalon.core.model.FailureHandling as FailureHandling

String caseId = 'TC-BUILDERS-BLUEPRINT-REQUEST-SMOKE-001'
List failures = []
List warnings = []

// ── URL base ──────────────────────────────────────────────────────────────────
String buildersTestUrl = CustomKeywords.'CommonKeywords.getRequiredGlobal'(
	'BUILDERS_TEST_URL',
	'https://testing-templet-builders.vercel.app/'
)
String managerUrl = buildersTestUrl.replaceAll('/+$', '') + '/blueprint/manager/power-user'

// ── TC1: hace login (primer TC de la suite) ───────────────────────────────────
CustomKeywords.'TempletPortalKeywords.openBrowserAndLoginWithMicrosoft'(managerUrl)
WebUI.waitForPageLoad(20)

boolean validSession = CustomKeywords.'TempletPortalKeywords.isValidAppSession'()
if (!validSession) {
	failures.add('[AUTH] No se pudo establecer sesion valida en builders')
	CustomKeywords.'CommonKeywords.logCaseSummary'(caseId, failures, warnings)
	KeywordUtil.markFailedAndStop("[${caseId}] Sin sesion — no puede continuar la suite")
}

KeywordUtil.logInfo("[${caseId}] Sesion activa. URL actual: ${CustomKeywords.'TempletPortalKeywords.currentUrlSafe'()}")

// ── 1. Verificar que estamos en /blueprint/manager ────────────────────────────
String currentUrl = CustomKeywords.'TempletPortalKeywords.currentUrlSafe'()
if (!currentUrl.contains('blueprint')) {
	warnings.add("[NAV] URL esperada contiene 'blueprint', actual: ${currentUrl}")
}

// ── 2. Buscar heading de Blueprint Manager ────────────────────────────────────
Map headingFound = CustomKeywords.'TempletPortalKeywords.verifyXPathPresentWithFallback'(
	'heading_blueprint_manager',
	"//h1[normalize-space(.)='Blueprint manager']",
	"//h1[contains(normalize-space(.),'Blueprint')]",
	10, 5
)
if (!headingFound.found) {
	warnings.add('[HEADING] No se encontro h1 de Blueprint — page puede estar en construccion')
} else if (headingFound.usedFallback) {
	warnings.add('[SELECTOR] Heading Blueprint via fallback h1 — UI puede haber cambiado texto')
}

// ── 3. Buscar boton "New Blueprint" ──────────────────────────────────────────
Map btnNewBlueprint = CustomKeywords.'TempletPortalKeywords.verifyXPathPresentWithFallback'(
	'btn_new_blueprint',
	"//button[normalize-space(.)='New Blueprint']",
	"//button[contains(normalize-space(.),'Blueprint') or contains(normalize-space(.),'New')]",
	10, 5
)
if (!btnNewBlueprint.found) {
	warnings.add('[BTN] No se encontro boton New Blueprint — puede ser que requiera otro rol o la UI cambio')
} else if (btnNewBlueprint.usedFallback) {
	warnings.add('[SELECTOR] btn New Blueprint via fallback — verificar texto exacto')
}

// ── 4. Descubrir URLs accesibles (discovery) ──────────────────────────────────
List<String> candidateUrls = [
	buildersTestUrl.replaceAll('/+$', '') + '/blueprint/create',
	buildersTestUrl.replaceAll('/+$', '') + '/blueprint/new',
	buildersTestUrl.replaceAll('/+$', '') + '/request/create',
	buildersTestUrl.replaceAll('/+$', '') + '/request/new',
	buildersTestUrl.replaceAll('/+$', '') + '/blueprint/manager/power-user/create'
]

String discoveredCreateUrl = null
for (String url : candidateUrls) {
	try {
		WebUI.navigateToUrl(url)
		WebUI.waitForPageLoad(10)
		boolean notFound = CustomKeywords.'TempletPortalKeywords.verifyXPathPresent'(
			'page_not_found', "//h1[contains(normalize-space(.),'404') or contains(normalize-space(.),'Not Found')]", 3
		)
		boolean sessionLost = !CustomKeywords.'TempletPortalKeywords.isValidAppSession'()
		if (!notFound && !sessionLost) {
			discoveredCreateUrl = url
			KeywordUtil.logInfo("[DISCOVER] URL de creacion accesible: ${url}")
			break
		}
	} catch (Exception e) {
		KeywordUtil.logInfo("[DISCOVER] URL no accesible: ${url} — ${e.message}")
	}
}

if (discoveredCreateUrl) {
	warnings.add("[DISCOVER] URL creacion encontrada via discovery: ${discoveredCreateUrl} — confirmar si es la correcta")
} else {
	warnings.add('[DISCOVER] Ninguna URL de creacion candidata respondio — flujo de creacion puede estar en otra ruta')
}

// ── 5. Volver al manager y capturar DOM visible para debug ────────────────────
WebUI.navigateToUrl(managerUrl)
WebUI.waitForPageLoad(15)

Map domState = (Map) WebUI.executeJavaScript('''
	var buttons = Array.from(document.querySelectorAll('button')).map(function(b) {
		return (b.innerText || b.textContent || '').trim().replace(/\\s+/g,' ').substring(0,60);
	}).filter(function(t) { return t.length > 0; });
	var links = Array.from(document.querySelectorAll('a[href]')).map(function(a) {
		return { text: (a.innerText||'').trim().substring(0,40), href: (a.href||'').substring(0,80) };
	}).filter(function(l) { return l.text.length > 0; }).slice(0,20);
	var headings = Array.from(document.querySelectorAll('h1,h2,h3')).map(function(h) {
		return { tag: h.tagName, text: (h.innerText||'').trim().replace(/\\s+/g,' ').substring(0,80) };
	});
	return { buttons: buttons, links: links, headings: headings, url: window.location.href };
''', null)

KeywordUtil.logInfo("[DOM] URL: ${domState?.url}")
KeywordUtil.logInfo("[DOM] Headings: ${domState?.headings}")
KeywordUtil.logInfo("[DOM] Buttons: ${domState?.buttons}")
KeywordUtil.logInfo("[DOM] Links: ${domState?.links}")

// ── Screenshot + resumen ──────────────────────────────────────────────────────
CustomKeywords.'TempletPortalKeywords.captureCaseScreenshot'(caseId, 'smoke_manager_page')

if (failures) {
	CustomKeywords.'CommonKeywords.logCaseSummary'(caseId, failures, warnings)
	KeywordUtil.markFailedAndStop("[${caseId}] Failures: ${failures}")
} else {
	CustomKeywords.'CommonKeywords.logCaseSummary'(caseId, failures, warnings)
	KeywordUtil.logInfo("[${caseId}] PASSED" + (warnings ? " con ${warnings.size()} warnings" : ""))
}
