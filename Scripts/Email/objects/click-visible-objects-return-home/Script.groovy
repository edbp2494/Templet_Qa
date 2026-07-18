// ─────────────────────────────────────────────────────────────────────────────
// TC: TC-EMAIL-SIDEBAR-NAV-002
// Plataforma: Email | Área: objects
// Descripción: Recorre los items visibles de la sidebar autenticada en Email, valida navegacion item por item, vuelve a Home y finaliza con Logout.
// Suites: Master/Full-Regression, Platforms/Email/Objects/Visible-Clicks
// Precondiciones: credenciales MS en Include/config/templet-credentials.properties
// ─────────────────────────────────────────────────────────────────────────────
CustomKeywords.'TempletPortalKeywords.runAuthenticatedSidebarSequence'([
	caseId: 'TC-EMAIL-SIDEBAR-NAV-002',
	platformLabel: 'Email TEST',
	urlVariableName: 'EMAIL_TEST_URL',
	fallbackUrl: 'https://emails-test.templet.io/admin/manager.php',
	settleSeconds: 2,
	navigationRules: [
		[item: 'Home', settleSeconds: 3, readSeconds: 1],
		[item: 'Email', allowClickOnly: true, settleSeconds: 3],
		[item: 'Emails', allowClickOnly: true, settleSeconds: 3],
		[item: 'Decks', expectNewTab: true, loginInNewTab: true, allowClickOnly: true, settleSeconds: 3, newTabExpectedUrlFragments: ['deck.templet.io', 'decks-test.templet.io', '/admin/manager.php']],
		[item: 'Sheets', expectNewTab: true, loginInNewTab: true, allowClickOnly: true, settleSeconds: 3, newTabExpectedUrlFragments: ['sheets.templet.io', 'sheets-test.templet.io', '/admin/manager.php']],
		[item: 'Builders', expectNewTab: true, loginInNewTab: true, allowClickOnly: true, settleSeconds: 3, newTabExpectedUrlFragments: ['templet-builders', 'builders.templet', 'vercel.app']],
		[item: 'Schedulers', expectNewTab: true, loginInNewTab: true, allowClickOnly: true, settleSeconds: 3, newTabExpectedUrlFragments: ['templet-schedulers', 'schedulers.templet', 'vercel.app']],
		[item: 'Requests (View)', expectNewTab: true, allowClickOnly: true, settleSeconds: 3],
		[item: 'Resources (View)', expectNewTab: true, allowClickOnly: true, settleSeconds: 3],
		[item: 'Output', expectNewTab: true, allowClickOnly: true, settleSeconds: 3]
	],
	sequenceItems: [],
	homeItem: 'Home',
	enforceHomeInAutoSequence: false,
	allowMissingHomeBeforeLogout: true,
	logoutItem: 'Logout',
	allowMissingLogout: true,
	logoutAllowClickOnly: true,
	logoutSettleSeconds: 3,
	autoMaxItems: 40,
	autoExcludeTokens: [
		'create document',
		'create initiative',
		'help',
		'support'
	]
])
