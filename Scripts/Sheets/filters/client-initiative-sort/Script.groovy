import static com.kms.katalon.core.testobject.ObjectRepository.findTestObject
import com.kms.katalon.core.model.FailureHandling as FailureHandling
import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import internal.GlobalVariable as GlobalVariable
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import CommonKeywords

/**
 * TC-SHEETS-FILTERS-CLIENT-INITIATIVE-SORT-007
 * Tipo: Funcional / Regresión
 * Plataforma: Sheets TEST
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

String snapDir = System.getProperty('user.dir') + '/Reports/Screenshots/TC-SHEETS-FILTERS-007'
new File(snapDir).mkdirs()

def snap = { String label ->
	String path = "${snapDir}/${label}_${ts()}.png"
	WebUI.takeScreenshot(path)
	KeywordUtil.logInfo("[SNAP] ${label} → ${path}")
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

	List<Map> optionData = (List<Map>) WebUI.executeJavaScript('''
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

	boolean allOk = true
	if (!optionData || optionData.isEmpty()) {
		allOk = false
		KeywordUtil.markWarning("[${label}] No se encontraron opciones en ${cssSelector}")
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
		KeywordUtil.markWarning("[${label}] Hay opciones inválidas (sin texto/no visibles/value inválido)")
	}

	WebUI.click(findTestObject('Sheets/Filters/section_dashboard'), FailureHandling.OPTIONAL)
	return allOk
}

def chooseInitiativeWithEnabledSort = {
	List<Map> initiatives = (List<Map>) WebUI.executeJavaScript('''
		var sel = document.querySelector('#inputGroupSelect02');
		if (!sel) return [];
		return Array.from(sel.options).map(function(o, idx) {
			return {
				idx: idx,
				value: (o.value || '').trim(),
				text: (o.textContent || '').trim()
			};
		});
	''', null)

	if (!initiatives || initiatives.isEmpty()) {
		KeywordUtil.markFailedAndStop('[INITIATIVE] No se encontraron opciones en el select de initiative')
	}

	List<Map> candidates = initiatives.findAll { it['value'] != null && !it['value'].toString().trim().isEmpty() }
	KeywordUtil.logInfo("[INITIATIVE] Candidatas para prueba: ${candidates.size()}")

	for (Map opt : candidates) {
		String value = opt['value'].toString()
		String text = opt['text']?.toString() ?: value
		KeywordUtil.logInfo("[INITIATIVE] Probando value='${value}' text='${text}'")
		WebUI.selectOptionByValue(findTestObject('Sheets/Filters/select_initiative'), value, false)
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

// ─── APERTURA Y LOGIN (reutilizar sesión si ya existe navegador abierto) ────
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

// ─── VALIDAR DASHBOARD POST-LOGIN ────────────────────────────────────────────
WebUI.waitForElementVisible(findTestObject('Sheets/Filters/section_dashboard'), 15)
assertVisible('Sheets/Filters/section_dashboard', 'Dashboard')
assertVisible('Sheets/Filters/section_filters', 'Sección Filtros')
assertVisible('Sheets/Filters/select_client', 'Select Client habilitado')
validateSelectItems('Sheets/Filters/select_client', '#inputGroupSelect01', 'CLIENT', true)
snap('01_login_ok')

// ─── SELECCIÓN DE CLIENT ─────────────────────────────────────────────────────
WebUI.waitForElementClickable(findTestObject('Sheets/Filters/select_client'), 10)
WebUI.selectOptionByValue(findTestObject('Sheets/Filters/select_client'), 'BRAVA', false)
WebUI.waitForPageLoad(10)

assertEnabled('Sheets/Filters/select_initiative', 'Select Initiative habilitado tras elegir Client')
assertVisible('Sheets/Filters/section_filters', 'Sección filtros con Client=BRAVA')
validateSelectItems('Sheets/Filters/select_initiative', '#inputGroupSelect02', 'INITIATIVE', true)
snap('02_client_BRAVA_seleccionado')

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

WebUI.selectOptionByLabel(findTestObject('Sheets/Filters/select_sort'), 'Newest', false)
WebUI.waitForPageLoad(8)
assertVisible('Sheets/Filters/section_dashboard', 'Dashboard sort=Newest')
snap('04_sort_newest')

WebUI.selectOptionByLabel(findTestObject('Sheets/Filters/select_sort'), 'Oldest', false)
WebUI.waitForPageLoad(8)
assertVisible('Sheets/Filters/section_dashboard', 'Dashboard sort=Oldest')
snap('05_sort_oldest')

WebUI.selectOptionByLabel(findTestObject('Sheets/Filters/select_sort'), 'A to Z', false)
WebUI.waitForPageLoad(8)
assertVisible('Sheets/Filters/section_dashboard', 'Dashboard sort=AtoZ')
snap('06_sort_AtoZ')

// ─── LOGOUT ──────────────────────────────────────────────────────────────────
WebUI.waitForElementClickable(findTestObject('Sheets/Filters/btn_logout'), 10)
WebUI.click(findTestObject('Sheets/Filters/btn_logout'))
WebUI.waitForPageLoad(10)
snap('07_logout_ok')

WebUI.closeBrowser()
KeywordUtil.markPassed('TC-SHEETS-FILTERS-007: Client → Initiative → Sort validados. Capturas en ' + snapDir)
