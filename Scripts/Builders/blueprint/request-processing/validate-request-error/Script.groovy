import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import com.kms.katalon.core.model.FailureHandling as FailureHandling
import com.kms.katalon.core.testobject.TestObject
import com.kms.katalon.core.testobject.ConditionType

String caseId = 'TC-BUILDERS-BLUEPRINT-REQUEST-ERROR-001'
List failures = []
List warnings = []

// ── Reusar sesion ─────────────────────────────────────────────────────────────
String buildersTestUrl = CustomKeywords.'CommonKeywords.getRequiredGlobal'(
	'BUILDERS_TEST_URL',
	'https://testing-templet-builders.vercel.app/'
)
String managerUrl = buildersTestUrl.replaceAll('/+$', '') + '/blueprint/manager/power-user'

if (!CustomKeywords.'TempletPortalKeywords.ensureAuthenticatedSession'(managerUrl, 15)) {
	failures.add('[AUTH] Sesion no valida')
	CustomKeywords.'CommonKeywords.logCaseSummary'(caseId, failures, warnings)
	KeywordUtil.markFailedAndStop("[${caseId}] Sin sesion")
}

// ── 1. Abrir formulario ───────────────────────────────────────────────────────
boolean clicked = CustomKeywords.'TempletPortalKeywords.clickXPathAndKeepValidSession'(
	'btn_new_blueprint',
	"//button[normalize-space(.)='New Blueprint']",
	10
)
if (!clicked) {
	String createUrl = buildersTestUrl.replaceAll('/+$', '') + '/blueprint/create'
	WebUI.navigateToUrl(createUrl)
	WebUI.waitForPageLoad(15)
	warnings.add("[NAV] New Blueprint btn no disponible — navegando a ${createUrl}")
}

// Esperar que el modal monte por título exacto (igual que TC-FORM y TC-SUBMIT)
boolean modalVisible = CustomKeywords.'TempletPortalKeywords.verifyXPathPresent'(
	'modal_title',
	"//h2[normalize-space(.)='Create New Blueprint']",
	10
)
if (!modalVisible) {
	warnings.add('[MODAL] Modal "Create New Blueprint" no apareció — skip submit vacío')
}

CustomKeywords.'TempletPortalKeywords.captureCaseScreenshot'(caseId, 'empty_form')

// ── 2. Submit sin llenar campos (validacion de campos requeridos) ─────────────
boolean submitClicked = false
List<String> submitXpaths = [
	"//button[normalize-space(.)='Save Blueprint']",
	"//button[contains(normalize-space(.),'Save Blueprint')]",
	"//button[@type='submit']",
	"//button[normalize-space(.)='Submit']",
	"//button[normalize-space(.)='Create']",
	"//button[normalize-space(.)='Create Blueprint']",
	"//button[normalize-space(.)='Send Request']",
	"//button[contains(normalize-space(.),'Submit') or contains(normalize-space(.),'Create') or contains(normalize-space(.),'Send')]"
]
for (String xp : submitXpaths) {
	TestObject obj = new TestObject().addProperty('xpath', ConditionType.EQUALS, xp)
	if (WebUI.verifyElementPresent(obj, 3, FailureHandling.OPTIONAL)) {
		try {
			WebUI.click(obj, FailureHandling.OPTIONAL)
			WebUI.delay(2)
			submitClicked = true
			KeywordUtil.logInfo("[ERROR] Submit vacio via: ${xp}")
			break
		} catch (Exception e) {
			KeywordUtil.logInfo("[ERROR] No se pudo clickear submit via ${xp}")
		}
	}
}

if (!submitClicked) {
	warnings.add('[ERROR] No se encontro boton submit — skip validacion de campos requeridos')
} else {
	// ── 3. Verificar que aparecen mensajes de validacion ──────────────────────
	Map validationState = (Map) WebUI.executeJavaScript('''
		var errors = Array.from(document.querySelectorAll(
			"[class*='error'], [class*='invalid'], [aria-invalid='true'], [class*='required'], [role='alert'], .text-red-500, .text-red-600, [class*='danger']"
		)).map(function(el){
			return (el.innerText||"").trim().replace(/\\s+/g," ").substring(0,100);
		}).filter(function(t){ return t.length > 0; });
		var inputsInvalid = Array.from(document.querySelectorAll("input:invalid, textarea:invalid")).map(function(el){
			return { id: el.id, name: el.name, validationMessage: el.validationMessage };
		});
		return { domErrors: errors, invalidInputs: inputsInvalid, url: window.location.href };
	''', null)

	KeywordUtil.logInfo("[ERROR] Errores DOM: ${validationState?.domErrors}")
	KeywordUtil.logInfo("[ERROR] Inputs invalidos: ${validationState?.invalidInputs}")

	List domErrors = (List) (validationState?.domErrors ?: [])
	List invalidInputs = (List) (validationState?.invalidInputs ?: [])

	if (domErrors.size() > 0 || invalidInputs.size() > 0) {
		KeywordUtil.logInfo("[ERROR] CORRECTO: el formulario muestra ${domErrors.size()} error(es) al hacer submit vacio")
	} else {
		warnings.add('[ERROR] Submit vacio no produjo mensajes de validacion visibles — verificar si la validacion es del lado servidor')
	}
}

CustomKeywords.'TempletPortalKeywords.captureCaseScreenshot'(caseId, 'after_empty_submit')

// ── 4. Verificar manejo de URL invalida (modulo routing E6) ──────────────────
String invalidUrl = buildersTestUrl.replaceAll('/+$', '') + '/blueprint/create?invalid_param=true'
WebUI.navigateToUrl(invalidUrl)
WebUI.waitForPageLoad(10)

boolean notFoundPage = CustomKeywords.'TempletPortalKeywords.verifyXPathPresent'(
	'page_not_found',
	"//h1[contains(normalize-space(.),'404') or contains(normalize-space(.),'Not Found') or contains(normalize-space(.),'Error')]",
	5
)
boolean validAppAfterInvalidUrl = CustomKeywords.'TempletPortalKeywords.isValidAppSession'()

if (!validAppAfterInvalidUrl) {
	failures.add('[ERROR] URL invalida rompio la sesion — la app no maneja parametros invalidos correctamente')
} else if (notFoundPage) {
	warnings.add('[ERROR] URL invalida retorna 404 — verificar si la app deberia redirigir en lugar de 404')
} else {
	KeywordUtil.logInfo("[ERROR] App sobrevivio URL invalida — manejo de errores OK")
}

CustomKeywords.'TempletPortalKeywords.captureCaseScreenshot'(caseId, 'after_invalid_url')

// ── Resumen ───────────────────────────────────────────────────────────────────
if (failures) {
	CustomKeywords.'CommonKeywords.logCaseSummary'(caseId, failures, warnings)
	KeywordUtil.markFailedAndStop("[${caseId}] Failures: ${failures}")
} else {
	CustomKeywords.'CommonKeywords.logCaseSummary'(caseId, failures, warnings)
	KeywordUtil.logInfo("[${caseId}] PASSED" + (warnings ? " con ${warnings.size()} warnings" : ""))
}
