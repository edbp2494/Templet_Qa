import static com.kms.katalon.core.testobject.ObjectRepository.findTestObject
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import com.kms.katalon.core.annotation.Keyword
import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.model.FailureHandling
import org.openqa.selenium.WebElement

public class SheetsKeywords {

	// ─── Constantes de Object Repository ─────────────────────────────────────────
	private static final String OBJ_SELECT_CLIENT     = 'Object Repository/Sheets/select_client_initiative'
	private static final String OBJ_SELECT_SORT       = 'Object Repository/Sheets/select_sort_field'
	private static final String OBJ_BTN_BACK          = 'Object Repository/Sheets/btn_back'
	private static final String OBJ_CONTAINER_TABS    = 'Object Repository/Sheets/container_tabs'
	private static final String OBJ_NAVBAR            = 'Object Repository/Sheets/navbar_main'
	private static final String OBJ_ICON_LIST         = 'Object Repository/Sheets/icon_list_view'

	/**
	 * Scroll al top para garantizar capturas consistentes
	 */
	@Keyword
	def static scrollToTop() {
		WebUI.executeJavaScript('window.scrollTo(0, 0);', null)
		WebUI.delay(1)
		KeywordUtil.logInfo('[SCROLL] Top OK')
	}

	// ─── Selectores de Cliente/Iniciativa ────────────────────────────────────────

	/**
	 * Selecciona una opción del dropdown Cliente/Iniciativa por texto visible
	 * @param optionLabel  Texto visible de la opción (ej: "BRAVA INITIATIVES Why Brava")
	 */
	@Keyword
	def static selectClientInitiative(String optionLabel) {
		WebUI.waitForElementVisible(findTestObject(OBJ_SELECT_CLIENT), 10)
		WebUI.selectOptionByLabel(findTestObject(OBJ_SELECT_CLIENT), optionLabel, false)
		WebUI.delay(1)
		KeywordUtil.logInfo("[SHEETS] Seleccionado cliente/iniciativa: ${optionLabel}")
	}

	/**
	 * Obtiene todas las opciones disponibles en el dropdown Cliente/Iniciativa
	 * @return Lista de textos de opciones
	 */
	@Keyword
	def static List<String> getClientInitiativeOptions() {
		WebUI.waitForElementVisible(findTestObject(OBJ_SELECT_CLIENT), 10)
		List<WebElement> options = WebUI.findWebElements(findTestObject(OBJ_SELECT_CLIENT), 5)
		List<String> labels = []
		String js = '''
			var select = document.getElementById('inputGroupSelect02');
			var opts = [];
			for (var i = 0; i < select.options.length; i++) {
				opts.push(select.options[i].text);
			}
			return opts;
		'''
		labels = WebUI.executeJavaScript(js, null)
		KeywordUtil.logInfo("[SHEETS] Opciones disponibles: ${labels.size()}")
		return labels
	}

	// ─── Ordenamiento ────────────────────────────────────────────────────────────

	/**
	 * Cambia el ordenamiento del listado
	 * @param sortOption  "Newest", "Oldest", "A to Z", "Z to A"
	 */
	@Keyword
	def static selectSortOrder(String sortOption) {
		WebUI.waitForElementVisible(findTestObject(OBJ_SELECT_SORT), 10)
		WebUI.selectOptionByLabel(findTestObject(OBJ_SELECT_SORT), sortOption, false)
		WebUI.delay(1)
		KeywordUtil.logInfo("[SHEETS] Ordenamiento cambiado a: ${sortOption}")
	}

	/**
	 * Verifica el ordenamiento actual seleccionado
	 * @return Texto del ordenamiento actual
	 */
	@Keyword
	def static String getCurrentSortOrder() {
		WebUI.waitForElementVisible(findTestObject(OBJ_SELECT_SORT), 10)
		String js = "return document.getElementById('sortField-alpha').options[document.getElementById('sortField-alpha').selectedIndex].text;"
		String current = WebUI.executeJavaScript(js, null)
		KeywordUtil.logInfo("[SHEETS] Ordenamiento actual: ${current}")
		return current
	}

	// ─── Navegación ──────────────────────────────────────────────────────────────

	/**
	 * Click en botón Back para volver al listado
	 */
	@Keyword
	def static clickBack() {
		if (WebUI.verifyElementPresent(findTestObject(OBJ_BTN_BACK), 5, FailureHandling.OPTIONAL)) {
			WebUI.click(findTestObject(OBJ_BTN_BACK))
			WebUI.waitForPageLoad(10)
			KeywordUtil.logInfo("[SHEETS] Click en Back")
			return true
		}
		KeywordUtil.logInfo("[SHEETS] Botón Back no visible")
		return false
	}

	/**
	 * Cambia a vista de lista
	 */
	@Keyword
	def static switchToListView() {
		if (WebUI.verifyElementPresent(findTestObject(OBJ_ICON_LIST), 5, FailureHandling.OPTIONAL)) {
			WebUI.click(findTestObject(OBJ_ICON_LIST))
			WebUI.delay(1)
			KeywordUtil.logInfo("[SHEETS] Cambiado a vista lista")
			return true
		}
		return false
	}

	// ─── Validaciones de estructura ──────────────────────────────────────────────

	/**
	 * Verifica que los elementos principales de Sheets están presentes
	 * @return Map con estado de cada elemento
	 */
	@Keyword
	def static Map<String, Boolean> verifyMainElementsPresent() {
		Map<String, Boolean> results = [:]
		
		results['navbar'] = WebUI.verifyElementPresent(findTestObject(OBJ_NAVBAR), 5, FailureHandling.OPTIONAL)
		results['select_client'] = WebUI.verifyElementPresent(findTestObject(OBJ_SELECT_CLIENT), 5, FailureHandling.OPTIONAL)
		results['select_sort'] = WebUI.verifyElementPresent(findTestObject(OBJ_SELECT_SORT), 5, FailureHandling.OPTIONAL)
		results['container_tabs'] = WebUI.verifyElementPresent(findTestObject(OBJ_CONTAINER_TABS), 5, FailureHandling.OPTIONAL)
		
		int present = results.values().count { it }
		KeywordUtil.logInfo("[SHEETS] Elementos principales: ${present}/${results.size()} presentes")
		
		return results
	}

	/**
	 * Navega directamente a una iniciativa usando el patrón de URL
	 * @param initiativeSlug  El slug de la iniciativa (ej: "BRAVA---INITIATIVES---Why-Brava")
	 * @param baseUrl         URL base (default: sheets-test.templet.io)
	 */
	@Keyword
	def static navigateToInitiative(String initiativeSlug, String baseUrl = 'https://sheets-test.templet.io') {
		String fullUrl = "${baseUrl}/admin/?BRAVA_${initiativeSlug}"
		WebUI.navigateToUrl(fullUrl)
		WebUI.waitForPageLoad(15)
		KeywordUtil.logInfo("[SHEETS] Navegado a iniciativa: ${fullUrl}")
	}

	/**
	 * Verifica presencia y visibilidad de los 3 filtros principales (fecha, estado, usuario)
	 * Retorna true si todos están presentes
	 */
	@Keyword
	def static boolean verifyFiltersPresent() {
		List<String> filters = [
			'Object Repository/Sheets/filter_date_filter',
			'Object Repository/Sheets/filter_status_filter',
			'Object Repository/Sheets/filter_user_filter'
		]
		boolean allPresent = true
		filters.each { path ->
			try {
				WebUI.waitForElementVisible(findTestObject(path), 8)
				KeywordUtil.logInfo("[FILTER] Presente: ${path}")
			} catch (Exception e) {
				KeywordUtil.logInfo("[FILTER] NO encontrado: ${path}")
				allPresent = false
			}
		}
		return allPresent
	}

	/**
	 * Verifica la estructura de la tabla: headers en orden y al menos minRows filas
	 * @param expectedHeaders  Lista de textos esperados en los <th>
	 * @param minRows          Número mínimo de filas de datos
	 */
	@Keyword
	def static boolean verifyTableStructure(List<String> expectedHeaders, int minRows = 1) {
		try {
			// Esperar tabla
			WebUI.waitForElementVisible(findTestObject('Object Repository/Sheets/table_main'), 10)

			// Obtener headers reales
			List<WebElement> thElements = WebUI.findWebElements(
				findTestObject('Object Repository/Sheets/table_headers'), 10)

			if (thElements.size() != expectedHeaders.size()) {
				KeywordUtil.logInfo("[TABLE] Columnas: esperado ${expectedHeaders.size()}, obtenido ${thElements.size()}")
				return false
			}
			for (int i = 0; i < expectedHeaders.size(); i++) {
				String actual = thElements[i].getText().trim()
				if (!actual.equalsIgnoreCase(expectedHeaders[i])) {
					KeywordUtil.logInfo("[TABLE] Columna ${i}: esperado '${expectedHeaders[i]}', obtenido '${actual}'")
					return false
				}
			}

			// Contar filas
			List<WebElement> rows = WebUI.findWebElements(
				findTestObject('Object Repository/Sheets/table_data_rows'), 10)
			if (rows.size() < minRows) {
				KeywordUtil.logInfo("[TABLE] Filas insuficientes: ${rows.size()} < ${minRows}")
				return false
			}
			KeywordUtil.logInfo("[TABLE] OK: ${thElements.size()} columnas, ${rows.size()} filas")
			return true
		} catch (Exception e) {
			KeywordUtil.logInfo("[TABLE] Error: ${e.message}")
			return false
		}
	}
}
