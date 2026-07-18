// ─────────────────────────────────────────────────────────────────────────────
// TC: TC-BUILDERS-TEMPLATE-LIST-001
// Plataforma: Builders | Área: template
// Descripción: Valida pantalla Active Templates de Builders (Fase 2). Heading, busqueda, boton New Template y links a /template/id.
// Suites: Platforms/QA/Repos-Coverage-Builders
// Precondiciones: credenciales MS en Include/config/templet-credentials.properties
// ─────────────────────────────────────────────────────────────────────────────
// TC-BUILDERS-TEMPLATE-LIST-001 - Fase 2 (cobertura generada desde repos frontend Templet-Product-Team)
// Element map: Include/config/element-maps/builders-template-list.json
// Patron: 1 login por suite (isReuseDriver=true); la keyword reusa el driver vivo
// y hace openBrowserAndLoginWithMicrosoft solo si no hay sesion valida.

CustomKeywords.'ReposCoverageKeywords.validateScreenFromElementMap'([
	caseId          : 'TC-BUILDERS-TEMPLATE-LIST-001',
	mapRelativePath : 'Include/config/element-maps/builders-template-list.json',
	snapshotSlug    : 'tc_builders_template_list_001'
])
