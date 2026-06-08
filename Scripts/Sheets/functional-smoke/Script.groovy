import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import internal.GlobalVariable as GlobalVariable

String caseId = 'TC-SHEETS-FUNCTIONAL-SMOKE-010'
String startUrl = CommonKeywords.getRequiredGlobal('SHEETS_TEST_URL', 'https://sheets-test.templet.io/admin/manager.php')
List<String> failures = []

try {
	CustomKeywords.'TempletPortalKeywords.openBrowserAndLoginWithMicrosoft'(startUrl)
	CustomKeywords.'TempletPortalKeywords.captureCaseScreenshot'(caseId, 'post_login')

	if (!CustomKeywords.'TempletPortalKeywords.verifyXPathText'('dashboard_h4', "//h4[contains(normalize-space(.),'Dashboard')]", 'Dashboard', 8)) {
		failures.add('Dashboard no visible o con texto inesperado')
	}
	if (!CustomKeywords.'TempletPortalKeywords.verifyXPathText'('create_document', "//a[contains(normalize-space(.),'Create Document')]", 'Create Document', 8)) {
		failures.add('Botón Create Document no visible')
	}
	if (!CustomKeywords.'TempletPortalKeywords.verifyXPathText'('logout', "//a[contains(normalize-space(.),'Log Out')]", 'Log Out', 8)) {
		failures.add('Botón Log Out no visible')
	}
	if (!CustomKeywords.'TempletPortalKeywords.verifyXPathPresent'('label_client', "//label[contains(normalize-space(.),'Client')]", 8)) {
		failures.add('Filtro Client no visible')
	}
	if (!CustomKeywords.'TempletPortalKeywords.verifyXPathPresent'('label_initiative', "//label[contains(normalize-space(.),'Initiative')]", 8)) {
		failures.add('Filtro Initiative no visible')
	}
	if (!CustomKeywords.'TempletPortalKeywords.verifyXPathPresent'('label_sort', "//label[contains(normalize-space(.),'Sort')]", 8)) {
		failures.add('Filtro Sort no visible')
	}

	if (!CustomKeywords.'TempletPortalKeywords.clickXPathAndKeepValidSession'('create_document_click', "//a[contains(normalize-space(.),'Create Document')]", 8)) {
		failures.add('No se pudo abrir la entrada de creación de documento con sesión válida')
	}

	WebUI.navigateToUrl(startUrl)
	WebUI.waitForPageLoad(15)
	if (!CustomKeywords.'TempletPortalKeywords.logoutAndVerify'(startUrl)) {
		failures.add('No se confirmó logout correctamente')
	}

	if (failures.isEmpty()) {
		KeywordUtil.markPassed(caseId + ' OK. Login, dashboard, creación básica y logout validados en Sheets TEST.')
	} else {
		KeywordUtil.markFailed(caseId + ' falló: ' + failures.join(' | '))
	}
} finally {
	CustomKeywords.'TempletPortalKeywords.safeCloseBrowser'()
}