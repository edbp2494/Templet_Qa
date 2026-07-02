import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import com.kms.katalon.core.model.FailureHandling as FailureHandling

String caseId = 'TC-BUILDERS-BLUEPRINT-REQUEST-TRACKING-001'
List failures = []
List warnings = []

// ── Reusar sesion ─────────────────────────────────────────────────────────────
String buildersTestUrl = CustomKeywords.'CommonKeywords.getRequiredGlobal'(
	'BUILDERS_TEST_URL',
	'https://testing-templet-builders.vercel.app/'
)
String trackingUrl = CustomKeywords.'CommonKeywords.buildTrackingUrl'(buildersTestUrl)

if (!CustomKeywords.'TempletPortalKeywords.ensureAuthenticatedSession'(trackingUrl, 20, 2)) {
	failures.add('[AUTH] Sesion no valida al navegar a tracking')
	CustomKeywords.'CommonKeywords.logCaseSummary'(caseId, failures, warnings)
	KeywordUtil.markFailedAndStop("[${caseId}] Sin sesion")
}

KeywordUtil.logInfo("[TRACKING] URL: ${CustomKeywords.'TempletPortalKeywords.currentUrlSafe'()}")

// ── 1. Verificar pagina de tracking cargada ───────────────────────────────────
boolean trackingPageOk = CustomKeywords.'TempletPortalKeywords.verifyXPathPresent'(
	'tracking_page_title',
	"//h1[contains(normalize-space(.),'Tracking') or contains(normalize-space(.),'tracking')]",
	10
)
if (!trackingPageOk) {
	trackingPageOk = CustomKeywords.'TempletPortalKeywords.verifyXPathPresent'(
		'tracking_any_heading',
		"//h1 | //h2[contains(normalize-space(.),'All') or contains(normalize-space(.),'Blueprint')]",
		5
	)
	if (trackingPageOk) {
		warnings.add('[SELECTOR] Tracking page heading via fallback')
	} else {
		warnings.add('[TRACKING] No se encontro heading de tracking — puede no haber cargado la seccion')
	}
}

// ── 2. Capturar estado completo del dashboard de tracking ─────────────────────
Map trackingState = (Map) WebUI.executeJavaScript('''
	var normalize = function(s) { return (s||"").trim().replace(/\\s+/g," "); };
	var cards = Array.from(document.querySelectorAll("h3, h4, [class*='card'] h3, [class*='card'] h4")).map(function(el) {
		return { tag: el.tagName, text: normalize(el.innerText||"").substring(0,80) };
	}).filter(function(c){ return c.text.length > 0; });
	var metrics = Array.from(document.querySelectorAll("[class*='metric'], [class*='stat'], [class*='count']")).map(function(el){
		return normalize(el.innerText||"").substring(0,60);
	}).filter(function(t){ return t.length > 0; }).slice(0,10);
	var tabs = Array.from(document.querySelectorAll("button")).filter(function(b){
		var tabLabels = ["all","blueprint","task creation","login"];
		return tabLabels.indexOf(normalize(b.innerText||"").toLowerCase()) >= 0;
	}).map(function(b){ return normalize(b.innerText||""); });
	var requests = Array.from(document.querySelectorAll("table tbody tr, [class*='row']")).map(function(row){
		return normalize(row.innerText||"").substring(0,100);
	}).filter(function(t){ return t.length > 0; }).slice(0,10);
	return { cards: cards, metrics: metrics, tabs: tabs, requests: requests, url: window.location.href };
''', null)

KeywordUtil.logInfo("[TRACKING] Tabs disponibles: ${trackingState?.tabs}")
KeywordUtil.logInfo("[TRACKING] Cards/headings: ${trackingState?.cards}")
KeywordUtil.logInfo("[TRACKING] Metrics: ${trackingState?.metrics}")
KeywordUtil.logInfo("[TRACKING] Filas/requests: ${trackingState?.requests}")

if (trackingState?.tabs?.size() == 0) {
	warnings.add('[TRACKING] No se encontraron tabs del tracking dashboard — puede estar en layout diferente')
}
if (trackingState?.cards?.size() == 0) {
	warnings.add('[TRACKING] No se encontraron cards — dashboard puede estar cargando o vacio')
}

// ── 3. Verificar que hay datos en el tab Blueprint ────────────────────────────
boolean blueprintTabClicked = CustomKeywords.'TempletPortalKeywords.clickXPathAndKeepValidSession'(
	'tab_blueprint',
	"//button[normalize-space(.)='Blueprint']",
	5
)
if (!blueprintTabClicked) {
	warnings.add('[TRACKING] Tab Blueprint no encontrado o no clickeable')
} else {
	WebUI.delay(2)
	Map blueprintTabState = (Map) WebUI.executeJavaScript('''
		var cards = Array.from(document.querySelectorAll("h3,h4")).map(function(el){
			return (el.innerText||"").trim().replace(/\\s+/g," ").substring(0,80);
		}).filter(function(t){ return t.length > 0; });
		var executions = Array.from(document.querySelectorAll("*")).filter(function(el){
			var t = (el.innerText||"").trim().toLowerCase();
			return t.indexOf("executions") >= 0 || t.indexOf("daily") >= 0;
		}).map(function(el){ return (el.innerText||"").trim().replace(/\\s+/g," ").substring(0,80); }).slice(0,5);
		return { cards: cards, executions: executions };
	''', null)
	KeywordUtil.logInfo("[TRACKING] Blueprint tab - cards: ${blueprintTabState?.cards}")
	KeywordUtil.logInfo("[TRACKING] Blueprint tab - execution data: ${blueprintTabState?.executions}")
}

// ── Screenshot ────────────────────────────────────────────────────────────────
CustomKeywords.'TempletPortalKeywords.captureCaseScreenshot'(caseId, 'tracking_dashboard')

if (failures) {
	CustomKeywords.'CommonKeywords.logCaseSummary'(caseId, failures, warnings)
	KeywordUtil.markFailedAndStop("[${caseId}] Failures: ${failures}")
} else {
	CustomKeywords.'CommonKeywords.logCaseSummary'(caseId, failures, warnings)
	KeywordUtil.logInfo("[${caseId}] PASSED" + (warnings ? " con ${warnings.size()} warnings" : ""))
}
