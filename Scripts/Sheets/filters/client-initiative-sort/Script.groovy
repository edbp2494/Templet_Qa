// ─────────────────────────────────────────────────────────────────────────────
// TC: TC-SHEETS-FILTERS-CLIENT-INITIATIVE-SORT-007
// Objetivo: Validar flujo completo de filtros Client → Initiative → Sort en
//           Sheets TEST, con validación de opciones de cada select y evidencia
//           por paso (Reports/Screenshots/TC-SHEETS-FILTERS-007).
// Precondiciones: credenciales MS; client BRAVA disponible.
// Lógica compartida: Keywords/AdminPhpKeywords.groovy → runClientInitiativeSortFlow(config)
// ─────────────────────────────────────────────────────────────────────────────
CustomKeywords.'AdminPhpKeywords.runClientInitiativeSortFlow'([
	caseId          : 'TC-SHEETS-FILTERS-CLIENT-INITIATIVE-SORT-007',
	urlVariableName : 'SHEETS_TEST_URL',
	fallbackUrl     : 'https://sheets-test.templet.io/admin/manager.php',
	snapDirName     : 'TC-SHEETS-FILTERS-007',
	dashboardObj    : 'Sheets/Filters/section_dashboard',
	filtersObj      : 'Sheets/Filters/section_filters',
	clientObj       : 'Sheets/Filters/select_client',
	initiativeObj   : 'Sheets/Filters/select_initiative',
	sortObj         : 'Sheets/Filters/select_sort',
	logoutObj       : 'Sheets/Filters/btn_logout',
	clientCss       : '#inputGroupSelect01',
	initiativeCss   : '#inputGroupSelect02',
	sortCss         : '#sortField-alpha',
	preferredClient : 'BRAVA'
])
