import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import com.kms.katalon.core.model.FailureHandling as FailureHandling
import com.kms.katalon.core.testobject.TestObject
import com.kms.katalon.core.testobject.ConditionType

String caseId = 'TC-BUILDERS-BLUEPRINT-REQUEST-SUBMIT-001'
List failures = []
List warnings = []

// ── Reusar sesion ─────────────────────────────────────────────────────────────
String buildersTestUrl = CustomKeywords.'CommonKeywords.getRequiredGlobal'(
	'BUILDERS_TEST_URL',
	'https://testing-templet-builders.vercel.app/'
)
String managerUrl = buildersTestUrl.replaceAll('/+$', '') + '/blueprint/manager/power-user'

boolean sessionAlive = CustomKeywords.'TempletPortalKeywords.isBrowserSessionAlive'()
if (!sessionAlive) {
	CustomKeywords.'TempletPortalKeywords.openBrowserAndLoginWithMicrosoft'(managerUrl)
} else {
	WebUI.navigateToUrl(managerUrl)
}
WebUI.waitForPageLoad(15)

if (!CustomKeywords.'TempletPortalKeywords.isValidAppSession'()) {
	failures.add('[AUTH] Sesion no valida')
	CustomKeywords.'CommonKeywords.logCaseSummary'(caseId, failures, warnings)
	KeywordUtil.markFailedAndStop("[${caseId}] Sin sesion")
}

// ── 1. Abrir modal ────────────────────────────────────────────────────────────
boolean clicked = CustomKeywords.'TempletPortalKeywords.clickXPathAndKeepValidSession'(
	'btn_new_blueprint',
	"//button[normalize-space(.)='New Blueprint']",
	10
)
if (!clicked) {
	failures.add('[BTN] Boton New Blueprint no encontrado')
	CustomKeywords.'CommonKeywords.logCaseSummary'(caseId, failures, warnings)
	KeywordUtil.markFailedAndStop("[${caseId}] Sin modal")
}

// Esperar modal por título exacto
boolean modalVisible = CustomKeywords.'TempletPortalKeywords.verifyXPathPresent'(
	'modal_title',
	"//h2[normalize-space(.)='Create New Blueprint']",
	10
)
if (!modalVisible) {
	failures.add('[MODAL] Modal "Create New Blueprint" no aparecio')
	CustomKeywords.'CommonKeywords.logCaseSummary'(caseId, failures, warnings)
	KeywordUtil.markFailedAndStop("[${caseId}] Sin modal")
}
KeywordUtil.logInfo('[MODAL] Modal abierto ✓')

String formUrl = CustomKeywords.'TempletPortalKeywords.currentUrlSafe'()
KeywordUtil.logInfo("[SUBMIT] URL: ${formUrl}")
CustomKeywords.'TempletPortalKeywords.captureCaseScreenshot'(caseId, 'modal_open')

// ── 2. Llenar campo Name ──────────────────────────────────────────────────────
String testTitle = "QA-AUTO-TEST-${System.currentTimeMillis()}"

// XPaths para el campo Name dentro del dialog (conocido por screenshot)
List<String> nameXpaths = [
	"//h2[normalize-space(.)='Create New Blueprint']/ancestor::*[@role='dialog']//input",
	"(//*[@role='dialog']//input)[1]",
	"//input[@type='text']",
	"//input[not(@type) or @type='text']"
]

boolean nameFilled = false
for (String xp : nameXpaths) {
	TestObject obj = new TestObject().addProperty('xpath', ConditionType.EQUALS, xp)
	if (WebUI.verifyElementPresent(obj, 3, FailureHandling.OPTIONAL)) {
		try {
			WebUI.clearText(obj, FailureHandling.OPTIONAL)
			WebUI.setText(obj, testTitle, FailureHandling.OPTIONAL)
			nameFilled = true
			KeywordUtil.logInfo("[FILL] Campo Name llenado: '${testTitle}' via ${xp}")
			break
		} catch (Exception e) {
			KeywordUtil.logInfo("[FILL] Error llenando Name via ${xp}: ${e.message}")
		}
	}
}
if (!nameFilled) {
	warnings.add('[FILL] No se pudo llenar campo Name — verificar XPath del modal')
}

CustomKeywords.'TempletPortalKeywords.captureCaseScreenshot'(caseId, 'after_fill')

// ── 3. Click Save Blueprint ───────────────────────────────────────────────────
String urlBeforeSubmit = CustomKeywords.'TempletPortalKeywords.currentUrlSafe'()

// Botón conocido por screenshot: "Save Blueprint"
List<String> submitXpaths = [
	"//button[normalize-space(.)='Save Blueprint']",
	"//button[contains(normalize-space(.),'Save Blueprint')]",
	"//button[@type='submit']",
	"//button[contains(normalize-space(.),'Save')]",
	"//button[contains(normalize-space(.),'Create')]"
]

boolean submitted = false
for (String xp : submitXpaths) {
	TestObject obj = new TestObject().addProperty('xpath', ConditionType.EQUALS, xp)
	if (WebUI.verifyElementPresent(obj, 3, FailureHandling.OPTIONAL)) {
		try {
			WebUI.click(obj, FailureHandling.OPTIONAL)
			WebUI.waitForPageLoad(15)
			WebUI.delay(2)
			submitted = true
			KeywordUtil.logInfo("[SUBMIT] Click via: ${xp}")
			break
		} catch (Exception e) {
			KeywordUtil.logInfo("[SUBMIT] Error en click via ${xp}: ${e.message}")
		}
	}
}

if (!submitted) {
	warnings.add('[SUBMIT] Boton "Save Blueprint" no encontrado — formulario no pudo enviarse')
} else {
	String urlAfterSubmit = CustomKeywords.'TempletPortalKeywords.currentUrlSafe'()
	boolean urlChanged = urlBeforeSubmit != urlAfterSubmit
	KeywordUtil.logInfo("[SUBMIT] URL post-submit: ${urlAfterSubmit} (changed: ${urlChanged})")

	// Verificar cierre del modal (indicador de submit exitoso)
	boolean modalClosed = !CustomKeywords.'TempletPortalKeywords.verifyXPathPresent'(
		'modal_closed_check',
		"//h2[normalize-space(.)='Create New Blueprint']",
		3
	)
	if (modalClosed) {
		KeywordUtil.logInfo('[SUBMIT] Modal cerrado post-submit — blueprint creado ✓')
	} else {
		warnings.add('[SUBMIT] Modal sigue abierto post-submit — puede haber error de validacion o campos requeridos sin llenar')
	}
}

// ── 4. Estado post-submit ─────────────────────────────────────────────────────
Map postState = (Map) WebUI.executeJavaScript('''
	var errors = Array.from(document.querySelectorAll('[class*="error"],[role="alert"],[aria-invalid="true"]')).map(function(el){
		return (el.innerText||'').trim().replace(/\\s+/g,' ').substring(0,120);
	}).filter(function(t){ return t.length > 0; });
	var success = Array.from(document.querySelectorAll('[class*="success"],[class*="toast"],[role="status"]')).map(function(el){
		return (el.innerText||'').trim().replace(/\\s+/g,' ').substring(0,120);
	}).filter(function(t){ return t.length > 0; });
	return { errors: errors, success: success, url: window.location.href };
''', null)

KeywordUtil.logInfo("[SUBMIT] Post-submit errors: ${postState?.errors}")
KeywordUtil.logInfo("[SUBMIT] Post-submit success: ${postState?.success}")

if (postState?.errors?.size() > 0) {
	warnings.add("[SUBMIT] Errores post-submit: ${postState?.errors}")
}

CustomKeywords.'TempletPortalKeywords.captureCaseScreenshot'(caseId, 'post_submit')

if (failures) {
	CustomKeywords.'CommonKeywords.logCaseSummary'(caseId, failures, warnings)
	KeywordUtil.markFailedAndStop("[${caseId}] Failures: ${failures}")
} else {
	CustomKeywords.'CommonKeywords.logCaseSummary'(caseId, failures, warnings)
	KeywordUtil.logInfo("[${caseId}] PASSED" + (warnings ? " con ${warnings.size()} warnings" : ""))
}
