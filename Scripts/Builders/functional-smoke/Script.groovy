CustomKeywords.'TempletPortalKeywords.runPublicSignInSmoke'([
	caseId: 'TC-BUILDERS-FUNCTIONAL-SMOKE-001',
	platformLabel: 'Builders TEST',
	urlVariableName: 'BUILDERS_TEST_URL',
	fallbackUrl: 'https://testing-templet-builders.vercel.app/',
	expectedUrlFragment: 'testing-templet-builders.vercel.app',
	requiredChecks: [
		[label: 'heading_builder', xpath: "//h1[contains(normalize-space(.),'The Builder App')]", compareText: false, timeoutSeconds: 10],
		[label: 'signin_outlook', xpath: "//*[self::a or self::button][contains(normalize-space(.),'Sign in with Outlook')]", compareText: false, timeoutSeconds: 10],
		[label: 'terms_text', xpath: "//*[contains(normalize-space(.),'Terms of Service')]", compareText: false, timeoutSeconds: 10],
		[label: 'contact_support', xpath: "//a[contains(@href,'mailto:developer@templet.io') and contains(normalize-space(.),'Contact Support')]", compareText: false, timeoutSeconds: 10]
	]
])
