// ─────────────────────────────────────────────────────────────────────────────
// TC: TC-BUILDERSAAS-SMOKE-PAGES-001
// Plataforma: BuilderSaas | Área: smoke
// Descripción: TC-BUILDERSAAS-SMOKE-PAGES-001 — Abre sesion MS y valida que Home, Brand Properties, Templates, Blueprints, Brand assets y QA rendericen sin "Failed to load" ni 404. Detecta la regresion del middleware (10/07/2026).
// Suites: Platforms/BuilderSaas/Smoke
// Precondiciones: credenciales MS en Include/config/templet-credentials.properties
// ─────────────────────────────────────────────────────────────────────────────
import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI

// TC-BUILDERSAAS-SMOKE-PAGES-001 — Primer TC de la suite: abre la sesion MS y valida
// que las paginas server-side del Builder SaaS renderizan sin "Failed to load" ni 404.
// El 10/07/2026 la regresion del middleware rompe /brand-properties, /templates, /blueprints.
String caseId = 'TC-BUILDERSAAS-SMOKE-PAGES-001'
List failures = []
List warnings = []

String base = CustomKeywords.'CommonKeywords.getRequiredGlobal'(
	'BUILDER_SAAS_TEST_URL',
	'https://testing-templet-builder-saas.vercel.app'
).toString().replaceAll('/+$', '')

// Abre navegador + login Microsoft (primer TC de la suite reusa driver en los siguientes).
CustomKeywords.'TempletPortalKeywords.openBrowserAndLoginWithMicrosoft'(base)

// Paginas a validar: [path relativo, slug, etiqueta]
List<List<String>> pages = [
	['',                 'home',             'Home'],
	['/brand-properties', 'brand-properties', 'Brand Properties'],
	['/templates',        'templates',        'Templates'],
	['/blueprints',       'blueprints',       'Blueprints'],
	['/layout',           'layout',           'Brand assets'],
	['/qa',               'qa',               'QA']
]

List<Map> pageResults = []

pages.each { List<String> p ->
	String path = p[0]
	String slug = p[1]
	String label = p[2]
	String url = base + path
	List reasons = []

	WebUI.navigateToUrl(url)
	WebUI.waitForPageLoad(20)
	WebUI.delay(2)

	boolean sessionOk = CustomKeywords.'TempletPortalKeywords.isValidAppSession'()
	if (!sessionOk) {
		reasons.add('sesion invalida (posible expulsion a login MS)')
	}

	// Texto del body para detectar errores de carga y marcadores 404.
	String bodyText = ''
	try {
		Object raw = WebUI.executeJavaScript('return document.body ? document.body.innerText : "";', null)
		bodyText = (raw ?: '').toString()
	} catch (Exception e) {
		reasons.add('no se pudo leer el body: ' + e.getMessage())
	}
	String lower = bodyText.toLowerCase()

	// "Failed to load ..." (case-insensitive) — capturar el texto exacto.
	int idx = lower.indexOf('failed to load')
	if (idx >= 0) {
		int end = Math.min(bodyText.length(), idx + 200)
		String exact = bodyText.substring(idx, end).replaceAll('\\s+', ' ').trim()
		reasons.add('contiene error de carga: "' + exact + '"')
	}

	// Marcadores de 404.
	if (lower.contains('this page could not be found') || bodyText.contains('404: NOT_FOUND')) {
		reasons.add('pagina 404')
	}

	// Solo Home: heuristica de contadores (hojas dentro de main con texto numerico puro <=10 chars).
	if (slug == 'home') {
		List counters = []
		try {
			Object rawNums = WebUI.executeJavaScript(
				'var main = document.querySelector("main"); if(!main){return [];}' +
				' function pureNum(t){ if(t.length===0||t.length>10){return false;} for(var i=0;i<t.length;i++){var c=t.charCodeAt(i); if(c<48||c>57){return false;}} return true; }' +
				' var out=[]; var all = main.querySelectorAll("*");' +
				' for(var j=0;j<all.length;j++){ var el=all[j]; if(el.children.length>0){continue;} var t=(el.textContent||"").trim(); if(pureNum(t)){ out.push(t); } }' +
				' return out;', null)
			if (rawNums instanceof List) {
				counters = (List) rawNums
			}
		} catch (Exception e) {
			warnings.add('[HOME] No se pudo evaluar contadores: ' + e.getMessage())
		}
		if (counters.isEmpty()) {
			warnings.add('[HOME] No se encontraron contadores numericos (selector debil, sin data-testid)')
		} else {
			boolean todosCero = counters.every { Integer.parseInt((it ?: '0').toString()) == 0 }
			if (todosCero) {
				reasons.add('todos los contadores del Home en 0 (' + counters.size() + ' encontrados)')
			}
		}
	}

	CustomKeywords.'TempletPortalKeywords.captureCaseScreenshot'(caseId, 'page_' + slug)

	boolean pageOk = reasons.isEmpty()
	if (!pageOk) {
		failures.add('[' + label + ' (' + (path ?: '/') + ')] ' + reasons.join(' | '))
	}
	pageResults.add([
		path   : (path ?: '/'),
		slug   : slug,
		label  : label,
		url    : url,
		ok     : pageOk,
		reasons: reasons
	])
}

// Snapshot de evidencia: latest SIEMPRE (no es baseline) + copia en history.
String stamp = new Date().format('yyyyMMdd_HHmmss')
Map snap = [
	caseId   : caseId,
	timestamp: new Date().format("yyyy-MM-dd'T'HH:mm:ss"),
	baseUrl  : base,
	pages    : pageResults,
	failures : failures,
	warnings : warnings
]
String dir = System.getProperty('user.dir') + '/Reports/BuilderSaas/snapshots'
try {
	CustomKeywords.'TempletPortalKeywords.writeJsonSnapshot'(dir + '/builder_saas_smoke_latest.json', snap)
	CustomKeywords.'TempletPortalKeywords.writeJsonSnapshot'(dir + '/history/builder_saas_smoke_' + stamp + '.json', snap)
} catch (Throwable t) {
	warnings.add('[SNAPSHOT] No se pudo escribir snapshot: ' + t.getMessage())
}

CustomKeywords.'CommonKeywords.logCaseSummary'(caseId, failures, warnings)
if (failures) {
	KeywordUtil.markFailedAndStop('[' + caseId + '] Failures: ' + failures)
} else {
	KeywordUtil.logInfo('[' + caseId + '] PASSED' + (warnings ? ' con ' + warnings.size() + ' warnings' : ''))
}
