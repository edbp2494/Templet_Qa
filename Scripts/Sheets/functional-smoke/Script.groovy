// ─────────────────────────────────────────────────────────────────────────────
// TC: TC-SHEETS-FUNCTIONAL-SMOKE-010
// Objetivo: Smoke funcional de Sheets TEST — login MS, elementos base del
//           dashboard, entrada de creación con sesión válida y logout verificado.
// Precondiciones: credenciales MS en Include/config/templet-credentials.properties
// Lógica compartida: Keywords/AdminPhpKeywords.groovy → runFunctionalSmoke(config)
// ─────────────────────────────────────────────────────────────────────────────
CustomKeywords.'AdminPhpKeywords.runFunctionalSmoke'([
	caseId          : 'TC-SHEETS-FUNCTIONAL-SMOKE-010',
	urlVariableName : 'SHEETS_TEST_URL',
	fallbackUrl     : 'https://sheets-test.templet.io/admin/manager.php',
	checks          : [
		[name: 'dashboard_h4', xpath: "//h4[contains(normalize-space(.),'Dashboard')]", expectedText: 'Dashboard', failureMessage: 'Dashboard no visible o con texto inesperado'],
		[name: 'create_document', xpath: "//a[contains(normalize-space(.),'Create Document')]", expectedText: 'Create Document', failureMessage: 'Botón Create Document no visible'],
		[name: 'logout', xpath: "//a[contains(normalize-space(.),'Log Out')]", expectedText: 'Log Out', failureMessage: 'Botón Log Out no visible'],
		[name: 'label_client', xpath: "//label[contains(normalize-space(.),'Client')]", failureMessage: 'Filtro Client no visible'],
		[name: 'label_initiative', xpath: "//label[contains(normalize-space(.),'Initiative')]", failureMessage: 'Filtro Initiative no visible'],
		[name: 'label_sort', xpath: "//label[contains(normalize-space(.),'Sort')]", failureMessage: 'Filtro Sort no visible']
	],
	createClickXPath: "//a[contains(normalize-space(.),'Create Document')]",
	verifyLogoutAgainstStartUrl: true,
	okMessage       : 'Login, dashboard, creación básica y logout validados en Sheets TEST.'
])
