// ─────────────────────────────────────────────────────────────────────────────
// TC: TC-ARCHMATCH-MODULOS-KITS-001
// Plataforma: ArchitectMatching | Área: modulos
// Descripción: PLACEHOLDER: modulo Catalogo GTM Kits (boton Ver kits) visto en el home, pendiente de mapeo de detalle.
// Suites: Platforms/ArchitectMatching/Agendar-Regression
// Precondiciones: credenciales MS en Include/config/templet-credentials.properties
// ─────────────────────────────────────────────────────────────────────────────
import static com.kms.katalon.core.testobject.ObjectRepository.findTestObject

import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI

/*
 * TC-ARCHMATCH-MODULOS-KITS-001 — PLACEHOLDER del modulo "Catalogo GTM Kits" (boton "Ver kits" en el home de Architect Matching).
 * Sin mapeo de detalle aun. TODO: explorar el modulo, mapear DOM (element-map) y
 * reemplazar este placeholder por asserts reales.
 */

String caseId = 'TC-ARCHMATCH-MODULOS-KITS-001'
List failures = []
List warnings = []

String baseUrl = 'https://architect-matching-git-testing-daniel-templetios-projects.vercel.app/'
String bypassToken = System.getenv('VERCEL_AUTOMATION_BYPASS_SECRET') ?: ''
String base = baseUrl.replaceAll('/+$', '')

try {
    WebUI.openBrowser('')
    WebUI.maximizeWindow()
    WebUI.navigateToUrl("${base}/?x-vercel-protection-bypass=${bypassToken}&x-vercel-set-bypass-cookie=true")
    WebUI.click(findTestObject('ArchitectMatching/Home/btn_por_texto', ['texto': 'Ver kits']))
    WebUI.delay(3)
    WebUI.takeScreenshot()
    warnings.add('[PENDIENTE] Placeholder: mapear DOM del modulo Catalogo GTM Kits y agregar asserts reales.')
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
