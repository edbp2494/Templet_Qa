String buildersTestUrl = CustomKeywords.'CommonKeywords.getRequiredGlobal'('BUILDERS_TEST_URL', 'https://testing-templet-builders.vercel.app/')
String trackingUrl = CustomKeywords.'CommonKeywords.buildTrackingUrl'(buildersTestUrl)

CustomKeywords.'TempletPortalKeywords.validateBuildersTrackingTabDashboard'([
	caseId: 'TC-BUILDERS-TRACKING-BLUEPRINT-001',
	platformLabel: 'Builders TEST - Tracking Blueprint',
	tabLabel: 'Blueprint',
	expectedTheme: 'blueprint',
	expectedDailyPrefix: 'blueprint operations per day',
	requiredCardTitles: [
		'Blueprint Work Plan',
		'Blueprint Creation Admin',
		'Blueprint Creation Poweruser',
		'Blueprint Ai Draft Generation',
		'Blueprint Task Creation Flow'
	],
	urlVariableName: 'BUILDERS_TEST_URL',
	fallbackUrl: buildersTestUrl,
	directUrl: trackingUrl,
	validateEmailFilters: true,
	snapshotLatestPath: System.getProperty('user.dir') + '/Reports/Tracking/snapshots/tracking_blueprint_latest.json',
	snapshotHistoryDir: System.getProperty('user.dir') + '/Reports/Tracking/snapshots/history'
])
