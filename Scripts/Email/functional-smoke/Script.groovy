// ─────────────────────────────────────────────────────────────────────────────
// TC: TC-EMAIL-FUNCTIONAL-SMOKE-012
// Objetivo: Smoke funcional de Email TEST — login MS, elementos base del
//           dashboard, entrada de creación con sesión válida y logout verificado.
// Precondiciones: credenciales MS en Include/config/templet-credentials.properties
// Lógica compartida: Keywords/AdminPhpKeywords.groovy → runFunctionalSmoke(config)
// ─────────────────────────────────────────────────────────────────────────────
CustomKeywords.'AdminPhpKeywords.runFunctionalSmoke'([
	caseId          : 'TC-EMAIL-FUNCTIONAL-SMOKE-012',
	urlVariableName : 'EMAIL_TEST_URL',
	fallbackUrl     : 'https://emails-test.templet.io/admin/manager.php',
	checks          : [
		[name: 'brand_email', xpath: "//*[contains(normalize-space(.),'email.templet')]", failureMessage: 'Marca email.templet no visible'],
		[name: 'dashboard_h4', xpath: "//h4[contains(normalize-space(.),'Dashboard')]", expectedText: 'Dashboard', failureMessage: 'Dashboard no visible o con texto inesperado'],
		[name: 'create_email', xpath: "//a[contains(normalize-space(.),'Create Email')]", expectedText: 'Create Email', failureMessage: 'Botón Create Email no visible'],
		[name: 'client_placeholder', xpath: "//*[contains(normalize-space(.),'Select Client')]", failureMessage: 'Placeholder Select Client no visible'],
		[name: 'sort_default', xpath: "//*[contains(normalize-space(.),'Newest')]", failureMessage: 'Orden default Newest no visible']
	],
	createClickXPath: "//a[contains(normalize-space(.),'Create Email')]",
	verifyLogoutAgainstStartUrl: false,
	okMessage       : 'Login, dashboard, creación básica y logout validados en Email TEST.'
])
