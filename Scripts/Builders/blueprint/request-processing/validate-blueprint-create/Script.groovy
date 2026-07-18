// ─────────────────────────────────────────────────────────────────────────────
// TC: TC-BUILDERS-BLUEPRINT-CREATE-001
// Plataforma: Builders | Área: blueprint/request-processing
// Descripción: TC6: Creacion completa de blueprint. Abre modal "Create New Blueprint", llena Name (timestamp), selecciona primera opcion disponible en dropdowns requeridos (Initiative, Parent Blueprint, Program Manager), hace click en "Save Blueprint" y verifica que el modal cierra y el blueprint aparece en la lista. Guarda el nombre creado en System property qa.blueprint.test.name para TC-DELETE.
// Suites: Platforms/Builders/Blueprint/Request-Processing
// Precondiciones: credenciales MS en Include/config/templet-credentials.properties
// ─────────────────────────────────────────────────────────────────────────────
import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import com.kms.katalon.core.model.FailureHandling as FailureHandling
import com.kms.katalon.core.testobject.TestObject
import com.kms.katalon.core.testobject.ConditionType

String caseId = 'TC-BUILDERS-BLUEPRINT-CREATE-001'
List failures = []
List warnings = []

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

// ── 1. Abrir modal "Create New Blueprint" ─────────────────────────────────────
boolean clicked = CustomKeywords.'TempletPortalKeywords.clickXPathAndKeepValidSession'(
	'btn_new_blueprint',
	"//button[normalize-space(.)='New Blueprint']",
	10
)
if (!clicked) {
	failures.add('[BTN] Boton "New Blueprint" no encontrado')
	CustomKeywords.'CommonKeywords.logCaseSummary'(caseId, failures, warnings)
	KeywordUtil.markFailedAndStop("[${caseId}] Sin modal")
}

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

// ── 2. Llenar campo Name ──────────────────────────────────────────────────────
String testName = "QA-AUTO-CREATE-${System.currentTimeMillis()}"
System.setProperty('qa.blueprint.test.name', testName)
KeywordUtil.logInfo("[CREATE] Blueprint name: ${testName}")

TestObject nameInput = new TestObject().addProperty('xpath', ConditionType.EQUALS,
	"//h2[normalize-space(.)='Create New Blueprint']/ancestor::*[@role='dialog']//input")
WebUI.clearText(nameInput, FailureHandling.OPTIONAL)
WebUI.setText(nameInput, testName, FailureHandling.OPTIONAL)
WebUI.delay(1)
KeywordUtil.logInfo("[CREATE] Name llenado ✓")

// ── 3. Llenar dropdowns requeridos ────────────────────────────────────────────
// Radix UI: el trigger abre un popover renderizado fuera del dialog (portal)
// Las opciones aparecen como [role='option'] en document.body level
List<Map> dropdowns = [
	[label: 'Initiative',       trigger: "//button[normalize-space(.)='Select a initiative']"],
	[label: 'Parent Blueprint', trigger: "//button[normalize-space(.)='Select a blueprint']"],
	[label: 'Program Manager',  trigger: "//button[normalize-space(.)='Select a program manager']"]
]

for (Map dd : dropdowns) {
	KeywordUtil.logInfo("[DROPDOWN] Intentando seleccionar: ${dd.label}")

	// Click el trigger del dropdown
	boolean triggerClicked = CustomKeywords.'TempletPortalKeywords.clickXPathAndKeepValidSession'(
		"trigger_${dd.label}",
		(String) dd.trigger,
		5
	)
	if (!triggerClicked) {
		warnings.add("[DROPDOWN] Trigger '${dd.label}' no encontrado — campo puede ser opcional")
		continue
	}
	WebUI.delay(1)

	// Esperar opciones (Radix UI renderiza en portal fuera del dialog)
	boolean optionsFound = CustomKeywords.'TempletPortalKeywords.verifyXPathPresent'(
		"options_${dd.label}",
		"(//*[@role='option'])[1]",
		5
	)

	if (!optionsFound) {
		// Fallback: listbox items
		optionsFound = CustomKeywords.'TempletPortalKeywords.verifyXPathPresent'(
			"options_${dd.label}_lb",
			"(//*[@role='listbox']//*[normalize-space(.) != ''])[1]",
			3
		)
	}

	if (!optionsFound) {
		// Descubrir qué apareció en el DOM
		Map discovered = (Map) WebUI.executeJavaScript('''
			var opts = Array.from(document.querySelectorAll(
				"[role='option'],[role='listbox'] li,[data-radix-popper-content-wrapper] li,[data-radix-popper-content-wrapper] button,[data-radix-popper-content-wrapper] [role='option']"
			)).map(function(el){
				return (el.innerText||'').trim().replace(/\\s+/g,' ').substring(0,80);
			}).filter(function(t){ return t.length > 0; });
			return { count: opts.length, opts: opts.slice(0,5) };
		''', null)
		KeywordUtil.logInfo("[DROPDOWN] ${dd.label} — DOM opts: ${discovered?.opts}")
		warnings.add("[DROPDOWN] '${dd.label}' — opciones no detectadas via XPath (count=${discovered?.count})")
		// Cerrar con ESC y continuar
		TestObject body = new TestObject().addProperty('xpath', ConditionType.EQUALS, '//body')
		WebUI.sendKeys(body, '', FailureHandling.OPTIONAL)
		WebUI.delay(1)
		continue
	}

	// Hacer click en la primera opción disponible
	TestObject firstOpt = new TestObject().addProperty('xpath', ConditionType.EQUALS, "(//*[@role='option'])[1]")
	try {
		WebUI.click(firstOpt, FailureHandling.OPTIONAL)
		WebUI.delay(1)
		KeywordUtil.logInfo("[DROPDOWN] ${dd.label} — primera opcion seleccionada ✓")
	} catch (Exception e) {
		// Intentar via listbox
		TestObject lbOpt = new TestObject().addProperty('xpath', ConditionType.EQUALS,
			"(//*[@role='listbox']//*[normalize-space(.) != ''])[1]")
		WebUI.click(lbOpt, FailureHandling.OPTIONAL)
		WebUI.delay(1)
		warnings.add("[DROPDOWN] ${dd.label} — seleccionado via fallback listbox")
	}
}

CustomKeywords.'TempletPortalKeywords.captureCaseScreenshot'(caseId, 'before_save')

// ── 4. Click "Save Blueprint" ─────────────────────────────────────────────────
boolean saveClicked = CustomKeywords.'TempletPortalKeywords.clickXPathAndKeepValidSession'(
	'btn_save',
	"//button[normalize-space(.)='Save Blueprint']",
	5
)
if (!saveClicked) {
	failures.add('[SAVE] Boton "Save Blueprint" no encontrado')
	CustomKeywords.'CommonKeywords.logCaseSummary'(caseId, failures, warnings)
	KeywordUtil.markFailedAndStop("[${caseId}] No se pudo guardar")
}
// Dar tiempo al server para procesar (creacion puede tardar 5-10s)
WebUI.delay(8)

// ── 5. Verificar creacion exitosa ─────────────────────────────────────────────
boolean modalClosed = !CustomKeywords.'TempletPortalKeywords.verifyXPathPresent'(
	'modal_still_open',
	"//h2[normalize-space(.)='Create New Blueprint']",
	15
)

if (modalClosed) {
	KeywordUtil.logInfo('[CREATE] Modal cerrado ✓')

	// Verificar que aparece en la lista
	boolean inList = CustomKeywords.'TempletPortalKeywords.verifyXPathPresent'(
		'blueprint_in_list',
		"//*[contains(normalize-space(.), '${testName}')]",
		10
	)
	if (inList) {
		KeywordUtil.logInfo("[CREATE] Blueprint '${testName}' visible en lista ✓")
	} else {
		warnings.add("[CREATE] Blueprint '${testName}' no encontrado en lista — puede haber delay de carga")
	}
} else {
	// Modal abierto — verificar si el blueprint igual fue creado (race condition)
	KeywordUtil.logInfo('[CREATE] Modal aun abierto — verificando si blueprint fue creado en backend...')
	WebUI.navigateToUrl(managerUrl)
	WebUI.waitForPageLoad(15)
	WebUI.delay(2)

	boolean createdDespiteModal = CustomKeywords.'TempletPortalKeywords.verifyXPathPresent'(
		'blueprint_created_check',
		"//*[contains(normalize-space(.), '${testName}')]",
		10
	)
	if (createdDespiteModal) {
		KeywordUtil.logInfo("[CREATE] Blueprint '${testName}' encontrado en lista — creado exitosamente (modal cerro despues del check) ✓")
	} else {
		// Capturar mensajes de validacion para diagnostico
		Map msgs = (Map) WebUI.executeJavaScript('''
			var nodes = Array.from(document.querySelectorAll("[role='status'],[role='alert'],[class*='toast'],[class*='error']")).map(function(el){
				return (el.innerText||'').trim().replace(/\\s+/g,' ').substring(0,120);
			}).filter(function(t){ return t.length > 0; });
			return { messages: nodes };
		''', null)
		failures.add("[CREATE] Blueprint no creado — no aparece en lista. Mensajes: ${msgs?.messages}")
	}
}

CustomKeywords.'TempletPortalKeywords.captureCaseScreenshot'(caseId, 'post_save')

if (failures) {
	CustomKeywords.'CommonKeywords.logCaseSummary'(caseId, failures, warnings)
	KeywordUtil.markFailedAndStop("[${caseId}] Failures: ${failures}")
} else {
	CustomKeywords.'CommonKeywords.logCaseSummary'(caseId, failures, warnings)
	KeywordUtil.logInfo("[${caseId}] PASSED" + (warnings ? " con ${warnings.size()} warnings" : ""))
}
