// ─────────────────────────────────────────────────────────────────────────────
// TC: TC-SHEETS-ADMIN-SURFACE-001
// Plataforma: Sheets | Área: admin
// Descripción: Valida superficie admin de Sheets (Fase 2): manager.php post-login + chequeo de seguridad de info.php (phpinfo expuesto = warning MEDIUM) + render de 403.php.
// Suites: Platforms/QA/Repos-Coverage-AdminPHP, QA/Revalidate-Open-Issues
// Precondiciones: credenciales MS en Include/config/templet-credentials.properties
// ─────────────────────────────────────────────────────────────────────────────
// TC-SHEETS-ADMIN-SURFACE-001 - Fase 2 (cobertura generada desde repos frontend Templet-Product-Team)
// Element map: Include/config/element-maps/sheets-admin-surface.json
// Patron: 1 login por suite (isReuseDriver=true); la keyword reusa el driver vivo
// y hace openBrowserAndLoginWithMicrosoft solo si no hay sesion valida.

CustomKeywords.'ReposCoverageKeywords.validateScreenFromElementMap'([
	caseId          : 'TC-SHEETS-ADMIN-SURFACE-001',
	mapRelativePath : 'Include/config/element-maps/sheets-admin-surface.json',
	snapshotSlug    : 'tc_sheets_admin_surface_001'
])
