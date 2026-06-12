import static com.kms.katalon.core.testobject.ObjectRepository.findTestObject
import com.kms.katalon.core.model.FailureHandling as FailureHandling
import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import CommonKeywords

/**
 * TC-EMAIL-FILTERS-INITIATIVE-VALIDATION-008
 * Objetivo:
 * - Validar listado del filtro Initiative (opciones visibles y con texto)
 * - Recorrer iniciativas reales y confirmar que al menos una habilita Sort
 */

String ts() { LocalDateTime.now().format(DateTimeFormatter.ofPattern('yyyyMMdd_HHmmss')) }
String snapDir = System.getProperty('user.dir') + '/Reports/Screenshots/TC-EMAIL-INITIATIVE-008'
new File(snapDir).mkdirs()
String CLIENT_CSS = '#inputGroupSelect01'
String INITIATIVE_CSS = "select[data-toggle='drop-initiatives']"

def snap = { String label ->
	String path = "${snapDir}/${label}_${ts()}.png"
	if (CommonKeywords.captureScreenshotSafe(path)) {
		KeywordUtil.logInfo("[SNAP] ${label} -> ${path}")
	} else {
		KeywordUtil.markWarning("[SNAP] ${label} sin captura (sesión no disponible)")
	}
}

def waitInitiativeReady = { int timeoutSec ->
	boolean ready = CommonKeywords.waitForSelectOptions(INITIATIVE_CSS, timeoutSec, 1)
	if (!ready) {
		KeywordUtil.markFailedAndStop('[INITIATIVE] El select no quedó listo (enabled + opciones) tras seleccionar client')
	}
}

def setSelectValueByCss = { String cssSelector, String value ->
	Boolean ok = (Boolean) WebUI.executeJavaScript('''
		var sel = document.querySelector(arguments[0]);
		var value = arguments[1];
		if (!sel) return false;
		sel.value = value;
		sel.dispatchEvent(new Event('input', { bubbles: true }));
		sel.dispatchEvent(new Event('change', { bubbles: true }));
		return true;
	''', [cssSelector, value])
	return Boolean.TRUE.equals(ok)
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
	CommonKeywords.readSelectOptionsWhenReady(cssSelector, 20, 1)
}

def chooseClient = {
	def initiativeCss = INITIATIVE_CSS
	def clickInitiativeSelect = {
		try {
			WebUI.executeJavaScript('''
				var sel = document.querySelector(arguments[0]);
				if (!sel) return false;
				sel.focus();
				sel.dispatchEvent(new MouseEvent('mousedown', { bubbles: true }));
				sel.dispatchEvent(new MouseEvent('click', { bubbles: true }));
				return true;
			''', [initiativeCss])
			snap('02a_initiative_clicked_after_client')
			return true
		} catch (Exception e) {
			KeywordUtil.markWarning("[CLIENT] No se pudo clickear Initiative tras elegir client: ${e.message}")
			return false
		}
	}
	def initiativeReadyLenient = { int timeoutSec ->
		for (int i = 0; i < timeoutSec; i++) {
			Boolean clickableJs = (Boolean) WebUI.executeJavaScript('''
				var sel = document.querySelector(arguments[0]);
				if (!sel) return false;
				return !sel.disabled;
			''', [initiativeCss])
			boolean clickable = Boolean.TRUE.equals(clickableJs)
			List<Map> options = (List<Map>) WebUI.executeJavaScript('''
				var sel = document.querySelector(arguments[0]);
				if (!sel) return [];
				return Array.from(sel.options || []).map(function(o){
					return { value: (o.value || '').trim(), text: (o.textContent || '').trim() };
				});
			''', [initiativeCss])
			int nonBlank = options.count { Map opt ->
				String value = (opt?.get('value') ?: '').toString().trim()
				return value.length() > 0
			}
			if (clickable || nonBlank >= 1) {
				KeywordUtil.logInfo("[CLIENT] Initiative lista (lenient): clickable=${clickable}, opcionesConValue=${nonBlank}")
				return true
			}
			WebUI.delay(1)
		}
		return false
	}

	for (int attempt = 1; attempt <= 3; attempt++) {
		Map selected = CommonKeywords.selectPreferredOption('Sheets/Filters/select_client', CLIENT_CSS, 'BRAVA', 20)
		try {
			WebUI.executeJavaScript('''
				var css = arguments[0];
				var value = arguments[1];
				var sel = document.querySelector(css);
				if (!sel) return false;
				sel.value = value;
				sel.dispatchEvent(new Event('input', { bubbles: true }));
				sel.dispatchEvent(new Event('change', { bubbles: true }));
				return true;
			''', [CLIENT_CSS, selected.value.toString()])
		} catch (Exception ignored) {
			// Se valida por wait del dependiente
		}

		if (initiativeReadyLenient(25)) {
			KeywordUtil.logInfo("[CLIENT] Initiative lista tras intento ${attempt}/3")
			clickInitiativeSelect()
			return selected
		}

		KeywordUtil.markWarning("[CLIENT] Initiative no lista tras intento ${attempt}/3; reintentando selección de client")
	}

	KeywordUtil.markFailedAndStop('[CLIENT] No fue posible habilitar Initiative tras seleccionar Client en 3 intentos')
	return null
}

def validateSelectItems = { String objPath, String cssSelector, String label, boolean allowBlankFirstValue ->
	if (objPath != null && !objPath.trim().isEmpty()) {
		WebUI.waitForElementVisible(findTestObject(objPath), 10)
		WebUI.click(findTestObject(objPath), FailureHandling.OPTIONAL)
	} else {
		WebUI.executeJavaScript('''
			var sel = document.querySelector(arguments[0]);
			if (!sel) return false;
			sel.focus();
			sel.dispatchEvent(new MouseEvent('mousedown', { bubbles: true }));
			sel.dispatchEvent(new MouseEvent('click', { bubbles: true }));
			return true;
		''', [cssSelector])
	}
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

	String emailTestUrl = CommonKeywords.getRequiredGlobal('EMAIL_TEST_URL', 'https://emails-test.templet.io/admin/manager.php')

	if (!browserAlreadyOpen) {
		// Login centralizado: SSO Microsoft con retry (reemplaza bloque manual duplicado)
		CustomKeywords.'TempletPortalKeywords.openBrowserAndLoginWithMicrosoft'(emailTestUrl)
	}
	WebUI.waitForPageLoad(10)

	WebUI.waitForElementVisible(findTestObject('Sheets/Filters/section_dashboard'), 15)
	WebUI.waitForElementVisible(findTestObject('Sheets/Filters/section_filters'), 15)
	snap('01_login_ok')

	WebUI.waitForElementClickable(findTestObject('Sheets/Filters/select_client'), 10)
	Map selectedClient = chooseClient()

	validateSelectItems(null, INITIATIVE_CSS, 'INITIATIVE', true)
	snap("02_initiative_list_validated_client_${sanitize(selectedClient.text.toString())}")

	List<Map> initiatives = readSelectOptions(INITIATIVE_CSS)
	List<Map> candidates = initiatives.findAll { it['value'] != null && !it['value'].toString().trim().isEmpty() }
	KeywordUtil.logInfo("[INITIATIVE] Candidatas para recorrido: ${candidates.size()}")

	List<String> initiativesWithDocs = []
	List<String> initiativesEmpty = []
	Map selectedInitiative = null
	for (Map opt : candidates) {
		String value = opt['value'].toString()
		String text = opt['text']?.toString() ?: value
		if (!setSelectValueByCss(INITIATIVE_CSS, value)) {
			KeywordUtil.markWarning("[INITIATIVE] No se pudo seleccionar value='${value}' en ${INITIATIVE_CSS}")
			continue
		}
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
	setSelectValueByCss(INITIATIVE_CSS, selectedInitiative.value.toString())
	WebUI.waitForPageLoad(8)
	snap("04_initiative_selected_${sanitize(selectedInitiative.text.toString())}")

	if (hasNoDocumentsMessage()) {
		KeywordUtil.markFailedAndStop('[INITIATIVE] La initiative seleccionada para evidencia final quedó vacía.')
	}

	WebUI.waitForElementClickable(findTestObject('Sheets/Filters/btn_logout'), 10)
	WebUI.click(findTestObject('Sheets/Filters/btn_logout'))
	WebUI.waitForPageLoad(10)
	snap('05_logout_ok')

	KeywordUtil.markPassed('TC-EMAIL-INITIATIVE-008: Initiative validado. Evidencias en ' + snapDir)
} finally {
	WebUI.closeBrowser(FailureHandling.OPTIONAL)
}
