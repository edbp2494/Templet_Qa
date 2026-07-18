// ─────────────────────────────────────────────────────────────────────────────
// TC: TC-SCHEDULERS-RESOURCES-BOARD-001
// Plataforma: Schedulers | Área: resources
// Descripción: Valida pantalla Resources de Schedulers (Fase 2). Heading, subtitulo y board de equipo.
// Suites: Platforms/QA/Repos-Coverage-Schedulers, QA/Revalidate-Open-Issues
// Precondiciones: credenciales MS en Include/config/templet-credentials.properties
// ─────────────────────────────────────────────────────────────────────────────
// TC-SCHEDULERS-RESOURCES-BOARD-001 - Fase 2 (cobertura generada desde repos frontend Templet-Product-Team)
// Element map: Include/config/element-maps/schedulers-resources.json
// Patron: 1 login por suite (isReuseDriver=true); la keyword reusa el driver vivo
// y hace openBrowserAndLoginWithMicrosoft solo si no hay sesion valida.

CustomKeywords.'ReposCoverageKeywords.validateScreenFromElementMap'([
	caseId          : 'TC-SCHEDULERS-RESOURCES-BOARD-001',
	mapRelativePath : 'Include/config/element-maps/schedulers-resources.json',
	snapshotSlug    : 'tc_schedulers_resources_board_001'
])
