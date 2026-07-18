// ─────────────────────────────────────────────────────────────────────────────
// TC: TC-BUILDERSAAS-FINSUMMARY-GUARD-ADMIN-003
// Plataforma: BuilderSaas | Área: financial-summary
// Descripción: TC-BUILDERSAAS-FINSUMMARY-GUARD-ADMIN-003 — [E9 · US-03] Guard de rol permitido: role=Admin (el plan decia SuperAdmin; rol canonico real: Admin) accede a /financial-summary y el dashboard carga completo.
// Suites: Platforms/BuilderSaas/Financial-Summary
// Precondiciones: credenciales MS en Include/config/templet-credentials.properties
// ─────────────────────────────────────────────────────────────────────────────
import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI

// TC-BUILDERSAAS-FINSUMMARY-GUARD-ADMIN-003 — [E9 · US-03] Guard de rol: acceso PERMITIDO.
// NOTA plan vs app real: el plan decia "SuperAdmin"; el rol canonico de la app es "Admin"
// (atributo title del switcher DEV, verificado en vivo 2026-07-14). Roles: Admin |
// ContractOwner | Specialist. Sin ?role= la app tambien cae en Admin por defecto.
String caseId = 'TC-BUILDERSAAS-FINSUMMARY-GUARD-ADMIN-003'
List failures = []
List warnings = []

String base = CustomKeywords.'CommonKeywords.getRequiredGlobal'(
	'BUILDER_SAAS_TEST_URL',
	'https://testing-templet-builder-saas.vercel.app'
).toString().replaceAll('/+$', '')

boolean sessionAlive = true
try { WebUI.getUrl() } catch (Exception ignored) { sessionAlive = false }
if (!sessionAlive) {
	WebUI.openBrowser('')
	WebUI.maximizeWindow()
}

// ── Acceso directo a /financial-summary con rol Admin ───────────────────────
WebUI.navigateToUrl(base + '/financial-summary?role=Admin')
WebUI.waitForPageLoad(30)
WebUI.delay(2)

String urlNow = CustomKeywords.'TempletPortalKeywords.currentUrlSafe'()
if (!urlNow.contains('/financial-summary')) {
	failures.add('[GUARD Admin] Esperado: acceso permitido a /financial-summary. Encontrado: redirect a ' + urlNow)
}

boolean h1Ok = CustomKeywords.'TempletPortalKeywords.verifyXPathPresent'(
	'h1_financial_summary',
	"//h1[normalize-space(.)='Financial Summary']",
	10
)
if (!h1Ok) {
	failures.add('[GUARD Admin] H1 "Financial Summary" no visible con role=Admin.')
}

boolean kpiOk = CustomKeywords.'TempletPortalKeywords.verifyXPathPresent'(
	'kpi_total_spend',
	"//p[normalize-space(.)='Total Spend']",
	10
)
if (!kpiOk) {
	failures.add('[GUARD Admin] KPI "Total Spend" no visible: el dashboard no cargo completo con role=Admin.')
}

// El sidebar tambien debe ofrecer el item para Admin.
boolean itemVisible = CustomKeywords.'TempletPortalKeywords.verifyXPathPresent'(
	'sidebar_link_financial_summary',
	"//aside//a[@href='/financial-summary']",
	5
)
if (!itemVisible) {
	warnings.add('[GUARD Admin] Item "Financial Summary" no visible en el sidebar con role=Admin.')
}

CustomKeywords.'TempletPortalKeywords.captureCaseScreenshot'(caseId, 'guard_admin_allowed')

String stamp = new Date().format('yyyyMMdd_HHmmss')
Map snap = [
	caseId   : caseId,
	timestamp: new Date().format("yyyy-MM-dd'T'HH:mm:ss"),
	baseUrl  : base,
	role     : 'Admin',
	url      : urlNow,
	failures : failures,
	warnings : warnings
]
String dir = System.getProperty('user.dir') + '/Reports/BuilderSaas/snapshots'
try {
	CustomKeywords.'TempletPortalKeywords.writeJsonSnapshot'(dir + '/financial_summary_guard_admin_latest.json', snap)
	CustomKeywords.'TempletPortalKeywords.writeJsonSnapshot'(dir + '/history/financial_summary_guard_admin_' + stamp + '.json', snap)
} catch (Throwable t) {
	warnings.add('[SNAPSHOT] No se pudo escribir snapshot: ' + t.getMessage())
}

CustomKeywords.'CommonKeywords.logCaseSummary'(caseId, failures, warnings)
if (failures) {
	KeywordUtil.markFailedAndStop('[' + caseId + '] Failures: ' + failures)
} else {
	KeywordUtil.logInfo('[' + caseId + '] PASSED' + (warnings ? ' con ' + warnings.size() + ' warnings' : ''))
}
