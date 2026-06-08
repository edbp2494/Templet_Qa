import com.kms.katalon.core.annotation.AfterTestCase
import com.kms.katalon.core.annotation.AfterTestSuite
import com.kms.katalon.core.annotation.BeforeTestCase
import com.kms.katalon.core.annotation.BeforeTestSuite
import com.kms.katalon.core.context.TestCaseContext
import com.kms.katalon.core.context.TestSuiteContext
import com.kms.katalon.core.util.KeywordUtil

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * SmokeTestListener — Asegura que la suite de smoke continúe ejecutando
 * incluso si un caso individual falla. Genera un resumen final.
 *
 * Comportamiento:
 *   - Antes de cada caso: Log timestamp + nombre
 *   - Después de cada caso: Registra resultado (PASS/FAIL/ERROR)
 *   - Después de la suite: Genera resumen con estadísticas
 *
 * Katalon por defecto NO detiene la suite cuando un caso falla,
 * pero este listener agrega trazabilidad extra y un resumen claro.
 */
class SmokeTestListener {

	static List<Map<String, String>> results = []
	static String suiteStartTime = ''

	@BeforeTestSuite
	def beforeSuite(TestSuiteContext suiteContext) {
		results = []
		suiteStartTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern('yyyy-MM-dd HH:mm:ss'))
		KeywordUtil.logInfo("═══════════════════════════════════════════════")
		KeywordUtil.logInfo("🚀 SUITE INICIADA: ${suiteContext.getTestSuiteId()}")
		KeywordUtil.logInfo("   Hora: ${suiteStartTime}")
		KeywordUtil.logInfo("═══════════════════════════════════════════════")
	}

	@BeforeTestCase
	def beforeTestCase(TestCaseContext testCaseContext) {
		String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern('HH:mm:ss'))
		KeywordUtil.logInfo("───────────────────────────────────────────────")
		KeywordUtil.logInfo("▶ [${ts}] EJECUTANDO: ${testCaseContext.getTestCaseId()}")
		KeywordUtil.logInfo("───────────────────────────────────────────────")
	}

	@AfterTestCase
	def afterTestCase(TestCaseContext testCaseContext) {
		String status = testCaseContext.getTestCaseStatus()
		String caseId = testCaseContext.getTestCaseId()
		String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern('HH:mm:ss'))

		String icon = status == 'PASSED' ? '✅' : (status == 'FAILED' ? '❌' : '⚠️')
		KeywordUtil.logInfo("${icon} [${ts}] ${caseId} → ${status}")

		results.add([
			caseId: caseId,
			status: status,
			time: ts
		])
	}

	@AfterTestSuite
	def afterSuite(TestSuiteContext suiteContext) {
		String endTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern('yyyy-MM-dd HH:mm:ss'))
		int total = results.size()
		int passed = results.count { it.status == 'PASSED' }
		int failed = results.count { it.status == 'FAILED' }
		int error = total - passed - failed

		KeywordUtil.logInfo("")
		KeywordUtil.logInfo("═══════════════════════════════════════════════")
		KeywordUtil.logInfo("📊 RESUMEN DE SUITE: ${suiteContext.getTestSuiteId()}")
		KeywordUtil.logInfo("═══════════════════════════════════════════════")
		KeywordUtil.logInfo("   Inicio : ${suiteStartTime}")
		KeywordUtil.logInfo("   Fin    : ${endTime}")
		KeywordUtil.logInfo("   Total  : ${total}")
		KeywordUtil.logInfo("   ✅ Pass : ${passed}")
		KeywordUtil.logInfo("   ❌ Fail : ${failed}")
		KeywordUtil.logInfo("   ⚠️  Error: ${error}")
		KeywordUtil.logInfo("───────────────────────────────────────────────")

		results.each { r ->
			String icon = r.status == 'PASSED' ? '✅' : (r.status == 'FAILED' ? '❌' : '⚠️')
			KeywordUtil.logInfo("   ${icon} ${r.caseId}")
		}

		KeywordUtil.logInfo("═══════════════════════════════════════════════")

		// Guardar resumen en archivo
		try {
			String reportDir = System.getProperty('user.dir') + '/Reports/Smoke-Summary'
			new File(reportDir).mkdirs()
			String reportTs = LocalDateTime.now().format(DateTimeFormatter.ofPattern('yyyyMMdd_HHmmss'))
			String reportPath = reportDir + '/smoke_summary_' + reportTs + '.txt'

			StringBuilder sb = new StringBuilder()
			sb.append("SMOKE SUMMARY — ${suiteContext.getTestSuiteId()}\n")
			sb.append("Inicio: ${suiteStartTime} | Fin: ${endTime}\n")
			sb.append("Total: ${total} | Pass: ${passed} | Fail: ${failed} | Error: ${error}\n")
			sb.append("─────────────────────────────────────────\n")
			results.each { r ->
				sb.append("  [${r.status}] ${r.caseId}\n")
			}
			new File(reportPath).text = sb.toString()
			KeywordUtil.logInfo("📝 Resumen guardado: ${reportPath}")
		} catch (Exception e) {
			KeywordUtil.logInfo("No se pudo guardar resumen: ${e.message}")
		}
	}
}
