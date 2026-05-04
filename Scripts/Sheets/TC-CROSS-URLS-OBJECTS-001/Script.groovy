import static com.kms.katalon.core.testobject.ObjectRepository.findTestObject
import com.kms.katalon.core.model.FailureHandling as FailureHandling
import com.kms.katalon.core.testobject.ConditionType
import com.kms.katalon.core.testobject.TestObject
import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI

List<Map<String, String>> targets = [
	[name: 'SHEETS_TEST', url: 'https://sheets-test.templet.io/admin/manager.php'],
	[name: 'SHEETS_PROD', url: 'https://sheets.templet.io/admin/manager.php'],
	[name: 'DECKS_TEST',  url: 'https://decks-test.templet.io/admin/manager.php'],
	[name: 'DECKS_PROD',  url: 'https://deck.templet.io/admin/manager.php'],
	[name: 'EMAIL_TEST',  url: 'https://emails-test.templet.io/admin/manager.php'],
	[name: 'EMAIL_PROD',  url: 'https://email.templet.io/admin/manager.php'],
	[name: 'MEDIA_TEST',  url: 'https://media-test.templet.io'],
	[name: 'MEDIA_PROD',  url: 'https://media.templet.io']
]

String screenshotDir = System.getProperty('user.dir') + '/Reports/Screenshots/cross-urls'
new File(screenshotDir).mkdirs()

List<String> failedTargets = []

def toXpathObject = { String name, String xpath ->
	TestObject obj = new TestObject(name)
	obj.addProperty('xpath', ConditionType.EQUALS, xpath)
	return obj
}

def existsObject = { String repoPath, int timeoutSeconds ->
	return WebUI.verifyElementPresent(findTestObject(repoPath), timeoutSeconds, FailureHandling.OPTIONAL)
}

def hasLoginWords = {
	String pageText = (WebUI.executeJavaScript("return (document && document.body) ? document.body.innerText : ''", null) ?: '').toString().toLowerCase()
	String pageHtml = (WebUI.executeJavaScript("return (document && document.documentElement) ? document.documentElement.outerHTML : ''", null) ?: '').toString().toLowerCase()
	return pageText.contains('sign in') || pageText.contains('log in') || pageText.contains('login') ||
		pageHtml.contains('sign in') || pageHtml.contains('log in') || pageHtml.contains('login')
}

def clickLoginEntryPoint = {
	List<TestObject> candidates = [
		toXpathObject('login_by_href', "//a[contains(@href,'saml/login.php') or contains(@href,'login')]"),
		toXpathObject('login_by_text_microsoft', "//a[contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'microsoft')]"),
		toXpathObject('login_by_text_signin', "//a[contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'sign in')]"),
		toXpathObject('login_by_text_login', "//a[contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'log in') or contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'login')]"),
		toXpathObject('button_by_text_signin', "//button[contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'sign in') or contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'log in') or contains(translate(normalize-space(.),'ABCDEFGHIJKLMNOPQRSTUVWXYZ','abcdefghijklmnopqrstuvwxyz'),'login')]")
	]
	for (TestObject candidate : candidates) {
		if (WebUI.verifyElementPresent(candidate, 3, FailureHandling.OPTIONAL)) {
			WebUI.click(candidate)
			WebUI.waitForPageLoad(15)
			WebUI.delay(1)
			return true
		}
	}
	return false
}

for (Map<String, String> target : targets) {
	WebUI.openBrowser('')
	WebUI.maximizeWindow()
	WebUI.navigateToUrl(target.url)
	WebUI.waitForPageLoad(20)
	WebUI.delay(2)

	boolean msButton = existsObject('Object Repository/Common/a_Log in with Microsoft', 8)
	boolean msEmail = existsObject('Object Repository/Common/input_ms_email', 5)
	boolean msPassword = existsObject('Object Repository/Common/input_ms_password', 5)
	boolean looksLikeApp = WebUI.getUrl().toLowerCase().contains('/admin/')
	boolean loginWords = hasLoginWords()

	if (!(msButton || msEmail || msPassword || looksLikeApp) && loginWords) {
		boolean clicked = clickLoginEntryPoint()
		if (clicked) {
			msButton = existsObject('Object Repository/Common/a_Log in with Microsoft', 3)
			msEmail = existsObject('Object Repository/Common/input_ms_email', 6)
			msPassword = existsObject('Object Repository/Common/input_ms_password', 6)
			looksLikeApp = WebUI.getUrl().toLowerCase().contains('/admin/')
		}
	}

	String snap = screenshotDir + '/' + target.name + '.png'
	WebUI.takeScreenshot(snap)

	boolean isOk = [msButton, msEmail, msPassword, looksLikeApp, loginWords].any { it }

	if (isOk) {
		KeywordUtil.logInfo("[OK] ${target.name} -> ${target.url}")
	} else {
		failedTargets.add(target.name)
		KeywordUtil.logInfo("[FAIL] ${target.name} -> ${target.url}")
	}
	WebUI.closeBrowser()
}

if (failedTargets.isEmpty()) {
	KeywordUtil.markPassed('TC-CROSS-URLS-OBJECTS-001 OK en todas las URLs.')
} else {
	KeywordUtil.markFailed('TC-CROSS-URLS-OBJECTS-001 con fallos en: ' + failedTargets.join(', '))
}
