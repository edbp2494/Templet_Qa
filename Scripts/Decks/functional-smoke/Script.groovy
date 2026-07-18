// ─────────────────────────────────────────────────────────────────────────────
// TC: TC-DECKS-FUNCTIONAL-SMOKE-011
// Objetivo: Smoke funcional de Decks TEST — login MS, elementos base del
//           dashboard, entrada de creación con sesión válida y logout verificado.
// Precondiciones: credenciales MS en Include/config/templet-credentials.properties
// Lógica compartida: Keywords/AdminPhpKeywords.groovy → runFunctionalSmoke(config)
// ─────────────────────────────────────────────────────────────────────────────
CustomKeywords.'AdminPhpKeywords.runFunctionalSmoke'([
	caseId          : 'TC-DECKS-FUNCTIONAL-SMOKE-011',
	urlVariableName : 'DECKS_TEST_URL',
	fallbackUrl     : 'https://decks-test.templet.io/admin/manager.php',
	checks          : [
		[name: 'brand_decks', xpath: "//*[contains(normalize-space(.),'decks.templet')]", failureMessage: 'Marca decks.templet no visible'],
		[name: 'dashboard_h4', xpath: "//h4[contains(normalize-space(.),'Dashboard')]", expectedText: 'Dashboard', failureMessage: 'Dashboard no visible o con texto inesperado'],
		[name: 'create_document', xpath: "//a[contains(normalize-space(.),'Create Document')]", expectedText: 'Create Document', failureMessage: 'Botón Create Document no visible'],
		[name: 'client_placeholder', xpath: "//*[contains(normalize-space(.),'Select Client')]", failureMessage: 'Placeholder Select Client no visible'],
		[name: 'sort_default', xpath: "//*[contains(normalize-space(.),'Newest')]", failureMessage: 'Orden default Newest no visible']
	],
	createClickXPath: "//a[contains(normalize-space(.),'Create Document')]",
	verifyLogoutAgainstStartUrl: false,
	okMessage       : 'Login, dashboard, creación básica y logout validados en Decks TEST.'
])
