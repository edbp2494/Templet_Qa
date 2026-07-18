// ─────────────────────────────────────────────────────────────────────────────
// TC: TC-BUILDERS-TASKCREATION-NONSTANDARD-001
// Plataforma: Builders | Área: task-creation
// Descripción: Valida pantalla Task Creation Non-Standard de Builders desde el codigo fuente del repo templet-builders (Fase 2). Heading, form selectors, botones New sub task / Set it up! / Reset.
// Suites: Platforms/QA/Repos-Coverage-Builders, QA/Revalidate-Open-Issues
// Precondiciones: credenciales MS en Include/config/templet-credentials.properties
// ─────────────────────────────────────────────────────────────────────────────
// TC-BUILDERS-TASKCREATION-NONSTANDARD-001 - Fase 2 (cobertura generada desde repos frontend Templet-Product-Team)
// Element map: Include/config/element-maps/builders-task-creation-non-standard.json
// Patron: 1 login por suite (isReuseDriver=true); la keyword reusa el driver vivo
// y hace openBrowserAndLoginWithMicrosoft solo si no hay sesion valida.

CustomKeywords.'ReposCoverageKeywords.validateScreenFromElementMap'([
	caseId          : 'TC-BUILDERS-TASKCREATION-NONSTANDARD-001',
	mapRelativePath : 'Include/config/element-maps/builders-task-creation-non-standard.json',
	snapshotSlug    : 'tc_builders_taskcreation_nonstandard_001'
])
