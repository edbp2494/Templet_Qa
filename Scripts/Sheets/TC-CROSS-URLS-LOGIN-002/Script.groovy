import static com.kms.katalon.core.testobject.ObjectRepository.findTestObject
import com.kms.katalon.core.model.FailureHandling as FailureHandling
import com.kms.katalon.core.testobject.ConditionType
import com.kms.katalon.core.testobject.TestObject
import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI

String msUser = CustomKeywords.'TempletPortalKeywords.resolveCredential'('MS_USER', 'USERNAME')
String msPass = CustomKeywords.'TempletPortalKeywords.resolveCredential'('MS_PASS', 'PASSWORD')

List<Map<String, Object>> targets = [
	[name: 'SHEETS_TEST', platform: 'SHEETS', url: 'https://sheets-test.templet.io/admin/manager.php'],
	[name: 'SHEETS_PROD', platform: 'SHEETS', url: 'https://sheets.templet.io/admin/manager.php'],
	[name: 'DECKS_TEST',  platform: 'DECKS',  url: 'https://decks-test.templet.io/admin/manager.php'],
	[name: 'DECKS_PROD',  platform: 'DECKS',  url: 'https://deck.templet.io/admin/manager.php'],
	[name: 'EMAIL_TEST',  platform: 'EMAIL',  url: 'https://emails-test.templet.io/admin/manager.php'],
	[name: 'EMAIL_PROD',  platform: 'EMAIL',  url: 'https://email.templet.io/admin/manager.php'],
	[name: 'MEDIA_TEST',  platform: 'MEDIA',  url: 'https://media-test.templet.io'],
	[name: 'MEDIA_PROD',  platform: 'MEDIA',  url: 'https://media.templet.io']
]

String screenshotDir = System.getProperty('user.dir') + '/Reports/Screenshots/cross-login'
new File(screenshotDir).mkdirs()
List<String> failedTargets = []

TestObject toXpathObject(String name, String xpath) {
	TestObject obj = new TestObject(name)
	obj.addProperty('xpath', ConditionType.EQUALS, xpath)
	return obj
}

boolean clickIfPresent(TestObject obj, int timeoutSeconds) {
	if (WebUI.verifyElementPresent(obj, timeoutSeconds, FailureHandling.OPTIONAL)) {
		WebUI.click(obj, FailureHandling.OPTIONAL)
		return true
	}
	return false
}

boolean clickFirstPresent(List<TestObject> candidates, int timeoutSeconds) {
	for (TestObject candidate : candidates) {
		if (clickIfPresent(candidate, timeoutSeconds)) return true
	}
	return false
}

void performMicrosoftLogin(String username, String password) {
	List<TestObject> entryCandidates = [
		findTestObject('Object Repository/Common/a_Log in with Microsoft'),
		toXpathObject('login_signin_link', "//a[contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'sign in') or contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'log in') or contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'login')]"),
		toXpathObject('login_signin_button', "//button[contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'sign in') or contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'log in') or contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'login')]"),
		toXpathObject('login_href', "//a[contains(@href,'saml/login.php') or contains(@href,'login')]")
	]
	clickFirstPresent(entryCandidates, 5)
	WebUI.waitForPageLoad(20)
	WebUI.delay(1)
	if (WebUI.verifyElementPresent(findTestObject('Object Repository/Common/input_ms_email'), 10, FailureHandling.OPTIONAL)) {
		WebUI.setText(findTestObject('Object Repository/Common/input_ms_email'), username)
		clickFirstPresent([
			toXpathObject('ms_next_input', "//input[@id='idSIButton9' and @type='submit']"),
			toXpathObject('ms_next_button', "//button[contains(.,'Next') or contains(.,'Siguiente')]")
		], 5)
		WebUI.waitForElementVisible(findTestObject('Object Repository/Common/input_ms_password'), 15)
		WebUI.setText(findTestObject('Object Repository/Common/input_ms_password'), password)
		clickFirstPresent([
			toXpathObject('ms_signin_input', "//input[@id='idSIButton9' and @type='submit']"),
			toXpathObject('ms_signin_button', "//button[contains(.,'Sign in') or contains(.,'Iniciar')]")
		], 5)
		clickFirstPresent([
			toXpathObject('ms_yes_input', "//input[@id='idSIButton9' and @type='submit']"),
			toXpathObject('ms_yes_button', "//button[contains(.,'Yes') or contains(.,'Si')]")
		], 4)
	}
}

boolean validatePostLogin(String platform) {
	String currentUrl = WebUI.getUrl().toLowerCase()
	boolean inAdmin = currentUrl.contains('/admin/')
	boolean logoutVisible = WebUI.verifyElementPresent(findTestObject('Tc1/Page_sheets.templet.  Admin/a_Log Out'), 3, FailureHandling.OPTIONAL)
	String pageText = (WebUI.executeJavaScript("return (document && document.body) ? document.body.innerText : ''", null) ?: '').toString().toLowerCase()
	return inAdmin || logoutVisible || pageText.contains('log out') || pageText.contains('dashboard')
}

for (Map<String, Object> target : targets) {
	WebUI.openBrowser('')
	WebUI.maximizeWindow()
	WebUI.navigateToUrl(target.url.toString())
	WebUI.waitForPageLoad(20)
	WebUI.delay(2)
	performMicrosoftLogin(msUser, msPass)
	WebUI.waitForPageLoad(20)
	WebUI.delay(2)
	boolean ok = validatePostLogin(target.platform.toString())
	String snap = screenshotDir + '/' + target.name + '.png'
	WebUI.takeScreenshot(snap)
	if (ok) {
		KeywordUtil.logInfo('[OK] ' + target.name + ' login validado. url=' + WebUI.getUrl())
	} else {
		failedTargets.add(target.name.toString())
		KeywordUtil.logInfo('[FAIL] ' + target.name + ' login no confirmado. url=' + WebUI.getUrl())
	}
	WebUI.closeBrowser()
}

if (failedTargets.isEmpty()) {
	KeywordUtil.markPassed('TC-CROSS-URLS-LOGIN-002 OK en todas las URLs.')
} else {
	KeywordUtil.markFailed('TC-CROSS-URLS-LOGIN-002 con fallos en: ' + failedTargets.join(', '))
}
