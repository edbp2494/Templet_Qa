// ─────────────────────────────────────────────────────────────────────────────
// TC: TC-BUILDERS-API-LAYOUTS-001
// Plataforma: Builders | Área: api-regression
// Descripción: TC-BUILDERS-API-LAYOUTS-001 — Smoke regresión US-08 Brand Layouts: filtros brandId/contentTemplateId, CRUD, optimistic locking 409, duplicate
// Suites: Platforms/BuilderSaas/API-Regression
// Precondiciones: credenciales MS en Include/config/templet-credentials.properties
// ─────────────────────────────────────────────────────────────────────────────
import com.kms.katalon.core.util.KeywordUtil

// Smoke de regresion US-08 Brand Layouts (BC-02): filtros, CRUD, optimistic locking, duplicate.
String caseId = 'TC-BUILDERS-API-LAYOUTS-001'
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

// Listado y filtros (criterio de aceptacion)
Map list = CustomKeywords.'ApiKeywords.callJson'('GET', "${base}/api/layouts")
CustomKeywords.'ApiKeywords.assertStatus'(failures, 'GET /api/layouts -> 200', list, [200])
def templateId = list.json?.data?.getAt(0)?.contentTemplateId

Map fBrand = CustomKeywords.'ApiKeywords.callJson'('GET', "${base}/api/layouts?brandId=${brandId}")
CustomKeywords.'ApiKeywords.assertStatus'(failures, 'GET ?brandId -> 200', fBrand, [200])
if (templateId) {
	Map fTpl = CustomKeywords.'ApiKeywords.callJson'('GET', "${base}/api/layouts?contentTemplateId=${templateId}")
	CustomKeywords.'ApiKeywords.assertStatus'(failures, 'GET ?contentTemplateId -> 200', fTpl, [200])
}

// CRUD + locking
Map created = CustomKeywords.'ApiKeywords.callJson'('POST', "${base}/api/layouts",
	[name: "QA-KAT-Layout-${System.currentTimeMillis()}", brandId: brandId, contentTemplateId: templateId, description: 'regresion QA'])
CustomKeywords.'ApiKeywords.assertStatus'(failures, 'POST layout -> 201', created, [201])
String id = created.json?.data?.id

if (id) {
	Map get1 = CustomKeywords.'ApiKeywords.callJson'('GET', "${base}/api/layouts/${id}")
	def v = get1.json?.data?.version
	Map patch = CustomKeywords.'ApiKeywords.callJson'('PATCH', "${base}/api/layouts/${id}", [description: 'editado', version: v])
	CustomKeywords.'ApiKeywords.assertStatus'(failures, 'PATCH version real -> 200', patch, [200])
	Map stale = CustomKeywords.'ApiKeywords.callJson'('PATCH', "${base}/api/layouts/${id}", [description: 'stale', version: v])
	CustomKeywords.'ApiKeywords.assertStatus'(failures, 'PATCH version vieja -> 409', stale, [409])

	Map dup = CustomKeywords.'ApiKeywords.callJson'('POST', "${base}/api/layouts/${id}/duplicate")
	CustomKeywords.'ApiKeywords.assertStatus'(failures, 'POST duplicate -> 201', dup, [201])
	if (dup.json?.data?.id) CustomKeywords.'ApiKeywords.cleanupResource'("${base}/api/layouts/${dup.json.data.id}")

	CustomKeywords.'ApiKeywords.cleanupResource'("${base}/api/layouts/${id}")
	Map gone = CustomKeywords.'ApiKeywords.callJson'('GET', "${base}/api/layouts/${id}")
	CustomKeywords.'ApiKeywords.assertStatus'(failures, 'GET tras DELETE -> 404', gone, [404])
}

// Negativo
Map nf = CustomKeywords.'ApiKeywords.callJson'('GET', "${base}/api/layouts/00000000-0000-0000-0000-000000000000")
CustomKeywords.'ApiKeywords.assertStatus'(failures, 'GET id inexistente -> 404', nf, [404])

if (failures) {
	CustomKeywords.'CommonKeywords.logCaseSummary'(caseId, failures, warnings)
	KeywordUtil.markFailedAndStop("[${caseId}] Failures: ${failures}")
} else {
	CustomKeywords.'CommonKeywords.logCaseSummary'(caseId, failures, warnings)
	KeywordUtil.logInfo("[${caseId}] PASSED" + (warnings ? " con ${warnings.size()} warnings" : ""))
}
