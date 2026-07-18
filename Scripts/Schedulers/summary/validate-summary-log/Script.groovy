// ─────────────────────────────────────────────────────────────────────────────
// TC: TC-SCHEDULERS-SUMMARY-LOG-001
// Plataforma: Schedulers | Área: summary
// Descripción: Valida pantalla Summary (task creation log) de Schedulers (Fase 2). Heading, busqueda, Clear Filters, headers ordenables (click en Type) y loading async.
// Suites: Platforms/QA/Repos-Coverage-Schedulers
// Precondiciones: credenciales MS en Include/config/templet-credentials.properties
// ─────────────────────────────────────────────────────────────────────────────
// TC-SCHEDULERS-SUMMARY-LOG-001 - Fase 2 (cobertura generada desde repos frontend Templet-Product-Team)
// Element map: Include/config/element-maps/schedulers-summary.json
// Patron: 1 login por suite (isReuseDriver=true); la keyword reusa el driver vivo
// y hace openBrowserAndLoginWithMicrosoft solo si no hay sesion valida.

CustomKeywords.'ReposCoverageKeywords.validateScreenFromElementMap'([
	caseId          : 'TC-SCHEDULERS-SUMMARY-LOG-001',
	mapRelativePath : 'Include/config/element-maps/schedulers-summary.json',
	snapshotSlug    : 'tc_schedulers_summary_log_001'
])
