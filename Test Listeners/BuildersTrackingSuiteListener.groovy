import com.kms.katalon.core.annotation.AfterTestSuite
import com.kms.katalon.core.context.TestSuiteContext
import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import com.kms.katalon.core.webui.driver.DriverFactory
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Test Listener para Builders Tracking Suite
 * Ejecuta POST-suite para generar tickets Asana basado en errores
 */
class BuildersTrackingSuiteListener {

	@AfterTestSuite
	def afterTestSuite(TestSuiteContext testSuiteContext) {
		String suiteName = testSuiteContext.getTestSuiteId()
		LocalDateTime endTime = LocalDateTime.now()
		
		// Solo procesar si es la suite de Builders Tracking o las suites Fase 2 Repos-Coverage
		if (!suiteName.contains('Tracking') && !suiteName.contains('builders') && !suiteName.contains('Repos-Coverage')) {
			return
		}
		
		KeywordUtil.logInfo("""
╔════════════════════════════════════════════════════════════╗
║         BUILDERS TRACKING - POST-SUITE HOOK                ║
║                   Fin de ejecución: ${endTime.format(DateTimeFormatter.ofPattern('yyyy-MM-dd HH:mm:ss'))}
╚════════════════════════════════════════════════════════════╝
""")
		
		try {
			// Cerrar navegador si está abierto
			try {
				if (DriverFactory.getWebDriver() != null) {
					WebUI.closeBrowser()
				}
			} catch (Exception ignored) {}
			
			// Procesar errores y generar tickets Asana
			CustomKeywords.'AsanaErrorTicketGeneratorKeyword.processBuildersTrackingErrors'()
			
		} catch (Exception e) {
			KeywordUtil.logInfo('[ERROR] Fallo en post-suite: ' + e.message)
			e.printStackTrace()
		}
		
		KeywordUtil.logInfo('═══════════════════════════════════════════════════════════════')
	}
}
