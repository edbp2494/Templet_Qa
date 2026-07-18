// ─────────────────────────────────────────────────────────────────────────────
// TC: TC-SCHEDULERS-SMOKE-000
// Plataforma: Schedulers | Área: (raíz)
// Descripción: Smoke base de Schedulers TEST.
// Suites: Master/Full-Regression, Platforms/Super-Suite-Validation, Platforms/Schedulers/Landing/Smoke
// Precondiciones: credenciales MS en Include/config/templet-credentials.properties
// ─────────────────────────────────────────────────────────────────────────────
CustomKeywords.'TempletPortalKeywords.runPublicLandingSmoke'([
	caseId: 'TC-SCHEDULERS-SMOKE-000',
	platformLabel: 'Schedulers TEST',
	urlVariableName: 'SCHEDULERS_TEST_URL',
	fallbackUrl: 'https://testing-templet-schedulers.vercel.app/',
	expectedUrlFragment: 'testing-templet-schedulers.vercel.app'
])
