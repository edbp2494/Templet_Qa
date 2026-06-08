String buildersTestUrl = CustomKeywords.'CommonKeywords.getRequiredGlobal'('BUILDERS_TEST_URL', 'https://testing-templet-builders.vercel.app/')
String trackingUrl = CustomKeywords.'CommonKeywords.buildTrackingUrl'(buildersTestUrl)

CustomKeywords.'TempletPortalKeywords.validateBuildersTrackingTabDashboard'([
	caseId: 'TC-BUILDERS-TRACKING-TASK-001',
	platformLabel: 'Builders TEST - Tracking Task Creation',
	tabLabel: 'Task Creation',
	expectedTheme: 'task',
	expectedDailyPrefix: 'task creation operations per day',
	requiredCardTitles: [
		'Task Creation Non Standard Request',
		'Task Creation One Off Request'
	],
	urlVariableName: 'BUILDERS_TEST_URL',
	fallbackUrl: buildersTestUrl,
	directUrl: trackingUrl,
	snapshotLatestPath: System.getProperty('user.dir') + '/Reports/Tracking/snapshots/tracking_task_creation_latest.json',
	snapshotHistoryDir: System.getProperty('user.dir') + '/Reports/Tracking/snapshots/history'
])