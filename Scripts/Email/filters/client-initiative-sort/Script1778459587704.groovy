// ─────────────────────────────────────────────────────────────────────────────
// TC: TC-EMAIL-FILTERS-CLIENT-INITIATIVE-SORT-007
// Objetivo: Validar flujo completo de filtros Client → Initiative → Sort en
//           Email TEST, con validación de opciones de cada select y evidencia
//           por paso (Reports/Screenshots/TC-EMAIL-FILTERS-007).
// Precondiciones: credenciales MS; client BRAVA disponible.
// Nota: Email usa select[data-toggle='drop-initiatives'] para initiative.
// Lógica compartida: Keywords/AdminPhpKeywords.groovy → runClientInitiativeSortFlow(config)
// ─────────────────────────────────────────────────────────────────────────────
CustomKeywords.'AdminPhpKeywords.runClientInitiativeSortFlow'([
	caseId          : 'TC-EMAIL-FILTERS-CLIENT-INITIATIVE-SORT-007',
	urlVariableName : 'EMAIL_TEST_URL',
	fallbackUrl     : 'https://emails-test.templet.io/admin/manager.php',
	snapDirName     : 'TC-EMAIL-FILTERS-007',
	dashboardObj    : 'Email/Filters/section_dashboard',
	clientObj       : 'Email/Filters/select_client',
	clientCss       : '#inputGroupSelect01',
	initiativeCss   : "select[data-toggle='drop-initiatives']",
	sortCss         : '#sortField-alpha',
	preferredClient : 'BRAVA'
])
