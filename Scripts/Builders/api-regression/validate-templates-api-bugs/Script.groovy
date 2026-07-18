// ─────────────────────────────────────────────────────────────────────────────
// TC: TC-BUILDERS-API-TEMPLATES-001
// Plataforma: Builders | Área: api-regression
// Descripción: TC-BUILDERS-API-TEMPLATES-001 — Regresión bugs US-07 Templates: POST sin validación crea registro vacío (debe 400), POST devuelve version 0 (PATCH inmediato 409)
// Suites: Platforms/BuilderSaas/API-Regression
// Precondiciones: credenciales MS en Include/config/templet-credentials.properties
// ─────────────────────────────────────────────────────────────────────────────
import com.kms.katalon.core.util.KeywordUtil

// Regresion de bugs US-07 Templates (BC-03): POST sin validacion y version:0.
String caseId = 'TC-BUILDERS-API-TEMPLATES-001'
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

String brandId = '9e0fbc87-2c13-eb11-a813-000d3a3ab875'

// BUG 1: POST invalido debe dar 400 (hoy 201 + registro basura)
Map bad = CustomKeywords.'ApiKeywords.callJson'('POST', "${base}/api/templates", [foo: 'bar'])
CustomKeywords.'ApiKeywords.assertStatus'(failures, 'POST invalido -> 400 (bug 201)', bad, [400])
if (bad.status == 201 && bad.json?.data?.id) {
	CustomKeywords.'ApiKeywords.cleanupResource'("${base}/api/templates/${bad.json.data.id}")
	warnings.add('[DATA] POST invalido creo registro basura (borrado en cleanup)')
}

// BUG 2: POST debe devolver version real (hoy version:0 -> PATCH inmediato da 409)
Map created = CustomKeywords.'ApiKeywords.callJson'('POST', "${base}/api/templates",
	[name: "QA-KAT-Template-${System.currentTimeMillis()}", brandId: brandId, resourceType: 473400002, description: 'regresion QA'])
CustomKeywords.'ApiKeywords.assertStatus'(failures, 'POST valido -> 201', created, [201])
String id = created.json?.data?.id
def postVersion = created.json?.data?.version

if (id) {
	Map get1 = CustomKeywords.'ApiKeywords.callJson'('GET', "${base}/api/templates/${id}")
	def realVersion = get1.json?.data?.version
	if (postVersion == 0 || postVersion != realVersion) {
		failures.add("[API][version en POST] POST devolvio version=${postVersion}, GET devolvio ${realVersion} — deben coincidir")
	}

	// PATCH usando la version del POST debe funcionar sin GET extra
	Map patch = CustomKeywords.'ApiKeywords.callJson'('PATCH', "${base}/api/templates/${id}",
		[description: 'editado por regresion', version: postVersion])
	CustomKeywords.'ApiKeywords.assertStatus'(failures, 'PATCH con version del POST -> 200 (bug 409)', patch, [200])

	// Locking sigue funcionando: version vieja -> 409
	Map stale = CustomKeywords.'ApiKeywords.callJson'('PATCH', "${base}/api/templates/${id}",
		[description: 'stale', version: realVersion])
	CustomKeywords.'ApiKeywords.assertStatus'(failures, 'PATCH version vieja -> 409', stale, [409])

	CustomKeywords.'ApiKeywords.cleanupResource'("${base}/api/templates/${id}")
	Map gone = CustomKeywords.'ApiKeywords.callJson'('GET', "${base}/api/templates/${id}")
	CustomKeywords.'ApiKeywords.assertStatus'(failures, 'GET tras DELETE -> 404', gone, [404])
}

if (failures) {
	CustomKeywords.'CommonKeywords.logCaseSummary'(caseId, failures, warnings)
	KeywordUtil.markFailedAndStop("[${caseId}] Failures: ${failures}")
} else {
	CustomKeywords.'CommonKeywords.logCaseSummary'(caseId, failures, warnings)
	KeywordUtil.logInfo("[${caseId}] PASSED" + (warnings ? " con ${warnings.size()} warnings" : ""))
}
