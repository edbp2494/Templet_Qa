import com.kms.katalon.core.util.KeywordUtil

String msUser = CustomKeywords.'TempletPortalKeywords.resolveCredential'('MS_USER', 'USERNAME')
String msPass = CustomKeywords.'TempletPortalKeywords.resolveCredential'('MS_PASS', 'PASSWORD')
String testUrl       = 'https://emails-test.templet.io/admin/manager.php'
String prodUrl       = 'https://email.templet.io/admin/manager.php'
String screenshotDir = System.getProperty('user.dir') + '/Reports/Screenshots/email-test-vs-prod'

List<Map<String, Object>> objectsToCompare = [
	[name: 'login_button',           xpath: "//a[contains(@href,'saml/login.php') or contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'microsoft')]",  compareText: false, expectedPresent: false],
	[name: 'brand_email',            xpath: "//*[contains(normalize-space(.),'email.templet')]",                                                                                                        compareText: false, expectedPresent: true],
	[name: 'dashboard_h4',           xpath: "//h4[contains(normalize-space(.),'Dashboard')]",                                                                                                          compareText: true,  expected: 'Dashboard',         expectedPresent: true],
	[name: 'label_client',           xpath: "//label[contains(normalize-space(.),'Client')]",                                                                                                          compareText: true,  expected: 'Client',            expectedPresent: true],
	[name: 'label_initiative',       xpath: "//label[contains(normalize-space(.),'Initiative')]",                                                                                                      compareText: true,  expected: 'Initiative',        expectedPresent: true],
	[name: 'label_sort',             xpath: "//label[contains(normalize-space(.),'Sort')]",                                                                                                            compareText: true,  expected: 'Sort',              expectedPresent: true],
	[name: 'logout_text',            xpath: "//a[contains(normalize-space(.),'Log Out')]",                                                                                                             compareText: true,  expected: 'Log Out',           expectedPresent: true],
	[name: 'create_email',           xpath: "//a[contains(normalize-space(.),'Create Email')]",                                                                                                        compareText: true,  expected: 'Create Email',       expectedPresent: true],
	[name: 'create_initiative',      xpath: "//a[contains(normalize-space(.),'Create Initiative')]",                                                                                                   compareText: true,  expected: 'Create Initiative', expectedPresent: true],
	[name: 'client_placeholder',     xpath: "//*[contains(normalize-space(.),'Select Client')]",                                                                                                       compareText: false, expectedPresent: true],
	[name: 'initiative_placeholder', xpath: "//*[contains(normalize-space(.),'Select a client first')]",                                                                                               compareText: false, expectedPresent: true],
	[name: 'sort_default',           xpath: "//*[contains(normalize-space(.),'Newest')]",                                                                                                              compareText: false, expectedPresent: true],
	[name: 'footer_marker',          xpath: "//*[contains(normalize-space(.),'All Rights Reserved') and contains(normalize-space(.),'Terms') and contains(normalize-space(.),'Policies')]",            compareText: false, expectedPresent: true],
	[name: 'navbar_marker',          xpath: "//*[contains(@class,'navbar') and not(contains(@class,'navbar-toggler'))]",                                                                                compareText: false, expectedPresent: true]
]

Map testState = CustomKeywords.'TempletPortalKeywords.collectPlatformState'('EMAIL_TEST', testUrl, msUser, msPass, objectsToCompare, screenshotDir)
Map prodState = CustomKeywords.'TempletPortalKeywords.collectPlatformState'('EMAIL_PROD', prodUrl, msUser, msPass, objectsToCompare, screenshotDir)

List<String> mismatches = CustomKeywords.'TempletPortalKeywords.comparePlatformStates'(testState, prodState, objectsToCompare)

if (mismatches.isEmpty()) {
	KeywordUtil.markPassed('TC-EMAIL-TEST-PROD-OBJECTS-005 OK.')
} else {
	KeywordUtil.markFailed('TC-EMAIL-TEST-PROD-OBJECTS-005 diferencias: ' + mismatches.join(' | '))
}
