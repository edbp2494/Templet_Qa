// ─────────────────────────────────────────────────────────────────────────────
// TC: TC-SHEETS-FILTERS-INITIATIVE-VALIDATION-008
// Objetivo: Recorrer todas las initiatives del client BRAVA en Sheets TEST,
//           clasificarlas en con-contenido/vacías y exigir al menos una con
//           documentos. Evidencia por initiative en Reports/Screenshots.
// Precondiciones: credenciales MS; client BRAVA disponible.
// Lógica compartida: Keywords/AdminPhpKeywords.groovy → validateInitiativeContent(config)
// ─────────────────────────────────────────────────────────────────────────────
CustomKeywords.'AdminPhpKeywords.validateInitiativeContent'([
	caseId          : 'TC-SHEETS-FILTERS-INITIATIVE-VALIDATION-008',
	urlVariableName : 'SHEETS_TEST_URL',
	fallbackUrl     : 'https://sheets-test.templet.io/admin/manager.php',
	snapDirName     : 'TC-SHEETS-INITIATIVE-008',
	dashboardObj    : 'Sheets/Filters/section_dashboard',
	filtersObj      : 'Sheets/Filters/section_filters',
	clientObj       : 'Sheets/Filters/select_client',
	initiativeObj   : 'Sheets/Filters/select_initiative',
	logoutObj       : 'Sheets/Filters/btn_logout',
	clientCss       : '#inputGroupSelect01',
	initiativeCss   : '#inputGroupSelect02',
	preferredClient : 'BRAVA',
	emptyMessages   : ['No hay documentos disponibles en esta iniciativa.']
])
