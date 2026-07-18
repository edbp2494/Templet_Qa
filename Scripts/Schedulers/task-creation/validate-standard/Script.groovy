// ─────────────────────────────────────────────────────────────────────────────
// TC: TC-SCHEDULERS-TASKCREATION-STANDARD-001
// Plataforma: Schedulers | Área: task-creation
// Descripción: Valida pantalla Standard Tasks de Schedulers (Fase 2). Heading, selects de formulario (client/project/brand), boton New project (aria-label) y Set it up! (solo presencia).
// Suites: Platforms/QA/Repos-Coverage-Schedulers
// Precondiciones: credenciales MS en Include/config/templet-credentials.properties
// ─────────────────────────────────────────────────────────────────────────────
// TC-SCHEDULERS-TASKCREATION-STANDARD-001 - Fase 2 (cobertura generada desde repos frontend Templet-Product-Team)
// Element map: Include/config/element-maps/schedulers-task-creation-standard.json
// Patron: 1 login por suite (isReuseDriver=true); la keyword reusa el driver vivo
// y hace openBrowserAndLoginWithMicrosoft solo si no hay sesion valida.

CustomKeywords.'ReposCoverageKeywords.validateScreenFromElementMap'([
	caseId          : 'TC-SCHEDULERS-TASKCREATION-STANDARD-001',
	mapRelativePath : 'Include/config/element-maps/schedulers-task-creation-standard.json',
	snapshotSlug    : 'tc_schedulers_taskcreation_standard_001'
])
