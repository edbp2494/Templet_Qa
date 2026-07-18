// ─────────────────────────────────────────────────────────────────────────────
// TC: TC-BUILDERS-BRAND-AICREATE-001
// Plataforma: Builders | Área: brand
// Descripción: Valida que las opciones de generacion AI del menu "..." de un asset (Create HTML / IDML / PPTX / TEXT) abran ai.templet.io/drafts en pestaña nueva con el type correcto y la cierren. Opciones deshabilitadas para el asset -> warning (no fail).
// Suites: Platforms/Builders/Brand/AI-Create-Options-Flow
// Precondiciones: credenciales MS en Include/config/templet-credentials.properties
// ─────────────────────────────────────────────────────────────────────────────
import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import com.kms.katalon.core.model.FailureHandling as FailureHandling
import com.kms.katalon.core.testobject.TestObject
import com.kms.katalon.core.webui.driver.DriverFactory
import org.openqa.selenium.WebDriver
import org.openqa.selenium.interactions.Actions
import org.openqa.selenium.Keys

// TC-BUILDERS-BRAND-AICREATE-001
// Valida que las opciones de generacion AI del menu "..." de un asset
// (Create HTML / IDML / PPTX / TEXT) abran ai.templet.io/drafts en pestaña nueva
// con el type correcto, y la cierra. Deshabilitadas -> warning (no fail).
// El menu Radix se ABRE con click nativo (JS .click no lo abre) y se CIERRA con Escape.

String caseId = 'TC-BUILDERS-BRAND-AICREATE-001'
List failures = []
List warnings = []

String escalaBrandUrl = 'https://testing-templet-builders.vercel.app/brand/3ac229a2-3bcd-ef11-b8e9-6045bd034dc5'
String buildersUrl = CustomKeywords.'CommonKeywords.getRequiredGlobal'('BUILDERS_TEST_URL', 'https://testing-templet-builders.vercel.app/')
String fixtureUrl = CustomKeywords.'CommonKeywords.getRequiredGlobal'('BUILDERS_BRAND_FIXTURE_URL', escalaBrandUrl)
String brandUrl = (fixtureUrl != null && fixtureUrl.trim().length() > 0) ? fixtureUrl.trim() : escalaBrandUrl

WebDriver driver = null

// Click por JS via XPath (sirve para <a> normales; NO abre menus Radix).
def jsClickXpath = { String xp ->
	try {
		return Boolean.TRUE.equals(WebUI.executeJavaScript(
			"var r=document.evaluate(arguments[0],document,null,XPathResult.FIRST_ORDERED_NODE_TYPE,null).singleNodeValue;" +
			"if(r){r.scrollIntoView({block:'center'});r.click();return true;}return false;", [xp]))
	} catch (Throwable t) { return false }
}
// Abre el menu "..." del asset idx (click nativo, con scroll previo).
def openAssetMenu = { int idx ->
	try {
		WebUI.executeJavaScript(
			"var b=document.querySelectorAll(\"button[aria-haspopup='menu']\")[arguments[0]];if(b)b.scrollIntoView({block:'center'});", [idx - 1])
	} catch (Throwable ignore) { }
	TestObject t = CustomKeywords.'TempletPortalKeywords.xpathObject'("trig_${idx}", "(//button[@aria-haspopup='menu'])[${idx}]")
	return CustomKeywords.'TempletPortalKeywords.clickIfPresent'(t, 6)
}
// Cierra cualquier menu Radix abierto (Escape).
def closeMenu = {
	try { new Actions(driver).sendKeys(Keys.ESCAPE).perform() } catch (Throwable ignore) { }
}

// ── Login + brand detail + tab Layouts (keyword compartida BrandAiKeywords) ──────
driver = CustomKeywords.'BrandAiKeywords.enterBrandLayouts'([caseId: caseId, buildersUrl: buildersUrl, brandUrl: brandUrl, failures: failures])
if (driver == null) {
	CustomKeywords.'CommonKeywords.logCaseSummary'(caseId, failures, warnings)
	KeywordUtil.markFailedAndStop("[${caseId}] Failures: ${failures}")
}

boolean assetMenus = CustomKeywords.'TempletPortalKeywords.verifyXPathPresent'('asset_menu_trigger', "(//button[@aria-haspopup='menu'])[1]", 10)
if (!assetMenus) {
	failures.add('[LAYOUTS] No se encontraron menus "..." de assets en Layouts.')
	CustomKeywords.'CommonKeywords.logCaseSummary'(caseId, failures, warnings)
	KeywordUtil.markFailedAndStop("[${caseId}] Failures: ${failures}")
}
CustomKeywords.'TempletPortalKeywords.captureCaseScreenshot'(caseId, 'brand_layouts')

// ── Descubrir el primer asset con ALGUNA opcion "Create ..." habilitada ──────────
int chosenIndex = -1
for (int k = 1; k <= 12; k++) {
	TestObject trig = CustomKeywords.'TempletPortalKeywords.xpathObject'("menu_trig_disc_${k}", "(//button[@aria-haspopup='menu'])[${k}]")
	if (!CustomKeywords.'TempletPortalKeywords.isPresentQuiet'(trig, 2)) { break }
	openAssetMenu(k)
	WebUI.delay(1)
	TestObject anyCreate = CustomKeywords.'TempletPortalKeywords.xpathObject'('any_create', "//a[starts-with(normalize-space(.),'Create ')]")
	boolean found = CustomKeywords.'TempletPortalKeywords.isPresentQuiet'(anyCreate, 2)
	closeMenu()
	WebUI.delay(1)
	if (found) { chosenIndex = k; break }
}
if (chosenIndex < 1) {
	failures.add('[FIXTURE] Ningun asset (primeros 12) tiene opciones "Create ..." habilitadas.')
	CustomKeywords.'CommonKeywords.logCaseSummary'(caseId, failures, warnings)
	KeywordUtil.markFailedAndStop("[${caseId}] Failures: ${failures}")
}
KeywordUtil.logInfo("[${caseId}] Asset elegido (indice de menu): ${chosenIndex}")

// ── Validar cada opcion de generacion AI ─────────────────────────────────────────
List createTypes = [
	[label: 'Create HTML', key: 'html'],
	[label: 'Create IDML', key: 'idml'],
	[label: 'Create PPTX', key: 'pptx'],
	[label: 'Create TEXT', key: 'text']
]
String mainHandle = driver.getWindowHandle()
int validated = 0

createTypes.each { Map ty ->
	String label = ty.label
	String key = ty.key

	openAssetMenu(chosenIndex)
	WebUI.delay(1)

	TestObject itemLink = CustomKeywords.'TempletPortalKeywords.xpathObject'("ai_${key}", "//a[normalize-space(.)='${label}']")
	if (CustomKeywords.'TempletPortalKeywords.isPresentQuiet'(itemLink, 3)) {
		List<String> before = new ArrayList<String>(driver.getWindowHandles())
		jsClickXpath("//a[normalize-space(.)='${label}']")
		boolean switched = CustomKeywords.'TempletPortalKeywords.switchToNewestTabIfPresent'(before, 8)
		if (!switched) {
			failures.add("[${key.toUpperCase()}] '${label}' habilitado pero no abrio pestaña drafts")
			closeMenu()
		} else {
			WebUI.waitForPageLoad(20)
			String u = CustomKeywords.'TempletPortalKeywords.currentUrlSafe'()
			boolean draftsOk = u.contains('drafts') || u.contains('ai.templet')
			boolean typeOk = u.toLowerCase().contains('type=' + key)
			if (!draftsOk) {
				failures.add("[${key.toUpperCase()}] URL inesperada (no es drafts): ${u}")
			} else if (!typeOk) {
				warnings.add("[${key.toUpperCase()}] Abrio drafts pero sin 'type=${key}' en la URL: ${u}")
			} else {
				validated++
				KeywordUtil.logInfo("[${caseId}] ${label} OK -> ${u}")
			}
			CustomKeywords.'TempletPortalKeywords.captureCaseScreenshot'(caseId, "create_${key}")
			try { driver.close() } catch (Throwable ignore) { }
			try { driver.switchTo().window(mainHandle) } catch (Throwable t) {
				CustomKeywords.'TempletPortalKeywords.closeExtraTabsKeepCurrent'('post_create')
			}
			WebUI.delay(2)
		}
	} else {
		TestObject itemDisabled = CustomKeywords.'TempletPortalKeywords.xpathObject'("ai_dis_${key}", "//*[@role='menuitem'][contains(normalize-space(.),'${label}')]")
		if (CustomKeywords.'TempletPortalKeywords.isPresentQuiet'(itemDisabled, 2)) {
			warnings.add("[${key.toUpperCase()}] '${label}' deshabilitado para este asset (no aplica al resourceType/plantilla).")
		} else {
			warnings.add("[${key.toUpperCase()}] '${label}' no encontrado en el menu del asset.")
		}
		closeMenu()
		WebUI.delay(1)
	}
}

KeywordUtil.logInfo("[${caseId}] Opciones Create validadas (drafts + type correcto): ${validated}/4")
CustomKeywords.'TempletPortalKeywords.captureCaseScreenshot'(caseId, 'ai_create_final')

if (failures) {
	CustomKeywords.'CommonKeywords.logCaseSummary'(caseId, failures, warnings)
	KeywordUtil.markFailedAndStop("[${caseId}] Failures: ${failures}")
} else {
	CustomKeywords.'CommonKeywords.logCaseSummary'(caseId, failures, warnings)
	KeywordUtil.logInfo("[${caseId}] PASSED" + (warnings ? " con ${warnings.size()} warnings" : ""))
}
