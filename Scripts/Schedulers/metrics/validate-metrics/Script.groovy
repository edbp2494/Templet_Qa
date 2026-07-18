// ─────────────────────────────────────────────────────────────────────────────
// TC: TC-SCHEDULERS-METRICS-DASHBOARD-001
// Plataforma: Schedulers | Área: metrics
// Descripción: Valida Metrics Dashboard de Schedulers (oleada 2): heading, subtitulo, Load Data (click) y loading.
// Suites: Platforms/QA/Repos-Coverage-Schedulers
// Precondiciones: credenciales MS en Include/config/templet-credentials.properties
// ─────────────────────────────────────────────────────────────────────────────
// TC-SCHEDULERS-METRICS-DASHBOARD-001 - Oleada 2 Fase 2 (cobertura desde repos frontend)
// Element map: Include/config/element-maps/schedulers-metrics.json

CustomKeywords.'ReposCoverageKeywords.validateScreenFromElementMap'([
	caseId          : 'TC-SCHEDULERS-METRICS-DASHBOARD-001',
	mapRelativePath : 'Include/config/element-maps/schedulers-metrics.json',
	snapshotSlug    : 'tc_schedulers_metrics_dashboard_001'
])
