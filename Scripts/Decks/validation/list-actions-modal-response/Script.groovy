// ─────────────────────────────────────────────────────────────────────────────
// TC: TC-DECKS-LIST-ACTIONS-MODAL-001
// Objetivo: Validar que cada acción del menú de la primera card en List View
//           de Decks TEST (Edit/Rename/URL/Duplicate/Download/Move/Delete)
//           responde con modal, subwindow o navegación.
// Precondiciones: credenciales MS; client BRAVA con initiative con documentos.
// Lógica compartida: Keywords/AdminPhpKeywords.groovy → validateListActionsModal(config)
// ─────────────────────────────────────────────────────────────────────────────
CustomKeywords.'AdminPhpKeywords.validateListActionsModal'([
	caseId          : 'TC-DECKS-LIST-ACTIONS-MODAL-001',
	urlVariableName : 'DECKS_TEST_URL',
	fallbackUrl     : 'https://decks-test.templet.io/admin/manager.php',
	dashboardObj    : 'Decks/Filters/section_dashboard',
	clientObj       : 'Decks/Filters/select_client',
	listViewObj     : 'Decks/Objects/ListView/icon_list_view',
	listShapeObj    : 'Decks/Objects/ListView/icon_list_shape',
	clientCss       : '#inputGroupSelect01',
	initiativeCss   : "select[data-toggle='drop-initiatives']",
	preferredClient : 'BRAVA',
	cardSelectors   : '.document-item',
	urlActionLabels : ['URL', 'Copy URL', 'Copiar URL'],
	emptyMessages   : ['No hay documentos disponibles']
])
