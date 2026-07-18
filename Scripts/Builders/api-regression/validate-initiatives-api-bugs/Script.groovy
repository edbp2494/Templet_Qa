// ─────────────────────────────────────────────────────────────────────────────
// TC: TC-BUILDERS-API-INITIATIVES-001
// Plataforma: Builders | Área: api-regression
// Descripción: TC-BUILDERS-API-INITIATIVES-001 — Regresión bugs US-09 Initiatives: POST sin validación (debe 400), version 0 en POST, criterio scope multi-valor
// Suites: Platforms/BuilderSaas/API-Regression
// Precondiciones: credenciales MS en Include/config/templet-credentials.properties
// ─────────────────────────────────────────────────────────────────────────────
import com.kms.katalon.core.util.KeywordUtil

// Regresion de bugs US-09 Initiatives (BC-09): POST sin validacion, version:0 y scope multi-valor.
String caseId = 'TC-BUILDERS-API-INITIATIVES-001'
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

String clientId = '662af832-4f66-ef11-bfe3-000d3a37bb95'
String brandId  = '9e0fbc87-2c13-eb11-a813-000d3a3ab875'

// BUG 1: POST invalido debe dar 400 (hoy 201 + registro basura)
Map bad = CustomKeywords.'ApiKeywords.callJson'('POST', "${base}/api/initiatives", [foo: 'bar'])
CustomKeywords.'ApiKeywords.assertStatus'(failures, 'POST invalido -> 400 (bug 201)', bad, [400])
if (bad.status == 201 && bad.json?.data?.id) {
	CustomKeywords.'ApiKeywords.cleanupResource'("${base}/api/initiatives/${bad.json.data.id}")
	warnings.add('[DATA] POST invalido creo registro basura (borrado en cleanup)')
}

// BUG 2 + criterio scope comma-separated
Map created = CustomKeywords.'ApiKeywords.callJson'('POST', "${base}/api/initiatives", [
	name: "QA-KAT-Initiative-${System.currentTimeMillis()}",
	clientId: clientId, brandId: brandId, description: 'regresion QA',
	priority: 0, initiativeScope: [473400000, 473400001],
	detailedStatus: 473400000, initiativeType: 473400006, team: 473400001
])
CustomKeywords.'ApiKeywords.assertStatus'(failures, 'POST valido -> 201', created, [201])
String id = created.json?.data?.id
def postVersion = created.json?.data?.version

if (id) {
	Map get1 = CustomKeywords.'ApiKeywords.callJson'('GET', "${base}/api/initiatives/${id}")
	def realVersion = get1.json?.data?.version
	if (postVersion == 0 || postVersion != realVersion) {
		failures.add("[API][version en POST] POST devolvio version=${postVersion}, GET devolvio ${realVersion} — deben coincidir")
	}

	// Criterio de aceptacion: scope multi-valor se guarda y recupera integro
	def scope = get1.json?.data?.initiativeScope
	if (!(scope instanceof List) || scope.size() != 2) {
		failures.add("[DATA][initiativeScope] esperado [473400000, 473400001], obtenido ${scope}")
	}

	Map patch = CustomKeywords.'ApiKeywords.callJson'('PATCH', "${base}/api/initiatives/${id}",
		[description: 'editado por regresion', version: postVersion])
	CustomKeywords.'ApiKeywords.assertStatus'(failures, 'PATCH con version del POST -> 200 (bug 409)', patch, [200])

	CustomKeywords.'ApiKeywords.cleanupResource'("${base}/api/initiatives/${id}")
	Map gone = CustomKeywords.'ApiKeywords.callJson'('GET', "${base}/api/initiatives/${id}")
	CustomKeywords.'ApiKeywords.assertStatus'(failures, 'GET tras DELETE -> 404', gone, [404])
}

if (failures) {
	CustomKeywords.'CommonKeywords.logCaseSummary'(caseId, failures, warnings)
	KeywordUtil.markFailedAndStop("[${caseId}] Failures: ${failures}")
} else {
	CustomKeywords.'CommonKeywords.logCaseSummary'(caseId, failures, warnings)
	KeywordUtil.logInfo("[${caseId}] PASSED" + (warnings ? " con ${warnings.size()} warnings" : ""))
}
