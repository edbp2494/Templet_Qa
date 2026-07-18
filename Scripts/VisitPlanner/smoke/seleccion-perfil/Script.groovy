// ─────────────────────────────────────────────────────────────────────────────
// TC: TC-VISITPLAN-SMOKE-PERFIL-001
// Plataforma: VisitPlanner | Área: smoke
// Descripción: Smoke de la unica pantalla relevada: selector de perfil con C-Level, Comercial, Regional Manager y Gestionar plataforma.
// Suites: Platforms/VisitPlanner/Smoke
// Precondiciones: credenciales MS en Include/config/templet-credentials.properties
// ─────────────────────────────────────────────────────────────────────────────
import static com.kms.katalon.core.testobject.ObjectRepository.findTestObject

import com.kms.katalon.core.model.FailureHandling
import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI

/*
 * TC-VISITPLAN-SMOKE-PERFIL-001 — Smoke de Visit Planner (Escala Visit Planner / CIO Visit Tracker, TESTING).
 * Unica pantalla relevada: selector de perfil (sin auth diferenciada):
 * "Con que perfil desea ingresar?" -> C-Level / Comercial / Regional Manager / Gestionar plataforma.
 */

String caseId = 'TC-VISITPLAN-SMOKE-PERFIL-001'
List failures = []
List warnings = []

String baseUrl = (CustomKeywords.'CommonKeywords.getRequiredGlobal'('VISIT_PLANNER_TEST_URL',
    'https://escala-visit-planner-git-testing-daniel-templetios-projects.vercel.app/')) as String
String bypassToken = (CustomKeywords.'CommonKeywords.getRequiredGlobal'('VERCEL_BYPASS_TOKEN',
    System.getenv('VERCEL_AUTOMATION_BYPASS_SECRET'))) as String
String base = baseUrl.replaceAll('/+$', '')

try {
    WebUI.openBrowser('')
    WebUI.maximizeWindow()
    WebUI.navigateToUrl("${base}/?x-vercel-protection-bypass=${bypassToken}&x-vercel-set-bypass-cookie=true")

    if (!WebUI.verifyElementPresent(findTestObject('VisitPlanner/Home/heading_perfil'), 15, FailureHandling.OPTIONAL)) {
        failures.add('[HOME] Heading "Con que perfil desea ingresar?" no encontrado')
    }
    for (perfil in ['C-Level', 'Comercial', 'Regional Manager', 'Gestionar plataforma']) {
        if (!WebUI.verifyElementPresent(findTestObject('VisitPlanner/Home/opcion_perfil', ['perfil': perfil]), 10, FailureHandling.OPTIONAL)) {
            failures.add("[HOME] Opcion de perfil '${perfil}' no encontrada")
        }
    }
    WebUI.takeScreenshot()
} catch (Exception e) {
    failures.add("[EXCEPCION] ${e.class.simpleName}: ${e.message}")
} finally {
    WebUI.closeBrowser()
}

CustomKeywords.'CommonKeywords.logCaseSummary'(caseId, failures, warnings)
if (failures) {
    KeywordUtil.markFailedAndStop("[${caseId}] Failures: ${failures}")
} else {
    KeywordUtil.logInfo("[${caseId}] PASSED" + (warnings ? " con ${warnings.size()} warnings" : ''))
}
