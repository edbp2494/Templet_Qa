// ─────────────────────────────────────────────────────────────────────────────
// TC: TC-BUILDERS-SMOKE-000
// Plataforma: Builders | Área: (raíz)
// Descripción: Smoke base de Builders TEST.
// Suites: Master/Full-Regression, Platforms/Super-Suite-Validation, Platforms/Builders/Landing/Smoke
// Precondiciones: credenciales MS en Include/config/templet-credentials.properties
// ─────────────────────────────────────────────────────────────────────────────
CustomKeywords.'TempletPortalKeywords.runPublicLandingSmoke'([
	caseId: 'TC-BUILDERS-SMOKE-000',
	platformLabel: 'Builders TEST',
	urlVariableName: 'BUILDERS_TEST_URL',
	fallbackUrl: 'https://testing-templet-builders.vercel.app/',
	expectedUrlFragment: 'testing-templet-builders.vercel.app'
])
