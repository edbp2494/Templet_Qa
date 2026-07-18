// ─────────────────────────────────────────────────────────────────────────────
// TC: TC-DECKS-SIDEBAR-ORDER-001
// Plataforma: Decks | Área: objects
// Descripción: Valida visibilidad y orden de sidebar autenticada de Decks tras login.
// Suites: Master/Full-Regression, Platforms/Full-Validation-All-Platforms, Platforms/Super-Suite-Validation, Platforms/Decks/Decks-Full-Regression-ReuseDriver, Platforms/Decks/Decks-List-Actions-Response-ReuseDriver, Platforms/Decks/Decks-Objects-Validation-ReuseDriver, Platforms/Decks/Objects/Visible-Clicks
// Precondiciones: credenciales MS en Include/config/templet-credentials.properties
// ─────────────────────────────────────────────────────────────────────────────
CustomKeywords.'TempletPortalKeywords.verifyAuthenticatedSidebarOrderOnly'([
	caseId: 'TC-DECKS-SIDEBAR-ORDER-001',
	platformLabel: 'Decks TEST',
	urlVariableName: 'DECKS_TEST_URL',
	fallbackUrl: 'https://decks-test.templet.io/admin/manager.php',
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
