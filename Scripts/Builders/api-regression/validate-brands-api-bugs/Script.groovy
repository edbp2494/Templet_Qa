// ─────────────────────────────────────────────────────────────────────────────
// TC: TC-BUILDERS-API-BRANDS-001
// Plataforma: Builders | Área: api-regression
// Descripción: TC-BUILDERS-API-BRANDS-001 — Regresión bugs US-04 Brand Properties: POST samples 500, GET formatting-props sin ?type 400, POST inválido 500, PATCH techtionary sin parcial
// Suites: Platforms/BuilderSaas/API-Regression
// Precondiciones: credenciales MS en Include/config/templet-credentials.properties
// ─────────────────────────────────────────────────────────────────────────────
import com.kms.katalon.core.util.KeywordUtil

// Regresion de bugs US-04 Brand Properties (BC-01). Ejecutar cuando dev reporte fix.
String caseId = 'TC-BUILDERS-API-BRANDS-001'
List failures = []
List warnings = []

String base = CustomKeywords.'CommonKeywords.getRequiredGlobal'(
	'BUILDER_SAAS_TEST_URL',
	'https://testing-templet-builder-saas.vercel.app'
).toString().replaceAll('/+$', '')

// 07/2026: el middleware exige sesion MS — login + adoptar cookies del browser para HttpClient
if (!CustomKeywords.'TempletPortalKeywords.isBrowserSessionAlive'() ||
    !CustomKeywords.'TempletPortalKeywords.currentUrlSafe'().startsWith(base)) {
    CustomKeywords.'TempletPortalKeywords.openBrowserAndLoginWithMicrosoft'(base)
}
CustomKeywords.'ApiKeywords.useBrowserSession'()

String clientId = 'e4709860-8583-f011-b4cb-00224809ed53'

// BUG 3 (US-04): POST invalido debe dar 400, no 500
Map bad = CustomKeywords.'ApiKeywords.callJson'('POST', "${base}/api/brands", [foo: 'bar'])
CustomKeywords.'ApiKeywords.assertStatus'(failures, 'POST brand invalido -> 400', bad, [400])
if (bad.status == 201 && bad.json?.data?.id) {
	CustomKeywords.'ApiKeywords.cleanupResource'("${base}/api/brands/${bad.json.data.id}")
	warnings.add('[DATA] POST invalido creo registro basura (borrado en cleanup)')
}

// Brand contenedor para sub-recursos
Map created = CustomKeywords.'ApiKeywords.callJson'('POST', "${base}/api/brands",
	[name: "QA-KAT-Brand-${System.currentTimeMillis()}", clientId: clientId, description: 'regresion QA'])
CustomKeywords.'ApiKeywords.assertStatus'(failures, 'POST brand valido -> 201', created, [201])
String brandId = created.json?.data?.id

if (brandId) {
	// BUG 1 (US-04): POST samples daba 500 aun con payload correcto
	Map sample = CustomKeywords.'ApiKeywords.callJson'('POST', "${base}/api/brands/${brandId}/samples",
		[name: 'QA-KAT-sample', downloadLink: 'https://example.com/qa.pdf'])
	CustomKeywords.'ApiKeywords.assertStatus'(failures, 'POST sample -> 201 (bug 500)', sample, [201])
	if (sample.json?.data?.id) {
		CustomKeywords.'ApiKeywords.cleanupResource'("${base}/api/brands/${brandId}/samples/${sample.json.data.id}")
	}

	// BUG 2 (US-04): GET formatting-props sin ?type daba 400
	Map props = CustomKeywords.'ApiKeywords.callJson'('GET', "${base}/api/brands/${brandId}/formatting-props")
	CustomKeywords.'ApiKeywords.assertStatus'(failures, 'GET formatting-props sin type -> 200 (bug 400)', props, [200])

	// BUG 4 (US-04): PATCH techtionary no aceptaba actualizacion parcial
	Map term = CustomKeywords.'ApiKeywords.callJson'('POST', "${base}/api/brands/${brandId}/techtionary",
		[incorrectTerm: 'QA-KAT-wifi', correctTerm: 'QA-KAT-Wi-Fi'])
	CustomKeywords.'ApiKeywords.assertStatus'(failures, 'POST techtionary -> 201', term, [201])
	String termId = term.json?.data?.id
	if (termId) {
		Map full = CustomKeywords.'ApiKeywords.callJson'('GET', "${base}/api/brands/${brandId}/techtionary")
		def rec = (full.json?.data ?: []).find { it.id == termId }
		Map patch = CustomKeywords.'ApiKeywords.callJson'('PATCH', "${base}/api/brands/${brandId}/techtionary/${termId}",
			[correctTerm: 'QA-KAT-WiFi-edit', version: rec?.version])
		CustomKeywords.'ApiKeywords.assertStatus'(failures, 'PATCH techtionary parcial -> 200 (bug 400)', patch, [200])
		CustomKeywords.'ApiKeywords.cleanupResource'("${base}/api/brands/${brandId}/techtionary/${termId}")
	}

	// Limpieza y verificacion 404
	CustomKeywords.'ApiKeywords.cleanupResource'("${base}/api/brands/${brandId}")
	Map gone = CustomKeywords.'ApiKeywords.callJson'('GET', "${base}/api/brands/${brandId}")
	CustomKeywords.'ApiKeywords.assertStatus'(failures, 'GET tras DELETE -> 404', gone, [404])
}

if (failures) {
	CustomKeywords.'CommonKeywords.logCaseSummary'(caseId, failures, warnings)
	KeywordUtil.markFailedAndStop("[${caseId}] Failures: ${failures}")
} else {
	CustomKeywords.'CommonKeywords.logCaseSummary'(caseId, failures, warnings)
	KeywordUtil.logInfo("[${caseId}] PASSED" + (warnings ? " con ${warnings.size()} warnings" : ""))
}
