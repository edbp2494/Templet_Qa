// ─────────────────────────────────────────────────────────────────────────────
// TC: TC-EMAIL-FILTERS-INITIATIVE-VALIDATION-008
// Objetivo: Recorrer todas las initiatives del client BRAVA en Email TEST,
//           clasificarlas en con-contenido/vacías y exigir al menos una con
//           emails. Evidencia por initiative en Reports/Screenshots.
// Precondiciones: credenciales MS; client BRAVA disponible.
// Nota: Email usa select[data-toggle='drop-initiatives'] para initiative.
// Lógica compartida: Keywords/AdminPhpKeywords.groovy → validateInitiativeContent(config)
// ─────────────────────────────────────────────────────────────────────────────
CustomKeywords.'AdminPhpKeywords.validateInitiativeContent'([
	caseId          : 'TC-EMAIL-FILTERS-INITIATIVE-VALIDATION-008',
	urlVariableName : 'EMAIL_TEST_URL',
	fallbackUrl     : 'https://emails-test.templet.io/admin/manager.php',
	snapDirName     : 'TC-EMAIL-INITIATIVE-008',
	dashboardObj    : 'Email/Filters/section_dashboard',
	clientObj       : 'Email/Filters/select_client',
	clientCss       : '#inputGroupSelect01',
	initiativeCss   : "select[data-toggle='drop-initiatives']",
	preferredClient : 'BRAVA',
	emptyMessages   : ['No hay documentos disponibles', 'No hay documentos', 'No documents available', 'No hay emails disponibles', 'No emails available']
])
