CustomKeywords.'TempletPortalKeywords.runPublicSignInSmoke'([
	caseId: 'TC-SCHEDULERS-FUNCTIONAL-SMOKE-001',
	platformLabel: 'Schedulers TEST',
	urlVariableName: 'SCHEDULERS_TEST_URL',
	fallbackUrl: 'https://testing-templet-schedulers.vercel.app/',
	expectedUrlFragment: 'testing-templet-schedulers.vercel.app',
	requiredChecks: [
		[label: 'heading_scheduler', xpath: "//h1[contains(normalize-space(.),'Scheduler App')]", compareText: false, timeoutSeconds: 10],
		[label: 'signin_subtitle', xpath: "//*[contains(normalize-space(.),'Sign in with your Outlook account to continue')]", compareText: false, timeoutSeconds: 10],
		[label: 'signin_outlook', xpath: "//*[self::a or self::button][contains(normalize-space(.),'Sign in with Outlook')]", compareText: false, timeoutSeconds: 10],
		[label: 'contact_support', xpath: "//a[contains(@href,'mailto:developer@templet.io') and contains(normalize-space(.),'Contact Support')]", compareText: false, timeoutSeconds: 10]
	]
])
