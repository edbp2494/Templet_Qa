// ─────────────────────────────────────────────────────────────────────────────
// TC: TC-BUILDERS-TRACKING-001
// Plataforma: Builders | Área: direct-redirect
// Descripción: Valida redirección directa de Tracking en Builder y carga de objetos visibles.
// Suites: Master/Full-Regression, Platforms/Super-Suite-Validation, Platforms/Builders/Redirects/Direct-Redirect
// Precondiciones: credenciales MS en Include/config/templet-credentials.properties
// ─────────────────────────────────────────────────────────────────────────────
CustomKeywords.'TempletPortalKeywords.validateDirectRedirect'([
	caseId: 'TC-BUILDERS-TRACKING-001',
	platformLabel: 'Builders TEST',
	urlVariableName: 'BUILDERS_TEST_URL',
	fallbackUrl: 'https://testing-templet-builders.vercel.app/',
	directUrl: 'https://testing-templet-builders.vercel.app/tracking',
	routePath: '/tracking',
	expectedUrlFragments: ['/tracking']
])
