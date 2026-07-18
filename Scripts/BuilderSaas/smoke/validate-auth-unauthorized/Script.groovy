// ─────────────────────────────────────────────────────────────────────────────
// TC: TC-BUILDERSAAS-AUTH-003
// Plataforma: BuilderSaas | Área: smoke
// Descripción: TC-BUILDERSAAS-AUTH-003 — Verifica que el middleware protege los /api/*: sin sesion deben responder 401. Llamada HttpClient anonima (sin navegador).
// Suites: Platforms/BuilderSaas/Smoke
// Precondiciones: credenciales MS en Include/config/templet-credentials.properties
// ─────────────────────────────────────────────────────────────────────────────
import com.kms.katalon.core.util.KeywordUtil

// TC-BUILDERSAAS-AUTH-003 — Verifica que el middleware protege los endpoints: sin sesion
// los /api/* deben responder 401. No usa navegador (pura llamada HttpClient anonima).
String caseId = 'TC-BUILDERSAAS-AUTH-003'
List failures = []
List warnings = []

String base = CustomKeywords.'CommonKeywords.getRequiredGlobal'(
	'BUILDER_SAAS_TEST_URL',
	'https://testing-templet-builder-saas.vercel.app'
).toString().replaceAll('/+$', '')

// Garantiza anonimato aunque otro TC haya adoptado cookies en la misma JVM.
CustomKeywords.'ApiKeywords.clearSession'()

List<String> paths = ['/api/brands', '/api/templates', '/api/layouts']

paths.each { String path ->
	String url = base + path
	Map resp = CustomKeywords.'ApiKeywords.callJson'('GET', url)
	int status = (resp.status ?: -1) as int
	String raw = (resp.raw ?: '').toString()

	if (status != 401) {
		if (status == 200) {
			failures.add('[' + path + '] DATOS EXPUESTOS SIN AUTH: status=200 body=' + raw.take(120))
		} else {
			failures.add('[' + path + '] middleware inesperado: status=' + status + ' body=' + raw.take(120))
		}
	} else {
		if (!raw.toLowerCase().contains('unauthorized')) {
			warnings.add('[' + path + '] 401 pero body no contiene "unauthorized": ' + raw.take(120))
		}
	}
}

CustomKeywords.'CommonKeywords.logCaseSummary'(caseId, failures, warnings)
if (failures) {
	KeywordUtil.markFailedAndStop('[' + caseId + '] Failures: ' + failures)
} else {
	KeywordUtil.logInfo('[' + caseId + '] PASSED' + (warnings ? ' con ' + warnings.size() + ' warnings' : ''))
}
