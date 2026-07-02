import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import com.kms.katalon.core.model.FailureHandling as FailureHandling
import com.kms.katalon.core.testobject.TestObject
import com.kms.katalon.core.testobject.ConditionType

String caseId = 'TC-BUILDERS-BLUEPRINT-DELETE-001'
List failures = []
List warnings = []

// Recuperar nombre del blueprint creado por TC-CREATE (misma JVM)
String testName = System.getProperty('qa.blueprint.test.name', '')
if (!testName) {
	warnings.add('[DELETE] qa.blueprint.test.name no definido — TC-CREATE no se ejecuto o fallo. Buscando por prefijo.')
	testName = 'QA-AUTO-CREATE-'
}
KeywordUtil.logInfo("[DELETE] Buscando blueprint: '${testName}'")

String buildersTestUrl = CustomKeywords.'CommonKeywords.getRequiredGlobal'(
	'BUILDERS_TEST_URL',
	'https://testing-templet-builders.vercel.app/'
)
String managerUrl = buildersTestUrl.replaceAll('/+$', '') + '/blueprint/manager/power-user'

if (!CustomKeywords.'TempletPortalKeywords.ensureAuthenticatedSession'(managerUrl, 15, 2)) {
	failures.add('[AUTH] Sesion no valida')
	CustomKeywords.'CommonKeywords.logCaseSummary'(caseId, failures, warnings)
	KeywordUtil.markFailedAndStop("[${caseId}] Sin sesion")
}

// ── 1. Localizar el blueprint en la lista ─────────────────────────────────────
boolean blueprintFound = CustomKeywords.'TempletPortalKeywords.verifyXPathPresent'(
	'blueprint_card',
	"//*[normalize-space(text())='${testName}']",
	10
)

if (!blueprintFound) {
	// Fallback con contains por si hay espacios extra
	blueprintFound = CustomKeywords.'TempletPortalKeywords.verifyXPathPresent'(
		'blueprint_card_fallback',
		"//*[contains(normalize-space(.), '${testName}')]",
		5
	)
}

if (!blueprintFound) {
	failures.add("[DELETE] Blueprint '${testName}' no encontrado en la lista")
	CustomKeywords.'CommonKeywords.logCaseSummary'(caseId, failures, warnings)
	KeywordUtil.markFailedAndStop("[${caseId}] Blueprint no encontrado")
}
KeywordUtil.logInfo("[DELETE] Blueprint '${testName}' encontrado ✓")
CustomKeywords.'TempletPortalKeywords.captureCaseScreenshot'(caseId, 'before_delete')

// ── 2. Hover sobre la card ────────────────────────────────────────────────────
TestObject blueprintCard = new TestObject().addProperty('xpath', ConditionType.EQUALS,
	"(//*[normalize-space(text())='${testName}'])[1]")
try {
	WebUI.mouseOver(blueprintCard, FailureHandling.OPTIONAL)
	WebUI.delay(1)
} catch (Exception e) {
	KeywordUtil.logInfo("[DELETE] Hover: ${e.message}")
}

// ── 3. Click boton ⋮ de la card ───────────────────────────────────────────────
// Grid de cards: el ⋮ es el unico button dentro del ancestro mas cercano con buttons
// Patron: subir desde el nombre hasta el primer ancestro que contenga un button
List<String> actionXpaths = [
	"(//*[normalize-space(text())='${testName}'])[1]/ancestor::*[.//button][1]//button[last()]",
	"(//*[normalize-space(text())='${testName}'])[1]/ancestor::*[.//button][1]//button[1]",
	"(//*[normalize-space(text())='${testName}'])[1]/ancestor::*[.//button][2]//button[last()]"
]

boolean menuOpened = false
for (String xp : actionXpaths) {
	TestObject obj = new TestObject().addProperty('xpath', ConditionType.EQUALS, xp)
	if (WebUI.verifyElementPresent(obj, 2, FailureHandling.OPTIONAL)) {
		try {
			WebUI.click(obj, FailureHandling.OPTIONAL)
			WebUI.delay(1)

			// Verificar si se abrió un menu con opcion Delete
			boolean deleteInMenu = CustomKeywords.'TempletPortalKeywords.verifyXPathPresent'(
				'delete_option_in_menu',
				"//*[@role='menuitem'][contains(normalize-space(.),'Delete') or contains(normalize-space(.),'Eliminar') or contains(normalize-space(.),'Remove')]",
				3
			)
			if (deleteInMenu) {
				menuOpened = true
				KeywordUtil.logInfo("[DELETE] Menu ⋮ abierto via: ${xp}")
				break
			}

			// O: dialog de confirmacion directo
			boolean confirmVisible = CustomKeywords.'TempletPortalKeywords.verifyXPathPresent'(
				'confirm_dialog',
				"//*[@role='alertdialog' or @role='dialog']",
				3
			)
			if (confirmVisible) {
				menuOpened = true
				KeywordUtil.logInfo("[DELETE] Dialog directo via: ${xp}")
				break
			}
		} catch (Exception e) {
			KeywordUtil.logInfo("[DELETE] Error en '${xp}': ${e.message}")
		}
	}
}

if (!menuOpened) {
	// Descubrir estructura real via document.evaluate
	Map rowInfo = (Map) WebUI.executeJavaScript("""
		var result = document.evaluate(
			"//*[normalize-space(text())='${testName}']",
			document, null, XPathResult.FIRST_ORDERED_NODE_TYPE, null
		).singleNodeValue;
		if (!result) return { found: false };
		var el = result.parentElement;
		while (el && el !== document.body) {
			var btns = el.querySelectorAll('button');
			if (btns.length > 0) {
				return { found: true, tag: el.tagName, cls: el.className.substring(0,100),
					btns: Array.from(btns).map(function(b){
						return { text: (b.innerText||'').trim().substring(0,40), aria: b.getAttribute('aria-label')||'' };
					})
				};
			}
			el = el.parentElement;
		}
		return { found: true, tag: result.tagName, noBtns: true };
	""", null)
	KeywordUtil.logInfo("[DELETE] Estructura de card: ${rowInfo}")
	warnings.add("[DELETE] Boton ⋮ no identificado — estructura descubierta en log.")
} else {
	// ── 4. Click en opcion Delete del menu ───────────────────────────────────
	CustomKeywords.'TempletPortalKeywords.captureCaseScreenshot'(caseId, 'menu_open')

	List<String> deleteMenuXpaths = [
		"//*[@role='menuitem'][contains(normalize-space(.),'Delete') or contains(normalize-space(.),'Eliminar') or contains(normalize-space(.),'Remove')]",
		"//button[contains(normalize-space(.),'Delete') or contains(normalize-space(.),'Eliminar')]",
		"//*[contains(@class,'dropdown') or contains(@class,'menu')]//button[contains(normalize-space(.),'Delete')]"
	]

	boolean deleteClicked = false
	for (String xp : deleteMenuXpaths) {
		TestObject obj = new TestObject().addProperty('xpath', ConditionType.EQUALS, xp)
		if (WebUI.verifyElementPresent(obj, 3, FailureHandling.OPTIONAL)) {
			WebUI.click(obj, FailureHandling.OPTIONAL)
			WebUI.delay(1)
			deleteClicked = true
			KeywordUtil.logInfo("[DELETE] Click Delete via: ${xp}")
			break
		}
	}

	if (!deleteClicked) {
		boolean alreadyInConfirm = CustomKeywords.'TempletPortalKeywords.verifyXPathPresent'(
			'already_confirm', "//*[@role='alertdialog' or @role='dialog']", 2)
		if (!alreadyInConfirm) {
			warnings.add('[DELETE] Opcion Delete no encontrada en el menu')
		}
	}

	// ── 5. Confirmar eliminacion en dialog ────────────────────────────────────
	CustomKeywords.'TempletPortalKeywords.captureCaseScreenshot'(caseId, 'confirm_dialog')

	List<String> confirmXpaths = [
		"//button[normalize-space(.)='Delete Blueprint']",
		"//*[@role='alertdialog']//button[contains(normalize-space(.),'Delete') or contains(normalize-space(.),'Confirm') or contains(normalize-space(.),'Yes')]",
		"//button[normalize-space(.)='Delete']",
		"//button[normalize-space(.)='Confirm']",
		"//button[normalize-space(.)='Yes']",
		"//button[normalize-space(.)='Yes, delete']",
		"//button[normalize-space(.)='Eliminar']",
		"//button[contains(normalize-space(.),'Confirm') or contains(normalize-space(.),'Yes')]"
	]

	boolean confirmed = false
	for (String xp : confirmXpaths) {
		TestObject obj = new TestObject().addProperty('xpath', ConditionType.EQUALS, xp)
		if (WebUI.verifyElementPresent(obj, 3, FailureHandling.OPTIONAL)) {
			WebUI.click(obj, FailureHandling.OPTIONAL)
			WebUI.delay(2)
			confirmed = true
			KeywordUtil.logInfo("[DELETE] Confirmacion via: ${xp}")
			break
		}
	}

	if (!confirmed) {
		Map dialogInfo = (Map) WebUI.executeJavaScript('''
			var d = document.querySelector("[role='alertdialog'],[role='dialog']");
			if (!d) return { found: false };
			var btns = Array.from(d.querySelectorAll('button')).map(function(b){ return (b.innerText||'').trim().substring(0,50); });
			return { found: true, text: (d.innerText||'').trim().substring(0,200), btns: btns };
		''', null)
		KeywordUtil.logInfo("[DELETE] Dialog info: ${dialogInfo}")
		warnings.add('[DELETE] Dialog de confirmacion no encontrado o botones no identificados')
	}

	// ── 6. Verificar eliminacion ──────────────────────────────────────────────
	WebUI.delay(2)
	WebUI.waitForPageLoad(10)

	boolean stillPresent = CustomKeywords.'TempletPortalKeywords.verifyXPathPresent'(
		'blueprint_still_present',
		"//*[normalize-space(text())='${testName}']",
		5
	)

	if (!stillPresent) {
		KeywordUtil.logInfo("[DELETE] Blueprint '${testName}' eliminado ✓")
	} else {
		warnings.add("[DELETE] Blueprint '${testName}' aun visible post-delete — posible delay de UI")
	}
}

CustomKeywords.'TempletPortalKeywords.captureCaseScreenshot'(caseId, 'post_delete')

if (failures) {
	CustomKeywords.'CommonKeywords.logCaseSummary'(caseId, failures, warnings)
	KeywordUtil.markFailedAndStop("[${caseId}] Failures: ${failures}")
} else {
	CustomKeywords.'CommonKeywords.logCaseSummary'(caseId, failures, warnings)
	KeywordUtil.logInfo("[${caseId}] PASSED" + (warnings ? " con ${warnings.size()} warnings" : ""))
}
