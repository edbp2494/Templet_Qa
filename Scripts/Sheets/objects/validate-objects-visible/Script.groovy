// ─────────────────────────────────────────────────────────────────────────────
// TC: TC-SHEETS-SIDEBAR-ORDER-001
// Plataforma: Sheets | Área: objects
// Descripción: Valida visibilidad y orden de sidebar autenticada de Sheets tras login.
// Suites: Master/Full-Regression, Platforms/Full-Validation-All-Platforms, Platforms/Super-Suite-Validation, Platforms/Sheets/Sheets-Full-Regression-ReuseDriver, Platforms/Sheets/Sheets-List-Actions-Response-ReuseDriver, Platforms/Sheets/Sheets-Objects-Validation-ReuseDriver, Platforms/Sheets/Objects/Visible-Clicks
// Precondiciones: credenciales MS en Include/config/templet-credentials.properties
// ─────────────────────────────────────────────────────────────────────────────
CustomKeywords.'TempletPortalKeywords.verifyAuthenticatedSidebarOrderOnly'([
	caseId: 'TC-SHEETS-SIDEBAR-ORDER-001',
	platformLabel: 'Sheets TEST',
	urlVariableName: 'SHEETS_TEST_URL',
	fallbackUrl: 'https://sheets-test.templet.io/admin/manager.php',
	homeItem: 'Home',
	enforceHomeInAutoSequence: false,
	logoutItem: 'Logout',
	autoMaxItems: 40,
	autoExcludeTokens: [
		'create document',
		'create initiative',
		'help',
		'support'
	]
])
