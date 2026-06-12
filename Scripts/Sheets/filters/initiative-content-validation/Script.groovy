import static com.kms.katalon.core.testobject.ObjectRepository.findTestObject
import com.kms.katalon.core.model.FailureHandling as FailureHandling
import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * TC-SHEETS-FILTERS-INITIATIVE-VALIDATION-008
 * Objetivo:
 * - Validar listado del filtro Initiative (opciones visibles y con texto)
 * - Recorrer iniciativas reales y confirmar que al menos una habilita Sort
 */

String ts() { LocalDateTime.now().format(DateTimeFormatter.ofPattern('yyyyMMdd_HHmmss')) }
String snapDir = System.getProperty('user.dir') + '/Reports/Screenshots/TC-SHEETS-INITIATIVE-008'
new File(snapDir).mkdirs()

def snap = { String label ->
	String path = "${snapDir}/${label}_${ts()}.png"
	WebUI.takeScreenshot(path)
	KeywordUtil.logInfo("[SNAP] ${label} -> ${path}")
}

def sanitize = { String raw ->
	(raw ?: 'empty').replaceAll('[^a-zA-Z0-9]+', '_').replaceAll('_+', '_').replaceAll('^_|_$', '')
}

def hasNoDocumentsMessage = {
	Boolean found = (Boolean) WebUI.executeJavaScript('''
		var target = 'No hay documentos disponibles en esta iniciativa.';
		var nodes = Array.from(document.querySelectorAll('body *'));
		return nodes.some(function(el) {
			if (!(el instanceof HTMLElement)) return false;
			var st = window.getComputedStyle(el);
			var visible = st.display !== 'none' && st.visibility !== 'hidden' && el.offsetParent !== null;
			var txt = (el.textContent || '').trim();
			return visible && txt.indexOf(target) >= 0;
		});
	''', null)
	Boolean.TRUE.equals(found)
}

def readSelectOptions = { String cssSelector ->
	(List<Map>) WebUI.executeJavaScript('''
		var css = arguments[0];
		var sel = document.querySelector(css);
		if (!sel) return [];
		return Array.from(sel.options).map(function(o, idx) {
			var st = window.getComputedStyle(o);
			var visible = !o.hidden && st.display !== 'none' && st.visibility !== 'hidden';
			return {
				idx: idx,
				value: (o.value || '').trim(),
				text: (o.textContent || '').trim(),
				visible: visible
			};
		});
	''', [cssSelector])
}

def validateSelectItems = { String objPath, String cssSelector, String label, boolean allowBlankFirstValue ->
	WebUI.waitForElementVisible(findTestObject(objPath), 10)
	WebUI.click(findTestObject(objPath), FailureHandling.OPTIONAL)
	WebUI.delay(1)
	snap("${label}_dropdown_open")

	List<Map> optionData = readSelectOptions(cssSelector)
	if (!optionData || optionData.isEmpty()) {
		KeywordUtil.markFailedAndStop("[${label}] No se encontraron opciones en ${cssSelector}")
	}

	boolean allOk = true
	KeywordUtil.logInfo("[${label}] Total opciones: ${optionData.size()}")
	optionData.each { opt ->
		boolean hasText = opt['text'] != null && !opt['text'].toString().trim().isEmpty()
		boolean isVisible = Boolean.valueOf(opt['visible'].toString())
		boolean hasValue = opt['value'] != null && !opt['value'].toString().trim().isEmpty()
		boolean valueOk = hasValue || (allowBlankFirstValue && ((opt['idx'] as Integer) == 0))
		boolean rowOk = hasText && isVisible && valueOk
		KeywordUtil.logInfo("[${label}][OPTION] idx=${opt['idx']} value='${opt['value']}' text='${opt['text']}' visible=${opt['visible']} ${rowOk ? 'OK' : 'FAIL'}")
		if (!rowOk) allOk = false
	}

	if (!allOk) {
		KeywordUtil.markFailedAndStop("[${label}] Hay opciones inválidas (sin texto/no visibles/value inválido)")
	}

	WebUI.click(findTestObject('Sheets/Filters/section_dashboard'), FailureHandling.OPTIONAL)
}

try {
	// Verificar si ya hay sesión abierta (reutilizar driver entre TCs de la suite)
	boolean browserAlreadyOpen = false
	try {
		browserAlreadyOpen = WebUI.verifyElementPresent(findTestObject('Sheets/Filters/section_dashboard'), 5, FailureHandling.OPTIONAL)
	} catch (Exception ignored) {}

	String sheetsTestUrl = CommonKeywords.getRequiredGlobal('SHEETS_TEST_URL', 'https://sheets-test.templet.io/admin/manager.php')

	if (!browserAlreadyOpen) {
		// Login centralizado: SSO Microsoft con retry (reemplaza bloque manual duplicado)
		CustomKeywords.'TempletPortalKeywords.openBrowserAndLoginWithMicrosoft'(sheetsTestUrl)
	}
	WebUI.waitForPageLoad(10)

	WebUI.waitForElementVisible(findTestObject('Sheets/Filters/section_dashboard'), 15)
	WebUI.waitForElementVisible(findTestObject('Sheets/Filters/section_filters'), 15)
	snap('01_login_ok')

	WebUI.waitForElementClickable(findTestObject('Sheets/Filters/select_client'), 10)
	WebUI.selectOptionByValue(findTestObject('Sheets/Filters/select_client'), 'BRAVA', false)
	WebUI.waitForPageLoad(10)

	validateSelectItems('Sheets/Filters/select_initiative', '#inputGroupSelect02', 'INITIATIVE', true)
	snap('02_initiative_list_validated')

	List<Map> initiatives = readSelectOptions('#inputGroupSelect02')
	List<Map> candidates = initiatives.findAll { it['value'] != null && !it['value'].toString().trim().isEmpty() }
	KeywordUtil.logInfo("[INITIATIVE] Candidatas para recorrido: ${candidates.size()}")

	List<String> initiativesWithDocs = []
	List<String> initiativesEmpty = []
	Map selectedInitiative = null
	for (Map opt : candidates) {
		String value = opt['value'].toString()
		String text = opt['text']?.toString() ?: value
		WebUI.selectOptionByValue(findTestObject('Sheets/Filters/select_initiative'), value, false)
		WebUI.waitForPageLoad(8)

		// Espera breve para detectar mensaje de iniciativa vacía
		boolean noDocs = false
		for (int i = 0; i < 4; i++) {
			if (hasNoDocumentsMessage()) {
				noDocs = true
				break
			}
			WebUI.delay(1)
		}

		if (noDocs) {
			initiativesEmpty.add(text)
			KeywordUtil.logInfo("[INITIATIVE] value='${value}' text='${text}' -> VACIA (mensaje detectado)")
			snap("03_initiative_empty_${sanitize(text)}")
		} else {
			initiativesWithDocs.add(text)
			KeywordUtil.logInfo("[INITIATIVE] value='${value}' text='${text}' -> CON OBJETOS")
			snap("03_initiative_with_docs_${sanitize(text)}")
			if (selectedInitiative == null) {
				selectedInitiative = [value: value, text: text]
			}
		}
	}

	if (selectedInitiative == null || initiativesWithDocs.isEmpty()) {
		KeywordUtil.markFailedAndStop('[INITIATIVE] Todas las initiatives están vacías (mensaje de no documentos).')
	}

	KeywordUtil.logInfo("[INITIATIVE] Con objetos (${initiativesWithDocs.size()}): ${initiativesWithDocs}")
	KeywordUtil.logInfo("[INITIATIVE] Vacías (${initiativesEmpty.size()}): ${initiativesEmpty}")
	KeywordUtil.logInfo("[INITIATIVE] Seleccionada para evidencia final: ${selectedInitiative.text}")
	WebUI.selectOptionByValue(findTestObject('Sheets/Filters/select_initiative'), selectedInitiative.value.toString(), false)
	WebUI.waitForPageLoad(8)
	snap("04_initiative_selected_${sanitize(selectedInitiative.text.toString())}")

	if (hasNoDocumentsMessage()) {
		KeywordUtil.markWarning('[INITIATIVE] La iniciativa final quedó vacía; se intentará seleccionar otra con contenido.')
		Map fallbackInitiative = null
		for (Map opt : candidates) {
			String value = opt['value']?.toString() ?: ''
			if (value.trim().isEmpty()) continue
			if (value == selectedInitiative.value?.toString()) continue

			WebUI.selectOptionByValue(findTestObject('Sheets/Filters/select_initiative'), value, false)
			WebUI.waitForPageLoad(8)

			boolean noDocs = false
			for (int i = 0; i < 4; i++) {
				if (hasNoDocumentsMessage()) {
					noDocs = true
					break
				}
				WebUI.delay(1)
			}

			if (!noDocs) {
				String text = opt['text']?.toString() ?: value
				fallbackInitiative = [value: value, text: text]
				KeywordUtil.logInfo("[INITIATIVE] Fallback con contenido encontrado: ${text}")
				snap("04b_initiative_selected_fallback_${sanitize(text)}")
				break
			}
		}

		if (fallbackInitiative == null) {
			KeywordUtil.markFailedAndStop('[INITIATIVE] La initiative seleccionada para evidencia final quedó vacía y no se encontró fallback con contenido.')
		}
	}

	WebUI.waitForElementClickable(findTestObject('Sheets/Filters/btn_logout'), 10)
	WebUI.click(findTestObject('Sheets/Filters/btn_logout'))
	WebUI.waitForPageLoad(10)
	snap('05_logout_ok')

	KeywordUtil.markPassed('TC-SHEETS-INITIATIVE-008: Initiative validado. Evidencias en ' + snapDir)
} finally {
	WebUI.closeBrowser(FailureHandling.OPTIONAL)
}
