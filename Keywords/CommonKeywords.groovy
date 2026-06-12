import static com.kms.katalon.core.testobject.ObjectRepository.findTestObject
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import com.kms.katalon.core.annotation.Keyword
import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.exception.WebElementNotFoundException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

public class CommonKeywords {

	static String inferIssueCategory(String message) {
		String raw = (message ?: '').toString().trim()
		if (!raw.startsWith('[')) return 'GENERAL'
		int closeIdx = raw.indexOf(']')
		if (closeIdx <= 1) return 'GENERAL'
		String category = raw.substring(1, closeIdx).trim()
		return category.length() > 0 ? category : 'GENERAL'
	}

	@Keyword
	def static void logCaseSummary(String caseId, List failures = [], List warnings = []) {
		List<String> safeFailures = (failures ?: []).collect { (it ?: '').toString().trim() }.findAll { it.length() > 0 }
		List<String> safeWarnings = (warnings ?: []).collect { (it ?: '').toString().trim() }.findAll { it.length() > 0 }

		KeywordUtil.logInfo("[SUMMARY] case=${caseId} failures=${safeFailures.size()} warnings=${safeWarnings.size()}")

		def logBucket = { String tag, List<String> issues ->
			if (issues.isEmpty()) {
				KeywordUtil.logInfo("[SUMMARY][${tag}] total=0")
				return
			}
			Map<String, Integer> categories = new LinkedHashMap<String, Integer>()
			issues.each { String issue ->
				String category = inferIssueCategory(issue)
				categories[category] = (categories[category] ?: 0) + 1
			}
			String breakdown = categories.collect { k, v -> "${k}=${v}" }.join(', ')
			KeywordUtil.logInfo("[SUMMARY][${tag}] total=${issues.size()} categories=${breakdown}")
			for (int i = 0; i < issues.size(); i++) {
				KeywordUtil.logInfo("[SUMMARY][${tag}][${i + 1}/${issues.size()}] ${issues[i]}")
			}
		}

		logBucket('WARNINGS', safeWarnings)
		logBucket('FAILURES', safeFailures)
	}

	@com.kms.katalon.core.annotation.Keyword
	def static Object getRequiredGlobal(String name, Object fallback = null) {
		try {
			java.lang.reflect.Field f = internal.GlobalVariable.class.getDeclaredField(name)
			f.setAccessible(true)
			Object val = f.get(null)
			if (val == null || (val instanceof String && ((String)val).trim().isEmpty())) {
				if (fallback != null) return fallback
				KeywordUtil.markFailedAndStop("GlobalVariable '${name}' no definida o vacía. Añadirla en Profiles/global.glbl")
			}
			return val
		} catch (NoSuchFieldException e) {
			if (fallback != null) return fallback
			KeywordUtil.markFailedAndStop("GlobalVariable '${name}' no existe. Añadirla en Profiles/global.glbl")
		} catch (Exception e) {
			KeywordUtil.markFailedAndStop("Error leyendo GlobalVariable '${name}': ${e.message}")
		}
		return null
	}


	/**
	 * Construye la URL directa a /tracking desde la base URL de Builders.
	 * Normaliza trailing slash antes de concatenar.
	 */
	@Keyword
	static String buildTrackingUrl(String baseUrl) {
		return baseUrl.replaceAll('/+$', '') + '/tracking'
	}


	/**
	 * Login en la URL indicada con usuario y password del Profile activo
	 */
	@Keyword
	def static loginUser(String url, String username, String password) {
		try {
			WebUI.navigateToUrl(url)
			WebUI.waitForPageLoad(15)

			WebUI.waitForElementVisible(findTestObject('Object Repository/Common/login_username'), 15)
			WebUI.clearText(findTestObject('Object Repository/Common/login_username'))
			WebUI.setText(findTestObject('Object Repository/Common/login_username'), username)

			WebUI.waitForElementVisible(findTestObject('Object Repository/Common/login_password'), 10)
			WebUI.clearText(findTestObject('Object Repository/Common/login_password'))
			WebUI.setText(findTestObject('Object Repository/Common/login_password'), password)

			WebUI.click(findTestObject('Object Repository/Common/login_button'))
			WebUI.waitForPageLoad(15)

			KeywordUtil.logInfo("[LOGIN] OK → ${url}")
		} catch (Exception e) {
			KeywordUtil.markFailed("[LOGIN] Error: ${e.message}")
		}
	}

	/**
	 * Capturar screenshot con nombre estandarizado: CASE_SECTION_ENV_timestamp.png
	 * Retorna el path del archivo generado
	 */
	@Keyword
	def static String captureScreenshot(String caseName, String section, String envLabel) {
		String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern('yyyyMMdd_HHmmss'))
		String fileName  = "${caseName}_${section}_${envLabel}_${timestamp}.png"
		String screenshotDir = System.getProperty('user.dir') + '/Reports/Screenshots'
		new File(screenshotDir).mkdirs()
		String filePath = screenshotDir + '/' + fileName
		WebUI.takeScreenshot(filePath)
		KeywordUtil.logInfo("[SCREENSHOT] Guardado: ${filePath}")
		return filePath
	}

	/**
	 * Captura screenshot sin romper el test en caso de sesión WebDriver inválida.
	 */
	@Keyword
	def static boolean captureScreenshotSafe(String filePath) {
		try {
			WebUI.takeScreenshot(filePath)
			KeywordUtil.logInfo("[SCREENSHOT] Guardado: ${filePath}")
			return true
		} catch (Exception e) {
			KeywordUtil.markWarning("[SCREENSHOT] No se pudo capturar evidencia: ${e.message}")
			return false
		}
	}

	/**
	 * Espera a que un select exista y tenga un mínimo de opciones con value no vacío.
	 */
	@Keyword
	def static boolean waitForSelectOptions(String cssSelector, int timeoutSeconds = 20, int minNonBlankOptions = 1) {
		for (int i = 0; i < timeoutSeconds; i++) {
			try {
				Map data = (Map) WebUI.executeJavaScript('''
					var css = arguments[0];
					var sel = document.querySelector(css);
					if (!sel) {
						return {exists:false, enabled:false, total:0, nonBlank:0};
					}
					var options = Array.from(sel.options || []);
					var nonBlank = options.filter(function(o) {
						return ((o.value || '').trim().length > 0);
					}).length;
					return {
						exists:true,
						enabled: !sel.disabled,
						total: options.length,
						nonBlank: nonBlank
					};
				''', [cssSelector])

				int nonBlank = ((data?.get('nonBlank') ?: 0) as Integer)
				if (Boolean.TRUE.equals(data?.get('exists')) && Boolean.TRUE.equals(data?.get('enabled')) && nonBlank >= minNonBlankOptions) {
					KeywordUtil.logInfo("[SELECT] ${cssSelector} listo: enabled=true, opcionesConValue=${nonBlank}")
					return true
				}
			} catch (Exception ignored) {
				// Reintento en siguiente ciclo
			}
			WebUI.delay(1)
		}

		KeywordUtil.markWarning("[SELECT] ${cssSelector} no quedó listo tras ${timeoutSeconds}s")
		return false
	}

	/**
	 * Lee opciones de un select de forma robusta esperando a que exista, esté habilitado
	 * y tenga opciones con value no vacío.
	 */
	@Keyword
	def static List<Map> readSelectOptionsWhenReady(String cssSelector, int timeoutSeconds = 20, int minNonBlankOptions = 1) {
		for (int i = 0; i < timeoutSeconds; i++) {
			try {
				Map data = (Map) WebUI.executeJavaScript('''
					var css = arguments[0];
					var sel = document.querySelector(css);
					if (!sel) {
						return {exists:false, enabled:false, options:[]};
					}
					var options = Array.from(sel.options || []).map(function(o, idx) {
						var st = window.getComputedStyle(o);
						var visible = !o.hidden && st.display !== 'none' && st.visibility !== 'hidden';
						return {
							idx: idx,
							value: (o.value || '').trim(),
							text: (o.textContent || '').trim(),
							visible: visible
						};
					});
					return {
						exists:true,
						enabled: !sel.disabled,
						options: options
					};
				''', [cssSelector])

				List<Map> options = (List<Map>) (data?.get('options') ?: [])
				int nonBlank = options.count { Map opt ->
					String value = (opt?.get('value') ?: '').toString().trim()
					return value.length() > 0
				}

				if (Boolean.TRUE.equals(data?.get('exists')) && Boolean.TRUE.equals(data?.get('enabled')) && nonBlank >= minNonBlankOptions) {
					KeywordUtil.logInfo("[SELECT] ${cssSelector} opciones listas: total=${options.size()}, conValue=${nonBlank}")
					return options
				}
			} catch (Exception ignored) {
				// Reintento en siguiente ciclo
			}
			WebUI.delay(1)
		}

		KeywordUtil.markWarning("[SELECT] ${cssSelector} sin opciones listas tras ${timeoutSeconds}s")
		return []
	}

	/**
	 * Selecciona una opción preferida por value y, si no existe, usa el primer value válido.
	 */
	@Keyword
	def static Map selectPreferredOption(String testObjectPath, String cssSelector, String preferredValue = null, int timeoutSeconds = 20) {
		List<Map> options = readSelectOptionsWhenReady(cssSelector, timeoutSeconds, 1)
		if (!options || options.isEmpty()) {
			KeywordUtil.markFailedAndStop("[SELECT] No se encontraron opciones en ${cssSelector}")
		}

		Map preferred = null
		if (preferredValue != null && !preferredValue.trim().isEmpty()) {
			preferred = options.find { Map opt ->
				(opt?.get('value') ?: '').toString().trim() == preferredValue.trim()
			}
		}

		Map fallback = options.find { Map opt ->
			(opt?.get('value') ?: '').toString().trim().length() > 0
		}
		Map selected = preferred ?: fallback

		if (selected == null) {
			KeywordUtil.markFailedAndStop("[SELECT] No hay opciones válidas para ${cssSelector}")
		}

		String value = (selected?.get('value') ?: '').toString().trim()
		String text = (selected?.get('text') ?: value).toString().trim()
		WebUI.selectOptionByValue(findTestObject(testObjectPath), value, false)
		WebUI.waitForPageLoad(10)
		KeywordUtil.logInfo("[SELECT] Seleccionado en ${cssSelector} value='${value}' text='${text}'")
		return [value: value, text: text]
	}

	/**
	 * Selecciona una opción en el select origen y espera que el select dependiente
	 * quede habilitado y con opciones válidas. Reintenta selección cuando aplica.
	 */
	@Keyword
	def static Map selectPreferredOptionAndWaitDependent(
		String sourceTestObjectPath,
		String sourceCssSelector,
		String preferredValue,
		String dependentCssSelector,
		int selectTimeoutSeconds = 20,
		int dependentTimeoutSeconds = 20,
		int maxAttempts = 3
	) {
		Map selected = null
		for (int attempt = 1; attempt <= maxAttempts; attempt++) {
			selected = selectPreferredOption(sourceTestObjectPath, sourceCssSelector, preferredValue, selectTimeoutSeconds)

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
				''', [sourceCssSelector, (selected?.get('value') ?: '').toString()])
			} catch (Exception ignored) {
				// Se continúa con wait de dependiente
			}

			if (waitForSelectOptions(dependentCssSelector, dependentTimeoutSeconds, 1)) {
				KeywordUtil.logInfo("[SELECT] Dependiente ${dependentCssSelector} listo en intento ${attempt}/${maxAttempts}")
				return selected
			}

			KeywordUtil.markWarning("[SELECT] Dependiente ${dependentCssSelector} no listo tras intento ${attempt}/${maxAttempts}; reintentando selección origen")
			WebUI.delay(1)
		}

		KeywordUtil.markFailedAndStop("[SELECT] ${dependentCssSelector} no quedó listo tras ${maxAttempts} intentos con ${sourceCssSelector}")
		return selected
	}

	/**
	 * Verificar que un elemento esté presente y visible
	 * Retorna true/false; si expectedText no es null, también verifica el texto
	 */
	@Keyword
	def static boolean verifyElementPresent(String objectPath, String expectedText = null) {
		try {
			def obj = findTestObject(objectPath)
			WebUI.waitForElementVisible(obj, 10)
			if (expectedText) {
				String actual = WebUI.getText(obj).trim()
				if (!actual.contains(expectedText)) {
					KeywordUtil.logInfo("[ELEMENT] Texto esperado '${expectedText}' no encontrado. Obtenido: '${actual}'")
					return false
				}
			}
			KeywordUtil.logInfo("[ELEMENT] Presente: ${objectPath}")
			return true
		} catch (WebElementNotFoundException ex) {
			KeywordUtil.logInfo("[ELEMENT] NO encontrado: ${objectPath}")
			return false
		}
	}

	/**
	 * Logout genérico
	 */
	@Keyword
	def static logout() {
		try {
			WebUI.click(findTestObject('Object Repository/Common/logout_button'))
			WebUI.waitForPageLoad(10)
			KeywordUtil.logInfo('[LOGOUT] OK')
		} catch (Exception e) {
			KeywordUtil.logInfo("[LOGOUT] Warning: ${e.message}")
		}
	}

	/**
	 * Valida las opciones de un <select> (extraccion del closure validateSelectItems duplicado en filters).
	 * cfg: objPath (TestObject a clickear), cssSelector, label, allowBlankFirstValue (bool),
	 *      failOnEmpty (bool, default false -> warning), closeObjectPath (opcional, click al cerrar),
	 *      snapper (Closure opcional: recibe String label para screenshot).
	 * Retorna true si todas las opciones son visibles, con texto y value valido.
	 */
	@Keyword
	def static boolean validateSelectOptions(Map cfg) {
		String objPath = cfg.objPath
		String cssSelector = cfg.cssSelector
		String label = (cfg.label ?: 'select').toString()
		boolean allowBlankFirstValue = Boolean.TRUE.equals(cfg.allowBlankFirstValue)
		boolean failOnEmpty = Boolean.TRUE.equals(cfg.failOnEmpty)

		WebUI.waitForElementVisible(findTestObject(objPath), 10)
		WebUI.click(findTestObject(objPath), com.kms.katalon.core.model.FailureHandling.OPTIONAL)
		WebUI.delay(1)
		if (cfg.snapper instanceof Closure) ((Closure) cfg.snapper).call("${label}_dropdown_open")

		List<Map> optionData = (List<Map>) WebUI.executeJavaScript('''
			var css = arguments[0];
			var sel = document.querySelector(css);
			if (!sel) return [];
			return Array.from(sel.options).map(function(o, idx) {
				var st = window.getComputedStyle(o);
				var visible = !o.hidden && st.display !== 'none' && st.visibility !== 'hidden';
				return { idx: idx, value: (o.value || '').trim(), text: (o.textContent || '').trim(), visible: visible };
			});
		''', [cssSelector])

		boolean allOk = true
		if (!optionData || optionData.isEmpty()) {
			allOk = false
			if (failOnEmpty) {
				KeywordUtil.markFailedAndStop("[${label}] No se encontraron opciones en ${cssSelector}")
			} else {
				KeywordUtil.markWarning("[${label}] No se encontraron opciones en ${cssSelector}")
			}
		} else {
			KeywordUtil.logInfo("[${label}] Total opciones: ${optionData.size()}")
			optionData.each { opt ->
				boolean hasText = opt['text'] != null && !opt['text'].toString().trim().isEmpty()
				boolean isVisible = Boolean.valueOf(opt['visible'].toString())
				boolean hasValue = opt['value'] != null && !opt['value'].toString().trim().isEmpty()
				boolean valueOk = hasValue || (allowBlankFirstValue && ((opt['idx'] as Integer) == 0))
				boolean rowOk = hasText && isVisible && valueOk
				KeywordUtil.logInfo("[${label}][OPTION] idx=${opt['idx']} value='${opt['value']}' text='${opt['text']}' visible=${opt['visible']} ${rowOk ? 'OK' : 'X'}")
				if (!rowOk) allOk = false
			}
		}

		if (allOk) {
			KeywordUtil.logInfo("[${label}] Todas las opciones son visibles y tienen texto OK")
		} else if (optionData && !optionData.isEmpty()) {
			KeywordUtil.markWarning("[${label}] Hay opciones invalidas (sin texto/no visibles/value invalido)")
		}

		if (cfg.closeObjectPath) {
			WebUI.click(findTestObject(cfg.closeObjectPath.toString()), com.kms.katalon.core.model.FailureHandling.OPTIONAL)
		}
		return allOk
	}
}
