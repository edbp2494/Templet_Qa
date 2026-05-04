import static com.kms.katalon.core.testobject.ObjectRepository.findTestObject
import com.kms.katalon.core.annotation.Keyword
import com.kms.katalon.core.model.FailureHandling as FailureHandling
import com.kms.katalon.core.testobject.ConditionType
import com.kms.katalon.core.testobject.TestObject
import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import com.kms.katalon.core.configuration.RunConfiguration
import internal.GlobalVariable as GlobalVariable
import groovy.xml.XmlSlurper
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

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
				KeywordUtil.markFailedAndStop('Safari solo es soportado en macOS.')
			}
			return
		}
		if (browserType == 'UNKNOWN') {
			KeywordUtil.logInfo('No se pudo detectar browserType; se intentará con configuración actual de Katalon.')
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
					if (value != null && value.toString().trim()) return value.toString().trim()
				}
			} catch (Exception ignored) {}
		}
		Map<String, List<String>> envCandidatesByName = [
			'MS_USER'  : ['TEMPLET_MS_USER'],
			'USERNAME' : ['TEMPLET_MS_USER'],
			'MS_PASS'  : ['TEMPLET_MS_PASS'],
			'PASSWORD' : ['TEMPLET_MS_PASS']
		]
		for (String candidate : candidateNames) {
			for (String envName : (envCandidatesByName[candidate] ?: [])) {
				String envValue = System.getenv(envName)
				if (envValue != null && envValue.trim()) return envValue.trim()
			}
		}
		try {
			String credPath = System.getProperty('user.dir') + '/Include/config/templet-credentials.properties'
			File credFile = new File(credPath)
			if (credFile.exists()) {
				Properties props = new Properties()
				credFile.withInputStream { props.load(it) }
				for (String candidate : candidateNames) {
					String val = props.getProperty(candidate)?.trim()
					if (val) {
						KeywordUtil.logInfo("Credencial '${candidate}' encontrada en templet-credentials.properties")
						return val
					}
				}
			}
		} catch (Exception ignored) {}
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
							KeywordUtil.logInfo("Credencial '${candidate}' encontrada en profile: ${profileFile.name}")
							return parsedValue
						}
					}
				}
			}
		} catch (Exception ignored) {}
		KeywordUtil.markFailedAndStop("Falta credencial. Edita Include/config/templet-credentials.properties → agrega MS_PASS=tuPassword")
		return ''
	}

	static TestObject xpathObject(String name, String xpath) {
		TestObject obj = new TestObject(name)
		obj.addProperty('xpath', ConditionType.EQUALS, xpath)
		return obj
	}

	static boolean clickIfPresent(TestObject obj, int timeoutSeconds) {
		if (WebUI.verifyElementPresent(obj, timeoutSeconds, FailureHandling.OPTIONAL)) {
			WebUI.click(obj, FailureHandling.OPTIONAL)
			return true
		}
		return false
	}

	static boolean clickFirstPresent(List<TestObject> candidates, int timeoutSeconds) {
		for (TestObject candidate : candidates) {
			if (clickIfPresent(candidate, timeoutSeconds)) return true
		}
		return false
	}

	@Keyword
	static String currentUrlSafe() {
		try { return WebUI.getUrl() } catch (Exception ignored) { return 'SESSION_LOST' }
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
		WebUI.takeScreenshot(path)
		KeywordUtil.logInfo('[SCREENSHOT] ' + path)
		return path
	}

	@Keyword
	static void safeCloseBrowser() {
		try { WebUI.closeBrowser() } catch (Exception ignored) {}
	}

	@Keyword
	static void openBrowserAndLoginWithMicrosoft(String targetUrl) {
		assertBrowserSupportForHost()
		String username = resolveCredential('MS_USER', 'USERNAME')
		String password = resolveCredential('MS_PASS', 'PASSWORD')
		WebUI.openBrowser('')
		WebUI.maximizeWindow()
		performMicrosoftLoginWithCredentials(targetUrl, username, password)
		if (!isValidAppSession()) {
			KeywordUtil.markFailed('No se completó el login Microsoft. URL final=' + currentUrlSafe())
		}
	}

	@Keyword
	static boolean verifyXPathPresent(String name, String xpath, int timeoutSeconds) {
		return WebUI.verifyElementPresent(xpathObject(name, xpath), timeoutSeconds, FailureHandling.OPTIONAL)
	}

	@Keyword
	static boolean verifyXPathText(String name, String xpath, String expectedText, int timeoutSeconds) {
		TestObject obj = xpathObject(name, xpath)
		if (!WebUI.verifyElementPresent(obj, timeoutSeconds, FailureHandling.OPTIONAL)) return false
		String actual = (WebUI.getText(obj, FailureHandling.OPTIONAL) ?: '').trim().replaceAll('\\s+', ' ')
		return actual == expectedText
	}

	@Keyword
	static boolean clickXPathAndKeepValidSession(String name, String xpath, int timeoutSeconds) {
		TestObject obj = xpathObject(name, xpath)
		if (!WebUI.verifyElementPresent(obj, timeoutSeconds, FailureHandling.OPTIONAL)) return false
		String beforeUrl = currentUrlSafe()
		WebUI.click(obj, FailureHandling.OPTIONAL)
		WebUI.waitForPageLoad(10)
		WebUI.delay(2)
		String afterUrl = currentUrlSafe()
		KeywordUtil.logInfo('[NAV] before=' + beforeUrl + ' after=' + afterUrl)
		return isValidAppSession()
	}

	@Keyword
	static boolean logoutAndVerify() {
		clickFirstPresent([
			xpathObject('logout_link', "//a[contains(normalize-space(.),'Log Out') or contains(normalize-space(.),'Logout') or contains(normalize-space(.),'Sign out')]")
		], 5)
		WebUI.waitForPageLoad(10)
		WebUI.delay(2)
		String currentUrl = currentUrlSafe()
		boolean loginVisible = clickIfPresent(findTestObject('Object Repository/Common/a_Log in with Microsoft'), 2)
		if (loginVisible) return true
		return currentUrl.contains('login.microsoftonline.com') || currentUrl.contains('saml') || !isValidAppSession()
	}

	static void performMicrosoftLoginWithCredentials(String targetUrl, String username, String password) {
		WebUI.navigateToUrl(targetUrl)
		WebUI.waitForPageLoad(20)
		WebUI.delay(1)
		clickFirstPresent([
			findTestObject('Object Repository/Common/a_Log in with Microsoft'),
			xpathObject('login_signin_link', "//a[contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'sign in') or contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'log in') or contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'login') or contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'microsoft')]"),
			xpathObject('login_signin_button', "//button[contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'sign in') or contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'log in') or contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'login')]")
		], 5)
		WebUI.waitForPageLoad(20)
		WebUI.delay(1)
		if (WebUI.verifyElementPresent(findTestObject('Object Repository/Common/input_ms_email'), 10, FailureHandling.OPTIONAL)) {
			WebUI.setText(findTestObject('Object Repository/Common/input_ms_email'), username)
			clickFirstPresent([
				xpathObject('ms_next_input', "//input[@id='idSIButton9' and @type='submit']"),
				xpathObject('ms_next_button', "//button[contains(.,'Next') or contains(.,'Siguiente')]")
			], 5)
			WebUI.waitForElementVisible(findTestObject('Object Repository/Common/input_ms_password'), 15)
			WebUI.setText(findTestObject('Object Repository/Common/input_ms_password'), password)
			clickFirstPresent([
				xpathObject('ms_signin_input', "//input[@id='idSIButton9' and @type='submit']"),
				xpathObject('ms_signin_button', "//button[contains(.,'Sign in') or contains(.,'Iniciar')]")
			], 5)
			clickFirstPresent([
				xpathObject('ms_yes_input', "//input[@id='idSIButton9' and @type='submit']"),
				xpathObject('ms_yes_button', "//button[contains(.,'Yes') or contains(.,'Si')]")
			], 4)
		}
		WebUI.waitForPageLoad(20)
		WebUI.delay(2)
	}

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
					mismatches.add(key + ' -> texto no coincide. TEST=' + testObj.text + ' PROD=' + prodObj.text + ' EXPECTED=' + expected)
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
				' PROD url=' + prodState['_meta']?.url)
		}
		return mismatches
	}
}
