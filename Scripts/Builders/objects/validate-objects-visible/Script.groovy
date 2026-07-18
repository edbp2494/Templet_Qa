// ─────────────────────────────────────────────────────────────────────────────
// TC: TC-BUILDERS-SIDEBAR-ORDER-001
// Plataforma: Builders | Área: objects
// Descripción: Valida toolbar izquierda autenticada de Builders, incluyendo orden esperado de items.
// Suites: Master/Full-Regression, Platforms/Full-Validation-All-Platforms, Platforms/Super-Suite-Validation, Platforms/Builders/Objects/Visible-Clicks
// Precondiciones: credenciales MS en Include/config/templet-credentials.properties
// ─────────────────────────────────────────────────────────────────────────────
CustomKeywords.'TempletPortalKeywords.verifyAuthenticatedSidebarOrderOnly'([
	caseId: 'TC-BUILDERS-SIDEBAR-ORDER-001',
	platformLabel: 'Builders TEST',
	urlVariableName: 'BUILDERS_TEST_URL',
	fallbackUrl: 'https://testing-templet-builders.vercel.app/',
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
