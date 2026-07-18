// ─────────────────────────────────────────────────────────────────────────────
// TC: TC-BUILDERSAAS-FINSUMMARY-GUARD-DENY-004
// Plataforma: BuilderSaas | Área: financial-summary
// Descripción: TC-BUILDERSAAS-FINSUMMARY-GUARD-DENY-004 — [E9 · US-03] Guard de rol denegado: role=Specialist NO debe acceder a /financial-summary. 🔴 FALLA POR DISEÑO desde 14/07/2026: RBAC no implementado, el dashboard renderiza completo (patron TC-ARCHMATCH-AGENDAR-BUGCONFIRMAR-002). Warnings adicionales: sidebar no oculta el item y badge hardcodeado "Contract Owner".
// Suites: Platforms/BuilderSaas/Financial-Summary
// Precondiciones: credenciales MS en Include/config/templet-credentials.properties
// ─────────────────────────────────────────────────────────────────────────────
import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI

// TC-BUILDERSAAS-FINSUMMARY-GUARD-DENY-004 — [E9 · US-03] Guard de rol: acceso DENEGADO.
// El plan decia "Project Specialist"; el valor canonico del query param es "Specialist"
// (display en UI: "Project Specialist").
//
// 🔴 BUG conocido (14/07/2026, verificado en vivo): el RBAC NO esta implementado.
// /financial-summary?role=Specialist renderiza el dashboard COMPLETO. Este TC
// **falla por diseño** mientras el bug exista (mismo criterio que
// TC-ARCHMATCH-AGENDAR-BUGCONFIRMAR-002). Cuando el guard se implemente y este TC
// pase, retirar esta nota y confirmar el mecanismo real de bloqueo (redirect vs 403).
// Bugs secundarios que este TC evidencia como warnings:
//   - Sidebar no oculta "Financial Summary" para Specialist.
//   - Badge del header hardcodeado "Contract Owner" con cualquier rol.
String caseId = 'TC-BUILDERSAAS-FINSUMMARY-GUARD-DENY-004'
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

// ── PASO 1: acceso directo con rol NO autorizado ────────────────────────────
WebUI.navigateToUrl(base + '/financial-summary?role=Specialist')
WebUI.waitForPageLoad(30)
WebUI.delay(2)

String urlNow = CustomKeywords.'TempletPortalKeywords.currentUrlSafe'()
boolean dashboardVisible = CustomKeywords.'TempletPortalKeywords.verifyXPathPresent'(
	'h1_financial_summary',
	"//h1[normalize-space(.)='Financial Summary']",
	10
) && CustomKeywords.'TempletPortalKeywords.verifyXPathPresent'(
	'kpi_total_spend',
	"//p[normalize-space(.)='Total Spend']",
	5
)

if (dashboardVisible && urlNow.contains('/financial-summary')) {
	failures.add('[GUARD Specialist] Esperado (E9-US03): acceso BLOQUEADO a /financial-summary ' +
		'(redirect o pantalla de acceso denegado). Encontrado: dashboard COMPLETO renderizado con role=Specialist. ' +
		'RBAC no implementado — BUG conocido 14/07/2026; este TC falla por diseño hasta el fix. ' +
		'ACCION SUGERIDA: bug de la app (middleware/guard), no de selector.')

	// Evidencia del badge hardcodeado mientras estamos en la pantalla.
	boolean badgeOwner = CustomKeywords.'TempletPortalKeywords.verifyXPathPresent'(
		'badge_contract_owner_hardcoded',
		"//*[normalize-space(.)='Contract Owner'][not(*)]",
		3
	)
	if (badgeOwner) {
		warnings.add('[BADGE] Header muestra "Contract Owner" con role=Specialist: badge hardcodeado en /financial-summary.')
	}
} else {
	// Camino esperado cuando implementen el guard: verificar el mecanismo de bloqueo.
	KeywordUtil.logInfo('[GUARD Specialist] Dashboard no visible con role=Specialist. Verificando mecanismo de bloqueo. URL=' + urlNow)
	if (urlNow.contains('/financial-summary')) {
		boolean deniedMsg = CustomKeywords.'TempletPortalKeywords.verifyXPathPresent'(
			'denied_marker',
			"//*[contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'denied') or contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'not authorized') or contains(normalize-space(.),'403')][not(*)]",
			5
		)
		if (!deniedMsg) {
			warnings.add('[GUARD Specialist] La URL sigue en /financial-summary sin dashboard ni mensaje de denegacion claro. Confirmar el mecanismo de bloqueo implementado y ajustar este TC.')
		}
	}
}

// ── PASO 2: el sidebar no deberia ofrecer el item a Specialist ──────────────
WebUI.navigateToUrl(base + '/home?role=Specialist')
WebUI.waitForPageLoad(30)
WebUI.delay(2)

boolean badgeSpec = CustomKeywords.'TempletPortalKeywords.verifyXPathPresent'(
	'badge_project_specialist',
	"//*[normalize-space(.)='Project Specialist'][not(*)]",
	5
)
if (!badgeSpec) {
	warnings.add('[PASO 2] Badge "Project Specialist" no visible en /home?role=Specialist (verificar mock de rol).')
}

boolean itemVisibleForSpecialist = CustomKeywords.'TempletPortalKeywords.verifyXPathPresent'(
	'sidebar_link_financial_summary',
	"//aside//a[@href='/financial-summary']",
	5
)
if (itemVisibleForSpecialist) {
	warnings.add('[SIDEBAR] Item "Financial Summary" visible en el sidebar para Specialist. ' +
		'Cuando implementen el RBAC deberia ocultarse; hoy queda como warning para no duplicar el failure del guard.')
}

CustomKeywords.'TempletPortalKeywords.captureCaseScreenshot'(caseId, 'guard_specialist_denied')

String stamp = new Date().format('yyyyMMdd_HHmmss')
Map snap = [
	caseId            : caseId,
	timestamp         : new Date().format("yyyy-MM-dd'T'HH:mm:ss"),
	baseUrl           : base,
	role              : 'Specialist',
	url               : urlNow,
	dashboardVisible  : dashboardVisible,
	sidebarItemVisible: itemVisibleForSpecialist,
	failures          : failures,
	warnings          : warnings
]
String dir = System.getProperty('user.dir') + '/Reports/BuilderSaas/snapshots'
try {
	CustomKeywords.'TempletPortalKeywords.writeJsonSnapshot'(dir + '/financial_summary_guard_deny_latest.json', snap)
	CustomKeywords.'TempletPortalKeywords.writeJsonSnapshot'(dir + '/history/financial_summary_guard_deny_' + stamp + '.json', snap)
} catch (Throwable t) {
	warnings.add('[SNAPSHOT] No se pudo escribir snapshot: ' + t.getMessage())
}

CustomKeywords.'CommonKeywords.logCaseSummary'(caseId, failures, warnings)
if (failures) {
	KeywordUtil.markFailedAndStop('[' + caseId + '] Failures: ' + failures)
} else {
	KeywordUtil.logInfo('[' + caseId + '] PASSED' + (warnings ? ' con ' + warnings.size() + ' warnings' : ''))
}
