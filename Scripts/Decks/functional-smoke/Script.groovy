import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import internal.GlobalVariable as GlobalVariable

String caseId = 'TC-DECKS-FUNCTIONAL-SMOKE-011'
String startUrl = CommonKeywords.getRequiredGlobal('DECKS_TEST_URL', 'https://decks-test.templet.io/admin/manager.php')
List<String> failures = []

try {
	CustomKeywords.'TempletPortalKeywords.openBrowserAndLoginWithMicrosoft'(startUrl)
	CustomKeywords.'TempletPortalKeywords.captureCaseScreenshot'(caseId, 'post_login')

	if (!CustomKeywords.'TempletPortalKeywords.verifyXPathPresent'('brand_decks', "//*[contains(normalize-space(.),'decks.templet')]", 8)) {
		failures.add('Marca decks.templet no visible')
	}
	if (!CustomKeywords.'TempletPortalKeywords.verifyXPathText'('dashboard_h4', "//h4[contains(normalize-space(.),'Dashboard')]", 'Dashboard', 8)) {
		failures.add('Dashboard no visible o con texto inesperado')
	}
	if (!CustomKeywords.'TempletPortalKeywords.verifyXPathText'('create_document', "//a[contains(normalize-space(.),'Create Document')]", 'Create Document', 8)) {
		failures.add('Botón Create Document no visible')
	}
	if (!CustomKeywords.'TempletPortalKeywords.verifyXPathPresent'('client_placeholder', "//*[contains(normalize-space(.),'Select Client')]", 8)) {
		failures.add('Placeholder Select Client no visible')
	}
	if (!CustomKeywords.'TempletPortalKeywords.verifyXPathPresent'('sort_default', "//*[contains(normalize-space(.),'Newest')]", 8)) {
		failures.add('Orden default Newest no visible')
	}

	if (!CustomKeywords.'TempletPortalKeywords.clickXPathAndKeepValidSession'('create_document_click', "//a[contains(normalize-space(.),'Create Document')]", 8)) {
		failures.add('No se pudo abrir la entrada de creación de Decks con sesión válida')
	}

	WebUI.navigateToUrl(startUrl)
	WebUI.waitForPageLoad(15)
	if (!CustomKeywords.'TempletPortalKeywords.logoutAndVerify'()) {
		failures.add('No se confirmó logout correctamente')
	}

	if (failures.isEmpty()) {
		KeywordUtil.markPassed(caseId + ' OK. Login, dashboard, creación básica y logout validados en Decks TEST.')
	} else {
		KeywordUtil.markFailed(caseId + ' falló: ' + failures.join(' | '))
	}
} finally {
	CustomKeywords.'TempletPortalKeywords.safeCloseBrowser'()
}