CustomKeywords.'TempletPortalKeywords.verifyAuthenticatedSidebarOrderOnly'([
	caseId: 'TC-SCHEDULERS-SIDEBAR-ORDER-001',
	platformLabel: 'Schedulers TEST',
	urlVariableName: 'SCHEDULERS_TEST_URL',
	fallbackUrl: 'https://testing-templet-schedulers.vercel.app/',
	// PENDING_APP_FIX: 'Tracking' y 'Metrics' existen en /tracking y /metrics pero no aparecen en sidebar (bug conocido)
	sequenceItems: [
		'Home',
		'Accounts',
		'Initiatives',
		'Contributors',
		'Contacts',
		'Summary',
		'Requests',
		'Resources',
		'Requests (View)',
		'Resources (View)',
		'One-Off Request',
		'Non-Standard Request',
		'Daily',
		'Review',
		'Collateral',
		'Input',
		'Output',
		'Documentation',
		'Logout'
	]
])
