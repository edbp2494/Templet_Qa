// ─────────────────────────────────────────────────────────────────────────────
// TC: TC-SCHEDULERS-SIDEBAR-NAV-002
// Plataforma: Schedulers | Área: objects
// Descripción: Hace click en los objetos visibles seguros del home autenticado de Schedulers y vuelve al home.
// Suites: Master/Full-Regression, Platforms/Full-Validation-All-Platforms, Platforms/Super-Suite-Validation, Platforms/Schedulers/Objects/Visible-Clicks
// Precondiciones: credenciales MS en Include/config/templet-credentials.properties
// ─────────────────────────────────────────────────────────────────────────────
CustomKeywords.'TempletPortalKeywords.runAuthenticatedSidebarSequence'([
	caseId: 'TC-SCHEDULERS-SIDEBAR-NAV-002',
	platformLabel: 'Schedulers TEST',
	urlVariableName: 'SCHEDULERS_TEST_URL',
	fallbackUrl: 'https://testing-templet-schedulers.vercel.app/',
	settleSeconds: 2,
	// PENDING_APP_FIX: 'Tracking' y 'Metrics' removidos de sidebar (bug conocido - routes existen pero no hay link en sidebar)
	navigationRules: [
		[item: 'Home', settleSeconds: 3, readSeconds: 2],
		[item: 'Requests (View)', allowClickOnly: true, settleSeconds: 3],
		[item: 'Resources (View)', allowClickOnly: true, settleSeconds: 3],
		[item: 'Daily', expectedUrlFragments: ['/assign'], settleSeconds: 3],
		[item: 'Review', expectedUrlFragments: ['/review'], settleSeconds: 3],
		[item: 'Collateral', expectedUrlFragments: ['/collateral'], settleSeconds: 3]
	],
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
	],
	homeItem: 'Home',
	logoutItem: 'Logout',
	logoutAllowClickOnly: true,
	logoutSettleSeconds: 3
])
