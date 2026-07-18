// ─────────────────────────────────────────────────────────────────────────────
// TC: TC-BUILDERS-TRACKING-ALL-001
// Plataforma: Builders | Área: tracking
// Descripción: Valida Tracking All: texto, forma, color, posicion de objetos y snapshot de metricas.
// Suites: Master/Full-Regression, Platforms/Full-Validation-All-Platforms, Platforms/Super-Suite-Validation, Platforms/Builders/Tracking/Tracking-Full-Flow
// Precondiciones: credenciales MS en Include/config/templet-credentials.properties
// ─────────────────────────────────────────────────────────────────────────────
String buildersTestUrl = CustomKeywords.'CommonKeywords.getRequiredGlobal'('BUILDERS_TEST_URL', 'https://testing-templet-builders.vercel.app/')
String trackingUrl = CustomKeywords.'CommonKeywords.buildTrackingUrl'(buildersTestUrl)

CustomKeywords.'TempletPortalKeywords.validateBuildersTrackingAllDashboard'([
	caseId: 'TC-BUILDERS-TRACKING-ALL-001',
	platformLabel: 'Builders TEST - Tracking All',
	urlVariableName: 'BUILDERS_TEST_URL',
	fallbackUrl: buildersTestUrl,
	directUrl: trackingUrl,
	snapshotLatestPath: System.getProperty('user.dir') + '/Reports/Tracking/snapshots/tracking_all_latest.json',
	snapshotHistoryDir: System.getProperty('user.dir') + '/Reports/Tracking/snapshots/history',
	positionTolerancePx: 48
])