import static com.kms.katalon.core.testobject.ObjectRepository.findTestObject
import com.kms.katalon.core.annotation.Keyword
import com.kms.katalon.core.model.FailureHandling as FailureHandling
import com.kms.katalon.core.testobject.ConditionType
import com.kms.katalon.core.testobject.TestObject
import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.driver.DriverFactory
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import com.kms.katalon.core.configuration.RunConfiguration
import internal.GlobalVariable as GlobalVariable
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovy.xml.XmlSlurper
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import org.openqa.selenium.By
import org.openqa.selenium.WebDriver
import org.openqa.selenium.WebElement

class TempletPortalKeywords {
	@Keyword
	static String detectConfiguredBrowser() {
		try {
			def props = RunConfiguration.getExecutionProperties()
			String browserType = props?.execution?.drivers?.system?.WebUI?.browserType?.toString()
			if (browserType && browserType.trim()) {
				return browserType.trim()
			}
		} catch (Exception ignored) {
		}
		return 'UNKNOWN'
	}

	static boolean fileExistsAny(List<String> candidates) {
		for (String path : candidates) {
			if (path && new File(path).exists()) {
				return true
			}
		}
		return false
	}

	@Keyword
	static void assertBrowserSupportForHost() {
		String browserType = detectConfiguredBrowser().toUpperCase()
		String osName = System.getProperty('os.name', '').toLowerCase()

		if (browserType.contains('SAFARI')) {
			if (!osName.contains('mac')) {
				KeywordUtil.markFailedAndStop('Safari solo es soportado en macOS. Host actual=' + System.getProperty('os.name') + ' browserType=' + browserType)
			}
			return
		}

		if (browserType.contains('FIREFOX')) {
			if (osName.contains('win')) {
				boolean firefoxInstalled = fileExistsAny([
					(System.getenv('ProgramFiles') ?: 'C:/Program Files') + '/Mozilla Firefox/firefox.exe',
					(System.getenv('ProgramFiles(x86)') ?: 'C:/Program Files (x86)') + '/Mozilla Firefox/firefox.exe'
				])
				if (!firefoxInstalled) {
					KeywordUtil.markFailedAndStop('Firefox seleccionado pero no instalado en rutas estándar. Instala Firefox o ejecuta la suite con Chrome/Edge. browserType=' + browserType)
				}
			}
			return
		}

		if (browserType.contains('EDGE')) {
			if (osName.contains('win')) {
				boolean edgeInstalled = fileExistsAny([
					(System.getenv('ProgramFiles') ?: 'C:/Program Files') + '/Microsoft/Edge/Application/msedge.exe',
					(System.getenv('ProgramFiles(x86)') ?: 'C:/Program Files (x86)') + '/Microsoft/Edge/Application/msedge.exe'
				])
				if (!edgeInstalled) {
					KeywordUtil.markFailedAndStop('Edge seleccionado pero no instalado en rutas estándar. Instala Edge o ejecuta con Chrome/Firefox. browserType=' + browserType)
				}
			}
			return
		}

		if (browserType == 'UNKNOWN') {
			KeywordUtil.logInfo('No se pudo detectar browserType desde RunConfiguration; se intentará abrir navegador con configuración actual de Katalon.')
		}
	}

	@Keyword
	static String resolveCredential(String primaryName, String secondaryName = null) {
		List<String> candidateNames = [primaryName, secondaryName].findAll { it != null && it.trim() }

		for (String candidate : candidateNames) {
			try {
				def metaProperty = GlobalVariable.metaClass.getMetaProperty(candidate)
				if (metaProperty != null) {
					def value = metaProperty.getProperty(GlobalVariable)
					if (value != null && value.toString().trim()) {
						return value.toString().trim()
					}
				}
			} catch (Exception ignored) {
			}
		}

		// Evitar colisión con variables del sistema como USERNAME en Windows (ej. e2494).
		// Solo aceptar variables de entorno explícitas para Templet.
		Map<String, List<String>> envCandidatesByName = [
			'MS_USER'  : ['TEMPLET_MS_USER'],
			'USERNAME' : ['TEMPLET_MS_USER'],
			'MS_PASS'  : ['TEMPLET_MS_PASS'],
			'PASSWORD' : ['TEMPLET_MS_PASS']
		]
		for (String candidate : candidateNames) {
			for (String envName : (envCandidatesByName[candidate] ?: [])) {
				String envValue = System.getenv(envName)
				if (envValue != null && envValue.trim()) {
					return envValue.trim()
				}
			}
		}

		// Leer archivo de credenciales externo (no gestionado por Katalon)
		try {
			String credPath = System.getProperty('user.dir') + '/Include/config/templet-credentials.properties'
			File credFile = new File(credPath)
			if (credFile.exists()) {
				Properties props = new Properties()
				credFile.withInputStream { props.load(it) }
				for (String candidate : candidateNames) {
					String val = props.getProperty(candidate)?.trim()
					if (val) {
						KeywordUtil.logInfo("✅ Credencial '${candidate}' encontrada en templet-credentials.properties")
						return val
					}
				}
			}
		} catch (Exception ignored) {
		}

		// Leer TODOS los profiles disponibles como último recurso
		try {
			String profilesDir = System.getProperty('user.dir') + '/Profiles'
			List<File> profilesToTry = []
			try {
				String activeProfile = com.kms.katalon.core.configuration.RunConfiguration.getExecutionProfile()
				if (activeProfile) {
					File activeFile = new File(profilesDir + '/' + activeProfile + '.glbl')
					if (activeFile.exists()) profilesToTry << activeFile
				}
			} catch (Exception ignored) {}
			File defaultFile = new File(profilesDir + '/default.glbl')
			if (defaultFile.exists() && !profilesToTry.contains(defaultFile)) profilesToTry << defaultFile
			new File(profilesDir).listFiles()?.each { File f ->
				if (f.name.endsWith('.glbl') && !profilesToTry.contains(f)) profilesToTry << f
			}
			for (File profileFile : profilesToTry) {
				def xml = new XmlSlurper().parse(profileFile)
				for (String candidate : candidateNames) {
					def variableNode = xml.variable.find { it.name.text() == candidate }
					if (variableNode != null) {
						String rawValue = variableNode.defaultValue.text()
						String parsedValue = rawValue?.replaceAll(/^'+|'+$/, '')?.trim()
						if (parsedValue) {
							KeywordUtil.logInfo("✅ Credencial '${candidate}' encontrada en profile: ${profileFile.name}")
							return parsedValue
						}
					}
				}
			}
		} catch (Exception ignored) {
		}

		KeywordUtil.markFailedAndStop("❌ Falta credencial. Edita: Include/config/templet-credentials.properties → agrega MS_PASS=tuPassword")
		return ''
	}

	static TestObject xpathObject(String name, String xpath) {
		TestObject obj = new TestObject(name)
		obj.addProperty('xpath', ConditionType.EQUALS, xpath)
		return obj
	}

	static List<By> toByLocators(TestObject obj) {
		List<By> locators = []
		if (obj == null) return locators
		try {
			obj.getActiveProperties()?.each { prop ->
				String name = (prop?.getName() ?: '').toLowerCase(Locale.ROOT)
				String value = (prop?.getValue() ?: '').toString().trim()
				if (value.length() == 0) return
				switch (name) {
					case 'xpath':
						locators.add(By.xpath(value))
						break
					case 'css':
					case 'cssselector':
						locators.add(By.cssSelector(value))
						break
					case 'id':
						locators.add(By.id(value))
						break
					case 'name':
						locators.add(By.name(value))
						break
				}
			}
		} catch (Exception ignored) {
		}
		return locators
	}

	static WebElement findElementQuiet(TestObject obj, int timeoutSeconds) {
		if (!isBrowserSessionAlive()) return null
		long deadline = System.currentTimeMillis() + (Math.max(1, timeoutSeconds) * 1000L)
		while (System.currentTimeMillis() <= deadline) {
			try {
				WebDriver driver = DriverFactory.getWebDriver()
				if (driver == null) return null
				for (By locator : toByLocators(obj)) {
					List<WebElement> found = driver.findElements(locator)
					if (found != null && !found.isEmpty()) {
						return found[0]
					}
				}
			} catch (Exception ignored) {
			}
			try {
				Thread.sleep(200)
			} catch (Exception ignored) {
				break
			}
		}
		return null
	}

	static boolean clickIfPresent(TestObject obj, int timeoutSeconds) {
		try {
			WebElement target = findElementQuiet(obj, timeoutSeconds)
			if (target != null) {
				target.click()
				return true
			}
		} catch (Exception ignored) {
			try {
				WebUI.click(obj, FailureHandling.OPTIONAL)
				return true
			} catch (Exception ignoredAgain) {
			}
		}
		return false
	}

	static boolean clickFirstPresent(List<TestObject> candidates, int timeoutSeconds) {
		for (TestObject candidate : candidates) {
			if (clickIfPresent(candidate, timeoutSeconds)) {
				return true
			}
		}
		return false
	}

	static boolean isPresentQuiet(TestObject obj, int timeoutSeconds) {
		try {
			return findElementQuiet(obj, timeoutSeconds) != null
		} catch (Exception ignored) {
			return false
		}
	}

	@Keyword
	static String currentUrlSafe() {
		try {
			WebDriver driver = DriverFactory.getWebDriver()
			if (driver == null) {
				return 'SESSION_LOST'
			}
			String url = driver.getCurrentUrl()
			return (url != null && url.trim().length() > 0) ? url : 'SESSION_LOST'
		} catch (Exception ignored) {
			return 'SESSION_LOST'
		}
	}

	@Keyword
	static boolean isValidAppSession() {
		String currentUrl = currentUrlSafe()
		return currentUrl != 'SESSION_LOST' &&
			!currentUrl.contains('login.microsoftonline.com') &&
			!currentUrl.contains('loginlive.com') &&
			!currentUrl.contains('live.com')
	}

	@Keyword
	static String captureCaseScreenshot(String caseName, String label) {
		String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern('yyyyMMdd_HHmmss'))
		String screenshotDir = System.getProperty('user.dir') + '/Reports/Screenshots/functional-smoke'
		new File(screenshotDir).mkdirs()
		String path = screenshotDir + '/' + caseName + '_' + label + '_' + timestamp + '.png'
		if (!isBrowserSessionAlive()) {
			KeywordUtil.markWarning('[SCREENSHOT] Se omite captura porque la sesión del navegador no está activa: ' + label)
			return path
		}
		try {
			WebUI.takeScreenshot(path)
			KeywordUtil.logInfo('[SCREENSHOT] ' + path)
		} catch (Exception e) {
			KeywordUtil.markWarning('[SCREENSHOT] No se pudo capturar evidencia: ' + e.message)
		}
		return path
	}

	@Keyword
	static void safeCloseBrowser() {
		try {
			WebUI.closeBrowser()
		} catch (Exception ignored) {
		}
	}

	static List<String> snapshotHandlesSafe() {
		try {
			WebDriver driver = DriverFactory.getWebDriver()
			if (driver == null) return []
			return new ArrayList<String>(driver.getWindowHandles())
		} catch (Exception ignored) {
			return []
		}
	}

	static boolean switchToNewestTabIfPresent(List<String> beforeHandles, int waitSeconds = 3) {
		try {
			WebDriver driver = DriverFactory.getWebDriver()
			if (driver == null) return false
			for (int i = 0; i < Math.max(1, waitSeconds); i++) {
				List<String> nowHandles = new ArrayList<String>(driver.getWindowHandles())
				List<String> newHandles = nowHandles.findAll { String h -> !(beforeHandles ?: []).contains(h) }
				if (!newHandles.isEmpty()) {
					driver.switchTo().window(newHandles[-1])
					KeywordUtil.logInfo('[AUTH] Detectada pestaña SSO nueva; cambiando a esa pestaña para continuar login.')
					return true
				}
				WebUI.delay(1)
			}
		} catch (Exception ignored) {
		}
		return false
	}

	static void closeExtraTabsKeepCurrent(String reason = '') {
		try {
			WebDriver driver = DriverFactory.getWebDriver()
			if (driver == null) return
			String current = driver.getWindowHandle()
			List<String> handles = new ArrayList<String>(driver.getWindowHandles())
			if (handles.size() <= 1) return

			handles.each { String handle ->
				if (handle != current) {
					try {
						driver.switchTo().window(handle)
						driver.close()
					} catch (Exception ignored) {
					}
				}
			}
			try {
				driver.switchTo().window(current)
			} catch (Exception ignored) {
				List<String> remaining = new ArrayList<String>(driver.getWindowHandles())
				if (!remaining.isEmpty()) {
					driver.switchTo().window(remaining[0])
				}
			}
			if (reason) {
				KeywordUtil.logInfo('[AUTH] Pestañas extra cerradas. motivo=' + reason)
			}
		} catch (Exception ignored) {
		}
	}

	@Keyword
	static void openBrowserAndLoginWithMicrosoft(String targetUrl) {
		assertBrowserSupportForHost()
		String safeTargetUrl = (targetUrl ?: '').trim()
		if (!safeTargetUrl) {
			KeywordUtil.markFailedAndStop('[AUTH] URL destino vacía. Configure la GlobalVariable correspondiente o envíe fallbackUrl en el test case.')
		}
		String username = resolveCredential('MS_USER', 'USERNAME')
		String password = resolveCredential('MS_PASS', 'PASSWORD')

		WebUI.openBrowser('')
		WebUI.maximizeWindow()
		closeExtraTabsKeepCurrent('auth_start')
		performMicrosoftLoginWithCredentials(safeTargetUrl, username, password)
		closeExtraTabsKeepCurrent('auth_after_first_attempt')

		boolean sessionReady = isValidAppSession()
		for (int attempt = 1; !sessionReady && attempt <= 2; attempt++) {
			String currentUrl = currentUrlSafe()
			boolean onMicrosoftHost = currentUrl.contains('login.microsoftonline.com') || currentUrl.contains('microsoftonline.com') || currentUrl.contains('loginlive.com') || currentUrl.contains('live.com')
			KeywordUtil.logInfo('[AUTH] Sesión aún no válida. Reintento ' + attempt + '/2. URL actual=' + currentUrl)

			if (onMicrosoftHost) {
				performMicrosoftLoginWithCredentials(safeTargetUrl, username, password)
			} else {
				WebUI.navigateToUrl(safeTargetUrl)
				WebUI.waitForPageLoad(20)
				WebUI.delay(2)
			}
			closeExtraTabsKeepCurrent('auth_retry_' + attempt)
			sessionReady = isValidAppSession()
		}

		if (!sessionReady) {
			KeywordUtil.markFailedAndStop('No se completó el login Microsoft. URL final=' + currentUrlSafe())
		}
	}

	@Keyword
	static String resolveOptionalSetting(String variableName, String fallback = '') {
		if (variableName != null && variableName.trim()) {
			try {
				def metaProperty = GlobalVariable.metaClass.getMetaProperty(variableName)
				if (metaProperty != null) {
					def value = metaProperty.getProperty(GlobalVariable)
					if (value != null && value.toString().trim()) {
						return value.toString().trim()
					}
				}
			} catch (Exception ignored) {
			}
		}
		return fallback ?: ''
	}

	@Keyword
	static void validateDirectRedirect(Map config) {
		assertBrowserSupportForHost()
		String caseId = (config.caseId ?: 'TC-DIRECT-REDIRECT').toString()
		String platformLabel = (config.platformLabel ?: 'Portal').toString()
		String baseUrl = resolveOptionalSetting(config.urlVariableName?.toString(), config.fallbackUrl?.toString())
		String routePath = (config.routePath ?: '/').toString().trim()
		String directUrl = (config.directUrl ?: '').toString().trim()
		List<String> expectedUrlFragments = ((List) (config.expectedUrlFragments ?: [])).collect { it.toString() }.findAll { it != null && it.trim().length() > 0 }

		if (!baseUrl) {
			KeywordUtil.markFailedAndStop(caseId + ' falló: URL base vacía para validar redirección directa')
		}

		String targetUrl = directUrl
		if (!targetUrl) {
			if (!routePath || routePath == '/') {
				targetUrl = baseUrl
			} else {
				if (!routePath.startsWith('/')) {
					routePath = '/' + routePath
				}
				targetUrl = baseUrl.replaceAll('/+$', '') + routePath
			}
		}

		try {
			openBrowserAndLoginWithMicrosoft(baseUrl)
			WebUI.navigateToUrl(targetUrl)
			WebUI.waitForPageLoad(20)
			WebUI.delay(2)

			String currentUrl = currentUrlSafe()
			List<String> checks = []
			checks.add(targetUrl)
			if (routePath && routePath != '/') {
				checks.add(routePath)
			}
			checks.addAll(expectedUrlFragments)

			boolean matched = checks.any { String token ->
				token != null && token.trim().length() > 0 && currentUrl.contains(token.trim())
			}

			captureCaseScreenshot(caseId, 'direct_redirect')

			if (!matched) {
				KeywordUtil.markFailed(caseId + ' falló: redirección no coincide. esperado=' + checks.join(' | ') + ' actual=' + currentUrl)
			} else {
				KeywordUtil.markPassed(caseId + ' OK. Redirección directa validada en ' + platformLabel + ' → ' + currentUrl)
			}
		} finally {
			safeCloseBrowser()
		}
	}

	@Keyword
	static void validateDirectRedirectBatch(Map config) {
		assertBrowserSupportForHost()
		String caseId = (config.caseId ?: 'TC-DIRECT-REDIRECT-BATCH').toString()
		String platformLabel = (config.platformLabel ?: 'Portal').toString()
		String baseUrl = resolveOptionalSetting(config.urlVariableName?.toString(), config.fallbackUrl?.toString())
		List<Map> routes = ((List) (config.routes ?: [])).findAll { it instanceof Map }
		List<String> failures = []

		if (!baseUrl) {
			KeywordUtil.markFailedAndStop(caseId + ' falló: URL base vacía para validar redirecciones directas')
		}
		if (routes.isEmpty()) {
			KeywordUtil.markFailedAndStop(caseId + ' falló: no se recibieron rutas para validar')
		}

		try {
			openBrowserAndLoginWithMicrosoft(baseUrl)

			routes.each { Map route ->
				String label = (route.label ?: route.routePath ?: route.directUrl ?: 'route').toString()
				String routePath = (route.routePath ?: '/').toString().trim()
				String directUrl = (route.directUrl ?: '').toString().trim()
				List<String> expectedUrlFragments = ((List) (route.expectedUrlFragments ?: [])).collect { it.toString() }.findAll { it != null && it.trim().length() > 0 }

				String targetUrl = directUrl
				if (!targetUrl) {
					if (!routePath || routePath == '/') {
						targetUrl = baseUrl
					} else {
						if (!routePath.startsWith('/')) {
							routePath = '/' + routePath
						}
						targetUrl = baseUrl.replaceAll('/+$', '') + routePath
					}
				}

				WebUI.navigateToUrl(targetUrl)
				WebUI.waitForPageLoad(20)
				WebUI.delay(2)

				String currentUrl = currentUrlSafe()
				List<String> checks = []
				checks.add(targetUrl)
				if (routePath && routePath != '/') {
					checks.add(routePath)
				}
				checks.addAll(expectedUrlFragments)

				boolean matched = checks.any { String token ->
					token != null && token.trim().length() > 0 && currentUrl.contains(token.trim())
				}

				captureCaseScreenshot(caseId, 'direct_' + label.replaceAll('[^a-zA-Z0-9]+', '_').toLowerCase())

				if (!matched) {
					failures.add(label + ' -> esperado=' + checks.join(' | ') + ' actual=' + currentUrl)
				} else {
					KeywordUtil.logInfo('[DIRECT] ' + label + ' OK -> ' + currentUrl)
				}
			}

			if (failures.isEmpty()) {
				KeywordUtil.markPassed(caseId + ' OK. Redirecciones directas validadas en ' + platformLabel)
			} else {
				KeywordUtil.markFailed(caseId + ' falló: ' + failures.join(' || '))
			}
		} finally {
			safeCloseBrowser()
		}
	}

	static boolean resolveBooleanSetting(Object rawValue, boolean defaultValue = true) {
		if (rawValue == null) {
			return defaultValue
		}
		if (rawValue instanceof Boolean) {
			return ((Boolean) rawValue).booleanValue()
		}
		if (rawValue instanceof Number) {
			return ((Number) rawValue).intValue() != 0
		}
		String text = rawValue.toString().trim().toLowerCase()
		if (text.length() == 0) {
			return defaultValue
		}
		if (['false', '0', 'no', 'off', 'n'].contains(text)) {
			return false
		}
		if (['true', '1', 'yes', 'on', 'y'].contains(text)) {
			return true
		}
		return defaultValue
	}

	@Keyword
	static void runPublicLandingSmoke(Map config) {
		assertBrowserSupportForHost()
		String caseId = (config.caseId ?: 'TC-PUBLIC-SMOKE').toString()
		String platformLabel = (config.platformLabel ?: 'Portal').toString()
		String targetUrl = resolveOptionalSetting(config.urlVariableName?.toString(), config.fallbackUrl?.toString())
		String expectedUrlFragment = (config.expectedUrlFragment ?: '').toString()
		List<String> failures = []

		try {
			WebUI.openBrowser('')
			WebUI.maximizeWindow()
			WebUI.navigateToUrl(targetUrl)
			WebUI.waitForPageLoad(20)
			WebUI.delay(1)

			String currentUrl = currentUrlSafe()
			String pageTitle = ''
			try {
				pageTitle = WebUI.getWindowTitle()
			} catch (Exception ignored) {
			}

			KeywordUtil.logInfo('[SMOKE] ' + platformLabel + ' currentUrl=' + currentUrl + ' title=' + pageTitle)
			captureCaseScreenshot(caseId, 'landing')

			if (!currentUrl || currentUrl == 'SESSION_LOST') {
				failures.add('No se pudo obtener URL actual')
			}
			if (expectedUrlFragment && !currentUrl.contains(expectedUrlFragment)) {
				failures.add('URL inesperada: ' + currentUrl)
			}

			if (failures.isEmpty()) {
				KeywordUtil.markPassed(caseId + ' OK. Landing pública de ' + platformLabel + ' accesible en ' + currentUrl)
			} else {
				KeywordUtil.markFailed(caseId + ' falló: ' + failures.join(' | '))
			}
		} finally {
			safeCloseBrowser()
		}
	}

	@Keyword
	static void runPublicSignInSmoke(Map config) {
		assertBrowserSupportForHost()
		String caseId = (config.caseId ?: 'TC-PUBLIC-FUNCTIONAL-SMOKE').toString()
		String platformLabel = (config.platformLabel ?: 'Portal').toString()
		String targetUrl = resolveOptionalSetting(config.urlVariableName?.toString(), config.fallbackUrl?.toString())
		String expectedUrlFragment = (config.expectedUrlFragment ?: '').toString()
		List<Map> requiredChecks = (List<Map>) (config.requiredChecks ?: [])
		List<String> failures = []

		try {
			WebUI.openBrowser('')
			WebUI.maximizeWindow()
			WebUI.navigateToUrl(targetUrl)
			WebUI.waitForPageLoad(20)
			WebUI.delay(1)

			String currentUrl = currentUrlSafe()
			KeywordUtil.logInfo('[FUNCTIONAL-SMOKE] ' + platformLabel + ' currentUrl=' + currentUrl)
			captureCaseScreenshot(caseId, 'landing')

			if (expectedUrlFragment && !currentUrl.contains(expectedUrlFragment)) {
				failures.add('URL inesperada: ' + currentUrl)
			}

			for (Map check : requiredChecks) {
				String label = (check.label ?: check.name ?: 'check').toString()
				String xpath = (check.xpath ?: '').toString()
				int timeoutSeconds = ((check.timeoutSeconds ?: 10) as Integer)
				boolean ok
				if (Boolean.TRUE.equals(check.compareText)) {
					ok = verifyXPathText(label, xpath, (check.expected ?: '').toString(), timeoutSeconds)
				} else {
					ok = verifyXPathPresent(label, xpath, timeoutSeconds)
				}
				KeywordUtil.logInfo('[CHECK] ' + platformLabel + ' ' + label + ' => ' + ok)
				if (!ok) {
					failures.add(label + ' no visible o con texto inesperado')
				}
			}

			if (failures.isEmpty()) {
				KeywordUtil.markPassed(caseId + ' OK. Landing pública de ' + platformLabel + ' validada.')
			} else {
				KeywordUtil.markFailed(caseId + ' falló: ' + failures.join(' | '))
			}
		} finally {
			safeCloseBrowser()
		}
	}

	static Map<String, Object> clickXPathAndCollectState(String xpath) {
		Map<String, Object> result = (Map<String, Object>) WebUI.executeJavaScript('''
			var xpath = arguments[0];
			var node = document.evaluate(xpath, document, null, XPathResult.FIRST_ORDERED_NODE_TYPE, null).singleNodeValue;
			if (!node) {
				return { found: false, visible: false, clicked: false, href: '', text: '', tagName: '' };
			}
			var host = node.closest('a,button,[role="button"]') || node;
			var visible = true;
			if (host instanceof HTMLElement) {
				var st = window.getComputedStyle(host);
				visible = st.display !== 'none' && st.visibility !== 'hidden';
			}
			var href = host.getAttribute ? (host.getAttribute('href') || '') : '';
			var text = (host.textContent || '').trim();
			if (host.scrollIntoView) {
				host.scrollIntoView({ block: 'center', inline: 'nearest' });
			}
			['mousedown', 'mouseup', 'click'].forEach(function(evt) {
				host.dispatchEvent(new MouseEvent(evt, { bubbles: true, cancelable: true }));
			});
			return {
				found: true,
				visible: visible,
				clicked: true,
				href: href,
				text: text,
				tagName: (host.tagName || '').toLowerCase()
			};
		''', [xpath])
		return result ?: [found: false, visible: false, clicked: false, href: '', text: '', tagName: '']
	}

	static boolean reopenLandingHome(String homeUrl, String homeMarkerXPath, int timeoutSeconds = 15) {
		WebUI.navigateToUrl(homeUrl)
		WebUI.waitForPageLoad(timeoutSeconds)
		WebUI.delay(1)
		if (homeMarkerXPath != null && homeMarkerXPath.trim()) {
			return verifyXPathPresent('landing_home_marker', homeMarkerXPath, timeoutSeconds)
		}
		return currentUrlSafe()?.contains(homeUrl)
	}

	@Keyword
	static void verifyLandingVisibleObjects(Map config) {
		assertBrowserSupportForHost()
		String caseId = (config.caseId ?: 'TC-LANDING-OBJECTS-VISIBLE').toString()
		String platformLabel = (config.platformLabel ?: 'Portal').toString()
		String targetUrl = resolveOptionalSetting(config.urlVariableName?.toString(), config.fallbackUrl?.toString())
		List<Map<String, Object>> requiredObjects = (List<Map<String, Object>>) (config.requiredObjects ?: [])
		List<String> failures = []

		try {
			WebUI.openBrowser('')
			WebUI.maximizeWindow()
			WebUI.navigateToUrl(targetUrl)
			WebUI.waitForPageLoad(20)
			WebUI.delay(1)

			requiredObjects.each { Map<String, Object> obj ->
				String label = (obj.label ?: obj.name ?: 'object').toString()
				String xpath = (obj.xpath ?: '').toString()
				int timeoutSeconds = ((obj.timeoutSeconds ?: 10) as Integer)
				boolean visible = Boolean.TRUE.equals(verifyXPathPresent(label, xpath, timeoutSeconds))
				KeywordUtil.logInfo('[VISIBLE] ' + platformLabel + ' ' + label + ' => ' + visible)
				if (!visible) {
					failures.add(label + ' no visible')
				}
			}

			captureCaseScreenshot(caseId, 'visible_objects')

			if (failures.isEmpty()) {
				KeywordUtil.markPassed(caseId + ' OK. Objetos visibles validados en ' + platformLabel)
			} else {
				KeywordUtil.markFailed(caseId + ' falló: ' + failures.join(' | '))
			}
		} finally {
			safeCloseBrowser()
		}
	}

	@Keyword
	static void clickLandingObjectsAndReturnHome(Map config) {
		assertBrowserSupportForHost()
		String caseId = (config.caseId ?: 'TC-LANDING-OBJECTS-CLICK').toString()
		String platformLabel = (config.platformLabel ?: 'Portal').toString()
		String targetUrl = resolveOptionalSetting(config.urlVariableName?.toString(), config.fallbackUrl?.toString())
		String homeMarkerXPath = (config.homeMarkerXPath ?: '').toString()
		List<Map<String, Object>> clickableObjects = (List<Map<String, Object>>) (config.clickableObjects ?: [])
		List<String> failures = []
		List<String> warnings = []

		try {
			WebUI.openBrowser('')
			WebUI.maximizeWindow()
			if (!reopenLandingHome(targetUrl, homeMarkerXPath, 20)) {
				KeywordUtil.markFailedAndStop(caseId + ' no pudo abrir el home inicial de ' + platformLabel)
			}

			clickableObjects.each { Map<String, Object> obj ->
				String label = (obj.label ?: obj.name ?: 'object').toString()
				String xpath = (obj.xpath ?: '').toString()
				String responseType = (obj.responseType ?: 'navigation').toString()
				String expectedHrefContains = (obj.expectedHrefContains ?: '').toString()
				List<String> expectedUrlFragments = ((List) (obj.expectedUrlFragments ?: [])).collect { it.toString() }
				String beforeUrl = currentUrlSafe()
				Map<String, Object> clickState = clickXPathAndCollectState(xpath)
				WebUI.delay(2)
				String afterUrl = currentUrlSafe()

				if (!Boolean.TRUE.equals(clickState.found) || !Boolean.TRUE.equals(clickState.visible) || !Boolean.TRUE.equals(clickState.clicked)) {
					failures.add(label + ' no se pudo clickear')
					reopenLandingHome(targetUrl, homeMarkerXPath, 15)
					return
				}

				boolean ok = false
				if ('mailto'.equalsIgnoreCase(responseType)) {
					ok = expectedHrefContains ? ((clickState.href ?: '').contains(expectedHrefContains)) : ((clickState.href ?: '').startsWith('mailto:'))
					if (!ok) {
						failures.add(label + ' no expone href esperado: ' + clickState.href)
					}
				} else {
					boolean urlChanged = beforeUrl != afterUrl
					boolean fragmentMatched = expectedUrlFragments.isEmpty() ? urlChanged : expectedUrlFragments.any { afterUrl.contains(it) }
					ok = urlChanged || fragmentMatched
					if (!ok) {
						failures.add(label + ' no mostró navegación esperada. before=' + beforeUrl + ' after=' + afterUrl)
					}
				}

				captureCaseScreenshot(caseId, 'click_' + label.replaceAll('[^a-zA-Z0-9]+', '_').toLowerCase())

				if (!reopenLandingHome(targetUrl, homeMarkerXPath, 20)) {
					failures.add(label + ' no pudo volver al home')
				} else if (!ok) {
					warnings.add(label + ' volvió al home pero sin señal fuerte de navegación')
				}
			}

			if (!warnings.isEmpty()) {
				KeywordUtil.markWarning(caseId + ' warnings: ' + warnings.join(' | '))
			}

			if (failures.isEmpty()) {
				KeywordUtil.markPassed(caseId + ' OK. Clicks visibles validados y retorno al home confirmado en ' + platformLabel)
			} else {
				KeywordUtil.markFailed(caseId + ' falló: ' + failures.join(' | '))
			}
		} finally {
			safeCloseBrowser()
		}
	}

	static List<Map<String, Object>> discoverVisibleSafeClickables(List<String> excludeTokens = [], int maxCandidates = 30) {
		List<Map<String, Object>> result = (List<Map<String, Object>>) WebUI.executeJavaScript('''
			var excluded = (arguments[0] || []).map(function(x) { return (x || '').toLowerCase().trim(); }).filter(Boolean);
			var maxCandidates = arguments[1] || 30;
			var normalizeSpace = function(value) {
				var raw = String(value || '');
				var parts = [];
				var current = '';
				for (var idx = 0; idx < raw.length; idx++) {
					var ch = raw.charAt(idx);
					var code = raw.charCodeAt(idx);
					var isWhitespace = code === 9 || code === 10 || code === 13 || code === 32;
					if (isWhitespace) {
						if (current.length > 0) {
							parts.push(current);
							current = '';
						}
					} else {
						current += ch;
					}
				}
				if (current.length > 0) {
					parts.push(current);
				}
				return parts.join(' ').trim();
			};
			var visible = function(el) {
				if (!(el instanceof HTMLElement)) return false;
				var st = window.getComputedStyle(el);
				return st.display !== 'none' && st.visibility !== 'hidden' && el.offsetParent !== null && !el.disabled;
			};
			var cssPath = function(el) {
				if (!(el instanceof Element)) return '';
				if (el.id) return '#' + CSS.escape(el.id);
				var parts = [];
				while (el && el.nodeType === 1 && el !== document.body) {
					var tag = el.nodeName.toLowerCase();
					var index = 1;
					var sibling = el.previousElementSibling;
					while (sibling) {
						if (sibling.nodeName.toLowerCase() === tag) index++;
						sibling = sibling.previousElementSibling;
					}
					parts.unshift(tag + ':nth-of-type(' + index + ')');
					el = el.parentElement;
				}
				parts.unshift('body');
				return parts.join(' > ');
			};
			var labelOf = function(el) {
				var text = normalizeSpace(el.innerText || el.textContent || '');
				var aria = (el.getAttribute('aria-label') || '').trim();
				var title = (el.getAttribute('title') || '').trim();
				return text || aria || title;
			};
			var elements = Array.from(document.querySelectorAll('a[href], button, [role="button"], [onclick], [data-testid]'));
			var dedupe = new Set();
			var items = [];
			for (var i = 0; i < elements.length; i++) {
				var el = elements[i];
				var host = el.closest('a,button,[role="button"]') || el;
				if (!visible(host)) continue;
				var label = labelOf(host);
				var href = (host.getAttribute('href') || '').trim();
				var haystack = (label + ' ' + href).toLowerCase();
				if (!label && !href) continue;
				if (excluded.some(function(token) { return haystack.indexOf(token) >= 0; })) continue;
				var path = cssPath(host);
				if (!path || dedupe.has(path)) continue;
				dedupe.add(path);
				items.push({
					label: label,
					href: href,
					cssPath: path,
					tagName: (host.tagName || '').toLowerCase()
				});
				if (items.length >= maxCandidates) break;
			}
			return items;
		''', [excludeTokens ?: [], maxCandidates])
		return result ?: []
	}

	static Map<String, Object> clickCssPath(String cssPath) {
		Map<String, Object> result = (Map<String, Object>) WebUI.executeJavaScript('''
			var cssPath = arguments[0];
			var normalizeSpace = function(value) {
				var raw = String(value || '');
				var parts = [];
				var current = '';
				for (var idx = 0; idx < raw.length; idx++) {
					var ch = raw.charAt(idx);
					var code = raw.charCodeAt(idx);
					var isWhitespace = code === 9 || code === 10 || code === 13 || code === 32;
					if (isWhitespace) {
						if (current.length > 0) {
							parts.push(current);
							current = '';
						}
					} else {
						current += ch;
					}
				}
				if (current.length > 0) {
					parts.push(current);
				}
				return parts.join(' ').trim();
			};
			var el = document.querySelector(cssPath);
			if (!el) {
				return { found: false, clicked: false, href: '', label: '' };
			}
			var host = el.closest('a,button,[role="button"]') || el;
			var label = normalizeSpace(host.innerText || host.textContent || host.getAttribute('aria-label') || host.getAttribute('title') || '');
			var href = (host.getAttribute('href') || '').trim();
			if (host.scrollIntoView) {
				host.scrollIntoView({ block: 'center', inline: 'nearest' });
			}
			['mousedown', 'mouseup', 'click'].forEach(function(evt) {
				host.dispatchEvent(new MouseEvent(evt, { bubbles: true, cancelable: true }));
			});
			return { found: true, clicked: true, href: href, label: label };
		''', [cssPath])
		return result ?: [found: false, clicked: false, href: '', label: '']
	}

	@Keyword
	static void verifyAuthenticatedVisibleObjects(Map config) {
		assertBrowserSupportForHost()
		String caseId = (config.caseId ?: 'TC-AUTH-OBJECTS-VISIBLE').toString()
		String platformLabel = (config.platformLabel ?: 'Portal').toString()
		String targetUrl = resolveOptionalSetting(config.urlVariableName?.toString(), config.fallbackUrl?.toString())
		int minCandidates = ((config.minCandidates ?: 1) as Integer)
		int maxCandidates = ((config.maxCandidates ?: 30) as Integer)
		List<String> excludeTokens = ((List) (config.excludeTokens ?: [])).collect { it.toString() }
		List<String> failures = []

		try {
			openBrowserAndLoginWithMicrosoft(targetUrl)
			String homeUrl = currentUrlSafe()
			List<Map<String, Object>> clickables = discoverVisibleSafeClickables(excludeTokens, maxCandidates)
			KeywordUtil.logInfo('[AUTH] ' + platformLabel + ' homeUrl=' + homeUrl + ' clickables=' + clickables.size())
			clickables.each { Map<String, Object> item ->
				KeywordUtil.logInfo('[AUTH][VISIBLE] ' + platformLabel + ' -> ' + ((item.label ?: item.href ?: item.cssPath) as String))
			}

			if (!isValidAppSession()) {
				failures.add('Login no quedó en sesión válida. URL=' + homeUrl)
			}
			if (clickables.size() < minCandidates) {
				failures.add('Se encontraron pocos objetos visibles tras login: ' + clickables.size() + ' < ' + minCandidates)
			}

			captureCaseScreenshot(caseId, 'authenticated_home')

			if (failures.isEmpty()) {
				KeywordUtil.markPassed(caseId + ' OK. Home autenticado visible en ' + platformLabel + ' con ' + clickables.size() + ' objetos clicables.')
			} else {
				KeywordUtil.markFailed(caseId + ' falló: ' + failures.join(' | '))
			}
		} finally {
			safeCloseBrowser()
		}
	}

	@Keyword
	static void clickAuthenticatedVisibleObjectsAndReturnHome(Map config) {
		assertBrowserSupportForHost()
		String caseId = (config.caseId ?: 'TC-AUTH-OBJECTS-CLICK').toString()
		String platformLabel = (config.platformLabel ?: 'Portal').toString()
		String targetUrl = resolveOptionalSetting(config.urlVariableName?.toString(), config.fallbackUrl?.toString())
		int minCandidates = ((config.minCandidates ?: 1) as Integer)
		int maxCandidates = ((config.maxCandidates ?: 30) as Integer)
		List<String> excludeTokens = ((List) (config.excludeTokens ?: [])).collect { it.toString() }
		List<String> failures = []
		List<String> warnings = []

		try {
			openBrowserAndLoginWithMicrosoft(targetUrl)
			String homeUrl = currentUrlSafe()
			List<Map<String, Object>> clickables = discoverVisibleSafeClickables(excludeTokens, maxCandidates)
			if (clickables.size() < minCandidates) {
				KeywordUtil.markFailedAndStop(caseId + ' encontró pocos objetos visibles tras login: ' + clickables.size())
			}

			clickables.each { Map<String, Object> item ->
				String label = ((item.label ?: item.href ?: item.cssPath) ?: 'object').toString()
				String beforeUrl = currentUrlSafe()
				Map<String, Object> clickResult = clickCssPath((item.cssPath ?: '').toString())
				WebUI.waitForPageLoad(8)
				WebUI.delay(1)
				String afterUrl = currentUrlSafe()
				boolean observableSignal = beforeUrl != afterUrl
				String href = (clickResult.href ?: '').toString()

				if (!Boolean.TRUE.equals(clickResult.found) || !Boolean.TRUE.equals(clickResult.clicked)) {
					failures.add(label + ' no se pudo clickear')
				} else if (href.startsWith('mailto:')) {
					KeywordUtil.logInfo('[AUTH][CLICK] ' + platformLabel + ' ' + label + ' -> mailto OK')
				} else if (observableSignal) {
					KeywordUtil.logInfo('[AUTH][CLICK] ' + platformLabel + ' ' + label + ' before=' + beforeUrl + ' after=' + afterUrl)
				} else {
					warnings.add(label + ' sin cambio de URL observable tras click')
				}

				captureCaseScreenshot(caseId, 'click_' + label.replaceAll('[^a-zA-Z0-9]+', '_').toLowerCase())

				WebUI.navigateToUrl(homeUrl)
				WebUI.waitForPageLoad(15)
				WebUI.delay(1)
				if (!isValidAppSession()) {
					failures.add(label + ' no pudo volver al home autenticado')
				}
			}

			if (!warnings.isEmpty()) {
				KeywordUtil.markWarning(caseId + ' warnings: ' + warnings.join(' | '))
			}

			if (failures.isEmpty()) {
				KeywordUtil.markPassed(caseId + ' OK. Clicks del home autenticado validados y retorno confirmado en ' + platformLabel)
			} else {
				KeywordUtil.markFailed(caseId + ' falló: ' + failures.join(' | '))
			}
		} finally {
			safeCloseBrowser()
		}
	}

	static String collapseSpaces(String value) {
		if (value == null) return ''
		return value
			.replace('\t', ' ')
			.replace('\n', ' ')
			.replace('\r', ' ')
			.split(' ')
			.findAll { it != null && it.trim().length() > 0 }
			.join(' ')
			.trim()
	}

	static String sanitizeForXPathContains(String value) {
		return collapseSpaces(value)
			.toLowerCase()
			.replace("'", " ")
			.replace('"', ' ')
			.trim()
	}

	static String sidebarTextXPathExact(String normalizedLowerText) {
		return "//*[self::a or self::button or @role='button' or self::span or self::div][translate(normalize-space(.), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz') = '${normalizedLowerText}']"
	}

	static String sidebarTextXPathContains(String normalizedLowerText) {
		return "//*[self::a or self::button or @role='button' or self::span or self::div][contains(translate(normalize-space(.), 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), '${normalizedLowerText}')]"
	}

	static boolean isBrowserSessionAlive() {
		try {
			WebDriver driver = DriverFactory.getWebDriver()
			if (driver == null) return false
			driver.getWindowHandle()
			return true
		} catch (Exception ignored) {
			return false
		}
	}

	static boolean clickHamburgerIfPresent() {
		try {
			Boolean clicked = (Boolean) WebUI.executeJavaScript('''
				var visible = function(el) {
					if (!(el instanceof HTMLElement)) return false;
					var st = window.getComputedStyle(el);
					return st.display !== 'none' && st.visibility !== 'hidden' && el.offsetParent !== null;
				};
				var clickNode = function(node) {
					if (!node) return false;
					node.scrollIntoView({ block: 'center', inline: 'nearest' });
					['mousedown', 'mouseup', 'click'].forEach(function(evt) {
						node.dispatchEvent(new MouseEvent(evt, { bubbles: true, cancelable: true }));
					});
					return true;
				};

				// Prioridad 1: botón exacto compartido (data-slot="button" + icono lucide-menu)
				var exactButtons = Array.from(document.querySelectorAll('button[data-slot="button"]')).filter(function(btn) {
					if (!visible(btn)) return false;
					return btn.querySelector('svg.lucide.lucide-menu, svg.lucide-menu') != null;
				});
				if (exactButtons.length > 0) {
					if (clickNode(exactButtons[0])) return true;
				}

				var selectors = [
					'button[aria-label*="menu" i]',
					'button[title*="menu" i]',
					'button[aria-label*="navigation" i]',
					'button[title*="navigation" i]',
					'button[aria-label*="sidebar" i]',
					'button[title*="sidebar" i]',
					'button[aria-label*="drawer" i]',
					'button[title*="drawer" i]',
					'button[class*="menu"]',
					'button[class*="hamb"]',
					'button[class*="sidebar"]',
					'button[class*="drawer"]',
					'button[class*="toggle"]',
					'[aria-controls*="sidebar" i]',
					'[data-testid*="sidebar" i]',
					'[data-testid*="menu"]',
					'.hamburger',
					'.menu-toggle',
					'.navbar-toggler'
				];
				for (var i = 0; i < selectors.length; i++) {
					var nodes = Array.from(document.querySelectorAll(selectors[i]));
					var node = nodes.find(visible);
					if (!node) continue;
					if (clickNode(node)) return true;
				}

				// Fallback: botón pequeño visible en esquina superior izquierda.
				// Útil cuando el ícono de hamburguesa no expone atributos semánticos.
				var candidates = Array.from(document.querySelectorAll('button,[role="button"],a,div,span')).filter(function(el) {
					if (!visible(el)) return false;
					var rect = el.getBoundingClientRect();
					if (rect.left > 180 || rect.top > 180) return false;
					if (rect.width <= 0 || rect.height <= 0) return false;
					if (rect.width > 90 || rect.height > 90) return false;
					var attrs = (
						String(el.className || '') + ' ' +
						String(el.getAttribute('aria-label') || '') + ' ' +
						String(el.getAttribute('title') || '') + ' ' +
						String(el.getAttribute('data-testid') || '')
					).toLowerCase();
					var score = 0;
					if (attrs.indexOf('menu') >= 0) score += 3;
					if (attrs.indexOf('hamb') >= 0) score += 3;
					if (attrs.indexOf('sidebar') >= 0) score += 3;
					if (attrs.indexOf('toggle') >= 0) score += 2;
					if (attrs.indexOf('drawer') >= 0) score += 2;
					el.__menuScore = score;
					return true;
				});

				if (candidates.length > 0) {
					candidates.sort(function(a, b) {
						if ((b.__menuScore || 0) !== (a.__menuScore || 0)) return (b.__menuScore || 0) - (a.__menuScore || 0);
						var ra = a.getBoundingClientRect();
						var rb = b.getBoundingClientRect();
						return (ra.left + ra.top) - (rb.left + rb.top);
					});
					if (clickNode(candidates[0])) return true;
				}

				return false;
			''', null)
			return Boolean.TRUE.equals(clicked)
		} catch (Exception ignored) {
			return false
		}
	}

	static Map<String, Object> sidebarItemsState(List<String> expectedItems) {
		try {
			Map<String, Object> state = (Map<String, Object>) WebUI.executeJavaScript('''
			var expected = arguments[0] || [];
			var normalize = function(value) {
				var raw = String(value || '');
				var parts = [];
				var current = '';
				for (var idx = 0; idx < raw.length; idx++) {
					var code = raw.charCodeAt(idx);
					var ch = raw.charAt(idx);
					var ws = code === 9 || code === 10 || code === 13 || code === 32;
					if (ws) {
						if (current.length > 0) {
							parts.push(current);
							current = '';
						}
					} else {
						current += ch;
					}
				}
				if (current.length > 0) parts.push(current);
				return parts.join(' ').trim();
			};
			var visible = function(el) {
				if (!(el instanceof HTMLElement)) return false;
				var st = window.getComputedStyle(el);
				return st.display !== 'none' && st.visibility !== 'hidden' && el.offsetParent !== null;
			};
			var expectedMap = {};
			expected.forEach(function(item) { expectedMap[normalize(item)] = true; });
			var candidates = Array.from(document.querySelectorAll('a,button,[role="button"],li,div,span'));
			var found = {};
			candidates.forEach(function(node) {
				if (!visible(node)) return;
				var rect = node.getBoundingClientRect();
				if (rect.left > (window.innerWidth * 0.55)) return;
				var text = normalize(node.textContent || '');
				if (!expectedMap[text]) return;
				if (found[text]) return;
				var top = rect.top;
				found[text] = { top: top };
			});
			var foundItems = [];
			var missingItems = [];
			var tops = [];
			expected.forEach(function(item) {
				var key = normalize(item);
				if (found[key]) {
					foundItems.push(item);
					tops.push(found[key].top);
				} else {
					missingItems.push(item);
				}
			});
			var orderOk = true;
			for (var i = 1; i < tops.length; i++) {
				if (tops[i] < tops[i - 1]) {
					orderOk = false;
					break;
				}
			}
			return { foundItems: foundItems, missingItems: missingItems, orderOk: orderOk };
			''', [expectedItems ?: []])
			Map<String, Object> safeState = state ?: [foundItems: [], missingItems: expectedItems ?: [], orderOk: false]
			List foundItems = (List) (safeState.foundItems ?: [])
			if (!foundItems.isEmpty()) {
				return safeState
			}
		} catch (Exception ignored) {
		}

		List<String> foundFallback = []
		List<String> missingFallback = []
		for (String expected : (expectedItems ?: [])) {
			String normalized = sanitizeForXPathContains(expected)
			if (normalized.length() == 0) continue
			boolean present = isPresentQuiet(
				xpathObject('sidebar_state_exact_' + normalized, sidebarTextXPathExact(normalized)),
				2
			)
			if (!present) {
				present = isPresentQuiet(
					xpathObject('sidebar_state_contains_' + normalized, sidebarTextXPathContains(normalized)),
					2
				)
			}
			if (present) {
				foundFallback.add(expected)
			} else {
				missingFallback.add(expected)
			}
		}
		return [foundItems: foundFallback, missingItems: missingFallback, orderOk: true]
	}

	static List<String> discoverSidebarItems(int maxItems = 40, List<String> excludeTokens = []) {
		try {
			List<String> discovered = (List<String>) WebUI.executeJavaScript('''
			var maxItems = arguments[0] || 40;
			var excluded = (arguments[1] || []).map(function(x) {
				return String(x || '').toLowerCase().trim();
			}).filter(Boolean);
			var normalize = function(value) {
				var raw = String(value || '');
				var parts = [];
				var current = '';
				for (var idx = 0; idx < raw.length; idx++) {
					var code = raw.charCodeAt(idx);
					var ch = raw.charAt(idx);
					var ws = code === 9 || code === 10 || code === 13 || code === 32;
					if (ws) {
						if (current.length > 0) {
							parts.push(current);
							current = '';
						}
					} else {
						current += ch;
					}
				}
				if (current.length > 0) parts.push(current);
				return parts.join(' ').trim();
			};
			var visible = function(el) {
				if (!(el instanceof HTMLElement)) return false;
				var st = window.getComputedStyle(el);
				return st.display !== 'none' && st.visibility !== 'hidden' && el.offsetParent !== null;
			};

			var nodes = Array.from(document.querySelectorAll('a,button,[role="button"],li,div,span'));
			var candidates = [];
			nodes.forEach(function(node) {
				if (!visible(node)) return;
				var rect = node.getBoundingClientRect();
				if (rect.left > (window.innerWidth * 0.55)) return;
				if (rect.width < 6 || rect.height < 6) return;
				var text = normalize(node.textContent || '');
				if (!text || text.length > 80) return;
				var key = text.toLowerCase();
				if (excluded.some(function(token) { return key.indexOf(token) >= 0; })) return;
				candidates.push({ text: text, key: key, top: rect.top, left: rect.left });
			});

			candidates.sort(function(a, b) {
				if (a.top !== b.top) return a.top - b.top;
				return a.left - b.left;
			});

			var unique = [];
			var seen = {};
			for (var i = 0; i < candidates.length; i++) {
				var item = candidates[i];
				if (seen[item.key]) continue;
				seen[item.key] = true;
				unique.push(item.text);
				if (unique.length >= maxItems) break;
			}

			return unique;
			''', [maxItems, excludeTokens ?: []])

			if (discovered == null) {
				return []
			}

			return discovered.collect { collapseSpaces(it?.toString()) }.findAll { it.length() > 0 }
		} catch (Exception ignored) {
			return []
		}
	}

	static boolean isSidebarItemVisible(String itemText) {
		if (!isBrowserSessionAlive()) {
			return false
		}
		try {
			Boolean visible = (Boolean) WebUI.executeJavaScript('''
			var target = String(arguments[0] || '');
			var normalize = function(value) {
				var raw = String(value || '');
				var parts = [];
				var current = '';
				for (var idx = 0; idx < raw.length; idx++) {
					var code = raw.charCodeAt(idx);
					var ch = raw.charAt(idx);
					var ws = code === 9 || code === 10 || code === 13 || code === 32;
					if (ws) {
						if (current.length > 0) {
							parts.push(current);
							current = '';
						}
					} else {
						current += ch;
					}
				}
				if (current.length > 0) parts.push(current);
				return parts.join(' ').trim();
			};
			var targetNorm = normalize(target).toLowerCase();
			var elements = Array.from(document.querySelectorAll('a,button,[role="button"],li,div,span'));
			for (var i = 0; i < elements.length; i++) {
				var el = elements[i];
				if (!(el instanceof HTMLElement)) continue;
				var st = window.getComputedStyle(el);
				if (st.display === 'none' || st.visibility === 'hidden' || el.offsetParent === null) continue;
				var rect = el.getBoundingClientRect();
				if (rect.left > (window.innerWidth * 0.55)) continue;
				var text = normalize(el.textContent || '').toLowerCase();
				if (!text) continue;
				if (text === targetNorm || text.indexOf(targetNorm) >= 0 || targetNorm.indexOf(text) >= 0) {
					return true;
				}
			}
			return false;
			''', [itemText])
			if (Boolean.TRUE.equals(visible)) {
				return true
			}
		} catch (Exception ignored) {
		}

		String normalized = sanitizeForXPathContains(itemText)
		if (normalized.length() == 0) return false
		boolean present = isPresentQuiet(
			xpathObject('sidebar_visible_exact_' + normalized, sidebarTextXPathExact(normalized)),
			2
		)
		if (!present) {
			present = isPresentQuiet(
				xpathObject('sidebar_visible_contains_' + normalized, sidebarTextXPathContains(normalized)),
				2
			)
		}
		return present
	}

	static boolean ensureSidebarOpenForItem(String itemText) {
		if (!isBrowserSessionAlive()) {
			return false
		}
		if (isSidebarItemVisible(itemText)) {
			return true
		}
		clickHamburgerIfPresent()
		WebUI.delay(1)
		if (isSidebarItemVisible(itemText)) {
			return true
		}
		clickHamburgerIfPresent()
		WebUI.delay(1)
		return isSidebarItemVisible(itemText)
	}

	static Map<String, Object> clickSidebarItemByText(String itemText) {
		if (!isBrowserSessionAlive()) {
			return [found: false, clicked: false, href: '', text: '']
		}
		try {
			Map<String, Object> result = (Map<String, Object>) WebUI.executeJavaScript('''
			var target = String(arguments[0] || '');
			var normalize = function(value) {
				var raw = String(value || '');
				var parts = [];
				var current = '';
				for (var idx = 0; idx < raw.length; idx++) {
					var code = raw.charCodeAt(idx);
					var ch = raw.charAt(idx);
					var ws = code === 9 || code === 10 || code === 13 || code === 32;
					if (ws) {
						if (current.length > 0) {
							parts.push(current);
							current = '';
						}
					} else {
						current += ch;
					}
				}
				if (current.length > 0) parts.push(current);
				return parts.join(' ').trim();
			};
			var visible = function(el) {
				if (!(el instanceof HTMLElement)) return false;
				var st = window.getComputedStyle(el);
				return st.display !== 'none' && st.visibility !== 'hidden' && el.offsetParent !== null;
			};
			var targetNorm = normalize(target);
			var isMeaningfulHref = function(href) {
				if (!href) return false;
				var h = href.trim();
				return h.length > 1 && h !== '#' && h.indexOf('javascript') < 0 && h.indexOf('mailto') < 0;
			};
			// Paso 1: preferir <a> con href real (links de sidebar) sobre divs/botones de contenido de pagina
			var anchors = Array.from(document.querySelectorAll('a[href]'));
			var anchorExact = null;
			var anchorPartial = null;
			for (var a = 0; a < anchors.length; a++) {
				var anc = anchors[a];
				if (!visible(anc)) continue;
				var ancRect = anc.getBoundingClientRect();
				if (ancRect.left > (window.innerWidth * 0.55)) continue;
				if (!isMeaningfulHref(anc.getAttribute('href'))) continue;
				var ancText = normalize(anc.textContent || '');
				if (!ancText) continue;
				if (ancText === targetNorm) { anchorExact = anc; break; }
				if (anchorPartial == null && (ancText.indexOf(targetNorm) >= 0 || targetNorm.indexOf(ancText) >= 0)) {
					anchorPartial = anc;
				}
			}
			// Paso 2: fallback a cualquier elemento visible en zona sidebar
			var nodes = Array.from(document.querySelectorAll('a,button,[role="button"],li,div,span'));
			var exact = anchorExact;
			var partial = anchorPartial;
			if (!exact) {
				for (var i = 0; i < nodes.length; i++) {
					var node = nodes[i];
					if (!visible(node)) continue;
					var rect = node.getBoundingClientRect();
					if (rect.left > (window.innerWidth * 0.55)) continue;
					var text = normalize(node.textContent || '');
					if (!text) continue;
					if (text === targetNorm) {
						exact = node.closest('a,button,[role="button"]') || node;
						break;
					}
					if (partial == null && (text.indexOf(targetNorm) >= 0 || targetNorm.indexOf(text) >= 0)) {
						partial = node.closest('a,button,[role="button"]') || node;
					}
				}
			}
			var found = exact || partial;
			if (!found) return { found: false, clicked: false };
			found.scrollIntoView({ block: 'center', inline: 'nearest' });
			['mousedown', 'mouseup', 'click'].forEach(function(evt) {
				found.dispatchEvent(new MouseEvent(evt, { bubbles: true, cancelable: true }));
			});
			return {
				found: true,
				clicked: true,
				href: (found.getAttribute('href') || ''),
				text: normalize(found.textContent || '')
			};
			''', [itemText])
			if (result != null && Boolean.TRUE.equals(result.found) && Boolean.TRUE.equals(result.clicked)) {
				return result
			}
		} catch (Exception ignored) {
		}

		String normalized = sanitizeForXPathContains(itemText)
		if (normalized.length() == 0) return [found: false, clicked: false, href: '', text: '']
		List<TestObject> fallbacks = [
			xpathObject('sidebar_click_exact_' + normalized, sidebarTextXPathExact(normalized)),
			xpathObject('sidebar_click_contains_' + normalized, sidebarTextXPathContains(normalized))
		]
		boolean clicked = clickFirstPresent(fallbacks, 2)
		return [found: clicked, clicked: clicked, href: '', text: collapseSpaces(itemText)]
	}

	static boolean isSidebarItemActive(String itemText) {
		if (!isBrowserSessionAlive()) {
			return false
		}
		try {
			Boolean active = (Boolean) WebUI.executeJavaScript('''
			var target = String(arguments[0] || '');
			var normalize = function(value) {
				var raw = String(value || '');
				var parts = [];
				var current = '';
				for (var idx = 0; idx < raw.length; idx++) {
					var code = raw.charCodeAt(idx);
					var ch = raw.charAt(idx);
					var ws = code === 9 || code === 10 || code === 13 || code === 32;
					if (ws) {
						if (current.length > 0) {
							parts.push(current);
							current = '';
						}
					} else {
						current += ch;
					}
				}
				if (current.length > 0) parts.push(current);
				return parts.join(' ').trim();
			};
			var visible = function(el) {
				if (!(el instanceof HTMLElement)) return false;
				var st = window.getComputedStyle(el);
				return st.display !== 'none' && st.visibility !== 'hidden' && el.offsetParent !== null;
			};
			var targetNorm = normalize(target);
			var nodes = Array.from(document.querySelectorAll('a,button,[role="button"],li,div,span'));
			for (var i = 0; i < nodes.length; i++) {
				var node = nodes[i];
				if (!visible(node)) continue;
				var rect = node.getBoundingClientRect();
				if (rect.left > (window.innerWidth * 0.55)) continue;
				var text = normalize(node.textContent || '');
				if (text !== targetNorm && text.indexOf(targetNorm) < 0 && targetNorm.indexOf(text) < 0) continue;
				var host = node.closest('a,button,[role="button"]') || node;
				var cls = String(host.className || '').toLowerCase();
				var aria = String(host.getAttribute('aria-current') || '').toLowerCase();
				if (aria === 'page' || cls.indexOf('active') >= 0 || cls.indexOf('selected') >= 0 || cls.indexOf('current') >= 0) {
					return true;
				}
			}
			return false;
			''', [itemText])
			if (Boolean.TRUE.equals(active)) {
				return true
			}
		} catch (Exception ignored) {
		}
		return isSidebarItemVisible(itemText)
	}

	static Map<String, Object> snapshotWindowState() {
		try {
			WebDriver driver = DriverFactory.getWebDriver()
			List<String> handles = new ArrayList<String>(driver.getWindowHandles())
			String current = driver.getWindowHandle()
			return [handles: handles, current: current]
		} catch (Exception ignored) {
			return [handles: [], current: '']
		}
	}

	static Map<String, Object> captureAndCloseNewTab(
		Map<String, Object> beforeState,
		int waitSeconds = 4,
		boolean loginInNewTab = false,
		String username = '',
		String password = '',
		int loginSettleSeconds = 3,
		List<String> expectedUrlFragmentsInNewTab = [],
		List<String> requiredXpathsInNewTab = [],
		int validateWaitSeconds = 4
	) {
		Map<String, Object> result = [
			opened: false,
			closed: false,
			url: '',
			loggedIn: false,
			newTabUrlMatched: false,
			newTabElementsOk: false,
			newTabMissingXpaths: []
		]
		try {
			WebDriver driver = DriverFactory.getWebDriver()
			if (driver == null) {
				return result
			}
			List<String> beforeHandles = (List<String>) (beforeState?.handles ?: [])
			String original = (beforeState?.current ?: '').toString()

			for (int i = 0; i < waitSeconds; i++) {
				List<String> currentHandles = new ArrayList<String>(driver.getWindowHandles())
				List<String> newHandles = currentHandles.findAll { !beforeHandles.contains(it) }
				if (!newHandles.isEmpty()) {
					String newHandle = newHandles[0]
					driver.switchTo().window(newHandle)
					try {
						WebUI.waitForPageLoad(10)
					} catch (Exception ignored) {
					}
					result.opened = true
					result.url = driver.getCurrentUrl()
					String urlBeforeLoginAttempt = currentUrlSafe()
					boolean onIdentityHost = urlBeforeLoginAttempt.contains('login.microsoftonline.com') ||
						urlBeforeLoginAttempt.contains('microsoftonline.com') ||
						urlBeforeLoginAttempt.contains('loginlive.com') ||
						urlBeforeLoginAttempt.contains('live.com') ||
						urlBeforeLoginAttempt.contains('saml')
					if (loginInNewTab && username && password && onIdentityHost) {
						try {
							performMicrosoftLoginWithCredentials(result.url.toString(), username, password)
							WebUI.waitForPageLoad(12)
							WebUI.delay(Math.max(1, loginSettleSeconds))
							result.loggedIn = isValidAppSession()
							result.url = currentUrlSafe()
						} catch (Exception ignored) {
							result.loggedIn = false
						}
					} else if (loginInNewTab) {
						result.loggedIn = isValidAppSession()
					}

					String currentTabUrl = currentUrlSafe()
					result.url = currentTabUrl
					boolean urlMatched = expectedUrlFragmentsInNewTab.isEmpty() || expectedUrlFragmentsInNewTab.any { String fragment ->
						fragment != null && fragment.trim().length() > 0 && currentTabUrl.contains(fragment.trim())
					}
					result.newTabUrlMatched = urlMatched

					List<String> missingXpaths = []
					if (!requiredXpathsInNewTab.isEmpty()) {
						int idx = 0
						requiredXpathsInNewTab.each { String rawXpath ->
							String xpath = (rawXpath ?: '').trim()
							if (xpath.length() == 0) return
							boolean present = WebUI.verifyElementPresent(
								xpathObject('new_tab_check_' + idx, xpath),
								Math.max(1, validateWaitSeconds),
								FailureHandling.OPTIONAL
							)
							if (!present) {
								missingXpaths.add(xpath)
							}
							idx++
						}
					}
					result.newTabMissingXpaths = missingXpaths
					result.newTabElementsOk = missingXpaths.isEmpty()

					List<String> handlesBeforeClose = new ArrayList<String>(driver.getWindowHandles())
					String returnHandle = ''
					if (original && handlesBeforeClose.contains(original) && original != newHandle) {
						returnHandle = original
					} else {
						List<String> candidates = handlesBeforeClose.findAll { it != newHandle }
						if (!candidates.isEmpty()) {
							returnHandle = candidates[0]
						}
					}

					if (returnHandle.length() == 0) {
						// Evita cerrar la última pestaña viva, que invalida toda la sesión WebDriver.
						result.closed = false
						return result
					}

					driver.close()
					driver.switchTo().window(returnHandle)
					result.closed = true
					return result
				}
				WebUI.delay(1)
			}
		} catch (Exception ignored) {
		}
		return result
	}

	@Keyword
	static void verifyAuthenticatedSidebarOrderOnly(Map config) {
		assertBrowserSupportForHost()
		String caseId = (config.caseId ?: 'TC-AUTH-SIDEBAR-ORDER').toString()
		String platformLabel = (config.platformLabel ?: 'Portal').toString()
		String targetUrl = resolveOptionalSetting(config.urlVariableName?.toString(), config.fallbackUrl?.toString())
		String homeItem = collapseSpaces((config.homeItem ?: 'Home').toString())
		String logoutItem = collapseSpaces((config.logoutItem ?: 'Logout').toString())
		boolean enforceHomeInAutoSequence = resolveBooleanSetting(config.enforceHomeInAutoSequence, true)
		int autoMaxItems = ((config.autoMaxItems ?: 40) as Integer)
		List<String> autoExcludeTokens = ((List) (config.autoExcludeTokens ?: [])).collect { it.toString() }
		List<String> sequenceItems = ((List) (config.sequenceItems ?: [])).collect { collapseSpaces(it?.toString()) }.findAll { it.length() > 0 }
		List<String> failures = []
		List<String> warnings = []

		try {
			openBrowserAndLoginWithMicrosoft(targetUrl)
			if (!ensureSidebarOpenForItem(sequenceItems ? sequenceItems[0] : homeItem)) {
				warnings.add('No se encontró botón hamburguesa visible; se valida con menú actual.')
			}

			// Scroll completo del sidebar y de la pagina para forzar render de items bajo el fold
			WebUI.executeJavaScript("""
				var sidebar = document.querySelector('nav, aside, [class*="sidebar"], [class*="side-nav"], [id*="sidebar"], [id*="sidenav"]');
				if (sidebar) {
					sidebar.scrollTop = sidebar.scrollHeight;
				}
				window.scrollTo(0, document.body.scrollHeight);
			""", null)
			WebUI.delay(1)
			WebUI.executeJavaScript("""
				var sidebar = document.querySelector('nav, aside, [class*="sidebar"], [class*="side-nav"], [id*="sidebar"], [id*="sidenav"]');
				if (sidebar) sidebar.scrollTop = 0;
				window.scrollTo(0, 0);
			""", null)
			WebUI.delay(1)
			KeywordUtil.logInfo('[SCROLL] Sidebar + pagina recorridos antes de leer items')

			if (sequenceItems.isEmpty()) {
				sequenceItems = discoverSidebarItems(autoMaxItems, autoExcludeTokens)
				if (!sequenceItems.isEmpty()) {
					if (sequenceItems.any { it.equalsIgnoreCase(logoutItem) }) {
						sequenceItems = sequenceItems.findAll { !it.equalsIgnoreCase(logoutItem) } + [logoutItem]
					}
					if (enforceHomeInAutoSequence && !sequenceItems.any { it.equalsIgnoreCase(homeItem) }) {
						sequenceItems = [homeItem] + sequenceItems
					}
					KeywordUtil.logInfo('[SIDEBAR][AUTO] sequenceItems=' + sequenceItems.join(' -> '))
				} else {
					failures.add('No se pudo descubrir automáticamente la secuencia del sidebar')
				}
			}

			Map<String, Object> orderState = sidebarItemsState(sequenceItems)
			List foundItems = (List) (orderState.foundItems ?: [])
			List missingItems = (List) (orderState.missingItems ?: [])
			boolean orderOk = Boolean.TRUE.equals(orderState.orderOk)

			captureCaseScreenshot(caseId, 'sidebar_scroll_bottom')
			KeywordUtil.logInfo('[SIDEBAR][ORDER] ' + platformLabel + ' encontrados=' + foundItems.join(' -> '))
			if (!missingItems.isEmpty()) {
				failures.add('Items no visibles en sidebar: ' + missingItems.join(', '))
			}
			if (!orderOk) {
				failures.add('Orden visual de sidebar no coincide con el esperado')
			}

			captureCaseScreenshot(caseId, 'sidebar_order')

			if (!warnings.isEmpty()) {
				KeywordUtil.markWarning(caseId + ' warnings: ' + warnings.join(' | '))
			}
			if (failures.isEmpty()) {
				KeywordUtil.markPassed(caseId + ' OK. Orden de sidebar validado en ' + platformLabel)
			} else {
				KeywordUtil.markFailed(caseId + ' falló: ' + failures.join(' | '))
			}
		} finally {
			safeCloseBrowser()
		}
	}

	@Keyword
	static void runAuthenticatedSidebarSequence(Map config) {
		assertBrowserSupportForHost()
		String caseId = (config.caseId ?: 'TC-AUTH-SIDEBAR-SEQUENCE').toString()
		String platformLabel = (config.platformLabel ?: 'Portal').toString()
		String targetUrl = resolveOptionalSetting(config.urlVariableName?.toString(), config.fallbackUrl?.toString())
		String visualPlatform = collapseSpaces((config.visualPlatform ?: platformLabel).toString())
		int settleSeconds = ((config.settleSeconds ?: 2) as Integer)
		double visualThreshold = ((config.visualThreshold ?: 3.0) as Double)
		int autoMaxItems = ((config.autoMaxItems ?: 40) as Integer)
		List<String> autoExcludeTokens = ((List) (config.autoExcludeTokens ?: [])).collect { it.toString() }
		List<String> sequenceItems = ((List) (config.sequenceItems ?: [])).collect { collapseSpaces(it?.toString()) }.findAll { it.length() > 0 }
		boolean enableVisualFullPage = resolveBooleanSetting(config.enableVisualFullPage, false)
		boolean enableVisualBaselineCompare = resolveBooleanSetting(config.enableVisualBaselineCompare, false)
		Map<String, Map> navigationRulesByItem = [:]
		((List) (config.navigationRules ?: [])).each { Object rawRule ->
			if (!(rawRule instanceof Map)) return
			Map rule = (Map) rawRule
			String key = collapseSpaces((rule.item ?: '').toString()).toLowerCase()
			if (key.length() > 0) {
				navigationRulesByItem[key] = rule
			}
		}
		String homeItem = collapseSpaces((config.homeItem ?: 'Home').toString())
		String logoutItem = collapseSpaces((config.logoutItem ?: 'Logout').toString())
		boolean enforceHomeInAutoSequence = resolveBooleanSetting(config.enforceHomeInAutoSequence, true)
		boolean allowMissingHomeBeforeLogout = resolveBooleanSetting(config.allowMissingHomeBeforeLogout, false)
		boolean allowMissingLogout = resolveBooleanSetting(config.allowMissingLogout, false)
		boolean logoutAllowClickOnly = Boolean.TRUE.equals(config.logoutAllowClickOnly)
		List<String> logoutExpectedUrlFragments = ((List) (config.logoutExpectedUrlFragments ?: [])).collect { it.toString() }
		int logoutSettleSeconds = ((config.logoutSettleSeconds ?: Math.max(2, settleSeconds)) as Integer)
		List<String> failures = []
		List<String> warnings = []
		List<Map<String, Object>> visualComparisons = []

		try {
			openBrowserAndLoginWithMicrosoft(targetUrl)
			if (!ensureSidebarOpenForItem(sequenceItems ? sequenceItems[0] : homeItem)) {
				warnings.add('No se encontró botón hamburguesa visible; se continúa con menú actual.')
			}

			if (sequenceItems.isEmpty()) {
				sequenceItems = discoverSidebarItems(autoMaxItems, autoExcludeTokens)
				if (!sequenceItems.isEmpty()) {
					if (sequenceItems.any { it.equalsIgnoreCase(logoutItem) }) {
						sequenceItems = sequenceItems.findAll { !it.equalsIgnoreCase(logoutItem) } + [logoutItem]
					}
					if (enforceHomeInAutoSequence && !sequenceItems.any { it.equalsIgnoreCase(homeItem) }) {
						sequenceItems = [homeItem] + sequenceItems
					}
					KeywordUtil.logInfo('[SIDEBAR][AUTO] sequenceItems=' + sequenceItems.join(' -> '))
				} else {
					KeywordUtil.markFailedAndStop(caseId + ' no pudo descubrir automáticamente la secuencia del sidebar')
				}
			}

			// Scroll completo del sidebar y de la pagina para forzar render de items bajo el fold
			WebUI.executeJavaScript("""
				var sidebar = document.querySelector('nav, aside, [class*="sidebar"], [class*="side-nav"], [id*="sidebar"], [id*="sidenav"]');
				if (sidebar) { sidebar.scrollTop = sidebar.scrollHeight; }
				window.scrollTo(0, document.body.scrollHeight);
			""", null)
			WebUI.delay(1)
			WebUI.executeJavaScript("""
				var sidebar = document.querySelector('nav, aside, [class*="sidebar"], [class*="side-nav"], [id*="sidebar"], [id*="sidenav"]');
				if (sidebar) sidebar.scrollTop = 0;
				window.scrollTo(0, 0);
			""", null)
			WebUI.delay(1)
			KeywordUtil.logInfo('[SCROLL] Sidebar + pagina recorridos antes de leer items')

			Map<String, Object> orderState = sidebarItemsState(sequenceItems)
			List foundItems = (List) (orderState.foundItems ?: [])
			List missingItems = (List) (orderState.missingItems ?: [])
			boolean orderOk = Boolean.TRUE.equals(orderState.orderOk)

			if (!missingItems.isEmpty()) {
				failures.add('Items no visibles en sidebar: ' + missingItems.join(', '))
			}
			if (!orderOk) {
				failures.add('Orden visual de sidebar no coincide con el esperado')
			}
			KeywordUtil.logInfo('[SIDEBAR] Encontrados en orden: ' + foundItems.join(' -> '))

			for (String item : sequenceItems) {
				if (item == null || item.length() == 0) continue
				if (item.equalsIgnoreCase(logoutItem)) continue
				if (!isBrowserSessionAlive()) {
					failures.add('La sesión del navegador se cerró inesperadamente antes de validar ' + item)
					break
				}
				Map navRule = navigationRulesByItem[item.toLowerCase()] ?: [:]
				boolean allowClickOnly = Boolean.TRUE.equals(navRule.allowClickOnly)
				boolean expectNewTab = Boolean.TRUE.equals(navRule.expectNewTab)
				boolean loginInNewTab = Boolean.TRUE.equals(navRule.loginInNewTab)
				int itemSettleSeconds = ((navRule.settleSeconds ?: settleSeconds) as Integer)
				int readSeconds = ((navRule.readSeconds ?: 0) as Integer)
				List<String> expectedUrlFragments = ((List) (navRule.expectedUrlFragments ?: [])).collect { it.toString() }
				List<String> newTabExpectedUrlFragments = ((List) (navRule.newTabExpectedUrlFragments ?: navRule.expectedNewTabUrlFragments ?: [])).collect { it.toString() }
				List<String> newTabRequiredXpaths = ((List) (navRule.newTabRequiredXpaths ?: [])).collect { it.toString() }
				int newTabValidateWaitSeconds = ((navRule.newTabValidateWaitSeconds ?: Math.max(2, itemSettleSeconds)) as Integer)

				ensureSidebarOpenForItem(item)
				String beforeUrl = currentUrlSafe()
				Map<String, Object> beforeWindow = snapshotWindowState()
				String msUser = loginInNewTab ? resolveCredential('MS_USER', 'USERNAME') : ''
				String msPass = loginInNewTab ? resolveCredential('MS_PASS', 'PASSWORD') : ''
				Map<String, Object> clickResult = clickSidebarItemByText(item)
				WebUI.waitForPageLoad(12)
				WebUI.delay(itemSettleSeconds)
				Map<String, Object> tabResult = captureAndCloseNewTab(
					beforeWindow,
					Math.max(2, itemSettleSeconds),
					loginInNewTab,
					msUser,
					msPass,
					Math.max(2, itemSettleSeconds),
					newTabExpectedUrlFragments,
					newTabRequiredXpaths,
					newTabValidateWaitSeconds
				)
				String afterUrl = currentUrlSafe()
				if (afterUrl == 'SESSION_LOST' || !isBrowserSessionAlive()) {
					failures.add('La sesión del navegador se cerró inesperadamente durante la validación de ' + item)
					break
				}
				boolean active = isSidebarItemActive(item)
				String href = (clickResult?.href ?: '').toString().trim()
				String hrefToken = href
				if (hrefToken.startsWith('http')) {
					try {
						hrefToken = new java.net.URL(hrefToken).getPath()
					} catch (Exception ignored) {
					}
				}
				boolean routeHint = hrefToken.length() > 1 && afterUrl.contains(hrefToken)
				boolean expectedRoute = expectedUrlFragments.any { String fragment ->
					fragment != null && fragment.trim().length() > 0 && afterUrl.contains(fragment.trim())
				}
				boolean newTabOk = Boolean.TRUE.equals(tabResult.opened) && Boolean.TRUE.equals(tabResult.closed)
				boolean newTabLoginOk = !loginInNewTab || Boolean.TRUE.equals(tabResult.loggedIn)
				boolean newTabUrlOk = newTabExpectedUrlFragments.isEmpty() || Boolean.TRUE.equals(tabResult.newTabUrlMatched)
				boolean newTabElementsOk = newTabRequiredXpaths.isEmpty() || Boolean.TRUE.equals(tabResult.newTabElementsOk)
				// Cuando se declaran expectedUrlFragments, la URL DEBE coincidir (active solo no es suficiente)
				boolean hasStrictUrlExpectation = !expectedUrlFragments.isEmpty()
				boolean loaded
				if (item.equalsIgnoreCase(homeItem)) {
					loaded = Boolean.TRUE.equals(clickResult.clicked)
				} else if (hasStrictUrlExpectation) {
					loaded = expectedRoute || (beforeUrl != afterUrl) || newTabOk
				} else {
					loaded = (beforeUrl != afterUrl) || active || routeHint || expectedRoute || newTabOk || (allowClickOnly && Boolean.TRUE.equals(clickResult.clicked))
				}

				// Fallback: si el click inicial no llevó a la ruta esperada, intentar click dirigido por fragmento de URL.
				if (!loaded && hasStrictUrlExpectation && !expectNewTab && isBrowserSessionAlive()) {
					boolean recovered = false
					for (String fragment : expectedUrlFragments) {
						String frag = (fragment ?: '').trim()
						if (frag.length() == 0) continue
						try {
							Boolean clickedByFragment = (Boolean) WebUI.executeJavaScript('''
							var frag = String(arguments[0] || '');
							var visible = function(el) {
								if (!(el instanceof HTMLElement)) return false;
								var st = window.getComputedStyle(el);
								return st.display !== 'none' && st.visibility !== 'hidden' && el.offsetParent !== null;
							};
							var anchors = Array.from(document.querySelectorAll('a[href]')).filter(function(a) {
								if (!visible(a)) return false;
								var href = String(a.getAttribute('href') || '');
								if (href.length === 0) return false;
								var rect = a.getBoundingClientRect();
								if (rect.left > (window.innerWidth * 0.55)) return false;
								return href.indexOf(frag) >= 0;
							});
							if (anchors.length === 0) return false;
							anchors[0].scrollIntoView({ block: 'center', inline: 'nearest' });
							['mousedown','mouseup','click'].forEach(function(evt) {
								anchors[0].dispatchEvent(new MouseEvent(evt, { bubbles: true, cancelable: true }));
							});
							return true;
							''', [frag])
							if (Boolean.TRUE.equals(clickedByFragment)) {
								WebUI.waitForPageLoad(12)
								WebUI.delay(itemSettleSeconds)
								afterUrl = currentUrlSafe()
								expectedRoute = expectedUrlFragments.any { String expectedFrag ->
									expectedFrag != null && expectedFrag.trim().length() > 0 && afterUrl.contains(expectedFrag.trim())
								}
								loaded = expectedRoute || (beforeUrl != afterUrl)
								if (loaded) {
									recovered = true
									KeywordUtil.logInfo('[SIDEBAR][NAV] ' + item + ' recuperado por fallback href fragment=' + frag + ' after=' + afterUrl)
									break
								}
							}
						} catch (Exception ignored) {
						}
					}
					if (!recovered && !loaded) {
						// No-op: conserva el fail original más abajo
					}
				}

				if (!Boolean.TRUE.equals(clickResult.found) || !Boolean.TRUE.equals(clickResult.clicked)) {
					failures.add(item + ' no se pudo clickear en sidebar')
				} else if (expectNewTab && !newTabOk) {
					failures.add(item + ' debía abrir nueva pestaña y no ocurrió')
				} else if (expectNewTab && loginInNewTab && !newTabLoginOk) {
					failures.add(item + ' abrió pestaña nueva pero no completó login en esa pestaña')
				} else if (expectNewTab && !newTabUrlOk) {
					failures.add(item + ' abrió pestaña nueva pero no coincidió con URL esperada. URL=' + (tabResult.url ?: 'N/A'))
				} else if (expectNewTab && !newTabElementsOk) {
					List missing = (List) (tabResult.newTabMissingXpaths ?: [])
					failures.add(item + ' abrió pestaña nueva pero faltan elementos esperados: ' + missing.join(' || '))
				} else if (!loaded) {
					failures.add(item + ' no mostró carga de nueva página (sin cambio URL ni estado activo)')
				} else {
					KeywordUtil.logInfo('[SIDEBAR][NAV] ' + item + ' OK before=' + beforeUrl + ' after=' + afterUrl + ' active=' + active + ' newTab=' + newTabOk)
					if (allowClickOnly && beforeUrl == afterUrl && !active && !routeHint && !expectedRoute) {
						warnings.add(item + ' validado por click (regla allowClickOnly) sin cambio de URL')
					}
					if (newTabOk) {
						warnings.add(item + ' abrió nueva pestaña y se cerró automáticamente. URL=' + (tabResult.url ?: 'N/A') + ' urlCheck=' + newTabUrlOk)
					}
				}

				if (readSeconds > 0) {
					WebUI.delay(readSeconds)
				}

				String navLabel = 'nav_' + item.replaceAll('[^a-zA-Z0-9]+', '_').toLowerCase()
				captureCaseScreenshot(caseId, navLabel)
				if (enableVisualFullPage && isBrowserSessionAlive()) {
					try {
						VisualKeywords.captureFullPage(caseId + '_' + navLabel)
						if (enableVisualBaselineCompare) {
							Map<String, Object> cmp = VisualKeywords.compareWithBaseline(visualPlatform, navLabel, visualThreshold)
							cmp.step = item
							visualComparisons.add(cmp)
							if (!Boolean.TRUE.equals(cmp.passed)) {
								warnings.add('Diferencia visual en ' + item + ': ' + (cmp.message ?: 'sin detalle'))
							}
						}
					} catch (Exception e) {
						warnings.add('No se pudo capturar evidencia visual extendida en ' + item + ': ' + (e.message ?: 'error'))
					}
				}
			}

			if (isBrowserSessionAlive()) {
				if (homeItem.length() > 0) {
					ensureSidebarOpenForItem(homeItem)
					Map<String, Object> homeClick = clickSidebarItemByText(homeItem)
					WebUI.waitForPageLoad(12)
					WebUI.delay(settleSeconds)
					if (!Boolean.TRUE.equals(homeClick.found) || !Boolean.TRUE.equals(homeClick.clicked)) {
						if (allowMissingHomeBeforeLogout) {
							warnings.add('No se encontró Home antes del logout; se continúa por configuración allowMissingHomeBeforeLogout=true')
						} else {
							failures.add('No se pudo volver a Home antes del logout')
						}
					}
					captureCaseScreenshot(caseId, 'return_home')
					if (enableVisualFullPage && isBrowserSessionAlive()) {
						try {
							VisualKeywords.captureFullPage(caseId + '_return_home')
							if (enableVisualBaselineCompare) {
								Map<String, Object> cmp = VisualKeywords.compareWithBaseline(visualPlatform, 'return_home', visualThreshold)
								cmp.step = 'return_home'
								visualComparisons.add(cmp)
							}
						} catch (Exception e) {
							warnings.add('No se pudo capturar evidencia visual extendida en return_home: ' + (e.message ?: 'error'))
						}
					}
				}

				if (logoutItem.length() > 0) {
					ensureSidebarOpenForItem(logoutItem)
					Map<String, Object> logoutClick = clickSidebarItemByText(logoutItem)
					if (!Boolean.TRUE.equals(logoutClick.found) || !Boolean.TRUE.equals(logoutClick.clicked)) {
						if (allowMissingLogout) {
							warnings.add('No se encontró Logout; se continúa por configuración allowMissingLogout=true')
						} else {
							failures.add('Logout no confirmado: item no visible o no clickeable')
						}
					} else {
						WebUI.waitForPageLoad(15)
						WebUI.delay(logoutSettleSeconds)
						String logoutUrl = currentUrlSafe()
						boolean logoutExpectedRoute = logoutExpectedUrlFragments.any { String fragment ->
							fragment != null && fragment.trim().length() > 0 && logoutUrl.contains(fragment.trim())
						}
						boolean logoutOk = !isValidAppSession() || logoutUrl.contains('login.microsoftonline.com') || logoutUrl.contains('login') || logoutExpectedRoute || (logoutAllowClickOnly && Boolean.TRUE.equals(logoutClick.clicked))
						if (!logoutOk) {
							if (allowMissingLogout) {
								warnings.add('Logout no confirmó salida de sesión; se tolera por allowMissingLogout=true')
							} else {
								failures.add('Logout no confirmó salida de sesión')
							}
						} else if (logoutAllowClickOnly && isValidAppSession() && !logoutExpectedRoute && !logoutUrl.contains('login')) {
							warnings.add('Logout validado por click (logoutAllowClickOnly) sin redirección inmediata a login')
						}
					}
					captureCaseScreenshot(caseId, 'logout')
					if (enableVisualFullPage && isBrowserSessionAlive()) {
						try {
							VisualKeywords.captureFullPage(caseId + '_logout')
							if (enableVisualBaselineCompare) {
								Map<String, Object> cmp = VisualKeywords.compareWithBaseline(visualPlatform, 'logout', visualThreshold)
								cmp.step = 'logout'
								visualComparisons.add(cmp)
							}
						} catch (Exception e) {
							warnings.add('No se pudo capturar evidencia visual extendida en logout: ' + (e.message ?: 'error'))
						}
					}
				}
			} else {
				warnings.add('Se omite retorno a Home y Logout porque la sesión del navegador ya no está activa')
			}

			if (!visualComparisons.isEmpty()) {
				try {
					String visualReportPath = VisualKeywords.generateHtmlReport(visualComparisons, caseId + ' Visual Compare')
					KeywordUtil.logInfo('[VISUAL] Reporte comparativo: ' + visualReportPath)
				} catch (Exception e) {
					warnings.add('No se pudo generar reporte visual HTML: ' + (e.message ?: 'error'))
				}
			}

			CommonKeywords.logCaseSummary(caseId, failures, warnings)

			if (!warnings.isEmpty()) {
				KeywordUtil.markWarning(caseId + ' warnings detectados: ' + warnings.size())
			}

			if (failures.isEmpty()) {
				KeywordUtil.markPassed(caseId + ' OK. Sidebar autenticada validada en ' + platformLabel)
			} else {
				KeywordUtil.markFailed(caseId + ' falló: ' + failures.join(' | '))
			}
		} finally {
			safeCloseBrowser()
		}
	}

	static Map<String, Object> captureVisualSnapshot(String alias, String objectPath, int timeoutSeconds = 12) {
		Map<String, Object> snapshot = [
			alias: alias,
			objectPath: objectPath,
			present: false,
			text: '',
			tagName: '',
			left: 0,
			top: 0,
			width: 0,
			height: 0,
			color: '',
			backgroundColor: '',
			borderRadius: '',
			fontSize: '',
			fontWeight: '',
			ariaChecked: '',
			dataState: ''
		]

		try {
			TestObject obj = findTestObject(objectPath)
			boolean present = WebUI.verifyElementPresent(obj, timeoutSeconds, FailureHandling.OPTIONAL)
			snapshot.present = present
			if (!present) {
				return snapshot
			}

			def webElement = WebUI.findWebElement(obj, timeoutSeconds)
			Map<String, Object> dom = (Map<String, Object>) WebUI.executeJavaScript('''
				var el = arguments[0];
				if (!el) return null;
				var st = window.getComputedStyle(el);
				var rect = el.getBoundingClientRect();
				var normalize = function(value) {
					var raw = String(value || '');
					var out = '';
					var space = false;
					for (var i = 0; i < raw.length; i++) {
						var ch = raw.charAt(i);
						var code = raw.charCodeAt(i);
						var isWs = code === 9 || code === 10 || code === 13 || code === 32;
						if (isWs) {
							if (!space) {
								out += ' ';
								space = true;
							}
						} else {
							out += ch;
							space = false;
						}
					}
					return out.trim();
				};
				return {
					text: normalize(el.innerText || el.textContent || ''),
					tagName: (el.tagName || '').toLowerCase(),
					left: Math.round(rect.left),
					top: Math.round(rect.top),
					width: Math.round(rect.width),
					height: Math.round(rect.height),
					color: st.color || '',
					backgroundColor: st.backgroundColor || '',
					borderRadius: st.borderRadius || '',
					fontSize: st.fontSize || '',
					fontWeight: st.fontWeight || '',
					ariaChecked: el.getAttribute('aria-checked') || '',
					dataState: el.getAttribute('data-state') || ''
				};
			''', [webElement])

			if (dom != null) {
				snapshot.putAll(dom)
			}
		} catch (Exception ignored) {
		}

		return snapshot
	}

	static Map<String, Object> extractTrackingMetrics() {
		Map<String, Object> metrics = (Map<String, Object>) WebUI.executeJavaScript('''
			var normalize = function(value) {
				var raw = String(value || '');
				var out = '';
				var space = false;
				for (var i = 0; i < raw.length; i++) {
					var ch = raw.charAt(i);
					var code = raw.charCodeAt(i);
					var isWs = code === 9 || code === 10 || code === 13 || code === 32;
					if (isWs) {
						if (!space) {
							out += ' ';
							space = true;
						}
					} else {
						out += ch;
						space = false;
					}
				}
				return out.trim();
			};

			var textEquals = function(node, expected) {
				return normalize(node && node.textContent).toLowerCase() === normalize(expected).toLowerCase();
			};

			var findByExactText = function(tagNames, text) {
				var nodes = Array.from(document.querySelectorAll(tagNames));
				for (var i = 0; i < nodes.length; i++) {
					if (textEquals(nodes[i], text)) return nodes[i];
				}
				return null;
			};

			var readCardValue = function(label) {
				var labelNode = findByExactText('p,span,div,h3,h4', label);
				if (!labelNode) return '';
				var card = labelNode.closest('div[class*="rounded"], section, article, div');
				if (!card) return '';
				var candidates = Array.from(card.querySelectorAll('h1,h2,h3,h4,p,span,div'));
				for (var i = 0; i < candidates.length; i++) {
					var txt = normalize(candidates[i].textContent);
					if (!txt || txt.toLowerCase() === normalize(label).toLowerCase()) continue;
					if (/^[0-9]+([.,][0-9]+)?%?$/.test(txt)) return txt;
				}
				return '';
			};

			var sectionExists = function(title) {
				return !!findByExactText('h2,h3,p,span,div', title);
			};

			var sectionDigest = function(title) {
				var node = findByExactText('h1,h2,h3,h4,p,span,div', title);
				if (!node) return '';
				var box = node.closest('div[class*="rounded"], section, article, div');
				if (!box) return '';
				return normalize(box.innerText || '').substring(0, 1200);
			};

			return {
				totalExecutions: readCardValue('Total Executions'),
				errorRate: readCardValue('Error Rate'),
				activeUsers: readCardValue('Active Users'),
				dailyExecutionsPresent: sectionExists('Daily Executions'),
				executionsByTracePresent: sectionExists('Executions by Trace'),
				errorRateByTracePresent: sectionExists('Error Rate by Trace'),
				topActiveUsersPresent: sectionExists('Top Active Users'),
				dailyExecutionsDigest: sectionDigest('Daily Executions'),
				executionsByTraceDigest: sectionDigest('Executions by Trace'),
				errorRateByTraceDigest: sectionDigest('Error Rate by Trace'),
				topActiveUsersDigest: sectionDigest('Top Active Users')
			};
		''', null)

		return metrics ?: [:]
	}

	static Map<String, Object> readJsonIfExists(String filePath) {
		try {
			File file = new File(filePath)
			if (!file.exists()) return [:]
			return (Map<String, Object>) new JsonSlurper().parse(file)
		} catch (Exception ignored) {
			return [:]
		}
	}

	static void writeJsonSnapshot(String filePath, Map<String, Object> data) {
		File file = new File(filePath)
		if (!file.parentFile.exists()) {
			file.parentFile.mkdirs()
		}
		file.text = JsonOutput.prettyPrint(JsonOutput.toJson(data))
	}

	static List<String> compareTrackingVisuals(Map<String, Map<String, Object>> currentVisual,
			Map<String, Map<String, Object>> previousVisual,
			List<Map<String, Object>> compareRules,
			int tolerancePx) {
		List<String> diffs = []
		compareRules.each { Map<String, Object> rule ->
			String alias = (rule.alias ?: '').toString()
			if (!alias) return

			Map<String, Object> current = currentVisual[alias] ?: [:]
			Map<String, Object> previous = previousVisual[alias] ?: [:]
			if (previous.isEmpty()) return
			if (!Boolean.TRUE.equals(current.present) || !Boolean.TRUE.equals(previous.present)) return

			if (Boolean.TRUE.equals(rule.compareText)) {
				String cText = collapseSpaces((current.text ?: '').toString())
				String pText = collapseSpaces((previous.text ?: '').toString())
				if (cText != pText) {
					diffs.add(alias + ' texto cambio. actual=' + cText + ' previo=' + pText)
				}
			}

			if (Boolean.TRUE.equals(rule.compareColor) && (current.color ?: '') != (previous.color ?: '')) {
				diffs.add(alias + ' color cambio. actual=' + current.color + ' previo=' + previous.color)
			}
			if (Boolean.TRUE.equals(rule.compareBackground) && (current.backgroundColor ?: '') != (previous.backgroundColor ?: '')) {
				diffs.add(alias + ' fondo cambio. actual=' + current.backgroundColor + ' previo=' + previous.backgroundColor)
			}
			if (Boolean.TRUE.equals(rule.compareShape) && (current.borderRadius ?: '') != (previous.borderRadius ?: '')) {
				diffs.add(alias + ' forma(borderRadius) cambio. actual=' + current.borderRadius + ' previo=' + previous.borderRadius)
			}
			if (Boolean.TRUE.equals(rule.comparePosition)) {
				double dx = Math.abs((((current.left ?: 0) as Number).doubleValue()) - (((previous.left ?: 0) as Number).doubleValue()))
				double dy = Math.abs((((current.top ?: 0) as Number).doubleValue()) - (((previous.top ?: 0) as Number).doubleValue()))
				if (dx > tolerancePx || dy > tolerancePx) {
					diffs.add(alias + ' posicion cambio. dx=' + ((int) dx) + ' dy=' + ((int) dy) + ' tol=' + tolerancePx)
				}
			}
		}
		return diffs
	}

	@Keyword
	static void validateBuildersTrackingAllDashboard(Map config) {
		assertBrowserSupportForHost()
		String caseId = (config.caseId ?: 'TC-BUILDERS-TRACKING-ALL-001').toString()
		String platformLabel = (config.platformLabel ?: 'Builders Tracking').toString()
		String baseUrl = resolveOptionalSetting(config.urlVariableName?.toString(), config.fallbackUrl?.toString())
		String directUrl = (config.directUrl ?: '').toString().trim()
		String trackingUrl = directUrl ? directUrl : (baseUrl.replaceAll('/+$', '') + '/tracking')
		String snapshotLatestPath = (config.snapshotLatestPath ?: System.getProperty('user.dir') + '/Reports/Tracking/snapshots/tracking_all_latest.json').toString()
		String snapshotHistoryDir = (config.snapshotHistoryDir ?: System.getProperty('user.dir') + '/Reports/Tracking/snapshots/history').toString()
		int tolerancePx = ((config.positionTolerancePx ?: 48) as Integer)
		List<String> failures = []
		List<String> warnings = []

		List<Map<String, Object>> visualObjects = (List<Map<String, Object>>) (config.visualObjects ?: [
			[alias: 'title', objectPath: 'Object Repository/Builders/Tracking/All/Page_Builders App/h1_Execution Tracking'],
			[alias: 'subtitle', objectPath: 'Object Repository/Builders/Tracking/All/Page_Builders App/p_Real-time telemetry overview  May 2026'],
			[alias: 'switchToggle', objectPath: 'Object Repository/Builders/Tracking/All/Page_Builders App/button_Production_data-source-switch'],
			[alias: 'monthButton', objectPath: 'Object Repository/Builders/Tracking/All/Page_Builders App/button_May'],
			[alias: 'yearButton', objectPath: 'Object Repository/Builders/Tracking/All/Page_Builders App/button_2026'],
			[alias: 'loadDataButton', objectPath: 'Object Repository/Builders/Tracking/All/Page_Builders App/button_Load Data'],
			[alias: 'totalExecutionsLabel', objectPath: 'Object Repository/Builders/Tracking/All/Page_Builders App/p_Total Executions'],
			[alias: 'errorRateLabel', objectPath: 'Object Repository/Builders/Tracking/All/Page_Builders App/p_Error Rate'],
			[alias: 'activeUsersLabel', objectPath: 'Object Repository/Builders/Tracking/All/Page_Builders App/p_Active Users'],
			[alias: 'dailyExecutionsTitle', objectPath: 'Object Repository/Builders/Tracking/All/Page_Builders App/h2_Daily Executions'],
			[alias: 'executionsByTraceTitle', objectPath: 'Object Repository/Builders/Tracking/All/Page_Builders App/div_Executions by TraceTotal completed  err_48fdf7'],
			[alias: 'errorRateByTraceTitle', objectPath: 'Object Repository/Builders/Tracking/All/Page_Builders App/div_Error Rate by TracePercentage of operat_761d93'],
			[alias: 'topActiveUsersTitle', objectPath: 'Object Repository/Builders/Tracking/All/Page_Builders App/h2_Top Active Users']
		])

		List<Map<String, Object>> compareRules = (List<Map<String, Object>>) (config.compareRules ?: [
			[alias: 'title', compareText: true, compareColor: true, compareBackground: false, compareShape: false, comparePosition: true],
			[alias: 'switchToggle', compareText: false, compareColor: false, compareBackground: true, compareShape: true, comparePosition: true],
			[alias: 'monthButton', compareText: false, compareColor: true, compareBackground: true, compareShape: true, comparePosition: true],
			[alias: 'yearButton', compareText: false, compareColor: true, compareBackground: true, compareShape: true, comparePosition: true],
			[alias: 'loadDataButton', compareText: true, compareColor: true, compareBackground: true, compareShape: true, comparePosition: true],
			[alias: 'totalExecutionsLabel', compareText: true, compareColor: true, compareBackground: false, compareShape: false, comparePosition: true],
			[alias: 'errorRateLabel', compareText: true, compareColor: true, compareBackground: false, compareShape: false, comparePosition: true],
			[alias: 'activeUsersLabel', compareText: true, compareColor: true, compareBackground: false, compareShape: false, comparePosition: true],
			[alias: 'dailyExecutionsTitle', compareText: true, compareColor: true, compareBackground: false, compareShape: false, comparePosition: true],
			[alias: 'executionsByTraceTitle', compareText: false, compareColor: true, compareBackground: false, compareShape: false, comparePosition: true],
			[alias: 'errorRateByTraceTitle', compareText: false, compareColor: true, compareBackground: false, compareShape: false, comparePosition: true],
			[alias: 'topActiveUsersTitle', compareText: true, compareColor: true, compareBackground: false, compareShape: false, comparePosition: true]
		])

		Map<String, Map<String, Object>> currentVisual = [:]

		try {
			openBrowserAndLoginWithMicrosoft(baseUrl)
			WebUI.navigateToUrl(trackingUrl)
			WebUI.waitForPageLoad(30)
			WebUI.delay(2)

			String monthLong = LocalDate.now().format(DateTimeFormatter.ofPattern('MMMM', Locale.ENGLISH))
			String monthShort = LocalDate.now().format(DateTimeFormatter.ofPattern('MMM', Locale.ENGLISH))
			String expectedYear = LocalDate.now().format(DateTimeFormatter.ofPattern('yyyy'))
			String expectedSubtitleLong = 'Real-time telemetry overview · ' + monthLong + ' ' + expectedYear
			String expectedSubtitleShort = 'Real-time telemetry overview · ' + monthShort + ' ' + expectedYear

			visualObjects.each { Map<String, Object> spec ->
				String alias = (spec.alias ?: '').toString()
				String objectPath = (spec.objectPath ?: '').toString()
				if (!alias || !objectPath) return
				Map<String, Object> snapshot = captureVisualSnapshot(alias, objectPath, 12)
				currentVisual[alias] = snapshot
				if (!Boolean.TRUE.equals(snapshot.present)) {
					failures.add(alias + ' no visible en Tracking All: ' + objectPath)
				}
			}

			String currentTitle = collapseSpaces((currentVisual.title?.text ?: '').toString())
			if (currentTitle != 'Execution Tracking') {
				failures.add('Titulo incorrecto. actual=' + currentTitle + ' esperado=Execution Tracking')
			}

			String subtitle = collapseSpaces((currentVisual.subtitle?.text ?: '').toString())
			if (!(subtitle == expectedSubtitleLong || subtitle == expectedSubtitleShort)) {
				failures.add('Subtitulo incorrecto. actual=' + subtitle + ' esperado=' + expectedSubtitleLong)
			}

			String switchAria = (currentVisual.switchToggle?.ariaChecked ?: '').toString().toLowerCase()
			String switchState = (currentVisual.switchToggle?.dataState ?: '').toString().toLowerCase()
			if (!(switchAria == 'false' || switchState == 'unchecked')) {
				failures.add('Switch debe iniciar apagado en Production. aria-checked=' + switchAria + ' data-state=' + switchState)
			}

			String monthButtonText = collapseSpaces((currentVisual.monthButton?.text ?: '').toString())
			if (!(monthButtonText == monthLong || monthButtonText == monthShort)) {
				failures.add('Mes actual incorrecto. actual=' + monthButtonText + ' esperado=' + monthLong)
			}

			String yearButtonText = collapseSpaces((currentVisual.yearButton?.text ?: '').toString())
			if (yearButtonText != expectedYear) {
				failures.add('Anio actual incorrecto. actual=' + yearButtonText + ' esperado=' + expectedYear)
			}

			String loadDataText = collapseSpaces((currentVisual.loadDataButton?.text ?: '').toString())
			if (loadDataText != 'Load Data') {
				failures.add('Texto de boton Load Data incorrecto. actual=' + loadDataText)
			}

			if (Boolean.TRUE.equals(currentVisual.totalExecutionsLabel?.present) &&
				Boolean.TRUE.equals(currentVisual.errorRateLabel?.present) &&
				Boolean.TRUE.equals(currentVisual.activeUsersLabel?.present)) {
				double totalX = ((currentVisual.totalExecutionsLabel.left ?: 0) as Number).doubleValue()
				double errorX = ((currentVisual.errorRateLabel.left ?: 0) as Number).doubleValue()
				double activeX = ((currentVisual.activeUsersLabel.left ?: 0) as Number).doubleValue()
				if (!(totalX < errorX && errorX < activeX)) {
					failures.add('Orden horizontal de modulos no coincide: Total -> Error Rate -> Active Users')
				}
			}

			Map<String, Object> metrics = extractTrackingMetrics()
			if (!metrics.totalExecutions?.toString()) failures.add('No se pudo leer valor de Total Executions')
			if (!metrics.errorRate?.toString()) failures.add('No se pudo leer valor de Error Rate')
			if (!metrics.activeUsers?.toString()) failures.add('No se pudo leer valor de Active Users')
			if (!Boolean.TRUE.equals(metrics.dailyExecutionsPresent)) failures.add('No se encontro seccion Daily Executions')
			if (!Boolean.TRUE.equals(metrics.executionsByTracePresent)) failures.add('No se encontro seccion Executions by Trace')
			if (!Boolean.TRUE.equals(metrics.errorRateByTracePresent)) failures.add('No se encontro seccion Error Rate by Trace')
			if (!Boolean.TRUE.equals(metrics.topActiveUsersPresent)) failures.add('No se encontro seccion Top Active Users')

			Map<String, Object> previousSnapshot = readJsonIfExists(snapshotLatestPath)
			Map<String, Map<String, Object>> previousVisual = (Map<String, Map<String, Object>>) (previousSnapshot.visual ?: [:])
			Map<String, Object> previousMetrics = (Map<String, Object>) (previousSnapshot.metrics ?: [:])

			if (!previousVisual.isEmpty()) {
				List<String> visualDiffs = compareTrackingVisuals(currentVisual, previousVisual, compareRules, tolerancePx)
				if (!visualDiffs.isEmpty()) {
					failures.addAll(visualDiffs)
				}
			} else {
				warnings.add('No existe baseline visual previo. Se crea baseline en esta ejecucion.')
			}

			if (!previousMetrics.isEmpty()) {
				warnings.add('Comparativo metricas: TotalExecutions actual=' + metrics.totalExecutions + ' previo=' + previousMetrics.totalExecutions)
				warnings.add('Comparativo metricas: ErrorRate actual=' + metrics.errorRate + ' previo=' + previousMetrics.errorRate)
				warnings.add('Comparativo metricas: ActiveUsers actual=' + metrics.activeUsers + ' previo=' + previousMetrics.activeUsers)
				warnings.add('Comparativo Daily Executions digest: actual=' + (metrics.dailyExecutionsDigest ?: 'N/A') + ' previo=' + (previousMetrics.dailyExecutionsDigest ?: 'N/A'))
				warnings.add('Comparativo Executions by Trace digest: actual=' + (metrics.executionsByTraceDigest ?: 'N/A') + ' previo=' + (previousMetrics.executionsByTraceDigest ?: 'N/A'))
				warnings.add('Comparativo Error Rate by Trace digest: actual=' + (metrics.errorRateByTraceDigest ?: 'N/A') + ' previo=' + (previousMetrics.errorRateByTraceDigest ?: 'N/A'))
				warnings.add('Comparativo Top Active Users digest: actual=' + (metrics.topActiveUsersDigest ?: 'N/A') + ' previo=' + (previousMetrics.topActiveUsersDigest ?: 'N/A'))
			} else {
				warnings.add('No existe snapshot de metricas previo. Se captura baseline inicial.')
			}

			Map<String, Object> snapshot = [
				meta: [
					caseId: caseId,
					platformLabel: platformLabel,
					url: currentUrlSafe(),
					timestamp: LocalDateTime.now().format(DateTimeFormatter.ofPattern('yyyy-MM-dd HH:mm:ss'))
				],
				visual: currentVisual,
				metrics: metrics
			]

			writeJsonSnapshot(snapshotLatestPath, snapshot)
			String historyFile = snapshotHistoryDir + '/tracking_all_' + LocalDateTime.now().format(DateTimeFormatter.ofPattern('yyyyMMdd_HHmmss')) + '.json'
			writeJsonSnapshot(historyFile, snapshot)

			captureCaseScreenshot(caseId, 'tracking_all_dashboard')

			if (!warnings.isEmpty()) {
				KeywordUtil.markWarning(caseId + ' warnings: ' + warnings.join(' | '))
			}

			if (failures.isEmpty()) {
				KeywordUtil.markPassed(caseId + ' OK. Tracking All validado (texto, forma, color, posicion y metricas) en ' + platformLabel)
			} else {
				KeywordUtil.markFailed(caseId + ' fallo: ' + failures.join(' | '))
			}
		} finally {
			safeCloseBrowser()
		}
	}

	@Keyword
	static boolean verifyXPathPresent(String name, String xpath, int timeoutSeconds) {
		return WebUI.verifyElementPresent(xpathObject(name, xpath), timeoutSeconds, FailureHandling.OPTIONAL)
	}

	static void discoverTrackingTabStructure(String tabLabel, String outputPath) {
		Map<String, Object> discovery = (Map<String, Object>) WebUI.executeJavaScript('''
			var targetLabel = String(arguments[0] || '').toLowerCase();
			var normalize = function(value) {
				var raw = String(value || '');
				var out = '';
				var space = false;
				for (var i = 0; i < raw.length; i++) {
					var ch = raw.charAt(i);
					var code = raw.charCodeAt(i);
					var isWs = code === 9 || code === 10 || code === 13 || code === 32;
					if (isWs) {
						if (!space) {
							out += ' ';
							space = true;
						}
					} else {
						out += ch;
						space = false;
					}
				}
				return out.trim();
			};

			var clickTab = function(label) {
				var buttons = Array.from(document.querySelectorAll('button'));
				for (var i = 0; i < buttons.length; i++) {
					var txt = normalize(buttons[i].innerText || buttons[i].textContent || '').toLowerCase();
					if (txt === label) {
						buttons[i].scrollIntoView({block: 'center', inline: 'nearest'});
						buttons[i].click();
						return true;
					}
				}
				return false;
			};

			var TAB_LABELS = ['all', 'blueprint', 'task creation', 'login'];
			var allButtons = Array.from(document.querySelectorAll('button'));

			// Click the target tab using exact TAB_LABELS match (avoids sidebar buttons)
			var tabButtons = allButtons.filter(function(b) {
				var txt = normalize(b.innerText || b.textContent || '').toLowerCase();
				return TAB_LABELS.indexOf(txt) >= 0;
			});
			var targetBtn = tabButtons.find(function(b) {
				return normalize(b.innerText || b.textContent || '').toLowerCase() === targetLabel;
			});
			if (targetBtn) { targetBtn.scrollIntoView({block: 'center', inline: 'nearest'}); targetBtn.click(); }
			else { clickTab(targetLabel); }

			var discover = {
				tabLabel: targetLabel,
				tabs: tabButtons.map(function(b) {
					return {
						text: normalize(b.innerText || b.textContent || ''),
						cls: String(b.className || '').substring(0, 140),
						bg: window.getComputedStyle(b).backgroundColor
					};
				}),
				chips: [],
				cards: [],
				dailySections: []
			};

			// Chips — "Showing stats for" text
			var chipEls = Array.from(document.querySelectorAll('*')).filter(function(el) {
				if (!(el instanceof HTMLElement)) return false;
				var txt = normalize(el.textContent || '').toLowerCase();
				return txt.indexOf('showing stats for') >= 0;
			});
			discover.chips = chipEls.map(function(el) {
				return {tag: el.tagName, cls: String(el.className || '').substring(0, 80), text: normalize(el.textContent || '').substring(0, 120)};
			});

			// Cards — short heading-like text in rounded containers
			var cardDivs = Array.from(document.querySelectorAll('div[class*="rounded-xl"], div[class*="p-5"], div[class*="p-6"]'));
			var cardTitles = [];
			cardDivs.forEach(function(card) {
				Array.from(card.querySelectorAll('h1,h2,h3,h4,h5,h6,p,span')).forEach(function(el) {
					if (!(el instanceof HTMLElement)) return;
					var style = window.getComputedStyle(el);
					if (style.display === 'none' || style.visibility === 'hidden') return;
					var txt = normalize(el.textContent || '').trim();
					if (txt.length > 3 && txt.length < 80 && el.children.length < 3 && cardTitles.indexOf(txt) < 0) {
						cardTitles.push(txt);
					}
				});
			});
			discover.cards = cardTitles;

			// Daily sections
			var dailyEls = Array.from(document.querySelectorAll('*')).filter(function(el) {
				return el.children && el.children.length > 0 && normalize(el.textContent || '').indexOf('Daily Executions') >= 0;
			});
			discover.dailySections = dailyEls.map(function(el) {
				return {tag: el.tagName, cls: String(el.className || '').substring(0, 100), textStart: normalize(el.textContent || '').substring(0, 120)};
			});

			// Button sample (top 10 non-tab buttons for context)
			discover.buttonsSample = allButtons.filter(function(b) {
				var txt = normalize(b.innerText || b.textContent || '').toLowerCase();
				return TAB_LABELS.indexOf(txt) < 0;
			}).slice(0, 10).map(function(b) {
				return {text: normalize(b.textContent || '').substring(0, 60), className: String(b.className || '').substring(0, 100)};
			});

			discover.chipsContaining_SHOWING = discover.chips;

			var cardElements = Array.from(document.querySelectorAll('div[class*="rounded"], div[class*="card"], section, article')).slice(0, 10).map(function(el) {
				return {className: String(el.className || '').substring(0, 100), textStart: normalize(el.textContent || '').substring(0, 100), tag: el.tagName};
			});
			discover.cardSample = cardElements;

			var dailyEl = Array.from(document.querySelectorAll('*')).find(function(el) {
				return normalize(el.textContent || '').indexOf('Daily') >= 0;
			});
			discover.dailyElementTag = dailyEl ? dailyEl.tagName : 'NOT_FOUND';

			return discover;
		''', [tabLabel ?: ''])

		KeywordUtil.logInfo('[DISCOVERY] ' + tabLabel + ' -> ' + JsonOutput.toJson(discovery))
		File outFile = new File(outputPath)
		outFile.parentFile.mkdirs()
		outFile.text = JsonOutput.prettyPrint(JsonOutput.toJson(discovery))
		KeywordUtil.logInfo('[DISCOVERY] Guardado en: ' + outputPath)
	}

	static Map<String, Object> collectTrackingTabState(String tabLabel) {
		Map<String, Object> state = (Map<String, Object>) WebUI.executeJavaScript('''
			var targetLabel = String(arguments[0] || '').toLowerCase();
			var TAB_LABELS = ['all', 'blueprint', 'task creation', 'login'];
			var normalize = function(value) {
				var raw = String(value || '');
				var out = '';
				var space = false;
				for (var i = 0; i < raw.length; i++) {
					var ch = raw.charAt(i);
					var code = raw.charCodeAt(i);
					var isWs = code === 9 || code === 10 || code === 13 || code === 32;
					if (isWs) { if (!space) { out += ' '; space = true; } }
					else { out += ch; space = false; }
				}
				return out.trim();
			};

			// Find tracking tab buttons by exact text match
			var allButtons = Array.from(document.querySelectorAll('button'));
			var tabButtons = allButtons.filter(function(b) {
				var txt = normalize(b.innerText || b.textContent || '').toLowerCase();
				return TAB_LABELS.indexOf(txt) >= 0;
			});
			var targetBtn = tabButtons.find(function(b) {
				return normalize(b.innerText || b.textContent || '').toLowerCase() === targetLabel;
			});
			var clicked = false;
			if (targetBtn) {
				targetBtn.scrollIntoView({block: 'center', inline: 'nearest'});
				targetBtn.click();
				clicked = true;
			} else {
				// Fallback: click any button with matching text (includes sidebar)
				var fallback = allButtons.find(function(b) {
					return normalize(b.innerText || b.textContent || '').toLowerCase() === targetLabel;
				});
				if (fallback) { fallback.scrollIntoView({block: 'center', inline: 'nearest'}); fallback.click(); clicked = true; }
			}

			// Read tab visual state
			var tabs = tabButtons.map(function(b) {
				var txt = normalize(b.innerText || b.textContent || '');
				var cls = String(b.className || '');
				var style = window.getComputedStyle(b);
				return { text: txt, cls: cls.substring(0, 140), bg: style.backgroundColor };
			});
			var activeTab = tabs.find(function(t) { return t.text.toLowerCase() === targetLabel; });
			var activeBg    = activeTab ? (activeTab.bg || '') : '';
			var activeClass = activeTab ? (activeTab.cls || '') : '';

			// "Showing stats for" chip
			// El <span> con texto exacto "Showing stats for" es el label;
			// su parentElement contiene también el tab label activo (ej: "Blueprint")
			var sfSpan = Array.from(document.querySelectorAll('span,div,p')).find(function(el) {
				return (el instanceof HTMLElement) && normalize(el.textContent || '').toLowerCase() === 'showing stats for';
			});
			var activeChip = '';
			if (sfSpan && sfSpan.parentElement) {
				var parentTxt = normalize(sfSpan.parentElement.textContent || '');
				var parentLow = parentTxt.toLowerCase();
				var sfIdx = parentLow.indexOf('showing stats for');
				activeChip = sfIdx >= 0 ? parentTxt.substring(sfIdx + 'showing stats for'.length).trim() : parentTxt;
			}

			// Production switch
			var switchEl = Array.from(document.querySelectorAll('[role="switch"], input[type="checkbox"], button[role="switch"]')).find(function(el) {
				var host = el.closest('label,div,span') || el;
				var txt = normalize(host.textContent || '').toLowerCase();
				var lbl = (el.getAttribute('aria-label') || '').toLowerCase();
				return txt.indexOf('production') >= 0 || lbl.indexOf('production') >= 0 ||
				       (el.getAttribute('data-testid') || '').toLowerCase().indexOf('switch') >= 0;
			});
			var switchAria  = switchEl ? (switchEl.getAttribute('aria-checked') || '') : '';
			var switchState = switchEl ? (switchEl.getAttribute('data-state') || '') : '';

			// Month, year, Load Data buttons
			var MONTHS = ['january','february','march','april','may','june','july','august','september','october','november','december'];
			var monthBtn = allButtons.find(function(b) {
				var txt = normalize(b.textContent || '').toLowerCase().trim();
				return MONTHS.indexOf(txt) >= 0 || MONTHS.some(function(m) { return txt === m.substring(0,3); });
			});
			var monthText = monthBtn ? normalize(monthBtn.textContent || '').trim() : '';
			var yearBtn = allButtons.find(function(b) { var yr = parseInt(normalize(b.textContent || '').trim()); return yr >= 2020 && yr <= 2099 && String(yr) === normalize(b.textContent || '').trim(); });
			var yearText = yearBtn ? normalize(yearBtn.textContent || '').trim() : '';
			var loadBtn = allButtons.find(function(b) { return normalize(b.textContent || '').toLowerCase().trim() === 'load data'; });
			var loadDataText = loadBtn ? normalize(loadBtn.textContent || '').trim() : '';

			// Card titles — short text nodes inside rounded card divs
			var cardTitles = [];
			var cardDivs = Array.from(document.querySelectorAll('div[class*="rounded-xl"], div[class*="p-5"], div[class*="p-6"]'));
			cardDivs.forEach(function(card) {
				Array.from(card.querySelectorAll('h1,h2,h3,h4,h5,h6,p,span')).forEach(function(el) {
					if (!(el instanceof HTMLElement)) return;
					var style = window.getComputedStyle(el);
					if (style.display === 'none' || style.visibility === 'hidden') return;
					var txt = normalize(el.textContent || '').trim();
					if (txt.length > 3 && txt.length < 80 && el.children.length < 3 && cardTitles.indexOf(txt) < 0) {
						cardTitles.push(txt);
					}
				});
			});

			// Daily Executions section — buscar el subtitle más específico (leaf con texto corto)
			var dailyPresent = Array.from(document.querySelectorAll('*')).some(function(el) {
				return el instanceof HTMLElement && normalize(el.textContent || '') === 'Daily Executions';
			});
			var dailySubtitle = '';
			var dailySubCandidates = Array.from(document.querySelectorAll('p,span,div')).filter(function(el) {
				if (!(el instanceof HTMLElement)) return false;
				var txt = normalize(el.textContent || '').toLowerCase();
				return txt.indexOf('operations per day') >= 0 && txt.length < 200;
			});
			dailySubCandidates.sort(function(a, b) {
				return normalize(a.textContent || '').length - normalize(b.textContent || '').length;
			});
			if (dailySubCandidates.length > 0) {
				dailySubtitle = normalize(dailySubCandidates[0].textContent || '');
			}

			// Top Active Users
			var topUsersPresent = Array.from(document.querySelectorAll('*')).some(function(el) {
				return normalize(el.textContent || '').indexOf('Top Active Users') >= 0;
			});

			return {
				clicked: clicked,
				tabs: tabs,
				activeBg: activeBg,
				activeClass: activeClass,
				activeChip: activeChip,
				switchAria: switchAria,
				switchState: switchState,
				monthText: monthText,
				yearText: yearText,
				loadDataText: loadDataText,
				cardTitles: cardTitles,
				dailyPresent: dailyPresent,
				topUsersPresent: topUsersPresent,
				dailySubtitle: dailySubtitle
			};
		''', [tabLabel ?: ''])

		return state ?: [:]
	}

	@Keyword
	static void validateBuildersTrackingTabDashboard(Map config) {
		assertBrowserSupportForHost()
		String caseId = (config.caseId ?: 'TC-BUILDERS-TRACKING-TAB-001').toString()
		String platformLabel = (config.platformLabel ?: 'Builders Tracking').toString()
		String tabLabel = (config.tabLabel ?: '').toString().trim()
		String baseUrl = resolveOptionalSetting(config.urlVariableName?.toString(), config.fallbackUrl?.toString())
		String directUrl = (config.directUrl ?: '').toString().trim()
		String trackingUrl = directUrl ? directUrl : (baseUrl.replaceAll('/+$', '') + '/tracking')
		String discoveryOutputPath = (config.discoveryOutputPath ?: (System.getProperty('user.dir') + '/Reports/Tracking/discovery/' + tabLabel.toLowerCase().replaceAll('[^a-z0-9]+', '_') + '_discovery.json')).toString()
		
		String expectedDailyPrefix = (config.expectedDailyPrefix ?: 'SHOWING').toString()
		String expectedTheme = (config.expectedTheme ?: '').toString()
		List<String> requiredCardTitles = (config.requiredCardTitles ?: []).asList()
		List<String> requiredTableHeaders = (config.requiredTableHeaders ?: []).asList()
		String snapshotHistoryDir = (config.snapshotHistoryDir ?: (System.getProperty('user.dir') + '/Reports/Tracking/history')).toString()
		String snapshotLatestPath = (config.snapshotLatestPath ?: (System.getProperty('user.dir') + '/Reports/Tracking/snapshots/tracking_tab_latest.json')).toString()

		List<String> failures = []
		List<String> warnings = []

		if (!tabLabel) {
			KeywordUtil.markFailedAndStop(caseId + ' fallo: tabLabel es requerido')
		}

		try {
			// Detecta si hay navegador abierto (reutilización de driver en suite integrada)
			boolean browserAlreadyOpen = false
			try {
				WebDriver driver = WebUI.getWebDriver()
				if (driver != null) {
					browserAlreadyOpen = true
					KeywordUtil.markInfo(caseId + ' INFO: Navegador ya abierto (reutilización de suite integrada)')
				}
			} catch (Exception e) {
				// No hay navegador abierto
			}

			// Solo autentica si es el primer TC de la suite
			if (!browserAlreadyOpen) {
				openBrowserAndLoginWithMicrosoft(baseUrl)
			}

			WebUI.navigateToUrl(trackingUrl)
			WebUI.waitForPageLoad(30)
			WebUI.delay(2)

			discoverTrackingTabStructure(tabLabel, discoveryOutputPath)
			WebUI.delay(2)

			Map<String, Object> state = collectTrackingTabState(tabLabel)
			WebUI.delay(2)

			if (!Boolean.TRUE.equals(state.clicked)) {
				failures.add('No se pudo clickear tab ' + tabLabel)
			}
			if (!collapseSpaces((state.activeChip ?: '').toString()).toLowerCase().contains(tabLabel.toLowerCase())) {
				warnings.add('[CHIP] SHOWING STATS FOR no refleja tab. esperado contiene="' + tabLabel + '" actual="' + (state.activeChip ?: '') + '"')
			}

			String switchAria = (state.switchAria ?: '').toString().toLowerCase()
			String switchState = (state.switchState ?: '').toString().toLowerCase()
			if (!(switchAria == 'false' || switchState == 'unchecked')) {
				failures.add('Switch Production debe iniciar apagado. aria-checked=' + switchAria + ' data-state=' + switchState)
			}

			String monthLong = LocalDate.now().format(DateTimeFormatter.ofPattern('MMMM', Locale.ENGLISH))
			String monthShort = LocalDate.now().format(DateTimeFormatter.ofPattern('MMM', Locale.ENGLISH))
			String expectedYear = LocalDate.now().format(DateTimeFormatter.ofPattern('yyyy'))
			String monthText = collapseSpaces((state.monthText ?: '').toString())
			String yearText = collapseSpaces((state.yearText ?: '').toString())
			String loadDataText = collapseSpaces((state.loadDataText ?: '').toString())

			if (!(monthText == monthLong || monthText == monthShort)) {
				failures.add('Mes no coincide. esperado=' + monthLong + ' actual=' + monthText)
			}
			if (yearText != expectedYear) {
				failures.add('Anio no coincide. esperado=' + expectedYear + ' actual=' + yearText)
			}
			if (loadDataText != 'Load Data') {
				failures.add('Boton Load Data no coincide. actual=' + loadDataText)
			}

			List<String> cardTitles = ((List) (state.cardTitles ?: [])).collect { collapseSpaces(it.toString()) }
			requiredCardTitles.each { String requiredTitle ->
				boolean found = cardTitles.any { String actual -> actual.equalsIgnoreCase(requiredTitle) }
				if (!found) {
					failures.add('No se encontro card requerido para ' + tabLabel + ': ' + requiredTitle)
				}
			}

			if (!Boolean.TRUE.equals(state.dailyPresent)) {
				failures.add('No se encontro seccion Daily Executions en tab ' + tabLabel)
			}
			if (!Boolean.TRUE.equals(state.topUsersPresent)) {
				failures.add('No se encontro seccion Top Active Users en tab ' + tabLabel)
			}

			String dailySubtitle = collapseSpaces((state.dailySubtitle ?: '').toString()).toLowerCase()
			if (expectedDailyPrefix && !dailySubtitle.startsWith(expectedDailyPrefix)) {
				failures.add('Texto de Daily Executions no coincide para ' + tabLabel + '. actual=' + (state.dailySubtitle ?: '') + ' esperadoPrefix=' + expectedDailyPrefix)
			}

			List<Map> tabs = (List<Map>) (state.tabs ?: [])
			String activeBg = (state.activeBg ?: '').toString()
			List<String> inactiveBgs = tabs
				.findAll { Map t -> !tabLabel.equalsIgnoreCase((t.text ?: '').toString()) }
				.collect { Map t -> (t.bg ?: '').toString() }
				.findAll { String x -> x.length() > 0 }

			if (!activeBg) {
				warnings.add('No se pudo leer color de fondo del tab activo ' + tabLabel)
			} else if (inactiveBgs.any { String bg -> bg == activeBg }) {
				failures.add('El color del tab activo parece igual a tabs inactivos para ' + tabLabel + '. activeBg=' + activeBg)
			}

			// Nota: el tab activo usa un único estilo oscuro (text-white border-transparent) para
			// todas las plataformas — no hay diferenciación de color por tab, solo se verifica
			// que el bg del tab activo sea distinto al de los inactivos (ya chequeado arriba).

			WebUI.executeJavaScript('var sc=document.querySelector(\'main\')||document.querySelector(\'[class*="overflow-auto"]\')||document.body;sc.scrollTop=sc.scrollHeight;', null)
			WebUI.delay(2)

			Map<String, Object> bottomState = collectTrackingTabState(tabLabel)
			List<String> headers = ((List) (bottomState.tableHeaders ?: [])).collect { collapseSpaces(it.toString()) }
			requiredTableHeaders.each { String expectedHeader ->
				boolean found = headers.any { String actual -> actual.equalsIgnoreCase(expectedHeader) }
				if (!found) {
					warnings.add('No se encontro header de tabla al hacer scroll en ' + tabLabel + ': ' + expectedHeader)
				}
			}

			WebUI.executeJavaScript('var sc=document.querySelector(\'main\')||document.querySelector(\'[class*="overflow-auto"]\')||document.body;sc.scrollTop=0;', null)
			WebUI.delay(1)

			// -------------------------------------------------------------------------
			// EMAIL FILTER LOOP (activar con validateEmailFilters: true en el config)
			// Para cada filtro descubierto: click → esperar → capturar métricas por card
			// → verificar chip → scroll abajo → verificar Daily subtitle → scroll arriba
			// -------------------------------------------------------------------------
			List<Map<String, Object>> emailFilterResults = []
			if (Boolean.TRUE.equals(config.validateEmailFilters)) {
				List<String> discoveredFilters = (List<String>) WebUI.executeJavaScript('''
					var TAB_LABELS = ['all', 'blueprint', 'task creation', 'login'];
					var CTRL = ['load data', 'production', 'testing', 'logout', 'home', 'tracking'];
					var normalize = function(v) {
						var raw = String(v || ''); var out = ''; var sp = false;
						for (var i = 0; i < raw.length; i++) {
							var code = raw.charCodeAt(i);
							var isWs = (code === 9 || code === 10 || code === 13 || code === 32);
							if (isWs) { if (!sp) { out += ' '; sp = true; } } else { out += raw.charAt(i); sp = false; }
						}
						return out.trim();
					};
					var allButtons = Array.from(document.querySelectorAll('button'));
					// Buscar el label "Email:" y los botones en su contenedor
					var emailLabel = Array.from(document.querySelectorAll('*')).find(function(el) {
						return el.children.length === 0 && normalize(el.textContent || '').toLowerCase() === 'email:';
					});
					var filterBtns = [];
					if (emailLabel) {
						var p = emailLabel.parentElement;
						filterBtns = p ? Array.from(p.querySelectorAll('button')) : [];
						if (filterBtns.length === 0 && p && p.parentElement) {
							filterBtns = Array.from(p.parentElement.querySelectorAll('button')).filter(function(b) {
								var t = normalize(b.textContent || '').toLowerCase();
								return TAB_LABELS.indexOf(t) < 0 && CTRL.indexOf(t) < 0 && t.length > 0 && t.length < 50;
							});
						}
					}
					// Fallback: botones en mitad derecha del viewport con texto corto
					if (filterBtns.length === 0) {
						var vpW = window.innerWidth || 1200;
						filterBtns = allButtons.filter(function(b) {
							var rect = b.getBoundingClientRect();
							var t = normalize(b.textContent || '');
							return rect.left > vpW / 2 && t.length > 0 && t.length < 30 &&
							       TAB_LABELS.indexOf(t.toLowerCase()) < 0 && CTRL.indexOf(t.toLowerCase()) < 0;
						});
					}
					return filterBtns.map(function(b) { return normalize(b.textContent || ''); });
				''', [])

				KeywordUtil.logInfo('[' + caseId + '] Email filters descubiertos: ' + discoveredFilters)

				if (!discoveredFilters || discoveredFilters.isEmpty()) {
					warnings.add('[EMAIL-FILTER] No se encontraron filtros de email en tab ' + tabLabel)
				}

				discoveredFilters.each { String filterName ->
					try {
						// --- Click en el filtro ---
						WebUI.executeJavaScript('''
							var fn = String(arguments[0]);
							var TAB_LABELS = ['all', 'blueprint', 'task creation', 'login'];
							var CTRL = ['load data', 'production', 'testing', 'logout', 'home', 'tracking'];
							var normalize = function(v) {
								var raw = String(v || ''); var out = ''; var sp = false;
								for (var i = 0; i < raw.length; i++) {
									var code = raw.charCodeAt(i);
									var isWs = (code === 9 || code === 10 || code === 13 || code === 32);
									if (isWs) { if (!sp) { out += ' '; sp = true; } } else { out += raw.charAt(i); sp = false; }
								}
								return out.trim();
							};
							var allButtons = Array.from(document.querySelectorAll('button'));
							var emailLabel = Array.from(document.querySelectorAll('*')).find(function(el) {
								return el.children.length === 0 && normalize(el.textContent || '').toLowerCase() === 'email:';
							});
							var target = null;
							if (emailLabel) {
								var p = emailLabel.parentElement;
								var cands = p ? Array.from(p.querySelectorAll('button')) : [];
								if (cands.length === 0 && p && p.parentElement) {
									cands = Array.from(p.parentElement.querySelectorAll('button'));
								}
								target = cands.find(function(b) { return normalize(b.textContent || '') === fn; });
							}
							if (!target) {
								var vpW = window.innerWidth || 1200;
								target = allButtons.find(function(b) {
									var rect = b.getBoundingClientRect();
									return normalize(b.textContent || '') === fn && rect.left > vpW / 2 &&
									       TAB_LABELS.indexOf(fn.toLowerCase()) < 0;
								});
							}
							if (target) { target.scrollIntoView({block: 'center', inline: 'nearest'}); target.click(); }
						''', [filterName])
						WebUI.delay(2)

						// --- Capturar estado post-click ---
						Map<String, Object> filterState = (Map<String, Object>) WebUI.executeJavaScript('''
							var normalize = function(v) {
								var raw = String(v || ''); var out = ''; var sp = false;
								for (var i = 0; i < raw.length; i++) {
									var code = raw.charCodeAt(i);
									var isWs = (code === 9 || code === 10 || code === 13 || code === 32);
									if (isWs) { if (!sp) { out += ' '; sp = true; } } else { out += raw.charAt(i); sp = false; }
								}
								return out.trim();
							};
							// Chip "Showing stats for" — buscar el <span> exacto y tomar su parentElement
							var sfSpan2 = Array.from(document.querySelectorAll('span,div,p')).find(function(el) {
								return (el instanceof HTMLElement) && normalize(el.textContent || '').toLowerCase() === 'showing stats for';
							});
							var chipText = '';
							if (sfSpan2 && sfSpan2.parentElement) {
								var sfParentTxt = normalize(sfSpan2.parentElement.textContent || '');
								var sfLow = sfParentTxt.toLowerCase();
								var sfOff = sfLow.indexOf('showing stats for');
								chipText = sfOff >= 0 ? sfParentTxt.substring(sfOff + 'showing stats for'.length).trim() : sfParentTxt;
							}

							// Métricas por card
							var METRIC_LABELS = ['Executions', 'Error Rate', 'Users', 'Avg Duration'];
							var cards = [];
							var cardDivs = Array.from(document.querySelectorAll(
								'div[class*="rounded-xl"], div[class*="border"], div[class*="shadow"]'
							)).filter(function(d) {
								var txt = normalize(d.textContent || '');
								return METRIC_LABELS.some(function(m) { return txt.indexOf(m) >= 0; }) && txt.length < 600;
							});
							cardDivs.forEach(function(card) {
								var titleEl = Array.from(card.querySelectorAll('h3,h4,p,span')).find(function(el) {
									var txt = normalize(el.textContent || '');
									return txt.indexOf('Blueprint') >= 0 && txt.length < 70 && el.children.length < 2;
								});
								if (!titleEl) return;
								var title = normalize(titleEl.textContent || '');
								if (cards.some(function(c) { return c.title === title; })) return;
								var texts = Array.from(card.querySelectorAll('p,span,div')).filter(function(el) {
									return el.children.length === 0 && normalize(el.textContent || '').length > 0;
								}).map(function(el) { return normalize(el.textContent || ''); });
								var getNext = function(lbl) {
									var idx = texts.indexOf(lbl);
									return (idx >= 0 && idx + 1 < texts.length) ? texts[idx + 1] : null;
								};
								cards.push({
									title: title,
									executions: getNext('Executions'),
									errorRate: getNext('Error Rate'),
									users: getNext('Users'),
									avgDuration: getNext('Avg Duration')
								});
							});

							// Subtítulo Daily Executions — filtrar por longitud y ordenar para evitar retornar <html>
							var dailySubCands = Array.from(document.querySelectorAll('p,span,div')).filter(function(el) {
								if (!(el instanceof HTMLElement)) return false;
								var txt = normalize(el.textContent || '').toLowerCase();
								return txt.indexOf('operations per day') >= 0 && txt.length < 200;
							});
							dailySubCands.sort(function(a, b) {
								return normalize(a.textContent || '').length - normalize(b.textContent || '').length;
							});
							var dailySubtitle = dailySubCands.length > 0 ? normalize(dailySubCands[0].textContent || '') : '';

							return { chipText: chipText, cards: cards, dailySubtitle: dailySubtitle };
						''', [])

						// Verificar chip refleja el filtro seleccionado
						// "All" filter → chip muestra el tab label (ej: "Blueprint") sin email — es correcto
						String filterChip = collapseSpaces((filterState.chipText ?: '').toString())
						String filterChipLow = filterChip.toLowerCase()
						boolean chipOk = filterChipLow.contains(filterName.toLowerCase()) ||
							(filterName.equalsIgnoreCase('all') && filterChipLow.contains(tabLabel.toLowerCase()))
						if (!chipOk) {
							warnings.add('[EMAIL-FILTER] Chip no refleja filtro "' + filterName + '". chip=' + filterChip)
						}

						// Scroll abajo + screenshot
						WebUI.executeJavaScript('var sc=document.querySelector(\'main\')||document.querySelector(\'[class*="overflow-auto"]\')||document.body;sc.scrollTop=sc.scrollHeight;', null)
						WebUI.delay(2)
						captureCaseScreenshot(caseId, 'email_filter_' + filterName.toLowerCase().replaceAll('[^a-z0-9]+', '_') + '_bottom')

						// Verificar subtítulo Daily Executions
						String dailySub = collapseSpaces((filterState.dailySubtitle ?: '').toString()).toLowerCase()
						if (!dailySub.contains('operations per day')) {
							warnings.add('[EMAIL-FILTER] Daily subtitle no encontrado para filtro "' + filterName + '"')
						}

						// Scroll arriba + screenshot
						WebUI.executeJavaScript('var sc=document.querySelector(\'main\')||document.querySelector(\'[class*="overflow-auto"]\')||document.body;sc.scrollTop=0;', null)
						WebUI.delay(1)
						captureCaseScreenshot(caseId, 'email_filter_' + filterName.toLowerCase().replaceAll('[^a-z0-9]+', '_') + '_top')

						emailFilterResults.add([
							filterName   : filterName,
							chipText     : filterChip,
							dailySubtitle: filterState.dailySubtitle ?: '',
							cards        : filterState.cards ?: []
						])
						KeywordUtil.logInfo('[' + caseId + '] Filtro "' + filterName + '" OK — cards=' + ((List)(filterState.cards ?: [])).size())

					} catch (Exception filterEx) {
						warnings.add('[EMAIL-FILTER] Error procesando filtro "' + filterName + '": ' + filterEx.getMessage())
					}
				}
			}
			// -------------------------------------------------------------------------

			// -------------------------------------------------------------------------
			// COMPARACIÓN vs RUN ANTERIOR: color de tab + métricas por filtro
			// Detecta: datos que cambiaron entre runs (esperado), datos en cero (alerta)
			// -------------------------------------------------------------------------
			Map<String, Object> previous = readJsonIfExists(snapshotLatestPath)
			if (!previous.isEmpty()) {
				String prevBg = collapseSpaces(((Map) (previous.state ?: [:])).activeBg?.toString())
				if (prevBg && activeBg && prevBg != activeBg) {
					warnings.add('Color tab activo cambio vs baseline en ' + tabLabel + '. actual=' + activeBg + ' previo=' + prevBg)
				}
			}
			List<Map> prevEmailResults = (List<Map>) (previous.emailFilterResults ?: [])
			if (!prevEmailResults.isEmpty() && !emailFilterResults.isEmpty()) {
				emailFilterResults.each { Map curFilter ->
					String fn = (curFilter.filterName ?: '').toString()
					Map prevFilter = prevEmailResults.find { (it.filterName ?: '').toString() == fn }
					if (!prevFilter) return
					List<Map> curCards  = (List<Map>) (curFilter.cards  ?: [])
					List<Map> prevCards = (List<Map>) (prevFilter.cards ?: [])
					List<String> changes = []
					curCards.each { Map curCard ->
						String ct = (curCard.title ?: '').toString()
						Map prevCard = prevCards.find { (it.title ?: '').toString() == ct }
						if (!prevCard) return
						['executions','users'].each { String metric ->
							String curVal  = (curCard[metric]  ?: '0').toString().replaceAll('[^0-9]', '')
							String prevVal = (prevCard[metric] ?: '0').toString().replaceAll('[^0-9]', '')
							int cv = curVal ? Integer.parseInt(curVal) : 0
							int pv = prevVal ? Integer.parseInt(prevVal) : 0
							if (cv == 0 && pv > 0) {
								warnings.add('[METRIC-CHANGE] ' + fn + '/' + ct + '.' + metric + ' cayó a 0 (anterior=' + pv + ')')
							} else if (cv != pv) {
								changes.add(ct + '.' + metric + ':' + pv + '->' + cv)
							}
						}
					}
					if (changes) {
						KeywordUtil.logInfo('[METRIC-CHANGE] Filtro "' + fn + '" — cambios vs run anterior: ' + changes.join(', '))
					}
				}
			}

			// -------------------------------------------------------------------------
			// ROLLING SCREENSHOT DE GRÁFICAS (por filtro de email)
			// Mantiene: {tabLabel}/filter_{name}_latest.png y _previous.png
			// Archiva: history/{timestamp}_filter_{name}.png — purga >30 días o >4 archivos
			// -------------------------------------------------------------------------
			if (!emailFilterResults.isEmpty()) {
				String chartDir = System.getProperty('user.dir') + '/Reports/Tracking/chart-snapshots/' + tabLabel.toLowerCase().replaceAll('[^a-z0-9]+', '_')
				String chartHistDir = chartDir + '/history'
				new File(chartHistDir).mkdirs()
				emailFilterResults.each { Map ef ->
					String fn = (ef.filterName ?: 'unknown').toString().toLowerCase().replaceAll('[^a-z0-9]+', '_')
					String latestPath  = chartDir + '/filter_' + fn + '_latest.png'
					String prevPath    = chartDir + '/filter_' + fn + '_previous.png'
					// Rotar: latest → previous → history (si previous ya existe, archivar)
					File latestFile = new File(latestPath)
					File prevFile   = new File(prevPath)
					if (prevFile.exists()) {
						String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern('yyyyMMdd_HHmmss'))
						prevFile.renameTo(new File(chartHistDir + '/filter_' + fn + '_' + ts + '.png'))
					}
					if (latestFile.exists()) {
						latestFile.renameTo(prevFile)
					}
					// Guardar nueva screenshot como latest
					try {
						WebUI.takeScreenshot(latestPath)
					} catch (Exception ignored) {}
					// Purgar history: eliminar archivos >30 días o si hay >4 archivos
					List<File> histFiles = new File(chartHistDir).listFiles(
						{ File f -> f.name.startsWith('filter_' + fn + '_') } as java.io.FilenameFilter
					)?.sort { a, b -> a.lastModified() <=> b.lastModified() } ?: []
					long cutoff = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)
					histFiles.each { File hf ->
						if (hf.lastModified() < cutoff || histFiles.size() > 4) {
							hf.delete()
						}
					}
				}
			}

			Map<String, Object> snapshot = [
				meta: [
					caseId: caseId,
					tabLabel: tabLabel,
					platformLabel: platformLabel,
					url: currentUrlSafe(),
					timestamp: LocalDateTime.now().format(DateTimeFormatter.ofPattern('yyyy-MM-dd HH:mm:ss'))
				],
				state: state,
				bottomState: bottomState,
				emailFilterResults: emailFilterResults
			]

			writeJsonSnapshot(snapshotLatestPath, snapshot)
			String historyFile = snapshotHistoryDir + '/tracking_tab_' + tabLabel.toLowerCase().replaceAll('[^a-z0-9]+', '_') + '_' + LocalDateTime.now().format(DateTimeFormatter.ofPattern('yyyyMMdd_HHmmss')) + '.json'
			writeJsonSnapshot(historyFile, snapshot)

			captureCaseScreenshot(caseId, 'tracking_tab_' + tabLabel.toLowerCase().replaceAll('[^a-z0-9]+', '_'))

			KeywordUtil.logInfo(caseId + ' [DISCOVERY] estructura guardada en ' + discoveryOutputPath)

			CustomKeywords.'CommonKeywords.logCaseSummary'(caseId, failures, warnings)
			if (!failures.isEmpty()) {
				KeywordUtil.markFailedAndStop('[' + caseId + '] FAILED: ' + failures.join(' | '))
			} else if (!warnings.isEmpty()) {
				KeywordUtil.markWarning('[' + caseId + '] PASSED con ' + warnings.size() + ' warnings: ' + warnings.join(' | '))
			} else {
				KeywordUtil.markPassed('[' + caseId + '] PASSED — tab ' + tabLabel + ' OK en ' + platformLabel)
			}
		} finally {
			safeCloseBrowser()
		}
	}

	@Keyword
	static boolean verifyXPathText(String name, String xpath, String expectedText, int timeoutSeconds) {
		TestObject obj = xpathObject(name, xpath)
		if (!WebUI.verifyElementPresent(obj, timeoutSeconds, FailureHandling.OPTIONAL)) {
			return false
		}
		String actual = (WebUI.getText(obj, FailureHandling.OPTIONAL) ?: '').trim().replaceAll('\\s+', ' ')
		return actual == expectedText
	}

	@Keyword
	static boolean clickXPathAndKeepValidSession(String name, String xpath, int timeoutSeconds) {
		TestObject obj = xpathObject(name, xpath)
		if (!WebUI.verifyElementPresent(obj, timeoutSeconds, FailureHandling.OPTIONAL)) {
			return false
		}
		String beforeUrl = currentUrlSafe()
		WebUI.click(obj, FailureHandling.OPTIONAL)
		WebUI.waitForPageLoad(6)
		WebUI.delay(1)
		String afterUrl = currentUrlSafe()
		KeywordUtil.logInfo('[NAV] before=' + beforeUrl + ' after=' + afterUrl)
		return isValidAppSession()
	}

	@Keyword
	static boolean logoutAndVerify(String protectedUrl = '') {
		boolean clicked = clickFirstPresent([
			xpathObject('logout_link', "//a[contains(normalize-space(.),'Log Out') or contains(normalize-space(.),'Logout') or contains(normalize-space(.),'Sign out') or contains(normalize-space(.),'Cerrar sesi')]")
		], 5)
		if (!clicked) {
			return false
		}

		WebUI.waitForPageLoad(10)
		WebUI.delay(2)

		if (isLoggedOutState()) {
			return true
		}

		if (protectedUrl != null && protectedUrl.trim().length() > 0) {
			try {
				WebUI.navigateToUrl(protectedUrl)
				WebUI.waitForPageLoad(12)
				WebUI.delay(1)
			} catch (Exception ignored) {
			}
			if (isLoggedOutState()) {
				return true
			}
		}

		return false
	}

	static boolean isLoggedOutState() {
		String currentUrl = currentUrlSafe()
		if (currentUrl == 'SESSION_LOST') {
			return true
		}

		if (currentUrl.contains('login.microsoftonline.com') || currentUrl.contains('microsoftonline.com') || currentUrl.contains('saml') || currentUrl.contains('signin')) {
			return true
		}

		boolean loginPromptVisible =
			isPresentQuiet(findTestObject('Object Repository/Common/a_Log in with Microsoft'), 2) ||
			isPresentQuiet(xpathObject('signin_link_generic', "//a[contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'sign in') or contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'log in') or contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'login') or contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'microsoft') or contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'iniciar sesi') or contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'acceder')]"), 2) ||
			isPresentQuiet(xpathObject('ms_email_input_generic', "//input[@name='loginfmt' or @id='i0116']"), 2)

		if (loginPromptVisible) {
			return true
		}

		boolean stillAuthenticatedUi =
			isPresentQuiet(xpathObject('auth_dashboard_h4', "//h4[contains(normalize-space(.),'Dashboard')]"), 2) ||
			isPresentQuiet(xpathObject('auth_create_document_link', "//a[contains(normalize-space(.),'Create Document')]"), 2) ||
			isPresentQuiet(xpathObject('auth_logout_link', "//a[contains(normalize-space(.),'Log Out') or contains(normalize-space(.),'Logout')]"), 2)

		return !stillAuthenticatedUi && !isValidAppSession()
	}

	/**
	 * Ejecuta el flujo completo de navegación + login Microsoft con credenciales explícitas.
	 * No abre navegador — asume que ya está abierto y maximizado.
	 */
	static void performMicrosoftLoginWithCredentials(String targetUrl, String username, String password) {
		Closure<Boolean> existsByXPath = { String xpathExpr ->
			try {
				WebDriver driver = DriverFactory.getWebDriver()
				return driver != null && !driver.findElements(By.xpath(xpathExpr)).isEmpty()
			} catch (Exception ignored) {
				return false
			}
		}

		WebUI.navigateToUrl(targetUrl)
		WebUI.waitForPageLoad(20)
		WebUI.delay(1)
		List<String> handlesBeforeLoginClick = snapshotHandlesSafe()

		clickFirstPresent([
			findTestObject('Object Repository/Common/a_Log in with Microsoft'),
			xpathObject('login_signin_link', "//a[contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'sign in') or contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'log in') or contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'login') or contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'microsoft')]"),
			xpathObject('login_signin_button', "//button[contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'sign in') or contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'log in') or contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'login')]")
		], 5)

		switchToNewestTabIfPresent(handlesBeforeLoginClick, 3)

		WebUI.waitForPageLoad(20)
		WebUI.delay(1)

		boolean msEmailVisible = existsByXPath("//input[@name='loginfmt' or @id='i0116']")
		boolean onMicrosoftHost = currentUrlSafe().contains('login.microsoftonline.com') || currentUrlSafe().contains('microsoftonline.com')

		// "Pick an account": si hay tile de la cuenta (sesion previa en el perfil), click directo
		if (onMicrosoftHost && !msEmailVisible) {
			boolean accountTileClicked = clickFirstPresent([
				xpathObject('ms_account_tile_exact', "//div[@data-test-id='" + username + "']"),
				xpathObject('ms_account_tile_generic', "//div[@role='button' and .//small[contains(normalize-space(.),'" + username + "')]]")
			], 3)
			if (accountTileClicked) {
				KeywordUtil.logInfo('[LOGIN] Tile de cuenta existente clickeado para ' + username)
				WebUI.delay(2)
				msEmailVisible = existsByXPath("//input[@name='loginfmt' or @id='i0116']")
			}
		}

		if (msEmailVisible) {
			WebUI.setText(findTestObject('Object Repository/Common/input_ms_email'), username)
			clickFirstPresent([
				xpathObject('ms_next_input', "//input[@id='idSIButton9' and @type='submit']"),
				xpathObject('ms_next_button', "//button[contains(.,'Next') or contains(.,'Siguiente')]")
			], 5)

			// Espera adaptativa post-Next: password / SSO silencioso / tile de cuenta / link "Use your password"
			boolean passwordHandled = false
			boolean flowResolved = false
			for (int waitTick = 0; waitTick < 20 && !flowResolved; waitTick++) {
				WebUI.delay(1)
				if (existsByXPath("//input[@name='passwd' or @id='i0118' or (@type='password' and not(@aria-hidden='true'))]")) {
					WebUI.setText(findTestObject('Object Repository/Common/input_ms_password'), password)
					clickFirstPresent([
						xpathObject('ms_signin_input', "//input[@id='idSIButton9' and @type='submit']"),
						xpathObject('ms_signin_button', "//button[contains(.,'Sign in') or contains(.,'Iniciar')]")
					], 5)
					passwordHandled = true
					flowResolved = true
				} else if (existsByXPath("//div[@data-test-id='" + username + "']")) {
					clickIfPresent(xpathObject('ms_account_tile_retry', "//div[@data-test-id='" + username + "']"), 3)
				} else if (existsByXPath("//*[@id='idA_PWD_SwitchToPassword']")) {
					clickIfPresent(xpathObject('ms_use_password_link', "//*[@id='idA_PWD_SwitchToPassword']"), 3)
				} else if (!currentUrlSafe().contains('microsoftonline') && isValidAppSession()) {
					KeywordUtil.logInfo('[LOGIN] Sesion resuelta via SSO silencioso sin password.')
					flowResolved = true
				}
			}
			if (!flowResolved) {
				KeywordUtil.logInfo('[LOGIN] Password no aparecio tras espera adaptativa (20s); se continua para validacion de sesion.')
			}

			// KMSI "Stay signed in?" -> Yes (aplica tanto a flujo con password como SSO)
			clickFirstPresent([
				xpathObject('ms_yes_input', "//input[@id='idSIButton9' and @type='submit']"),
				xpathObject('ms_yes_button', "//button[contains(.,'Yes') or contains(.,'Si')]")
			], 4)
		} else if (onMicrosoftHost) {
			KeywordUtil.logInfo('[LOGIN] Página Microsoft detectada sin campo de email visible; se omite login explícito (posible sesión previa o flujo alterno).')
		} else {
			KeywordUtil.logInfo('[LOGIN] La pestaña ya cargó destino autenticado; no se requiere login adicional.')
		}

		WebUI.waitForPageLoad(20)
		WebUI.delay(2)
		closeExtraTabsKeepCurrent('auth_post_login_flow')
	}

	/**
	 * Abre navegador, hace login Microsoft con credenciales explícitas, recolecta el estado
	 * de los objetos indicados y cierra el navegador. Retorna un mapa con los resultados.
	 * La clave '_meta' contiene: sessionActive, url, loginButtonStillVisible.
	 */
	@Keyword
	static Map<String, Map<String, Object>> collectPlatformState(
			String envName, String envUrl, String username, String password,
			List<Map<String, Object>> definitions, String shotsDir) {

		Map<String, Map<String, Object>> result = [:]
		String currentUrl = 'SESSION_NOT_STARTED'
		try {
			assertBrowserSupportForHost()
			WebUI.openBrowser('')
			WebUI.maximizeWindow()
			performMicrosoftLoginWithCredentials(envUrl, username, password)
			currentUrl = currentUrlSafe()

			boolean loginButtonStillVisible = WebUI.verifyElementPresent(
				findTestObject('Object Repository/Common/a_Log in with Microsoft'), 2, FailureHandling.OPTIONAL)
			boolean sessionActive = currentUrl != 'SESSION_LOST' &&
				!currentUrl.contains('login.microsoftonline.com') &&
				!currentUrl.contains('loginlive.com') &&
				!currentUrl.contains('live.com') &&
				!loginButtonStillVisible
			result['_meta'] = [sessionActive: sessionActive, url: currentUrl, loginButtonStillVisible: loginButtonStillVisible]

			if (sessionActive) {
				for (Map<String, Object> definition : definitions) {
					String key = definition.name.toString()
					TestObject obj = xpathObject(key, definition.xpath.toString())
					boolean present = WebUI.verifyElementPresent(obj, 5, FailureHandling.OPTIONAL)
					String text = ''
					if (present && Boolean.TRUE.equals(definition.compareText)) {
						text = (WebUI.getText(obj, FailureHandling.OPTIONAL) ?: '').trim().replaceAll('\\s+', ' ')
					}
					result[key] = [present: present, text: text]
				}
			} else {
				for (Map<String, Object> definition : definitions) {
					result[definition.name.toString()] = [present: false, text: '']
				}
			}

			new File(shotsDir).mkdirs()
			String shot = shotsDir + '/' + envName + '.png'
			try {
				WebUI.takeScreenshot(shot)
				KeywordUtil.logInfo(envName + ' screenshot=' + shot + ' url=' + currentUrl)
			} catch (Exception e) {
				KeywordUtil.logInfo(envName + ' screenshot failed: ' + e.message)
			}
		} finally {
			safeCloseBrowser()
		}
		return result
	}

	/**
	 * Compara los estados TEST y PROD recolectados por collectPlatformState.
	 * Retorna una lista de descripciones de diferencias (vacía = sin diferencias).
	 */
	@Keyword
	static List<String> comparePlatformStates(
			Map<String, Map<String, Object>> testState,
			Map<String, Map<String, Object>> prodState,
			List<Map<String, Object>> definitions) {

		List<String> mismatches = []

		for (Map<String, Object> definition : definitions) {
			String key = definition.name.toString()
			Map testObj = testState[key]
			Map prodObj = prodState[key]
			boolean expectedPresent = !definition.containsKey('expectedPresent') || Boolean.TRUE.equals(definition.expectedPresent)

			if (testObj.present != prodObj.present) {
				mismatches.add(key + ' -> presencia distinta TEST=' + testObj.present + ' PROD=' + prodObj.present)
				continue
			}

			if (testObj.present != expectedPresent || prodObj.present != expectedPresent) {
				mismatches.add(key + ' -> presencia fuera de lo esperado. TEST=' + testObj.present + ' PROD=' + prodObj.present + ' EXPECTED=' + expectedPresent)
				continue
			}

			if (!expectedPresent) continue

			if (Boolean.TRUE.equals(definition.compareText) && testObj.present && prodObj.present) {
				String expected = (definition.expected ?: '').toString().trim()
				if (expected && (testObj.text != expected || prodObj.text != expected)) {
					mismatches.add(key + ' -> texto no coincide con esperado. TEST=' + testObj.text + ' PROD=' + prodObj.text + ' EXPECTED=' + expected)
					continue
				}
				if (testObj.text != prodObj.text) {
					mismatches.add(key + ' -> texto distinto TEST=' + testObj.text + ' PROD=' + prodObj.text)
				}
			}
		}

		if (!(Boolean.TRUE.equals(testState['_meta']?.sessionActive) && Boolean.TRUE.equals(prodState['_meta']?.sessionActive))) {
			mismatches.add('session_state -> TEST sessionActive=' + testState['_meta']?.sessionActive +
				' PROD sessionActive=' + prodState['_meta']?.sessionActive +
				' TEST url=' + testState['_meta']?.url +
				' PROD url=' + prodState['_meta']?.url +
				' TEST loginVisible=' + testState['_meta']?.loginButtonStillVisible +
				' PROD loginVisible=' + prodState['_meta']?.loginButtonStillVisible)
		}

		return mismatches
	}
}