// ─────────────────────────────────────────────────────────────────────────────
// TC: TC-BUILDERS-TASKCREATION-CONTENT-001
// Plataforma: Builders | Área: task-creation
// Descripción: Valida pantalla Content Task Creation de Builders (Fase 2). Titulo con id estable y tabs New Content / New Edit.
// Suites: Platforms/QA/Repos-Coverage-Builders
// Precondiciones: credenciales MS en Include/config/templet-credentials.properties
// ─────────────────────────────────────────────────────────────────────────────
// TC-BUILDERS-TASKCREATION-CONTENT-001 - Fase 2 (cobertura generada desde repos frontend Templet-Product-Team)
// Element map: Include/config/element-maps/builders-task-creation-content.json
// Patron: 1 login por suite (isReuseDriver=true); la keyword reusa el driver vivo
// y hace openBrowserAndLoginWithMicrosoft solo si no hay sesion valida.

CustomKeywords.'ReposCoverageKeywords.validateScreenFromElementMap'([
	caseId          : 'TC-BUILDERS-TASKCREATION-CONTENT-001',
	mapRelativePath : 'Include/config/element-maps/builders-task-creation-content.json',
	snapshotSlug    : 'tc_builders_taskcreation_content_001'
])
