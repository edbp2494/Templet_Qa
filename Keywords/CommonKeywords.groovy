import static com.kms.katalon.core.testobject.ObjectRepository.findTestObject
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import com.kms.katalon.core.annotation.Keyword
import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.exception.WebElementNotFoundException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

public class CommonKeywords {

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
}
