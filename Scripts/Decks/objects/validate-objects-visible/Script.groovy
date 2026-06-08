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
