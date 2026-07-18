// ─────────────────────────────────────────────────────────────────────────────
// TC: TC-SCHEDULERS-TASKCREATION-NONSTD-001
// Plataforma: Schedulers | Área: task-creation
// Descripción: Valida Nonstandard tasks de Schedulers (oleada 2): heading, New sub task, Set it up! (presencia), Reset.
// Suites: Platforms/QA/Repos-Coverage-Schedulers
// Precondiciones: credenciales MS en Include/config/templet-credentials.properties
// ─────────────────────────────────────────────────────────────────────────────
// TC-SCHEDULERS-TASKCREATION-NONSTD-001 - Oleada 2 Fase 2 (cobertura desde repos frontend)
// Element map: Include/config/element-maps/schedulers-task-creation-non-standard.json

CustomKeywords.'ReposCoverageKeywords.validateScreenFromElementMap'([
	caseId          : 'TC-SCHEDULERS-TASKCREATION-NONSTD-001',
	mapRelativePath : 'Include/config/element-maps/schedulers-task-creation-non-standard.json',
	snapshotSlug    : 'tc_schedulers_taskcreation_nonstd_001'
])
