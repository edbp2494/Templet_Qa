// ─────────────────────────────────────────────────────────────────────────────
// TC: TC-BUILDERS-CURRENTSPEND-TRACK-001
// Plataforma: Builders | Área: current-spend
// Descripción: Valida Current spend de Builders (oleada 2): heading, busqueda y loading.
// Suites: Platforms/QA/Repos-Coverage-Builders
// Precondiciones: credenciales MS en Include/config/templet-credentials.properties
// ─────────────────────────────────────────────────────────────────────────────
// TC-BUILDERS-CURRENTSPEND-TRACK-001 - Oleada 2 Fase 2 (cobertura desde repos frontend)
// Element map: Include/config/element-maps/builders-current-spend.json

CustomKeywords.'ReposCoverageKeywords.validateScreenFromElementMap'([
	caseId          : 'TC-BUILDERS-CURRENTSPEND-TRACK-001',
	mapRelativePath : 'Include/config/element-maps/builders-current-spend.json',
	snapshotSlug    : 'tc_builders_currentspend_track_001'
])
