// ─────────────────────────────────────────────────────────────────────────────
// TC: TC-SHEETS-LIST-ACTIONS-MODAL-001
// Objetivo: Validar que cada acción del menú de la primera card en List View
//           de Sheets TEST (Edit/Rename/URL/Duplicate/Download/Move/Delete)
//           responde con modal, subwindow o navegación.
// Precondiciones: credenciales MS; client BRAVA con initiative con documentos.
// Lógica compartida: Keywords/AdminPhpKeywords.groovy → validateListActionsModal(config)
// ─────────────────────────────────────────────────────────────────────────────
CustomKeywords.'AdminPhpKeywords.validateListActionsModal'([
	caseId          : 'TC-SHEETS-LIST-ACTIONS-MODAL-001',
	urlVariableName : 'SHEETS_TEST_URL',
	fallbackUrl     : 'https://sheets-test.templet.io/admin/manager.php',
	dashboardObj    : 'Sheets/Filters/section_dashboard',
	clientObj       : 'Sheets/Filters/select_client',
	listViewObj     : 'Sheets/Objects/ListView/icon_list_view',
	listShapeObj    : 'Sheets/Objects/ListView/icon_list_shape',
	clientCss       : '#inputGroupSelect01',
	initiativeCss   : "select[data-toggle='drop-initiatives']",
	preferredClient : 'BRAVA',
	cardSelectors   : '.document-item',
	urlActionLabels : ['URL', 'URL Email', 'Copy URL', 'Copiar URL'],
	emptyMessages   : ['No hay documentos disponibles']
])
