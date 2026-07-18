// ─────────────────────────────────────────────────────────────────────────────
// TC: TC-DECKS-SORT-VALIDATION-001
// Objetivo: Validar que Sort (Newest/Oldest/A to Z/Z to A) reordena los
//           documentos en View Grid y View List de Decks TEST, exigiendo que
//           Newest/Oldest inviertan el orden. FALLA si Sort no funciona.
// Precondiciones: credenciales MS; client BRAVA con initiative con contenido.
// Lógica compartida: Keywords/AdminPhpKeywords.groovy → validateSortGridList(config)
// ─────────────────────────────────────────────────────────────────────────────
CustomKeywords.'AdminPhpKeywords.validateSortGridList'([
	caseId          : 'TC-DECKS-SORT-VALIDATION-001',
	urlVariableName : 'DECKS_TEST_URL',
	fallbackUrl     : 'https://decks-test.templet.io/admin/manager.php',
	dashboardObj    : 'Decks/Filters/section_dashboard',
	clientObj       : 'Decks/Filters/select_client',
	viewListObj     : 'Common/AdminPHP/a_View List',
	clientCss       : '#inputGroupSelect01',
	initiativeCss   : "select[data-toggle='drop-initiatives']",
	sortCss         : '#sortField-alpha',
	preferredClient : 'BRAVA',
	cardSelectors   : '.document-item',
	titleSelectors  : 'h6.title-card[data-title], h6.title-card, [data-title]',
	emptyMessages   : ['No hay documentos disponibles']
])
