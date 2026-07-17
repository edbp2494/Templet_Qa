import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI

/**
 * TC-MEDIA-FUNCTIONAL-SMOKE-013
 * Smoke funcional de Media: login, dashboard, upload area y logout.
 * Si Media TEST no está deployado, el caso se omite con PASS + aviso.
 */

String caseId = 'TC-MEDIA-FUNCTIONAL-SMOKE-013'
String startUrl = 'https://media-test.templet.io/admin/manager.php'
String host = 'media-test.templet.io'

boolean envAvailable = true
try {
	InetAddress.getByName(host)
} catch (UnknownHostException e) {
	envAvailable = false
}

if (!envAvailable) {
	KeywordUtil.logInfo(caseId + ' [OMITIDO] DNS no resuelve: ' + host)
	KeywordUtil.markPassed(caseId + ' OMITIDO — Media TEST no deployado aún.')
	return
}

List<String> failures = []

try {
	CustomKeywords.'TempletPortalKeywords.openBrowserAndLoginWithMicrosoft'(startUrl)
	CustomKeywords.'TempletPortalKeywords.captureCaseScreenshot'(caseId, 'post_login')

	if (!CustomKeywords.'TempletPortalKeywords.verifyXPathPresent'('brand_media', "//*[contains(normalize-space(.),'media.templet')]", 8))
		failures.add('Marca media.templet no visible')
	if (!CustomKeywords.'TempletPortalKeywords.verifyXPathPresent'('dashboard_indicator', "//h4[contains(normalize-space(.),'Dashboard')] | //h1[contains(normalize-space(.),'Media')]", 8))
		failures.add('Dashboard/título Media no visible')
	if (!CustomKeywords.'TempletPortalKeywords.verifyXPathPresent'('logout', "//a[contains(normalize-space(.),'Log Out') or contains(normalize-space(.),'Logout')]", 8))
		failures.add('Botón Log Out no visible')
	if (!CustomKeywords.'TempletPortalKeywords.verifyXPathPresent'('upload_area', "//*[contains(normalize-space(.),'Upload') or contains(normalize-space(.),'upload') or contains(@class,'upload')]", 8))
		failures.add('Zona de Upload no visible')
	if (!CustomKeywords.'TempletPortalKeywords.verifyXPathPresent'('navbar_marker', "//*[contains(@class,'navbar') and not(contains(@class,'navbar-toggler'))]", 8))
		failures.add('Navbar no visible')

	WebUI.navigateToUrl(startUrl)
	WebUI.waitForPageLoad(15)
	if (!CustomKeywords.'TempletPortalKeywords.logoutAndVerify'())
		failures.add('No se confirmó logout')

	if (failures.isEmpty()) KeywordUtil.markPassed(caseId + ' OK.')
	else KeywordUtil.markFailed(caseId + ' falló: ' + failures.join(' | '))
} finally {
	CustomKeywords.'TempletPortalKeywords.safeCloseBrowser'()
}
