// ─────────────────────────────────────────────────────────────────────────────
// TC: TC-BUILDERS-SIDEBAR-NAV-002
// Plataforma: Builders | Área: objects
// Descripción: Recorre items de la toolbar izquierda de Builders, valida carga por item, vuelve a Home y termina con Logout.
// Suites: Master/Full-Regression, Platforms/Full-Validation-All-Platforms, Platforms/Super-Suite-Validation, Platforms/Builders/Objects/Visible-Clicks
// Precondiciones: credenciales MS en Include/config/templet-credentials.properties
// ─────────────────────────────────────────────────────────────────────────────
CustomKeywords.'TempletPortalKeywords.runAuthenticatedSidebarSequence'([
	caseId: 'TC-BUILDERS-SIDEBAR-NAV-002',
	platformLabel: 'Builders TEST',
	visualPlatform: 'BUILDERS',
	enableVisualFullPage: true,
	enableVisualBaselineCompare: false,
	visualThreshold: 3.0,
	urlVariableName: 'BUILDERS_TEST_URL',
	fallbackUrl: 'https://testing-templet-builders.vercel.app/',
	settleSeconds: 2,
	navigationRules: [
		[item: 'Home', settleSeconds: 3, readSeconds: 2],
		[item: 'Tracking', settleSeconds: 3, readSeconds: 2],
		[item: 'Blueprints', allowClickOnly: true, settleSeconds: 3],
		[item: 'Emails', expectNewTab: true, loginInNewTab: true, settleSeconds: 3, newTabExpectedUrlFragments: ['email.templet.io', '/admin/manager.php']],
		[item: 'Sheets', expectNewTab: true, loginInNewTab: true, settleSeconds: 3, newTabExpectedUrlFragments: ['sheets.templet.io', '/admin/manager.php']]
	],
	sequenceItems: [
		'Home',
		'Tracking',
		'Brand properties',
		'Brand assets',
		'Templates',
		'Blueprint Templates',
		'One-off request',
		'Non standard tasks',
		'Blueprints',
		'Emails',
		'Sheets',
		'Project Schedule',
		'Work in Progress',
		'Current Spend',
		'Logout'
	],
	homeItem: 'Home',
	logoutItem: 'Logout'
])
