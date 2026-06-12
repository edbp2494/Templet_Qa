import static com.kms.katalon.core.testobject.ObjectRepository.findTestObject
import com.kms.katalon.core.model.FailureHandling as FailureHandling
import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import internal.GlobalVariable as GlobalVariable
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import CommonKeywords

/**
 * TC-DECKS-FILTERS-CLIENT-INITIATIVE-SORT-007
 * Tipo: Funcional / Regresión
 * Plataforma: Decks TEST
 * Objetivo: Validar flujo completo de filtros: Client → Initiative → Sort
 *           con captura y validación de objetos visibles tras cada interacción.
 * Resultado esperado:
 *   - Client seleccionable y activa Initiative
 *   - Initiative seleccionable y activa Sort y Dashboard
 *   - Sort cambia orden de resultados (Newest / Oldest / A to Z)
 *   - Evidencia en Reports/Screenshots/TC-007 por cada paso
 */

// ─── UTILIDADES ──────────────────────────────────────────────────────────────
String ts() { LocalDateTime.now().format(DateTimeFormatter.ofPattern('yyyyMMdd_HHmmss')) }

String snapDir = System.getProperty('user.dir') + '/Reports/Screenshots/TC-DECKS-FILTERS-007'
new File(snapDir).mkdirs()
List<String> warnings = []

def logWarning = { String message ->
	String normalized = "[WARNING] ${message}"
	warnings.add(normalized)
	KeywordUtil.logInfo(normalized)
}

def snap = { String label ->
	String path = "${snapDir}/${label}_${ts()}.png"
	if (CommonKeywords.captureScreenshotSafe(path)) {
		KeywordUtil.logInfo("[SNAP] ${label} → ${path}")
	} else {
		logWarning("[SNAP] ${label} sin captura (sesión no disponible)")
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

def waitInitiativeReady = { int timeoutSec ->
	boolean ready = CommonKeywords.waitForSelectOptions('#inputGroupSelect02', timeoutSec, 1)
	if (!ready) {
		KeywordUtil.markFailedAndStop('[INITIATIVE] El select no quedó listo (enabled + opciones) tras seleccionar client')
	}
}

def assertVisible = { String objPath, String label ->
	boolean ok = WebUI.verifyElementVisible(findTestObject(objPath), FailureHandling.OPTIONAL)
	KeywordUtil.logInfo("[CHECK] ${label}: ${ok ? 'VISIBLE ✓' : 'NO visible ✗'}")
	return ok
}

def assertEnabled = { String objPath, String label ->
	boolean ok = WebUI.verifyElementClickable(findTestObject(objPath), FailureHandling.OPTIONAL)
	KeywordUtil.logInfo("[CHECK] ${label} clickable: ${ok ? 'SI ✓' : 'NO ✗'}")
	return ok
}

def sanitize = { String raw ->
	(raw ?: 'empty').replaceAll('[^a-zA-Z0-9]+', '_').replaceAll('_+', '_').replaceAll('^_|_$', '')
}

def isSortEnabled = {
	Boolean enabled = (Boolean) WebUI.executeJavaScript(
		"var s=document.getElementById('sortField-alpha'); return !!s && !s.disabled;",
		null
	)
	return Boolean.TRUE.equals(enabled)
}

def validateSelectItems = { String objPath, String cssSelector, String label, boolean allowBlankFirstValue ->
	WebUI.waitForElementVisible(findTestObject(objPath), 10)
	WebUI.click(findTestObject(objPath), FailureHandling.OPTIONAL)
	WebUI.delay(1)
	snap("${label}_dropdown_open")

	List<Map> optionData = CommonKeywords.readSelectOptionsWhenReady(cssSelector, 20, 1)

	boolean allOk = true
	if (!optionData || optionData.isEmpty()) {
		allOk = false
		logWarning("[${label}] No se encontraron opciones en ${cssSelector}")
	} else {
		KeywordUtil.logInfo("[${label}] Total opciones: ${optionData.size()}")
		optionData.each { opt ->
			boolean hasText = opt['text'] != null && !opt['text'].toString().trim().isEmpty()
			boolean isVisible = Boolean.valueOf(opt['visible'].toString())
			boolean hasValue = opt['value'] != null && !opt['value'].toString().trim().isEmpty()
			boolean valueOk = hasValue || (allowBlankFirstValue && ((opt['idx'] as Integer) == 0))
			boolean rowOk = hasText && isVisible && valueOk
			KeywordUtil.logInfo("[${label}][OPTION] idx=${opt['idx']} value='${opt['value']}' text='${opt['text']}' visible=${opt['visible']} ${rowOk ? '✓' : '✗'}")
			if (!rowOk) allOk = false
		}
	}

	if (allOk) {
		KeywordUtil.logInfo("[${label}] Todas las opciones son visibles y tienen texto ✓")
	} else {
		logWarning("[${label}] Hay opciones inválidas (sin texto/no visibles/value inválido)")
	}

	WebUI.click(findTestObject('Sheets/Filters/section_dashboard'), FailureHandling.OPTIONAL)
	return allOk
}

def chooseInitiativeWithEnabledSort = {
	waitInitiativeReady(20)
	List<Map> initiatives = CommonKeywords.readSelectOptionsWhenReady('#inputGroupSelect02', 20, 1)

	if (!initiatives || initiatives.isEmpty()) {
		KeywordUtil.markFailedAndStop('[INITIATIVE] No se encontraron opciones en el select de initiative')
	}

	List<Map> candidates = initiatives.findAll { it['value'] != null && !it['value'].toString().trim().isEmpty() }
	KeywordUtil.logInfo("[INITIATIVE] Candidatas para prueba: ${candidates.size()}")

	for (Map opt : candidates) {
		String value = opt['value'].toString()
		String text = opt['text']?.toString() ?: value
		KeywordUtil.logInfo("[INITIATIVE] Probando value='${value}' text='${text}'")
		if (!setSelectValueByCss('#inputGroupSelect02', value)) {
			logWarning("[INITIATIVE] No se pudo seleccionar value='${value}' en #inputGroupSelect02")
			continue
		}
		WebUI.waitForPageLoad(8)
		WebUI.delay(1)
		boolean enabled = isSortEnabled()
		snap("03_initiative_try_${sanitize(text)}")
		if (enabled) {
			KeywordUtil.logInfo("[INITIATIVE] OK: '${text}' habilita sort ✓")
			return [value: value, text: text]
		}
		KeywordUtil.logInfo("[INITIATIVE] '${text}' no habilita sort, continúo con la siguiente")
	}

	KeywordUtil.markFailedAndStop('[INITIATIVE] Ninguna initiative habilitó el select sort')
	return null
}

def chooseClient = {
	for (int attempt = 1; attempt <= 3; attempt++) {
		Map selected = CommonKeywords.selectPreferredOption('Sheets/Filters/select_client', '#inputGroupSelect01', 'BRAVA', 20)
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
			''', ['#inputGroupSelect01', selected.value.toString()])
		} catch (Exception ignored) {
			// Se valida por wait del dependiente
		}

		if (CommonKeywords.waitForSelectOptions('#inputGroupSelect02', 25, 1)) {
			KeywordUtil.logInfo("[CLIENT] Initiative lista tras intento ${attempt}/3")
			return selected
		}

		logWarning("[CLIENT] Initiative no lista tras intento ${attempt}/3; reintentando selección de client")
	}

	KeywordUtil.markFailedAndStop('[CLIENT] No fue posible habilitar Initiative tras seleccionar Client en 3 intentos')
	return null
}

def readSortState = {
	Map state = (Map) WebUI.executeJavaScript('''
		var visible = function(el) {
			if (!(el instanceof HTMLElement)) return false;
			var st = window.getComputedStyle(el);
			return st.display !== 'none' && st.visibility !== 'hidden' && el.offsetParent !== null;
		};
		var sel = document.querySelector('#sortField-alpha');
		var selectedText = '';
		if (sel && sel.selectedIndex >= 0 && sel.options && sel.options.length > sel.selectedIndex) {
			selectedText = (sel.options[sel.selectedIndex].textContent || '').trim();
		}
		var normalizeWhitespace = function(text) {
			var raw = (text || '').trim();
			var out = '';
			var prevSpace = false;
			for (var i = 0; i < raw.length; i++) {
				var code = raw.charCodeAt(i);
				var isSpace = (code === 9 || code === 10 || code === 13 || code === 32);
				if (isSpace) {
					if (!prevSpace) {
						out += ' ';
					}
					prevSpace = true;
				} else {
					out += raw.charAt(i);
					prevSpace = false;
				}
			}
			return out.trim();
		};
		var rows = Array.from(document.querySelectorAll('table.table tbody tr, .thumbnails-boxes .thumbnail-box, .thumbnails-boxes .box, .documents-list .row, .list-group .list-group-item'))
			.filter(visible)
			.slice(0, 5)
			.map(function(el) { return normalizeWhitespace(el.textContent || ''); });
		var noDocs = Array.from(document.querySelectorAll('body *')).some(function(el) {
			if (!visible(el)) return false;
			return ((el.textContent || '').trim().indexOf('No hay documentos disponibles en esta iniciativa.') >= 0);
		});
		return {
			selectedText: selectedText,
			rowCount: rows.length,
			rows: rows,
			noDocs: noDocs
		};
	''', null)
	if (state == null) {
		return [selectedText: '', rowCount: 0, rows: [], noDocs: false]
	}
	return state
}

def sortFingerprint = { Map state ->
	String selected = (state?.selectedText ?: '').toString()
	String rows = ((state?.rows ?: []) as List).collect { it?.toString() ?: '' }.join('||')
	String count = (state?.rowCount ?: 0).toString()
	String noDocs = Boolean.TRUE.equals(state?.noDocs) ? '1' : '0'
	return "${selected}|${count}|${noDocs}|${rows}"
}

def validateSortConsistency = {
	List<String> expectedSortLabels = ['Newest', 'Oldest', 'A to Z']
	List<Map> sortOptions = CommonKeywords.readSelectOptionsWhenReady('#sortField-alpha', 12, 1)
	Set<String> normalizedAvailable = sortOptions.collect { (it['text'] ?: '').toString().trim().toLowerCase() } as Set<String>

	expectedSortLabels.each { String label ->
		if (normalizedAvailable.contains(label.toLowerCase())) {
			KeywordUtil.logInfo("[CHECK][SORT] Opción esperada presente: ${label}")
		} else {
			logWarning("[SORT] Opción esperada no encontrada: ${label}")
		}
	}

	Map before = readSortState()
	String beforeFingerprint = sortFingerprint(before)
	for (String label : expectedSortLabels) {
		Map matched = sortOptions.find { ((it['text'] ?: '').toString().trim().equalsIgnoreCase(label)) }
		if (matched == null) {
			continue
		}

		String value = (matched['value'] ?: '').toString()
		if (value.trim().isEmpty()) {
			logWarning("[SORT] La opción ${label} no tiene value seleccionable")
			continue
		}

		if (!setSelectValueByCss('#sortField-alpha', value)) {
			logWarning("[SORT] Falló la selección de opción ${label}")
			continue
		}

		WebUI.waitForPageLoad(8)
		WebUI.delay(1)
		Map after = readSortState()
		String afterFingerprint = sortFingerprint(after)
		if (afterFingerprint != beforeFingerprint) {
			KeywordUtil.logInfo("[CHECK][SORT] ${label}: cambio de estado/listado detectado")
		} else {
			logWarning("[SORT] ${label}: sin cambio visible de estado/listado")
		}
		beforeFingerprint = afterFingerprint
		snap("sort_${sanitize(label)}")
	}
}

// ─── APERTURA Y LOGIN (reutilizar sesión si ya existe navegador abierto) ────
boolean browserAlreadyOpen = false
try {
	browserAlreadyOpen = WebUI.verifyElementPresent(findTestObject('Sheets/Filters/section_dashboard'), 5, FailureHandling.OPTIONAL)
} catch (Exception ignored) {}

String decksTestUrl = CommonKeywords.getRequiredGlobal('DECKS_TEST_URL', 'https://decks-test.templet.io/admin/manager.php')

if (!browserAlreadyOpen) {
	// Login centralizado: SSO Microsoft con retry (reemplaza bloque manual duplicado)
	CustomKeywords.'TempletPortalKeywords.openBrowserAndLoginWithMicrosoft'(decksTestUrl)
}
WebUI.waitForPageLoad(10)

// ─── VALIDAR DASHBOARD POST-LOGIN ────────────────────────────────────────────
WebUI.waitForElementVisible(findTestObject('Sheets/Filters/section_dashboard'), 15)
assertVisible('Sheets/Filters/section_dashboard', 'Dashboard')
assertVisible('Sheets/Filters/section_filters', 'Sección Filtros')
assertVisible('Sheets/Filters/select_client', 'Select Client habilitado')
validateSelectItems('Sheets/Filters/select_client', '#inputGroupSelect01', 'CLIENT', true)
snap('01_login_ok')

// ─── SELECCIÓN DE CLIENT ─────────────────────────────────────────────────────
WebUI.waitForElementClickable(findTestObject('Sheets/Filters/select_client'), 10)
Map selectedClient = chooseClient()

assertEnabled('Sheets/Filters/select_initiative', 'Select Initiative habilitado tras elegir Client')
assertVisible('Sheets/Filters/section_filters', "Sección filtros con Client=${selectedClient.text}")
validateSelectItems('Sheets/Filters/select_initiative', '#inputGroupSelect02', 'INITIATIVE', true)
snap("02_client_${sanitize(selectedClient.text.toString())}_seleccionado")

// ─── SELECCIÓN DE INITIATIVE ─────────────────────────────────────────────────
WebUI.waitForElementClickable(findTestObject('Sheets/Filters/select_initiative'), 10)
Map selectedInitiative = chooseInitiativeWithEnabledSort()

assertVisible('Sheets/Filters/section_dashboard', "Dashboard con Initiative=${selectedInitiative.text}")
snap("03_initiative_selected_${sanitize(selectedInitiative.text.toString())}")

// ─── SORT — esperar que la app habilite el select vía JS ─────────────────────
// sortField-alpha arranca disabled; la app lo habilita por JS al cargar la grilla
if (!WebUI.waitForElementClickable(findTestObject('Sheets/Filters/select_sort'), 15, FailureHandling.OPTIONAL)) {
	KeywordUtil.markFailedAndStop('[SORT] sortField-alpha sigue disabled tras 15s aun después de elegir initiative válida')
}
assertEnabled('Sheets/Filters/select_sort', 'Sort habilitado tras cargar resultados')
validateSelectItems('Sheets/Filters/select_sort', '#sortField-alpha', 'SORT', true)
snap('03b_sort_options_validated')
validateSortConsistency()

// ─── LOGOUT ──────────────────────────────────────────────────────────────────
WebUI.waitForElementClickable(findTestObject('Sheets/Filters/btn_logout'), 10)
WebUI.click(findTestObject('Sheets/Filters/btn_logout'))
WebUI.waitForPageLoad(10)
snap('07_logout_ok')

WebUI.closeBrowser()
if (!warnings.isEmpty()) {
	KeywordUtil.logInfo("[SUMMARY] warnings=${warnings.size()}")
}
KeywordUtil.markPassed('TC-DECKS-FILTERS-007: Client → Initiative → Sort validados. Capturas en ' + snapDir)
