import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import internal.GlobalVariable as GlobalVariable

String caseId = 'TC-EMAIL-FUNCTIONAL-SMOKE-012'
String startUrl = CommonKeywords.getRequiredGlobal('EMAIL_TEST_URL', 'https://emails-test.templet.io/admin/manager.php')
List<String> failures = []

try {
	CustomKeywords.'TempletPortalKeywords.openBrowserAndLoginWithMicrosoft'(startUrl)
	CustomKeywords.'TempletPortalKeywords.captureCaseScreenshot'(caseId, 'post_login')

	if (!CustomKeywords.'TempletPortalKeywords.verifyXPathPresent'('brand_email', "//*[contains(normalize-space(.),'email.templet')]", 8)) {
		failures.add('Marca email.templet no visible')
	}
	if (!CustomKeywords.'TempletPortalKeywords.verifyXPathText'('dashboard_h4', "//h4[contains(normalize-space(.),'Dashboard')]", 'Dashboard', 8)) {
		failures.add('Dashboard no visible o con texto inesperado')
	}
	if (!CustomKeywords.'TempletPortalKeywords.verifyXPathText'('create_email', "//a[contains(normalize-space(.),'Create Email')]", 'Create Email', 8)) {
		failures.add('Botón Create Email no visible')
	}
	if (!CustomKeywords.'TempletPortalKeywords.verifyXPathPresent'('client_placeholder', "//*[contains(normalize-space(.),'Select Client')]", 8)) {
		failures.add('Placeholder Select Client no visible')
	}
	if (!CustomKeywords.'TempletPortalKeywords.verifyXPathPresent'('sort_default', "//*[contains(normalize-space(.),'Newest')]", 8)) {
		failures.add('Orden default Newest no visible')
	}

	if (!CustomKeywords.'TempletPortalKeywords.clickXPathAndKeepValidSession'('create_email_click', "//a[contains(normalize-space(.),'Create Email')]", 8)) {
		failures.add('No se pudo abrir la entrada de creación de Email con sesión válida')
	}

	WebUI.navigateToUrl(startUrl)
	WebUI.waitForPageLoad(15)
	if (!CustomKeywords.'TempletPortalKeywords.logoutAndVerify'()) {
		failures.add('No se confirmó logout correctamente')
	}

	if (failures.isEmpty()) {
		KeywordUtil.markPassed(caseId + ' OK. Login, dashboard, creación básica y logout validados en Email TEST.')
	} else {
		KeywordUtil.markFailed(caseId + ' falló: ' + failures.join(' | '))
	}
} finally {
	CustomKeywords.'TempletPortalKeywords.safeCloseBrowser'()
}