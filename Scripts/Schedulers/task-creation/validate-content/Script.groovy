// ─────────────────────────────────────────────────────────────────────────────
// TC: TC-SCHEDULERS-TASKCREATION-CONTENT-001
// Plataforma: Schedulers | Área: task-creation
// Descripción: Valida Content Task Creation de Schedulers (oleada 2): titulo con id estable y tabs New Content/New Edit.
// Suites: Platforms/QA/Repos-Coverage-Schedulers
// Precondiciones: credenciales MS en Include/config/templet-credentials.properties
// ─────────────────────────────────────────────────────────────────────────────
// TC-SCHEDULERS-TASKCREATION-CONTENT-001 - Oleada 2 Fase 2 (cobertura desde repos frontend)
// Element map: Include/config/element-maps/schedulers-task-creation-content.json

CustomKeywords.'ReposCoverageKeywords.validateScreenFromElementMap'([
	caseId          : 'TC-SCHEDULERS-TASKCREATION-CONTENT-001',
	mapRelativePath : 'Include/config/element-maps/schedulers-task-creation-content.json',
	snapshotSlug    : 'tc_schedulers_taskcreation_content_001'
])
