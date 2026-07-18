// ─────────────────────────────────────────────────────────────────────────────
// TC: TC-EMAIL-SIDEBAR-ORDER-001
// Plataforma: Email | Área: objects
// Descripción: Valida visibilidad y orden de sidebar autenticada de Email tras login.
// Suites: Master/Full-Regression, Platforms/Full-Validation-All-Platforms, Platforms/Super-Suite-Validation, Platforms/Email/Email-Full-Regression-ReuseDriver, Platforms/Email/Email-List-Actions-Response-ReuseDriver, Platforms/Email/Email-Objects-Validation-ReuseDriver, Platforms/Email/Objects/Visible-Clicks
// Precondiciones: credenciales MS en Include/config/templet-credentials.properties
// ─────────────────────────────────────────────────────────────────────────────
CustomKeywords.'TempletPortalKeywords.verifyAuthenticatedSidebarOrderOnly'([
	caseId: 'TC-EMAIL-SIDEBAR-ORDER-001',
	platformLabel: 'Email TEST',
	urlVariableName: 'EMAIL_TEST_URL',
	fallbackUrl: 'https://emails-test.templet.io/admin/manager.php',
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
