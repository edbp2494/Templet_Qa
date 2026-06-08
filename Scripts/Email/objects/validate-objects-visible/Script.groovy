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
