// ─────────────────────────────────────────────────────────────────────────────
// TC: TC-SHEETS-TEST-PROD-OBJECTS-003
// Plataforma: Sheets | Área: (raíz)
// Descripción: Comparación de objetos entre TEST y PROD para Sheets.
// Suites: CrossPlatform/Compare-Test-Prod, Master/Full-Regression, Platforms/Sheets/Smoke
// Precondiciones: credenciales MS en Include/config/templet-credentials.properties
// ─────────────────────────────────────────────────────────────────────────────
import com.kms.katalon.core.util.KeywordUtil
import internal.GlobalVariable as GlobalVariable

// Credenciales resueltas de forma segura desde el profile activo, env vars o default.glbl
String msUser = CustomKeywords.'TempletPortalKeywords.resolveCredential'('MS_USER', 'USERNAME')
String msPass = CustomKeywords.'TempletPortalKeywords.resolveCredential'('MS_PASS', 'PASSWORD')
String testUrl       = CommonKeywords.getRequiredGlobal('SHEETS_TEST_URL', 'https://sheets-test.templet.io/admin/manager.php')
String prodUrl       = CommonKeywords.getRequiredGlobal('SHEETS_PROD_URL', 'https://sheets.templet.io/admin/manager.php')
String screenshotDir = System.getProperty('user.dir') + '/Reports/Screenshots/sheets-test-vs-prod'

List<Map<String, Object>> objectsToCompare = [
	// Elementos de navegación y estructura (por ID )
	[name: 'navbar',              xpath: "//*[@id='navbarsExample07']",                                                                                                           compareText: false,                                expectedPresent: true],
	[name: 'select_client',       xpath: "//*[@id='inputGroupSelect02']",                                                                                                         compareText: false,                                expectedPresent: true],
	[name: 'select_sort',         xpath: "//*[@id='sortField-alpha']",                                                                                                            compareText: false,                                expectedPresent: true],
	[name: 'container_tabs',      xpath: "//*[@id='tabs-1']",                                                                                                                     compareText: false,                                expectedPresent: false],
	
	// Labels y textos
	[name: 'dashboard_h4',        xpath: "//h4[contains(normalize-space(.),'Dashboard')]",                                                                                        compareText: true,  expected: 'Dashboard',         expectedPresent: true],
	[name: 'label_client',        xpath: "//label[contains(normalize-space(.),'Client')]",                                                                                        compareText: true,  expected: 'Client',            expectedPresent: true],
	[name: 'label_initiative',    xpath: "//label[contains(normalize-space(.),'Initiative')]",                                                                                    compareText: true,  expected: 'Initiative',        expectedPresent: true],
	[name: 'label_sort',          xpath: "//label[contains(normalize-space(.),'Sort')]",                                                                                          compareText: true,  expected: 'Sort',              expectedPresent: true],
	
	// Acciones principales
	[name: 'create_document',     xpath: "//a[contains(normalize-space(.),'Create Document')]",                                                                                   compareText: true,  expected: 'Create Document',   expectedPresent: true],
	[name: 'create_initiative',   xpath: "//a[contains(normalize-space(.),'Create Initiative')]",                                                                                 compareText: true,  expected: 'Create Initiative', expectedPresent: true],
	[name: 'logout',              xpath: "//a[contains(normalize-space(.),'Log Out')]",                                                                                           compareText: true,  expected: 'Log Out',           expectedPresent: true]
]

Map testState = CustomKeywords.'TempletPortalKeywords.collectPlatformState'('SHEETS_TEST', testUrl, msUser, msPass, objectsToCompare, screenshotDir)
Map prodState = CustomKeywords.'TempletPortalKeywords.collectPlatformState'('SHEETS_PROD', prodUrl, msUser, msPass, objectsToCompare, screenshotDir)

List<String> mismatches = CustomKeywords.'TempletPortalKeywords.comparePlatformStates'(testState, prodState, objectsToCompare)

if (mismatches.isEmpty()) {
	KeywordUtil.markPassed('TC-SHEETS-TEST-PROD-OBJECTS-003 OK. Los objetos estables de Sheets TEST y PROD son equivalentes.')
} else {
	String mismatchSummary = mismatches.join(' | ')
	KeywordUtil.logInfo('Diferencias encontradas: ' + mismatchSummary)
	KeywordUtil.markFailed('TC-SHEETS-TEST-PROD-OBJECTS-003 encontró diferencias entre TEST y PROD: ' + mismatchSummary)
}
