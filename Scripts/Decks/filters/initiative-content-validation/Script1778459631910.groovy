// ─────────────────────────────────────────────────────────────────────────────
// TC: TC-DECKS-FILTERS-INITIATIVE-VALIDATION-008
// Objetivo: Recorrer todas las initiatives del client BRAVA en Decks TEST,
//           clasificarlas en con-contenido/vacías y exigir al menos una con
//           documentos. Evidencia por initiative en Reports/Screenshots.
// Precondiciones: credenciales MS; client BRAVA disponible.
// Lógica compartida: Keywords/AdminPhpKeywords.groovy → validateInitiativeContent(config)
// ─────────────────────────────────────────────────────────────────────────────
CustomKeywords.'AdminPhpKeywords.validateInitiativeContent'([
	caseId          : 'TC-DECKS-FILTERS-INITIATIVE-VALIDATION-008',
	urlVariableName : 'DECKS_TEST_URL',
	fallbackUrl     : 'https://decks-test.templet.io/admin/manager.php',
	snapDirName     : 'TC-DECKS-INITIATIVE-008',
	dashboardObj    : 'Decks/Filters/section_dashboard',
	clientObj       : 'Decks/Filters/select_client',
	clientCss       : '#inputGroupSelect01',
	initiativeCss   : '#inputGroupSelect02',
	preferredClient : 'BRAVA',
	emptyMessages   : ['No hay documentos disponibles', 'No hay documentos', 'No documents available']
])
