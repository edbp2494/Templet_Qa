// ─────────────────────────────────────────────────────────────────────────────
// TC: TC-EMAIL-SORT-VALIDATION-001
// Objetivo: Validar que Sort (Newest/Oldest/A to Z/Z to A) reordena los
//           documentos en View Grid y View List de Email TEST, exigiendo que
//           Newest/Oldest inviertan el orden. FALLA si Sort no funciona.
// Precondiciones: credenciales MS; client BRAVA con initiative con contenido.
// Lógica compartida: Keywords/AdminPhpKeywords.groovy → validateSortGridList(config)
// ─────────────────────────────────────────────────────────────────────────────
CustomKeywords.'AdminPhpKeywords.validateSortGridList'([
	caseId          : 'TC-EMAIL-SORT-VALIDATION-001',
	urlVariableName : 'EMAIL_TEST_URL',
	fallbackUrl     : 'https://emails-test.templet.io/admin/manager.php',
	dashboardObj    : 'Email/Filters/section_dashboard',
	clientObj       : 'Email/Filters/select_client',
	viewListObj     : 'Common/AdminPHP/a_View List',
	clientCss       : '#inputGroupSelect01',
	initiativeCss   : "select[data-toggle='drop-initiatives']",
	sortCss         : '#sortField-alpha',
	preferredClient : 'BRAVA',
	cardSelectors   : '.document-item, .email-item, .message-item, .thumbnails-boxes .thumbnail-box, .thumbnails-boxes .card, .documents-list .row, [class*="item"], [class*="card"]',
	titleSelectors  : 'h6.title-card[data-title], h6[data-title], h6.title-card, h6, [data-title]',
	emptyMessages   : ['No hay documentos disponibles', 'No hay documentos', 'No documents available', 'No hay emails disponibles', 'No emails available']
])
