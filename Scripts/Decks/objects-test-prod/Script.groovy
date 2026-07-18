// ─────────────────────────────────────────────────────────────────────────────
// TC: TC-DECKS-TEST-PROD-OBJECTS-004
// Plataforma: Decks | Área: (raíz)
// Descripción: Comparación de objetos entre TEST y PROD para Decks.
// Suites: CrossPlatform/Compare-Test-Prod, Master/Full-Regression, Platforms/Decks/Smoke
// Precondiciones: credenciales MS en Include/config/templet-credentials.properties
// ─────────────────────────────────────────────────────────────────────────────
import com.kms.katalon.core.util.KeywordUtil
import internal.GlobalVariable as GlobalVariable

// Credenciales resueltas de forma segura desde el profile activo, env vars o default.glbl
String msUser = CustomKeywords.'TempletPortalKeywords.resolveCredential'('MS_USER', 'USERNAME')
String msPass = CustomKeywords.'TempletPortalKeywords.resolveCredential'('MS_PASS', 'PASSWORD')
String testUrl       = CommonKeywords.getRequiredGlobal('DECKS_TEST_URL', 'https://decks-test.templet.io/admin/manager.php')
String prodUrl       = CommonKeywords.getRequiredGlobal('DECKS_PROD_URL', 'https://deck.templet.io/admin/manager.php')
String screenshotDir = System.getProperty('user.dir') + '/Reports/Screenshots/decks-test-vs-prod'

List<Map<String, Object>> objectsToCompare = [
	[name: 'login_button',           xpath: "//a[contains(@href,'saml/login.php') or contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'microsoft')]",                                                                                         compareText: false, expectedPresent: false],
	[name: 'brand_decks',            xpath: "//*[contains(normalize-space(.),'decks.templet')]",                                                                                                                                                                                              compareText: false, expectedPresent: true],
	[name: 'dashboard_h4',           xpath: "//h4[contains(normalize-space(.),'Dashboard')]",                                                                                                                                                                                                 compareText: true,  expected: 'Dashboard',           expectedPresent: true],
	[name: 'label_client',           xpath: "//label[contains(normalize-space(.),'Client')]",                                                                                                                                                                                                 compareText: true,  expected: 'Client',              expectedPresent: true],
	[name: 'label_initiative',       xpath: "//label[contains(normalize-space(.),'Initiative')]",                                                                                                                                                                                             compareText: true,  expected: 'Initiative',          expectedPresent: true],
	[name: 'label_sort',             xpath: "//label[contains(normalize-space(.),'Sort')]",                                                                                                                                                                                                   compareText: true,  expected: 'Sort',                expectedPresent: true],
	[name: 'logout_text',            xpath: "//a[contains(normalize-space(.),'Log Out')]",                                                                                                                                                                                                    compareText: true,  expected: 'Log Out',             expectedPresent: true],
	[name: 'create_document',        xpath: "//a[contains(normalize-space(.),'Create Document')]",                                                                                                                                                                                            compareText: true,  expected: 'Create Document',     expectedPresent: true],
	[name: 'create_initiative',      xpath: "//a[contains(normalize-space(.),'Create Initiative')]",                                                                                                                                                                                          compareText: true,  expected: 'Create Initiative',   expectedPresent: true],
	[name: 'client_placeholder',     xpath: "//*[contains(normalize-space(.),'Select Client')]",                                                                                                                                                                                              compareText: false, expectedPresent: true],
	[name: 'initiative_placeholder', xpath: "//*[contains(normalize-space(.),'Select a client first')]",                                                                                                                                                                                      compareText: false, expectedPresent: true],
	[name: 'sort_default',           xpath: "//*[contains(normalize-space(.),'Newest')]",                                                                                                                                                                                                     compareText: false, expectedPresent: true],
	[name: 'footer_marker',          xpath: "//*[contains(normalize-space(.),'All Rights Reserved') and contains(normalize-space(.),'Terms') and contains(normalize-space(.),'Policies')]",                                                                                                   compareText: false, expectedPresent: true],
	[name: 'navbar_marker',          xpath: "//*[contains(@class,'navbar') and not(contains(@class,'navbar-toggler'))]",                                                                                                                                                                      compareText: false, expectedPresent: true]
]

Map testState = CustomKeywords.'TempletPortalKeywords.collectPlatformState'('DECKS_TEST', testUrl, msUser, msPass, objectsToCompare, screenshotDir)
Map prodState = CustomKeywords.'TempletPortalKeywords.collectPlatformState'('DECKS_PROD', prodUrl, msUser, msPass, objectsToCompare, screenshotDir)

List<String> mismatches = CustomKeywords.'TempletPortalKeywords.comparePlatformStates'(testState, prodState, objectsToCompare)

if (mismatches.isEmpty()) {
	KeywordUtil.markPassed('TC-DECKS-TEST-PROD-OBJECTS-004 OK. Los objetos estables de Decks TEST y PROD son equivalentes.')
} else {
	String mismatchSummary = mismatches.join(' | ')
	KeywordUtil.logInfo('Diferencias encontradas: ' + mismatchSummary)
	KeywordUtil.markFailed('TC-DECKS-TEST-PROD-OBJECTS-004 encontró diferencias entre TEST y PROD: ' + mismatchSummary)
}
