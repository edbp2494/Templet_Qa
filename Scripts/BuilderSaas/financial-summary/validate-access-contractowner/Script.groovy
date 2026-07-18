// ─────────────────────────────────────────────────────────────────────────────
// TC: TC-BUILDERSAAS-FINSUMMARY-ACCESS-001
// Plataforma: BuilderSaas | Área: financial-summary
// Descripción: TC-BUILDERSAAS-FINSUMMARY-ACCESS-001 — [E9 · US-03] Login mock con ?role=ContractOwner, verifica "Financial Summary" visible en el sidebar (grupo Track; el plan decia Finance) y que el click navega a /financial-summary. Primer TC de la suite: abre el navegador.
// Suites: Platforms/BuilderSaas/Financial-Summary
// Precondiciones: credenciales MS en Include/config/templet-credentials.properties
// ─────────────────────────────────────────────────────────────────────────────
import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI

// TC-BUILDERSAAS-FINSUMMARY-ACCESS-001 — [E9 · US-03] Acceso con rol ContractOwner.
// Primer TC de la suite Financial-Summary.ts: ABRE el navegador (los siguientes reusan driver).
// Login mock del Builder SaaS: query param ?role=ContractOwner (sin SSO, sin bypass Vercel;
// verificado en vivo 2026-07-14: sin cookies/localStorage, el rol persiste en memoria
// durante la navegacion client-side).
// NOTA plan vs app real: el plan decia grupo 'Finance' en el sidebar; el grupo real es 'Track'.
String caseId = 'TC-BUILDERSAAS-FINSUMMARY-ACCESS-001'
List failures = []
List warnings = []

String base = CustomKeywords.'CommonKeywords.getRequiredGlobal'(
	'BUILDER_SAAS_TEST_URL',
	'https://testing-templet-builder-saas.vercel.app'
).toString().replaceAll('/+$', '')

// ── PASO 1: abrir browser y "login" con rol ContractOwner ──────────────────
WebUI.openBrowser('')
WebUI.maximizeWindow()
WebUI.navigateToUrl(base + '/home?role=ContractOwner')
WebUI.waitForPageLoad(30)
WebUI.delay(2)

// Badge de rol en /home confirma que el mock de rol tomo efecto (en /home NO esta hardcodeado).
boolean roleOk = CustomKeywords.'TempletPortalKeywords.verifyXPathPresent'(
	'badge_contract_owner',
	"//*[normalize-space(.)='Contract Owner'][not(*)]",
	10
)
if (!roleOk) {
	failures.add('[PASO 1: login rol] Badge "Contract Owner" no visible en /home?role=ContractOwner. El mock de rol por query param no tomo efecto.')
}
boolean ownerDash = CustomKeywords.'TempletPortalKeywords.verifyXPathPresent'(
	'h1_owner_dashboard',
	"//h1[normalize-space(.)='Owner Dashboard']",
	5
)
if (!ownerDash) {
	warnings.add('[PASO 1: home] H1 "Owner Dashboard" no encontrado (copy del home pudo cambiar).')
}

// ── PASO 2: "Financial Summary" visible en el sidebar (grupo Track) ─────────
boolean groupTrack = CustomKeywords.'TempletPortalKeywords.verifyXPathPresent'(
	'sidebar_group_track',
	"//aside//p[normalize-space(.)='Track']",
	5
)
if (!groupTrack) {
	warnings.add('[PASO 2: sidebar] Grupo "Track" no encontrado (el plan E9-US03 decia "Finance"; verificar si renombraron el grupo).')
}
boolean itemVisible = CustomKeywords.'TempletPortalKeywords.verifyXPathPresent'(
	'sidebar_link_financial_summary',
	"//aside//a[@href='/financial-summary']",
	10
)
if (!itemVisible) {
	itemVisible = CustomKeywords.'TempletPortalKeywords.verifyXPathPresent'(
		'sidebar_link_financial_summary_fallback',
		"//aside//a[normalize-space(.)='Financial Summary']",
		5
	)
	if (itemVisible) {
		warnings.add('[SELECTOR] Link Financial Summary encontrado solo por texto; el @href cambio. Actualizar element-map.')
	}
}
if (!itemVisible) {
	failures.add('[PASO 2: sidebar] Item "Financial Summary" NO visible en el sidebar con rol ContractOwner.')
}

// ── PASO 3: click navega a /financial-summary ───────────────────────────────
if (itemVisible) {
	boolean clicked = CustomKeywords.'TempletPortalKeywords.clickXPathAndKeepValidSession'(
		'click_financial_summary',
		"//aside//a[@href='/financial-summary']",
		10
	)
	if (!clicked) {
		clicked = CustomKeywords.'TempletPortalKeywords.clickXPathAndKeepValidSession'(
			'click_financial_summary_fallback',
			"//aside//a[normalize-space(.)='Financial Summary']",
			5
		)
	}
	if (!clicked) {
		failures.add('[PASO 3: click] No se pudo clickear "Financial Summary" en el sidebar.')
	} else {
		WebUI.delay(2)
		String urlNow = CustomKeywords.'TempletPortalKeywords.currentUrlSafe'()
		if (!urlNow.contains('/financial-summary')) {
			failures.add('[PASO 3: navegacion] Esperado: URL con /financial-summary. Encontrado: ' + urlNow)
		}
		boolean h1Ok = CustomKeywords.'TempletPortalKeywords.verifyXPathPresent'(
			'h1_financial_summary',
			"//h1[normalize-space(.)='Financial Summary']",
			10
		)
		if (!h1Ok) {
			failures.add('[PASO 3: pantalla] H1 "Financial Summary" no visible tras el click.')
		}
		boolean activeOk = CustomKeywords.'TempletPortalKeywords.verifyXPathPresent'(
			'sidebar_item_active',
			"//aside//a[@href='/financial-summary' and contains(@class,'bg-[#00FF7F]')]",
			5
		)
		if (!activeOk) {
			warnings.add('[PASO 3: estado activo] El item no muestra la clase activa bg-[#00FF7F] (clase Tailwind arbitraria, selector debil).')
		}
	}
}

CustomKeywords.'TempletPortalKeywords.captureCaseScreenshot'(caseId, 'access_contractowner')

// ── Snapshot de evidencia ───────────────────────────────────────────────────
String stamp = new Date().format('yyyyMMdd_HHmmss')
Map snap = [
	caseId   : caseId,
	timestamp: new Date().format("yyyy-MM-dd'T'HH:mm:ss"),
	baseUrl  : base,
	role     : 'ContractOwner',
	url      : CustomKeywords.'TempletPortalKeywords.currentUrlSafe'(),
	failures : failures,
	warnings : warnings
]
String dir = System.getProperty('user.dir') + '/Reports/BuilderSaas/snapshots'
try {
	CustomKeywords.'TempletPortalKeywords.writeJsonSnapshot'(dir + '/financial_summary_access_latest.json', snap)
	CustomKeywords.'TempletPortalKeywords.writeJsonSnapshot'(dir + '/history/financial_summary_access_' + stamp + '.json', snap)
} catch (Throwable t) {
	warnings.add('[SNAPSHOT] No se pudo escribir snapshot: ' + t.getMessage())
}

CustomKeywords.'CommonKeywords.logCaseSummary'(caseId, failures, warnings)
if (failures) {
	KeywordUtil.markFailedAndStop('[' + caseId + '] Failures: ' + failures)
} else {
	KeywordUtil.logInfo('[' + caseId + '] PASSED' + (warnings ? ' con ' + warnings.size() + ' warnings' : ''))
}
