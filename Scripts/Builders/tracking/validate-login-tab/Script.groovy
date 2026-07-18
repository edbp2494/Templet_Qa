// ─────────────────────────────────────────────────────────────────────────────
// TC: TC-BUILDERS-TRACKING-LOGIN-001
// Plataforma: Builders | Área: tracking
// Descripción: Valida click y contenido visual del tab Login en Tracking.
// Suites: Master/Full-Regression, Platforms/Full-Validation-All-Platforms, Platforms/Super-Suite-Validation, Platforms/Builders/Tracking/Tracking-Full-Flow
// Precondiciones: credenciales MS en Include/config/templet-credentials.properties
// ─────────────────────────────────────────────────────────────────────────────
String buildersTestUrl = CustomKeywords.'CommonKeywords.getRequiredGlobal'('BUILDERS_TEST_URL', 'https://testing-templet-builders.vercel.app/')
String trackingUrl = CustomKeywords.'CommonKeywords.buildTrackingUrl'(buildersTestUrl)

CustomKeywords.'TempletPortalKeywords.validateBuildersTrackingTabDashboard'([
	caseId: 'TC-BUILDERS-TRACKING-LOGIN-001',
	platformLabel: 'Builders TEST - Tracking Login',
	tabLabel: 'Login',
	expectedTheme: 'login',
	expectedDailyPrefix: 'login operations per day',
	requiredCardTitles: [
		'Login'
	],
	urlVariableName: 'BUILDERS_TEST_URL',
	fallbackUrl: buildersTestUrl,
	directUrl: trackingUrl,
	validateEmailFilters: true,
	snapshotLatestPath: System.getProperty('user.dir') + '/Reports/Tracking/snapshots/tracking_login_latest.json',
	snapshotHistoryDir: System.getProperty('user.dir') + '/Reports/Tracking/snapshots/history'
])