// ─────────────────────────────────────────────────────────────────────────────
// TC: TC-SCHEDULERS-TASKCREATION-EDITS-001
// Plataforma: Schedulers | Área: task-creation
// Descripción: Valida New Edit de Schedulers (oleada 2): heading, selects de formulario y Set it up! (solo presencia).
// Suites: Platforms/QA/Repos-Coverage-Schedulers
// Precondiciones: credenciales MS en Include/config/templet-credentials.properties
// ─────────────────────────────────────────────────────────────────────────────
// TC-SCHEDULERS-TASKCREATION-EDITS-001 - Oleada 2 Fase 2 (cobertura desde repos frontend)
// Element map: Include/config/element-maps/schedulers-task-creation-edits.json

CustomKeywords.'ReposCoverageKeywords.validateScreenFromElementMap'([
	caseId          : 'TC-SCHEDULERS-TASKCREATION-EDITS-001',
	mapRelativePath : 'Include/config/element-maps/schedulers-task-creation-edits.json',
	snapshotSlug    : 'tc_schedulers_taskcreation_edits_001'
])
