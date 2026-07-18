// ─────────────────────────────────────────────────────────────────────────────
// TC: TC-SCHEDULERS-DAILYPULSE-DASHBOARD-001
// Plataforma: Schedulers | Área: daily-pulse
// Descripción: Valida Daily Pulse de Schedulers (oleada 2): header, date picker con id estable.
// Suites: Platforms/QA/Repos-Coverage-Schedulers
// Precondiciones: credenciales MS en Include/config/templet-credentials.properties
// ─────────────────────────────────────────────────────────────────────────────
// TC-SCHEDULERS-DAILYPULSE-DASHBOARD-001 - Oleada 2 Fase 2 (cobertura desde repos frontend)
// Element map: Include/config/element-maps/schedulers-daily-pulse.json

CustomKeywords.'ReposCoverageKeywords.validateScreenFromElementMap'([
	caseId          : 'TC-SCHEDULERS-DAILYPULSE-DASHBOARD-001',
	mapRelativePath : 'Include/config/element-maps/schedulers-daily-pulse.json',
	snapshotSlug    : 'tc_schedulers_dailypulse_dashboard_001'
])
