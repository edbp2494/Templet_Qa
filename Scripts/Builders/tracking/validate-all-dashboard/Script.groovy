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