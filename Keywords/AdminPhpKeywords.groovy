import static com.kms.katalon.core.testobject.ObjectRepository.findTestObject
import com.kms.katalon.core.annotation.Keyword
import com.kms.katalon.core.model.FailureHandling as FailureHandling
import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.driver.DriverFactory
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * AdminPhpKeywords — Flujos compartidos de las plataformas admin.php (Sheets, Decks, Email).
 *
 * Consolida la lógica que antes estaba duplicada en 15 scripts (~3.400 líneas):
 *   - runFunctionalSmoke          → functional-smoke (Sheets/Decks/Email)
 *   - runClientInitiativeSortFlow → filters/client-initiative-sort
 *   - validateInitiativeContent   → filters/initiative-content-validation
 *   - validateSortGridList        → validation/sort-grid-list-validation
 *   - validateListActionsModal    → validation/list-actions-modal-response
 *
 * Cada keyword recibe un Map config con los datos específicos de la plataforma
 * (caseId, URL, prefijo de Object Repository, selectores CSS). Los objetos OR son
 * opcionales: si la plataforma no los tiene mapeados se usa el selector CSS.
 */
class AdminPhpKeywords {

	// ─── Helpers JS compartidos (privados, sin @Keyword) ─────────────────────

	/** Marca de tiempo para nombres de screenshot. */
	private static String ts() {
		LocalDateTime.now().format(DateTimeFormatter.ofPattern('yyyyMMdd_HHmmss'))
	}

	/** Limpia un texto para usarlo como nombre de archivo. */
	private static String sanitize(String raw) {
		(raw ?: 'empty').replaceAll('[^a-zA-Z0-9]+', '_').replaceAll('_+', '_').replaceAll('^_|_$', '')
	}

	/** Screenshot con etiqueta en el directorio del caso. */
	private static void snap(String snapDir, String label) {
		try {
			String path = "${snapDir}/${label}_${ts()}.png"
			WebUI.takeScreenshot(path)
			KeywordUtil.logInfo("[SNAP] ${label} → ${path}")
		} catch (Exception ignored) {}
	}

	/** Setea el value de un select vía JS disparando input+change (como lo hace la app). */
	private static boolean setSelectValueByCss(String cssSelector, String value) {
		Boolean ok = (Boolean) WebUI.executeJavaScript("""
			var sel = document.querySelector(arguments[0]);
			if (!sel) return false;
			sel.value = arguments[1];
			sel.dispatchEvent(new Event('input', { bubbles: true }));
			sel.dispatchEvent(new Event('change', { bubbles: true }));
			return true;
		""", [cssSelector, value])
		return Boolean.TRUE.equals(ok)
	}

	/** Lee las opciones de un select: [idx, value, text, visible]. */
	private static List<Map> readSelectOptions(String cssSelector) {
		List<Map> options = (List<Map>) WebUI.executeJavaScript('''
			var css = arguments[0];
			var sel = document.querySelector(css);
			if (!sel) return [];
			return Array.from(sel.options).map(function(o, idx) {
				var st = window.getComputedStyle(o);
				var visible = !o.hidden && st.display !== 'none' && st.visibility !== 'hidden';
				return { idx: idx, value: (o.value || '').trim(), text: (o.textContent || '').trim(), visible: visible };
			});
		''', [cssSelector])
		return options ?: []
	}

	/** Verifica si un select está habilitado. */
	private static boolean isSelectEnabled(String cssSelector) {
		Boolean enabled = (Boolean) WebUI.executeJavaScript(
			'var sel = document.querySelector(arguments[0]); return !!sel && !sel.disabled;', [cssSelector])
		return Boolean.TRUE.equals(enabled)
	}

	/** Cuenta tarjetas/documentos visibles según los selectores de la plataforma. */
	private static int countVisibleCards(String cardSelectors) {
		Integer count = (Integer) WebUI.executeJavaScript("""
			return Array.from(document.querySelectorAll(arguments[0])).filter(function(el) {
				var st = window.getComputedStyle(el);
				return st.display !== 'none' && st.visibility !== 'hidden';
			}).length;
		""", [cardSelectors])
		return count ?: 0
	}

	/** Detecta si hay un mensaje visible de "sin contenido" (cualquiera de la lista). */
	private static boolean hasEmptyMessage(List<String> messages) {
		Boolean found = (Boolean) WebUI.executeJavaScript("""
			var msgs = arguments[0];
			return Array.from(document.querySelectorAll('body *')).some(function(el) {
				if (!(el instanceof HTMLElement)) return false;
				var st = window.getComputedStyle(el);
				if (st.display === 'none' || st.visibility === 'hidden') return false;
				var txt = el.textContent.trim();
				return msgs.some(function(m) { return txt.indexOf(m) >= 0; });
			});
		""", [messages])
		return Boolean.TRUE.equals(found)
	}

	/** Lee títulos visibles en orden visual (por coordenadas, no por orden DOM). */
	private static List<String> getVisibleTitles(String cardSelectors, String titleSelectors) {
		List titles = (List) WebUI.executeJavaScript("""
			var cards = Array.from(document.querySelectorAll(arguments[0])).filter(function(card) {
				if (!(card instanceof HTMLElement)) return false;
				var st = window.getComputedStyle(card);
				return st.display !== 'none' && st.visibility !== 'hidden' && card.offsetParent !== null;
			}).map(function(card) {
				var titleEl = card.querySelector(arguments[1]);
				if (!titleEl) return null;
				var text = (titleEl.getAttribute('data-title') || titleEl.textContent || '').trim();
				if (!text) return null;
				var rect = card.getBoundingClientRect();
				return { text: text, top: rect.top, left: rect.left };
			}).filter(function(item) { return item !== null; });
			cards.sort(function(a, b) {
				if (Math.abs(a.top - b.top) > 2) return a.top - b.top;
				return a.left - b.left;
			});
			return cards.map(function(i) { return i.text; }).filter(function(t) { return t.length > 0; });
		""", [cardSelectors, titleSelectors])
		return (titles ?: []) as List<String>
	}

	/** Selecciona una opción: usa objeto OR si está mapeado, si no cae a JS por CSS. */
	private static boolean selectOptionSmart(String objPath, String cssSelector, String value) {
		if (objPath) {
			try {
				WebUI.selectOptionByValue(findTestObject(objPath), value, false)
				return true
			} catch (Exception ignored) {}
		}
		return setSelectValueByCss(cssSelector, value)
	}

	/** Reutiliza la sesión si el dashboard ya está visible; si no, hace login MS. */
	private static void ensureSessionOpen(String dashboardObj, String startUrl) {
		boolean browserAlreadyOpen = false
		try {
			browserAlreadyOpen = WebUI.verifyElementPresent(findTestObject(dashboardObj), 5, FailureHandling.OPTIONAL)
		} catch (Exception ignored) {}
		if (!browserAlreadyOpen) {
			CustomKeywords.'TempletPortalKeywords.openBrowserAndLoginWithMicrosoft'(startUrl)
		}
		WebUI.waitForPageLoad(10)
	}

	/** Resuelve la URL de la plataforma desde GlobalVariable con fallback. */
	private static String resolveUrl(Map config) {
		CommonKeywords.getRequiredGlobal(config.urlVariableName as String, config.fallbackUrl as String)
	}

	/** Cierra el resultado del TC con el patrón failures estándar. */
	private static void finishCase(String caseId, List<String> failures, String okMessage) {
		if (failures.isEmpty()) {
			KeywordUtil.markPassed("${caseId} OK. ${okMessage}")
		} else {
			KeywordUtil.markFailed("${caseId} FALLÓ. Failures: ${failures.join(' | ')}")
		}
	}

	/**
	 * Busca la primera initiative con contenido según un criterio.
	 * @param criteria 'sortEnabled' (habilita el sort) o 'hasCards' (tiene tarjetas visibles).
	 */
	private static Map findInitiativeWithContent(Map config, String criteria) {
		String initiativeCss = config.initiativeCss as String
		List<Map> initiatives = CommonKeywords.readSelectOptionsWhenReady(initiativeCss, 12, 1)
		List<Map> candidates = initiatives.findAll { it['value'] != null && !it['value'].toString().trim().isEmpty() }
		for (Map candidate : candidates) {
			String iValue = candidate['value'].toString()
			String iText  = candidate['text']?.toString() ?: iValue
			if (!selectOptionSmart(config.initiativeObj as String, initiativeCss, iValue)) continue
			WebUI.waitForPageLoad(5)
			WebUI.delay(2)
			if (hasEmptyMessage(config.emptyMessages as List<String>)) continue
			boolean ok = (criteria == 'sortEnabled') ?
				isSelectEnabled(config.sortCss as String) :
				(countVisibleCards(config.cardSelectors as String) > 0)
			if (ok) {
				KeywordUtil.logInfo("[SETUP] Initiative con contenido (${criteria}): ${iText}")
				return [value: iValue, text: iText]
			}
		}
		return null
	}

	// ─── 1) FUNCTIONAL SMOKE ─────────────────────────────────────────────────

	/**
	 * Smoke funcional de una plataforma admin.php: login MS, verificación de
	 * elementos base, click de creación con sesión válida y logout verificado.
	 *
	 * config:
	 *   caseId            : ID del TC (ej. 'TC-SHEETS-FUNCTIONAL-SMOKE-010')
	 *   urlVariableName   : GlobalVariable de la URL (ej. 'SHEETS_TEST_URL')
	 *   fallbackUrl       : URL si la global no existe
	 *   checks            : lista de [name, xpath, expectedText] — expectedText null = solo presencia
	 *   createClickXPath  : XPath del botón de creación a clickear manteniendo sesión
	 *   verifyLogoutAgainstStartUrl : true = logoutAndVerify(startUrl); false/null = logoutAndVerify()
	 *   okMessage         : mensaje de éxito
	 */
	@Keyword
	static void runFunctionalSmoke(Map config) {
		String caseId = config.caseId
		String startUrl = resolveUrl(config)
		List<String> failures = []
		try {
			CustomKeywords.'TempletPortalKeywords.openBrowserAndLoginWithMicrosoft'(startUrl)
			CustomKeywords.'TempletPortalKeywords.captureCaseScreenshot'(caseId, 'post_login')

			(config.checks as List<Map>).each { Map check ->
				boolean ok
				if (check.expectedText) {
					ok = CustomKeywords.'TempletPortalKeywords.verifyXPathText'(check.name as String, check.xpath as String, check.expectedText as String, 8)
				} else {
					ok = CustomKeywords.'TempletPortalKeywords.verifyXPathPresent'(check.name as String, check.xpath as String, 8)
				}
				if (!ok) failures.add(check.failureMessage as String ?: "Elemento '${check.name}' no visible".toString())
			}

			if (config.createClickXPath) {
				if (!CustomKeywords.'TempletPortalKeywords.clickXPathAndKeepValidSession'('create_click', config.createClickXPath as String, 8)) {
					failures.add('No se pudo abrir la entrada de creación con sesión válida')
				}
			}

			WebUI.navigateToUrl(startUrl)
			WebUI.waitForPageLoad(15)
			boolean logoutOk = config.verifyLogoutAgainstStartUrl ?
				CustomKeywords.'TempletPortalKeywords.logoutAndVerify'(startUrl) :
				CustomKeywords.'TempletPortalKeywords.logoutAndVerify'()
			if (!logoutOk) failures.add('No se confirmó logout correctamente')

			if (failures.isEmpty()) {
				KeywordUtil.markPassed("${caseId} OK. ${config.okMessage ?: 'Login, dashboard, creación básica y logout validados.'}")
			} else {
				KeywordUtil.markFailed("${caseId} falló: ${failures.join(' | ')}")
			}
		} finally {
			CustomKeywords.'TempletPortalKeywords.safeCloseBrowser'()
		}
	}

	// ─── 2) FLUJO CLIENT → INITIATIVE → SORT ────────────────────────────────

	/**
	 * Valida el flujo completo de filtros Client → Initiative → Sort con evidencia
	 * por paso: opciones del select válidas, initiative que habilita Sort, orden
	 * Newest/Oldest/A to Z aplicado y logout final.
	 *
	 * config:
	 *   caseId, urlVariableName, fallbackUrl
	 *   snapDirName      : subcarpeta de Reports/Screenshots para evidencias
	 *   dashboardObj     : objeto OR del dashboard (para detectar sesión ya abierta)
	 *   filtersObj       : objeto OR de la sección de filtros (opcional)
	 *   clientObj        : objeto OR del select client (opcional → CSS)
	 *   initiativeObj    : objeto OR del select initiative (opcional → CSS)
	 *   sortObj          : objeto OR del select sort (opcional → CSS)
	 *   logoutObj        : objeto OR del botón logout (opcional → XPath 'Log Out')
	 *   clientCss, initiativeCss, sortCss
	 *   preferredClient  : client a seleccionar (ej. 'BRAVA')
	 */
	@Keyword
	static void runClientInitiativeSortFlow(Map config) {
		String caseId = config.caseId
		String startUrl = resolveUrl(config)
		String snapDir = System.getProperty('user.dir') + '/Reports/Screenshots/' + config.snapDirName
		new File(snapDir).mkdirs()
		String clientCss = config.clientCss
		String initiativeCss = config.initiativeCss
		String sortCss = config.sortCss

		ensureSessionOpen(config.dashboardObj as String, startUrl)

		// Dashboard post-login
		WebUI.waitForElementVisible(findTestObject(config.dashboardObj as String), 15)
		if (config.filtersObj) {
			WebUI.verifyElementVisible(findTestObject(config.filtersObj as String), FailureHandling.OPTIONAL)
		}
		validateSelectItems(config, config.clientObj as String, clientCss, 'CLIENT', true, snapDir)
		snap(snapDir, '01_login_ok')

		// Client
		if (!selectOptionSmart(config.clientObj as String, clientCss, config.preferredClient as String)) {
			KeywordUtil.markFailedAndStop("[CLIENT] No fue posible seleccionar client ${config.preferredClient}")
		}
		WebUI.waitForPageLoad(10)
		boolean initiativeReady = CommonKeywords.waitForSelectOptions(initiativeCss, 15, 1)
		if (!initiativeReady) {
			KeywordUtil.markFailedAndStop('[INITIATIVE] El select no quedó listo (enabled + opciones) tras seleccionar client')
		}
		validateSelectItems(config, config.initiativeObj as String, initiativeCss, 'INITIATIVE', true, snapDir)
		snap(snapDir, "02_client_${sanitize(config.preferredClient as String)}_seleccionado")

		// Initiative que habilite Sort
		Map selectedInitiative = null
		List<Map> candidates = readSelectOptions(initiativeCss).findAll { it['value'] != null && !it['value'].toString().trim().isEmpty() }
		for (Map opt : candidates) {
			String value = opt['value'].toString()
			String text = opt['text']?.toString() ?: value
			selectOptionSmart(config.initiativeObj as String, initiativeCss, value)
			WebUI.waitForPageLoad(8)
			WebUI.delay(1)
			snap(snapDir, "03_initiative_try_${sanitize(text)}")
			if (isSelectEnabled(sortCss)) {
				selectedInitiative = [value: value, text: text]
				KeywordUtil.logInfo("[INITIATIVE] OK: '${text}' habilita sort")
				break
			}
		}
		if (selectedInitiative == null) {
			KeywordUtil.markFailedAndStop('[INITIATIVE] Ninguna initiative habilitó el select sort')
		}
		snap(snapDir, "03_initiative_selected_${sanitize(selectedInitiative.text.toString())}")

		// Sort: validar opciones y aplicar Newest / Oldest / A to Z
		validateSelectItems(config, config.sortObj as String, sortCss, 'SORT', true, snapDir)
		List<Map> sortOptions = readSelectOptions(sortCss)
		['Newest', 'Oldest', 'A to Z'].eachWithIndex { String sortLabel, int i ->
			Map option = sortOptions.find { it['text']?.toString()?.trim()?.equalsIgnoreCase(sortLabel) }
			if (!option?.value) {
				KeywordUtil.markWarning("[SORT] Opción '${sortLabel}' no encontrada en dropdown")
				return
			}
			selectOptionSmart(config.sortObj as String, sortCss, option.value.toString())
			WebUI.waitForPageLoad(8)
			snap(snapDir, "0${4 + i}_sort_${sanitize(sortLabel)}")
		}

		// Logout
		if (config.logoutObj) {
			WebUI.waitForElementClickable(findTestObject(config.logoutObj as String), 10)
			WebUI.click(findTestObject(config.logoutObj as String))
		} else {
			CustomKeywords.'TempletPortalKeywords.clickXPathAndKeepValidSession'('logout', "//a[contains(normalize-space(.),'Log Out')]", 8)
		}
		WebUI.waitForPageLoad(10)
		snap(snapDir, '07_logout_ok')

		WebUI.closeBrowser()
		KeywordUtil.markPassed("${caseId}: Client → Initiative → Sort validados. Capturas en ${snapDir}")
	}

	/** Valida que todas las opciones de un select tengan texto, sean visibles y con value válido. */
	private static boolean validateSelectItems(Map config, String objPath, String cssSelector, String label, boolean allowBlankFirstValue, String snapDir) {
		if (objPath) {
			WebUI.waitForElementVisible(findTestObject(objPath), 10)
			WebUI.click(findTestObject(objPath), FailureHandling.OPTIONAL)
		}
		WebUI.delay(1)
		snap(snapDir, "${label}_dropdown_open")

		List<Map> optionData = readSelectOptions(cssSelector)
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
				KeywordUtil.logInfo("[${label}][OPTION] idx=${opt['idx']} value='${opt['value']}' text='${opt['text']}' visible=${opt['visible']} ${rowOk ? 'OK' : 'FAIL'}")
				if (!rowOk) allOk = false
			}
		}
		if (allOk) {
			KeywordUtil.logInfo("[${label}] Todas las opciones son visibles y tienen texto")
		} else {
			KeywordUtil.markWarning("[${label}] Hay opciones inválidas (sin texto/no visibles/value inválido)")
		}
		if (config.dashboardObj) {
			WebUI.click(findTestObject(config.dashboardObj as String), FailureHandling.OPTIONAL)
		}
		return allOk
	}

	// ─── 3) VALIDACIÓN DE CONTENIDO POR INITIATIVE ──────────────────────────

	/**
	 * Recorre todas las initiatives del client preferido clasificándolas en
	 * con-contenido / vacías (mensaje de "no hay documentos") y exige al menos
	 * una con contenido. Deja evidencia por initiative y hace logout.
	 *
	 * config: caseId, urlVariableName, fallbackUrl, snapDirName, dashboardObj,
	 *   filtersObj, clientObj, initiativeObj (opcionales), clientCss, initiativeCss,
	 *   preferredClient, emptyMessages (lista de textos de vacío), logoutObj (opcional)
	 */
	@Keyword
	static void validateInitiativeContent(Map config) {
		String caseId = config.caseId
		String startUrl = resolveUrl(config)
		String snapDir = System.getProperty('user.dir') + '/Reports/Screenshots/' + config.snapDirName
		new File(snapDir).mkdirs()
		String initiativeCss = config.initiativeCss
		List<String> emptyMessages = config.emptyMessages as List<String>

		try {
			ensureSessionOpen(config.dashboardObj as String, startUrl)
			WebUI.waitForElementVisible(findTestObject(config.dashboardObj as String), 15)
			snap(snapDir, '01_login_ok')

			if (!selectOptionSmart(config.clientObj as String, config.clientCss as String, config.preferredClient as String)) {
				KeywordUtil.markFailedAndStop("[CLIENT] No fue posible seleccionar client ${config.preferredClient}")
			}
			WebUI.waitForPageLoad(10)
			boolean ready = CommonKeywords.waitForSelectOptions(initiativeCss, 15, 1)
			if (!ready) {
				KeywordUtil.markFailedAndStop('[INITIATIVE] El select no quedó listo tras seleccionar client')
			}
			validateSelectItems(config, config.initiativeObj as String, initiativeCss, 'INITIATIVE', true, snapDir)
			snap(snapDir, '02_initiative_list_validated')

			List<Map> candidates = readSelectOptions(initiativeCss).findAll { it['value'] != null && !it['value'].toString().trim().isEmpty() }
			KeywordUtil.logInfo("[INITIATIVE] Candidatas para recorrido: ${candidates.size()}")

			List<String> withDocs = []
			List<String> empty = []
			Map selectedInitiative = null
			for (Map opt : candidates) {
				String value = opt['value'].toString()
				String text = opt['text']?.toString() ?: value
				selectOptionSmart(config.initiativeObj as String, initiativeCss, value)
				WebUI.waitForPageLoad(8)

				boolean noDocs = false
				for (int i = 0; i < 4; i++) {
					if (hasEmptyMessage(emptyMessages)) { noDocs = true; break }
					WebUI.delay(1)
				}
				if (noDocs) {
					empty.add(text)
					KeywordUtil.logInfo("[INITIATIVE] '${text}' → VACIA (mensaje detectado)")
					snap(snapDir, "03_initiative_empty_${sanitize(text)}")
				} else {
					withDocs.add(text)
					KeywordUtil.logInfo("[INITIATIVE] '${text}' → CON OBJETOS")
					snap(snapDir, "03_initiative_with_docs_${sanitize(text)}")
					if (selectedInitiative == null) selectedInitiative = [value: value, text: text]
				}
			}

			if (selectedInitiative == null || withDocs.isEmpty()) {
				KeywordUtil.markFailedAndStop('[INITIATIVE] Todas las initiatives están vacías (mensaje de no documentos).')
			}
			KeywordUtil.logInfo("[INITIATIVE] Con objetos (${withDocs.size()}): ${withDocs}")
			KeywordUtil.logInfo("[INITIATIVE] Vacías (${empty.size()}): ${empty}")

			selectOptionSmart(config.initiativeObj as String, initiativeCss, selectedInitiative.value.toString())
			WebUI.waitForPageLoad(8)
			snap(snapDir, "04_initiative_selected_${sanitize(selectedInitiative.text.toString())}")

			// Logout
			if (config.logoutObj) {
				WebUI.waitForElementClickable(findTestObject(config.logoutObj as String), 10)
				WebUI.click(findTestObject(config.logoutObj as String))
			} else {
				CustomKeywords.'TempletPortalKeywords.clickXPathAndKeepValidSession'('logout', "//a[contains(normalize-space(.),'Log Out')]", 8)
			}
			WebUI.waitForPageLoad(10)
			snap(snapDir, '05_logout_ok')

			KeywordUtil.markPassed("${caseId}: Initiative validado. Evidencias en ${snapDir}")
		} finally {
			WebUI.closeBrowser(FailureHandling.OPTIONAL)
		}
	}

	// ─── 4) SORT EN GRID Y LIST ──────────────────────────────────────────────

	/**
	 * Valida que Sort reordena los documentos en View Grid y View List:
	 * aplica Newest/Oldest/A to Z/Z to A, verifica títulos visibles, y exige
	 * que Newest/Oldest inviertan el orden.
	 *
	 * config: caseId, urlVariableName, fallbackUrl, dashboardObj, clientObj,
	 *   clientCss, initiativeCss, sortCss, preferredClient,
	 *   cardSelectors, titleSelectors, emptyMessages, viewListObj
	 */
	@Keyword
	static void validateSortGridList(Map config) {
		String caseId = config.caseId
		String startUrl = resolveUrl(config)
		String sortCss = config.sortCss
		List<String> failures = []

		try {
			ensureSessionOpen(config.dashboardObj as String, startUrl)

			Map selectedClient = CommonKeywords.selectPreferredOption(config.clientObj as String, config.clientCss as String, config.preferredClient as String, 12)
			if (!selectedClient?.value) {
				failures.add("[SETUP] No fue posible seleccionar client ${config.preferredClient}")
				KeywordUtil.markFailed(failures.join(' | '))
			}

			Map selectedInitiative = findInitiativeWithContent(config, 'sortEnabled')
			if (selectedInitiative == null) {
				failures.add('[SETUP] Ninguna initiative tiene contenido con Sort habilitado')
				KeywordUtil.markFailed(failures.join(' | '))
			}

			validateSortInView(config, 'View Grid', failures, caseId)

			// Cambiar a View List
			def viewListBtn = findTestObject(config.viewListObj as String)
			WebUI.waitForElementVisible(viewListBtn, 10)
			WebUI.waitForElementClickable(viewListBtn, 10)
			WebUI.click(viewListBtn)
			WebUI.waitForPageLoad(5)
			WebUI.delay(2)
			KeywordUtil.logInfo("[${caseId}] Cambiado a View List")

			validateSortInView(config, 'View List', failures, caseId)

			CustomKeywords.'TempletPortalKeywords.captureCaseScreenshot'(caseId, 'final')
			finishCase(caseId, failures, 'Sort funciona correctamente en Grid y List.')
		} finally {
			try { WebUI.closeBrowser() } catch (Exception ignored) {}
		}
	}

	/** Aplica cada opción de sort en la vista actual y valida inversión Newest/Oldest. */
	private static void validateSortInView(Map config, String viewLabel, List<String> failures, String caseId) {
		String sortCss = config.sortCss
		String cardSelectors = config.cardSelectors
		String titleSelectors = config.titleSelectors
		KeywordUtil.logInfo("[${caseId}] Validando Sort en ${viewLabel}")

		if (!isSelectEnabled(sortCss)) {
			failures.add("[${viewLabel}] Sort select NO está habilitado")
			return
		}
		List<Map> sortOptions = readSelectOptions(sortCss)
		KeywordUtil.logInfo("[${viewLabel}] Sort opciones disponibles: ${sortOptions.collect { it['text'] }.join(', ')}")

		['Newest', 'Oldest', 'A to Z', 'Z to A'].each { String sortLabel ->
			Map option = sortOptions.find { it['text']?.toString()?.trim()?.equalsIgnoreCase(sortLabel) }
			if (!option?.value) {
				failures.add("[${viewLabel}] Opción Sort '${sortLabel}' NO encontrada en dropdown")
				return
			}
			if (!setSelectValueByCss(sortCss, option.value.toString())) {
				failures.add("[${viewLabel}] Sort '${sortLabel}' NO se pudo aplicar")
				return
			}
			WebUI.delay(2)
			List<String> titles = getVisibleTitles(cardSelectors, titleSelectors)
			if (titles.isEmpty()) {
				failures.add("[${viewLabel}] Sort '${sortLabel}' aplicado pero SIN TÍTULOS VISIBLES")
			} else {
				KeywordUtil.logInfo("[${viewLabel}] Sort '${sortLabel}' → ${titles.size()} títulos: ${titles.take(3).join(', ')}...")
				CustomKeywords.'TempletPortalKeywords.captureCaseScreenshot'(caseId, "sort_${viewLabel.replace(' ','_')}_${sortLabel.replace(' ','_')}")
			}
		}

		// Newest vs Oldest deben invertir orden
		setSelectValueByCss(sortCss, sortOptions.find { it['text']?.toString()?.trim()?.equalsIgnoreCase('Newest') }?.value?.toString())
		WebUI.delay(2)
		List<String> newestTitles = getVisibleTitles(cardSelectors, titleSelectors)
		setSelectValueByCss(sortCss, sortOptions.find { it['text']?.toString()?.trim()?.equalsIgnoreCase('Oldest') }?.value?.toString())
		WebUI.delay(2)
		List<String> oldestTitles = getVisibleTitles(cardSelectors, titleSelectors)

		if (!newestTitles.isEmpty() && !oldestTitles.isEmpty()) {
			KeywordUtil.logInfo("[DIAGNÓSTICO] ${viewLabel} Newest primeros 3: ${newestTitles.take(3).join(' | ')}")
			KeywordUtil.logInfo("[DIAGNÓSTICO] ${viewLabel} Oldest primeros 3: ${oldestTitles.take(3).join(' | ')}")
			KeywordUtil.logInfo("[DIAGNÓSTICO] ${viewLabel} ¿Listas idénticas? ${newestTitles == oldestTitles}")
		}
		if (newestTitles.isEmpty() || oldestTitles.isEmpty()) {
			failures.add("[${viewLabel}] No se pudo comparar Newest vs Oldest")
		} else if (newestTitles == oldestTitles) {
			failures.add("[${viewLabel}] DIAGNÓSTICO: Newest/Oldest devolvieron el MISMO ORDEN (${newestTitles.size()} elementos)")
		} else if (newestTitles.first() != oldestTitles.last() || newestTitles.last() != oldestTitles.first()) {
			failures.add("[${viewLabel}] Newest/Oldest NO invierten orden")
		} else {
			KeywordUtil.logInfo("[${viewLabel}] Newest/Oldest invierten orden correctamente ✓")
		}
	}

	// ─── 5) ACCIONES DE LIST VIEW → MODAL/SUBWINDOW ──────────────────────────

	/**
	 * Valida que cada acción del menú de la primera card en List View
	 * (Edit/Rename/URL/Duplicate/Download/Move/Delete) abre modal, subwindow
	 * o navega — y falla si la acción no responde.
	 *
	 * config: caseId, urlVariableName, fallbackUrl, dashboardObj, clientObj,
	 *   clientCss, initiativeCss, preferredClient, cardSelectors, emptyMessages,
	 *   listViewObj, listShapeObj, urlActionLabels (labels de la acción URL por plataforma)
	 */
	@Keyword
	static void validateListActionsModal(Map config) {
		String caseId = config.caseId
		String startUrl = resolveUrl(config)
		String cardSelectors = config.cardSelectors
		List<String> failures = []

		try {
			ensureSessionOpen(config.dashboardObj as String, startUrl)

			Map selectedClient = CommonKeywords.selectPreferredOption(config.clientObj as String, config.clientCss as String, config.preferredClient as String, 12)
			if (!selectedClient?.value) {
				failures.add("[SETUP] No fue posible seleccionar client ${config.preferredClient}")
				KeywordUtil.markFailed(failures.join(' | '))
			}

			Map selectedInitiative = findInitiativeWithContent(config, 'hasCards')
			if (selectedInitiative == null) {
				failures.add('[SETUP] Ninguna initiative tiene documentos visibles')
				KeywordUtil.markFailed(failures.join(' | '))
			}

			// Activar List View (OR objects + toggle JS, con reintentos)
			def listViewIcon = findTestObject(config.listViewObj as String)
			def listShape = findTestObject(config.listShapeObj as String)
			WebUI.waitForElementVisible(listViewIcon, 4, FailureHandling.OPTIONAL)
			boolean listViewReady = false
			for (int attempt = 1; attempt <= 3; attempt++) {
				WebUI.click(listViewIcon, FailureHandling.OPTIONAL)
				WebUI.click(listShape, FailureHandling.OPTIONAL)
				WebUI.executeJavaScript("""
					var btn = document.querySelector('a[href="#tabs-2"], .btn-view_list, button[data-view="list"]');
					if (btn) btn.click();
				""", null)
				WebUI.delay(1)
				if (countVisibleCards(cardSelectors) > 0) { listViewReady = true; break }
			}
			if (!listViewReady) {
				failures.add('[SETUP] No se pudo activar List View (0 cards visibles después de toggle)')
				KeywordUtil.markFailed(failures.join(' | '))
			}
			KeywordUtil.logInfo("[${caseId}] List View activado con ${countVisibleCards(cardSelectors)} documentos")

			List<Map> actions = [
				[id: 'edit',      labels: ['Edit', 'Editar']],
				[id: 'rename',    labels: ['Rename', 'Renombrar']],
				[id: 'url',       labels: (config.urlActionLabels ?: ['URL', 'Copy URL', 'Copiar URL']) as List<String>],
				[id: 'duplicate', labels: ['Duplicate', 'Duplicar']],
				[id: 'download',  labels: ['Download', 'Descargar', 'Export', 'Exportar']],
				[id: 'move',      labels: ['Move', 'Mover']],
				[id: 'delete',    labels: ['Delete', 'Eliminar']]
			]

			actions.each { Map action ->
				validateSingleAction(action, cardSelectors, failures, caseId)
			}

			CustomKeywords.'TempletPortalKeywords.captureCaseScreenshot'(caseId, 'final')
			finishCase(caseId, failures, 'Todas las acciones abren modal o subwindow.')
		} finally {
			try { WebUI.closeBrowser() } catch (Exception ignored) {}
		}
	}

	/** Ejecuta una acción del menú de la primera card y valida su respuesta (modal/subwindow/navegación). */
	private static void validateSingleAction(Map action, String cardSelectors, List<String> failures, String caseId) {
		String actionId = action.id
		List<String> labels = action.labels as List<String>

		boolean menuOpened = false
		for (int menuAttempt = 1; menuAttempt <= 3; menuAttempt++) {
			if (openFirstCardMenu(cardSelectors)) { menuOpened = true; break }
			if (menuAttempt < 3) WebUI.delay(1)
		}
		if (!menuOpened) {
			failures.add("[${actionId}] No se pudo abrir menú dropdown en 3 intentos")
			return
		}
		WebUI.delay(1)
		CustomKeywords.'TempletPortalKeywords.captureCaseScreenshot'(caseId, "action_${actionId}_menu")

		int modalsBefore = countModals()
		int subwindowsBefore = countSubwindows()
		String urlBefore = currentUrlQuiet()

		boolean clicked = clickActionEntry(labels)
		if (!clicked) {
			failures.add("[${actionId}] No se encontró opción con labels: ${labels.join('/')}")
			closeTransientUi()
			return
		}
		WebUI.delay(2)

		int modalsAfter = countModals()
		int subwindowsAfter = countSubwindows()
		String urlAfter = currentUrlQuiet()
		CustomKeywords.'TempletPortalKeywords.captureCaseScreenshot'(caseId, "action_${actionId}_after_click")

		boolean modalOpened = modalsAfter > modalsBefore
		boolean subwindowOpened = subwindowsAfter > subwindowsBefore
		boolean sameTabNavigation = (urlBefore && urlAfter && urlBefore != urlAfter)
		KeywordUtil.logInfo("[${actionId}] modals ${modalsBefore}→${modalsAfter}, subwindows ${subwindowsBefore}→${subwindowsAfter}")

		if (!modalOpened && !subwindowOpened && !sameTabNavigation) {
			failures.add("[${actionId}] NO abrió modal/subwindow/navegación. modals ${modalsBefore}→${modalsAfter}, subwindows ${subwindowsBefore}→${subwindowsAfter}, url '${urlBefore}'→'${urlAfter}'")
		} else {
			String responseType = modalOpened ? 'modal abierto' : (subwindowOpened ? 'subwindow abierto' : 'navegación misma pestaña')
			KeywordUtil.logInfo("[${actionId}] Respuesta OK: ${responseType} ✓")
		}

		closeTransientUi()
		if (sameTabNavigation) {
			try {
				WebUI.back(FailureHandling.OPTIONAL)
				WebUI.delay(2)
			} catch (Exception ignored) {}
			WebUI.executeJavaScript("""
				var listBtn = document.querySelector('a[href="#tabs-2"], .btn-view_list, button[data-view="list"]');
				if (listBtn) listBtn.click();
			""", null)
			WebUI.delay(1)
		}
		WebUI.delay(1)
	}

	/** Abre el menú dropdown de la primera card visible (múltiples selectores + fallback). */
	private static boolean openFirstCardMenu(String cardSelectors) {
		Boolean opened = (Boolean) WebUI.executeJavaScript("""
			var cards = Array.from(document.querySelectorAll(arguments[0])).filter(function(el) {
				if (!(el instanceof HTMLElement)) return false;
				var st = window.getComputedStyle(el);
				return st.display !== 'none' && st.visibility !== 'hidden' && el.offsetParent !== null;
			});
			if (cards.length === 0) return false;
			var card = cards[0];
			var selectors = ['.dropdown-toggle', 'button[data-toggle="dropdown"]', '.btn-menu',
				'button.dropdown-toggle', '[role="button"][aria-haspopup="true"]', 'a.dropdown-toggle',
				'.card-actions button', '.item-actions button'];
			for (var i = 0; i < selectors.length; i++) {
				var menuBtn = card.querySelector(selectors[i]);
				if (menuBtn && window.getComputedStyle(menuBtn).display !== 'none') { menuBtn.click(); return true; }
			}
			var allButtons = card.querySelectorAll('button, a, [role="button"]');
			for (var j = 0; j < allButtons.length; j++) {
				var btn = allButtons[j];
				if (window.getComputedStyle(btn).display !== 'none' && btn.offsetParent !== null) { btn.click(); return true; }
			}
			return false;
		""", [cardSelectors])
		return Boolean.TRUE.equals(opened)
	}

	/** Cuenta modales visibles. */
	private static int countModals() {
		Integer count = (Integer) WebUI.executeJavaScript("""
			return Array.from(document.querySelectorAll('.modal')).filter(function(m) {
				var st = window.getComputedStyle(m);
				return st.display !== 'none' && (m.classList.contains('show') || m.style.display === 'block');
			}).length;
		""", null)
		return count ?: 0
	}

	/** Cuenta ventanas/tabs del driver. */
	private static int countSubwindows() {
		int handles = 1
		try {
			handles = DriverFactory.getWebDriver().getWindowHandles().size()
		} catch (Exception ignored) {}
		return handles
	}

	/** URL actual sin lanzar excepción. */
	private static String currentUrlQuiet() {
		try {
			return WebUI.getUrl() ?: ''
		} catch (Exception ignored) {
			return ''
		}
	}

	/** Cierra modales/menus transitorios abiertos. */
	private static void closeTransientUi() {
		WebUI.executeJavaScript("""
			var modals = Array.from(document.querySelectorAll('.modal.show, .modal[style*="display: block"]'));
			modals.forEach(function(m) {
				var closeBtn = m.querySelector('.close, [data-dismiss="modal"], .btn-close');
				if (closeBtn) closeBtn.click();
			});
			document.body.click();
		""", null)
		WebUI.delay(1)
	}

	/** Click en la entrada del menú cuya etiqueta coincida (probando varias etiquetas). */
	private static boolean clickActionEntry(List<String> labels) {
		Boolean clicked = false
		for (String label : labels) {
			clicked = (Boolean) WebUI.executeJavaScript("""
				var items = Array.from(document.querySelectorAll('.dropdown-item, .menu-item, a, button'));
				var labelLower = String(arguments[0]).toLowerCase();
				var target = items.find(function(el) { return el.textContent.trim().toLowerCase() === labelLower; });
				if (target && window.getComputedStyle(target).display !== 'none') { target.click(); return true; }
				return false;
			""", [label])
			if (Boolean.TRUE.equals(clicked)) break
		}
		return Boolean.TRUE.equals(clicked)
	}
}
