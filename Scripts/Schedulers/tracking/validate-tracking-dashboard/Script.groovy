// ─────────────────────────────────────────────────────────────────────────────
// TC: TC-SCHEDULERS-TRACKING-DASHBOARD-001
// Plataforma: Schedulers | Área: tracking
// Descripción: Valida pantalla Execution Tracking de Schedulers (Fase 2). Heading, selectores de periodo, boton Load Data (click) y loading de telemetria.
// Suites: Platforms/QA/Repos-Coverage-Schedulers, QA/Revalidate-Open-Issues
// Precondiciones: credenciales MS en Include/config/templet-credentials.properties
// ─────────────────────────────────────────────────────────────────────────────
// TC-SCHEDULERS-TRACKING-DASHBOARD-001 - Fase 2 (cobertura generada desde repos frontend Templet-Product-Team)
// Element map: Include/config/element-maps/schedulers-tracking.json
// Patron: 1 login por suite (isReuseDriver=true); la keyword reusa el driver vivo
// y hace openBrowserAndLoginWithMicrosoft solo si no hay sesion valida.

CustomKeywords.'ReposCoverageKeywords.validateScreenFromElementMap'([
	caseId          : 'TC-SCHEDULERS-TRACKING-DASHBOARD-001',
	mapRelativePath : 'Include/config/element-maps/schedulers-tracking.json',
	snapshotSlug    : 'tc_schedulers_tracking_dashboard_001'
])
