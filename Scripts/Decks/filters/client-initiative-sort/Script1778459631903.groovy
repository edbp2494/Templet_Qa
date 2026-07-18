// ─────────────────────────────────────────────────────────────────────────────
// TC: TC-DECKS-FILTERS-CLIENT-INITIATIVE-SORT-007
// Objetivo: Validar flujo completo de filtros Client → Initiative → Sort en
//           Decks TEST, con validación de opciones de cada select y evidencia
//           por paso (Reports/Screenshots/TC-DECKS-FILTERS-007).
// Precondiciones: credenciales MS; client BRAVA disponible.
// Nota: Decks no tiene objetos OR para initiative/sort/logout → se usan CSS.
// Lógica compartida: Keywords/AdminPhpKeywords.groovy → runClientInitiativeSortFlow(config)
// ─────────────────────────────────────────────────────────────────────────────
CustomKeywords.'AdminPhpKeywords.runClientInitiativeSortFlow'([
	caseId          : 'TC-DECKS-FILTERS-CLIENT-INITIATIVE-SORT-007',
	urlVariableName : 'DECKS_TEST_URL',
	fallbackUrl     : 'https://decks-test.templet.io/admin/manager.php',
	snapDirName     : 'TC-DECKS-FILTERS-007',
	dashboardObj    : 'Decks/Filters/section_dashboard',
	clientObj       : 'Decks/Filters/select_client',
	clientCss       : '#inputGroupSelect01',
	initiativeCss   : '#inputGroupSelect02',
	sortCss         : '#sortField-alpha',
	preferredClient : 'BRAVA'
])
