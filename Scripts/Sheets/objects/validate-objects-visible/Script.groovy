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
