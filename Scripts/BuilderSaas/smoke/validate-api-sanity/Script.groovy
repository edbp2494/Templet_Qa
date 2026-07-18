// ─────────────────────────────────────────────────────────────────────────────
// TC: TC-BUILDERSAAS-API-SANITY-002
// Plataforma: BuilderSaas | Área: smoke
// Descripción: TC-BUILDERSAAS-API-SANITY-002 — Reusa la sesion MS y hace fetch same-origin a /api/brands, /api/templates, /api/layouts. Valida status 200 y counts sanos (referencia por snapshot previo o seed inicial).
// Suites: Platforms/BuilderSaas/Smoke
// Precondiciones: credenciales MS en Include/config/templet-credentials.properties
// ─────────────────────────────────────────────────────────────────────────────
import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI

// TC-BUILDERSAAS-API-SANITY-002 — Reusa la sesion MS abierta por SMOKE-PAGES-001.
// Hace fetch same-origin (credentials:'include') a los endpoints core y valida counts sanos.
// Los counts NO se hardcodean como asercion: los seeds son solo referencia inicial y el
// baseline real es el snapshot previo. Solo se falla ante status!=200, error, timeout o count<=0.
String caseId = 'TC-BUILDERSAAS-API-SANITY-002'
List failures = []
List warnings = []

String base = CustomKeywords.'CommonKeywords.getRequiredGlobal'(
	'BUILDER_SAAS_TEST_URL',
	'https://testing-templet-builder-saas.vercel.app'
).toString().replaceAll('/+$', '')

// Login defensivo: solo si no hay navegador vivo o no estamos en la app.
boolean alive = CustomKeywords.'TempletPortalKeywords.isBrowserSessionAlive'()
String currUrl = CustomKeywords.'TempletPortalKeywords.currentUrlSafe'()
if (!alive || !currUrl.startsWith(base)) {
	CustomKeywords.'TempletPortalKeywords.openBrowserAndLoginWithMicrosoft'(base)
}

// Referencia: snapshot previo si tiene counts numericos > 0; si no, seed inicial.
Map seeds = [brands: 62, templates: 215, layouts: 1779]
String latestPath = System.getProperty('user.dir') + '/Reports/BuilderSaas/snapshots/builder_saas_api_latest.json'
Map previous = (Map) (CustomKeywords.'TempletPortalKeywords.readJsonIfExists'(latestPath) ?: [:])
Map prevCounts = (Map) (previous['counts'] ?: [:])

// Endpoints: [key, path]
List<List<String>> endpoints = [
	['brands',    '/api/brands'],
	['templates', '/api/templates'],
	['layouts',   '/api/layouts']
]

Map counts = [:]
List<Map> details = []

endpoints.each { List<String> ep ->
	String key = ep[0]
	String url = base + ep[1]

	// Lanzar fetch asincrono; el resultado queda en window.__qaApiResult.
	String kick = 'window.__qaApiResult=null; fetch("' + url + '", {credentials:"include"})' +
		'.then(function(r){ return r.text().then(function(txt){ var j=null; try{ j=JSON.parse(txt); }catch(e){}' +
		' var count = Array.isArray(j) ? j.length : (j==null ? -1 : (j.data?.length ?? j.total ?? -1));' +
		' window.__qaApiResult = {status:r.status, count:count}; }); })' +
		'.catch(function(e){ window.__qaApiResult = {status:-1, count:-1, error:String(e)}; });'
	WebUI.executeJavaScript(kick, null)

	// Polling cada 1s hasta 30s.
	Map res = null
	for (int s = 0; s < 30 && res == null; s++) {
		WebUI.delay(1)
		Object r = WebUI.executeJavaScript('return window.__qaApiResult;', null)
		if (r instanceof Map) {
			res = (Map) r
		}
	}

	if (res == null) {
		failures.add('[' + key + '] timeout (>30s) sin respuesta de ' + url)
		counts[key] = -1
		details.add([key: key, url: url, status: -1, count: -1, error: 'timeout'])
		return
	}

	long status = ((Number) (res['status'] ?: -1)).longValue()
	long count = ((Number) (res['count'] ?: -1)).longValue()
	String err = (res['error'] ?: '').toString()
	counts[key] = count

	if (err) {
		failures.add('[' + key + '] error de fetch: ' + err)
	}
	if (status != 200) {
		failures.add('[' + key + '] status=' + status + ' esperado=200 (' + url + ')')
	}
	if (count <= 0) {
		failures.add('[' + key + '] count=' + count + ' (<=0) en ' + url)
	}

	// Warning por desvio >20% vs referencia.
	long reference
	String refSource
	Object prevVal = prevCounts[key]
	if (prevVal instanceof Number && ((Number) prevVal).longValue() > 0) {
		reference = ((Number) prevVal).longValue()
		refSource = 'snapshot previo'
	} else {
		reference = ((Number) seeds[key]).longValue()
		refSource = 'seed inicial'
	}
	if (count > 0 && reference > 0) {
		double dev = Math.abs(count - reference) / (double) reference
		if (dev > 0.20) {
			warnings.add('[' + key + '] count=' + count + ' desvia ' + ((int) Math.round(dev * 100)) +
				'% vs referencia=' + reference + ' (' + refSource + ')')
		}
	}

	details.add([key: key, url: url, status: status, count: count, reference: reference, refSource: refSource])
}

// Snapshot: history SIEMPRE; latest SOLO si el run no tuvo failures (no envenenar baseline).
String stamp = new Date().format('yyyyMMdd_HHmmss')
Map snap = [
	caseId   : caseId,
	timestamp: new Date().format("yyyy-MM-dd'T'HH:mm:ss"),
	baseUrl  : base,
	counts   : counts,
	details  : details,
	failures : failures,
	warnings : warnings
]
String dir = System.getProperty('user.dir') + '/Reports/BuilderSaas/snapshots'
try {
	CustomKeywords.'TempletPortalKeywords.writeJsonSnapshot'(dir + '/history/builder_saas_api_' + stamp + '.json', snap)
	if (!failures) {
		CustomKeywords.'TempletPortalKeywords.writeJsonSnapshot'(dir + '/builder_saas_api_latest.json', snap)
	}
} catch (Throwable t) {
	warnings.add('[SNAPSHOT] No se pudo escribir snapshot: ' + t.getMessage())
}

CustomKeywords.'CommonKeywords.logCaseSummary'(caseId, failures, warnings)
if (failures) {
	KeywordUtil.markFailedAndStop('[' + caseId + '] Failures: ' + failures)
} else {
	KeywordUtil.logInfo('[' + caseId + '] PASSED' + (warnings ? ' con ' + warnings.size() + ' warnings' : ''))
}
