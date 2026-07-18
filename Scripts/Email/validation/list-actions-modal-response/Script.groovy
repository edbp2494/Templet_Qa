// ─────────────────────────────────────────────────────────────────────────────
// TC: TC-EMAIL-LIST-ACTIONS-MODAL-001
// Objetivo: Validar que cada acción del menú de la primera card en List View
//           de Email TEST (Edit/Rename/URL/Duplicate/Download/Move/Delete)
//           responde con modal, subwindow o navegación.
// Precondiciones: credenciales MS; client BRAVA con initiative con documentos.
// Lógica compartida: Keywords/AdminPhpKeywords.groovy → validateListActionsModal(config)
// ─────────────────────────────────────────────────────────────────────────────
CustomKeywords.'AdminPhpKeywords.validateListActionsModal'([
	caseId          : 'TC-EMAIL-LIST-ACTIONS-MODAL-001',
	urlVariableName : 'EMAIL_TEST_URL',
	fallbackUrl     : 'https://emails-test.templet.io/admin/manager.php',
	dashboardObj    : 'Email/Filters/section_dashboard',
	clientObj       : 'Email/Filters/select_client',
	listViewObj     : 'Email/Objects/ListView/icon_list_view',
	listShapeObj    : 'Email/Objects/ListView/icon_list_shape',
	clientCss       : '#inputGroupSelect01',
	initiativeCss   : "select[data-toggle='drop-initiatives']",
	preferredClient : 'BRAVA',
	cardSelectors   : '.document-item',
	urlActionLabels : ['URL', 'URL Email', 'Copy URL', 'Copiar URL'],
	emptyMessages   : ['No hay documentos disponibles', 'No hay documentos', 'No documents available', 'No hay emails disponibles', 'No emails available']
])
