import static com.kms.katalon.core.testobject.ObjectRepository.findTestObject
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.testobject.TestObject
import com.kms.katalon.core.testobject.ConditionType
import internal.GlobalVariable
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * TC-SHEETS-SMOKE-000
 * Tipo: Smoke | Prioridad: Alta
 * Objetivo: Verificar que la página de Sheets TEST abre, carga y redirige al login SSO
 * Primer test ejecutable – sin login completo, sin dependencias de Object Repository
 */

String safeGv(String name, String fallback) {
	def prop = GlobalVariable.metaClass.hasProperty(GlobalVariable, name)
	if (prop == null) {
		return fallback
	}
	String value = GlobalVariable."${name}"?.toString()?.trim()
	return value ? value : fallback
}

String urlTest = safeGv('SHEETS_URL', 'https://sheets-test.templet.io/admin/manager.php')

WebUI.openBrowser('')
WebUI.maximizeWindow()
WebUI.navigateToUrl(urlTest)
WebUI.waitForPageLoad(15)

String currentUrl = WebUI.getUrl()
String pageTitle  = WebUI.getWindowTitle()
KeywordUtil.logInfo("URL actual: ${currentUrl}")
KeywordUtil.logInfo("Título: ${pageTitle}")

// Screenshot de evidencia
String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern('yyyyMMdd_HHmmss'))
String screenshotDir = System.getProperty('user.dir') + '/Reports/Screenshots'
new File(screenshotDir).mkdirs()
String screenshotPath = screenshotDir + "/TC-SHEETS-SMOKE-000_apertura_TEST_${timestamp}.png"
WebUI.takeScreenshot(screenshotPath)
KeywordUtil.logInfo("Screenshot: ${screenshotPath}")

// Validar: debe cargar algo (Microsoft login o el portal directo)
boolean loadedMs     = currentUrl?.contains('login.microsoftonline.com')
boolean loadedPortal = currentUrl?.contains('sheets-test.templet.io') || currentUrl?.contains('templet.io')

if (loadedMs || loadedPortal) {
	KeywordUtil.logInfo("✅ Página cargó correctamente → ${currentUrl}")
	WebUI.closeBrowser()
	KeywordUtil.markPassed('TC-SHEETS-SMOKE-000 OK. URL cargó sin errores (login SSO o portal directo).')
} else {
	WebUI.closeBrowser()
	KeywordUtil.markFailed("TC-SHEETS-SMOKE-000 FAIL. URL inesperada: ${currentUrl}")
}
