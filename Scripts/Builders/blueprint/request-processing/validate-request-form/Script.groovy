// ─────────────────────────────────────────────────────────────────────────────
// TC: TC-BUILDERS-BLUEPRINT-REQUEST-FORM-001
// Plataforma: Builders | Área: blueprint/request-processing
// Descripción: TC2: Abre el formulario de creacion de Blueprint (via boton New Blueprint o URL directa), descubre campos disponibles via DOM, verifica inputs y selects visibles, captura estado para ajuste.
// Suites: Platforms/Builders/Blueprint/Request-Processing
// Precondiciones: credenciales MS en Include/config/templet-credentials.properties
// ─────────────────────────────────────────────────────────────────────────────
import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import com.kms.katalon.core.model.FailureHandling as FailureHandling

String caseId = 'TC-BUILDERS-BLUEPRINT-REQUEST-FORM-001'
List failures = []
List warnings = []

// ── Reusar sesion del TC anterior ─────────────────────────────────────────────
String buildersTestUrl = CustomKeywords.'CommonKeywords.getRequiredGlobal'(
	'BUILDERS_TEST_URL',
	'https://testing-templet-builders.vercel.app/'
)
String managerUrl = buildersTestUrl.replaceAll('/+$', '') + '/blueprint/manager/power-user'

if (!CustomKeywords.'TempletPortalKeywords.ensureAuthenticatedSession'(managerUrl, 15)) {
	failures.add('[AUTH] Sesion perdida al navegar a blueprint manager')
	CustomKeywords.'CommonKeywords.logCaseSummary'(caseId, failures, warnings)
	KeywordUtil.markFailedAndStop("[${caseId}] Sin sesion")
}

// ── 1. Abrir modal "Create New Blueprint" ─────────────────────────────────────
boolean clicked = CustomKeywords.'TempletPortalKeywords.clickXPathAndKeepValidSession'(
	'btn_new_blueprint',
	"//button[normalize-space(.)='New Blueprint']",
	10
)
if (!clicked) {
	clicked = CustomKeywords.'TempletPortalKeywords.clickXPathAndKeepValidSession'(
		'btn_new_blueprint_fallback',
		"//button[contains(normalize-space(.),'New') or contains(normalize-space(.),'Blueprint')]",
		5
	)
	if (clicked) {
		warnings.add('[SELECTOR] btn New Blueprint via fallback')
	} else {
		failures.add('[BTN] No se pudo abrir modal New Blueprint')
		CustomKeywords.'CommonKeywords.logCaseSummary'(caseId, failures, warnings)
		KeywordUtil.markFailedAndStop("[${caseId}] Sin modal")
	}
}

// Esperar que el modal "Create New Blueprint" monte (título exacto conocido)
boolean modalVisible = CustomKeywords.'TempletPortalKeywords.verifyXPathPresent'(
	'modal_title',
	"//h2[normalize-space(.)='Create New Blueprint']",
	10
)
if (!modalVisible) {
	failures.add('[MODAL] Modal "Create New Blueprint" no apareció después de click')
	CustomKeywords.'CommonKeywords.logCaseSummary'(caseId, failures, warnings)
	KeywordUtil.markFailedAndStop("[${caseId}] Sin modal")
}
KeywordUtil.logInfo('[MODAL] Modal "Create New Blueprint" visible ✓')

// ── 2. Verificar campos del formulario ────────────────────────────────────────
// Campo Name (input de texto)
boolean nameFieldPresent = CustomKeywords.'TempletPortalKeywords.verifyXPathPresent'(
	'field_name',
	"//h2[normalize-space(.)='Create New Blueprint']/ancestor::*[@role='dialog']//input[@type='text' or not(@type)]",
	5
)
if (!nameFieldPresent) {
	// Fallback: cualquier input visible en la página
	nameFieldPresent = CustomKeywords.'TempletPortalKeywords.verifyXPathPresent'(
		'field_name_fallback',
		"//input[@type='text']",
		3
	)
	if (nameFieldPresent) {
		warnings.add('[SELECTOR] Campo Name via fallback //input[@type="text"]')
	} else {
		warnings.add('[FORM] Campo Name no encontrado — verificar estructura del modal')
	}
} else {
	KeywordUtil.logInfo('[FORM] Campo Name ✓')
}

// Botón Save Blueprint
boolean saveBtnPresent = CustomKeywords.'TempletPortalKeywords.verifyXPathPresent'(
	'btn_save_blueprint',
	"//button[normalize-space(.)='Save Blueprint']",
	5
)
if (!saveBtnPresent) {
	warnings.add('[FORM] Botón "Save Blueprint" no encontrado')
} else {
	KeywordUtil.logInfo('[FORM] Botón "Save Blueprint" ✓')
}

// Botón Cancel
boolean cancelBtnPresent = CustomKeywords.'TempletPortalKeywords.verifyXPathPresent'(
	'btn_cancel',
	"//button[normalize-space(.)='Cancel']",
	3
)
if (!cancelBtnPresent) {
	warnings.add('[FORM] Botón Cancel no encontrado')
} else {
	KeywordUtil.logInfo('[FORM] Botón Cancel ✓')
}

// ── 3. Dump completo del modal via JS ─────────────────────────────────────────
Map formState = (Map) WebUI.executeJavaScript('''
	var dialog = document.querySelector("[role='dialog']");
	var inputs = dialog
		? Array.from(dialog.querySelectorAll('input,textarea,select')).map(function(el) {
			return { tag: el.tagName, type: el.type||'', name: el.name||'', id: el.id||'',
				placeholder: (el.placeholder||'').substring(0,60), visible: el.offsetParent !== null };
		  })
		: [];
	var buttons = dialog
		? Array.from(dialog.querySelectorAll('button')).map(function(b) {
			return (b.innerText||'').trim().replace(/\\s+/g,' ').substring(0,60);
		  }).filter(function(t){ return t.length > 0; })
		: [];
	var labels = dialog
		? Array.from(dialog.querySelectorAll('label')).map(function(l) {
			return (l.innerText||'').trim().replace(/\\s+/g,' ').substring(0,60);
		  }).filter(function(t){ return t.length > 0; })
		: [];
	return { inputs: inputs, buttons: buttons, labels: labels, hasDialog: !!dialog };
''', null)

KeywordUtil.logInfo("[FORM] hasDialog: ${formState?.hasDialog}")
KeywordUtil.logInfo("[FORM] labels: ${formState?.labels}")
KeywordUtil.logInfo("[FORM] inputs: ${formState?.inputs}")
KeywordUtil.logInfo("[FORM] buttons: ${formState?.buttons}")

// ── Screenshot ────────────────────────────────────────────────────────────────
CustomKeywords.'TempletPortalKeywords.captureCaseScreenshot'(caseId, 'form_modal')

if (failures) {
	CustomKeywords.'CommonKeywords.logCaseSummary'(caseId, failures, warnings)
	KeywordUtil.markFailedAndStop("[${caseId}] Failures: ${failures}")
} else {
	CustomKeywords.'CommonKeywords.logCaseSummary'(caseId, failures, warnings)
	KeywordUtil.logInfo("[${caseId}] PASSED" + (warnings ? " con ${warnings.size()} warnings" : ""))
}
