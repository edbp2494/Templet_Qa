import com.kms.katalon.core.util.KeywordUtil

/**
 * TC-MEDIA-TEST-PROD-OBJECTS-006
 * Comparación de objetos estables entre Media TEST y PROD.
 * Si Media TEST no está deployado, el caso se omite con PASS + aviso.
 */

String host = 'media-test.templet.io'
boolean envAvailable = true
try {
	InetAddress.getByName(host)
} catch (UnknownHostException e) {
	envAvailable = false
}

if (!envAvailable) {
	KeywordUtil.logInfo('TC-MEDIA-TEST-PROD-OBJECTS-006 [OMITIDO] DNS no resuelve: ' + host)
	KeywordUtil.markPassed('TC-MEDIA-TEST-PROD-OBJECTS-006 OMITIDO — Media TEST no deployado aún.')
	return
}

String msUser = CustomKeywords.'TempletPortalKeywords.resolveCredential'('MS_USER', 'USERNAME')
String msPass = CustomKeywords.'TempletPortalKeywords.resolveCredential'('MS_PASS', 'PASSWORD')
String testUrl       = 'https://media-test.templet.io/admin/manager.php'
String prodUrl       = 'https://media.templet.io/admin/manager.php'
String screenshotDir = System.getProperty('user.dir') + '/Reports/Screenshots/media-test-vs-prod'

List<Map<String, Object>> objectsToCompare = [
	[name: 'login_button',        xpath: "//a[contains(@href,'saml/login.php') or contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'microsoft')]",  compareText: false, expectedPresent: false],
	[name: 'brand_media',         xpath: "//*[contains(normalize-space(.),'media.templet')]",                                                                                                       compareText: false, expectedPresent: true],
	[name: 'dashboard_indicator', xpath: "//h4[contains(normalize-space(.),'Dashboard')] | //h1[contains(normalize-space(.),'Media')]",                                                             compareText: false, expectedPresent: true],
	[name: 'logout_text',         xpath: "//a[contains(normalize-space(.),'Log Out') or contains(normalize-space(.),'Logout')]",                                                                    compareText: true,  expected: 'Log Out',  expectedPresent: true],
	[name: 'upload_area',         xpath: "//*[contains(normalize-space(.),'Upload') or contains(normalize-space(.),'upload') or contains(@class,'upload')]",                                        compareText: false, expectedPresent: true],
	[name: 'navbar_marker',       xpath: "//*[contains(@class,'navbar') and not(contains(@class,'navbar-toggler'))]",                                                                               compareText: false, expectedPresent: true],
	[name: 'footer_marker',       xpath: "//*[contains(normalize-space(.),'All Rights Reserved') or contains(normalize-space(.),'templet')]",                                                       compareText: false, expectedPresent: true]
]

Map testState = CustomKeywords.'TempletPortalKeywords.collectPlatformState'('MEDIA_TEST', testUrl, msUser, msPass, objectsToCompare, screenshotDir)
Map prodState = CustomKeywords.'TempletPortalKeywords.collectPlatformState'('MEDIA_PROD', prodUrl, msUser, msPass, objectsToCompare, screenshotDir)

List<String> mismatches = CustomKeywords.'TempletPortalKeywords.comparePlatformStates'(testState, prodState, objectsToCompare)

if (mismatches.isEmpty()) {
	KeywordUtil.markPassed('TC-MEDIA-TEST-PROD-OBJECTS-006 OK.')
} else {
	KeywordUtil.markFailed('TC-MEDIA-TEST-PROD-OBJECTS-006 diferencias: ' + mismatches.join(' | '))
}
