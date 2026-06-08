CustomKeywords.'TempletPortalKeywords.runAuthenticatedSidebarSequence'([
	caseId: 'TC-SHEETS-SIDEBAR-NAV-002',
	platformLabel: 'Sheets TEST',
	urlVariableName: 'SHEETS_TEST_URL',
	fallbackUrl: 'https://sheets-test.templet.io/admin/manager.php',
	settleSeconds: 2,
	navigationRules: [
		[item: 'Home', settleSeconds: 3, readSeconds: 1],
		[item: 'Email', expectNewTab: true, loginInNewTab: true, allowClickOnly: true, settleSeconds: 3, newTabExpectedUrlFragments: ['email.templet.io', 'emails-test.templet.io', '/admin/manager.php']],
		[item: 'Emails', expectNewTab: true, loginInNewTab: true, allowClickOnly: true, settleSeconds: 3, newTabExpectedUrlFragments: ['email.templet.io', 'emails-test.templet.io', '/admin/manager.php']],
		[item: 'Decks', expectNewTab: true, loginInNewTab: true, allowClickOnly: true, settleSeconds: 3, newTabExpectedUrlFragments: ['deck.templet.io', 'decks-test.templet.io', '/admin/manager.php']],
		[item: 'Sheets', allowClickOnly: true, settleSeconds: 3],
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
