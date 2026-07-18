// ─────────────────────────────────────────────────────────────────────────────
// TC: TC-BUILDERS-SCHEDULE-PROJECT-001
// Plataforma: Builders | Área: project-schedule
// Descripción: Valida pantalla Project Schedule de Builders (Fase 2). Heading, timeline grid y loading async.
// Suites: Platforms/QA/Repos-Coverage-Builders
// Precondiciones: credenciales MS en Include/config/templet-credentials.properties
// ─────────────────────────────────────────────────────────────────────────────
// TC-BUILDERS-SCHEDULE-PROJECT-001 - Fase 2 (cobertura generada desde repos frontend Templet-Product-Team)
// Element map: Include/config/element-maps/builders-project-schedule.json
// Patron: 1 login por suite (isReuseDriver=true); la keyword reusa el driver vivo
// y hace openBrowserAndLoginWithMicrosoft solo si no hay sesion valida.

CustomKeywords.'ReposCoverageKeywords.validateScreenFromElementMap'([
	caseId          : 'TC-BUILDERS-SCHEDULE-PROJECT-001',
	mapRelativePath : 'Include/config/element-maps/builders-project-schedule.json',
	snapshotSlug    : 'tc_builders_schedule_project_001'
])
