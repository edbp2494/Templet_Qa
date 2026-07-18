// ─────────────────────────────────────────────────────────────────────────────
// TC: TC-SCHEDULERS-REQUESTS-LIST-001
// Plataforma: Schedulers | Área: requests
// Descripción: Valida pantalla Requests de Schedulers (Fase 2). Heading, subtitulo, busqueda y filtros.
// Suites: Platforms/QA/Repos-Coverage-Schedulers, QA/Revalidate-Open-Issues, QA/Revalidate-Requests-Only
// Precondiciones: credenciales MS en Include/config/templet-credentials.properties
// ─────────────────────────────────────────────────────────────────────────────
// TC-SCHEDULERS-REQUESTS-LIST-001 - Fase 2 (cobertura generada desde repos frontend Templet-Product-Team)
// Element map: Include/config/element-maps/schedulers-requests.json
// Patron: 1 login por suite (isReuseDriver=true); la keyword reusa el driver vivo
// y hace openBrowserAndLoginWithMicrosoft solo si no hay sesion valida.

CustomKeywords.'ReposCoverageKeywords.validateScreenFromElementMap'([
	caseId          : 'TC-SCHEDULERS-REQUESTS-LIST-001',
	mapRelativePath : 'Include/config/element-maps/schedulers-requests.json',
	snapshotSlug    : 'tc_schedulers_requests_list_001'
])
