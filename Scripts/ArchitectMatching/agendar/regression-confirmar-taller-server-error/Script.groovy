// ─────────────────────────────────────────────────────────────────────────────
// TC: TC-ARCHMATCH-AGENDAR-BUGCONFIRMAR-002
// Plataforma: ArchitectMatching | Área: agendar
// Descripción: Regresion conocida: reproduce el bug confirmado de server error al clickear Confirmar taller (ERROR 3466207270, Next.js RSC). FALLA POR DISENO mientras el bug exista.
// Suites: Platforms/ArchitectMatching/Agendar-Regression
// Precondiciones: credenciales MS en Include/config/templet-credentials.properties
// ─────────────────────────────────────────────────────────────────────────────
import static com.kms.katalon.core.testobject.ObjectRepository.findTestObject

import com.kms.katalon.core.model.FailureHandling
import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI

/*
 * TC-ARCHMATCH-AGENDAR-BUGCONFIRMAR-002 — Regresion del BUG CONFIRMADO.
 * Al clickear "Confirmar taller" (cliente=Globex Inc, arquitecto=Daniel Okoro, franja=Dia completo,
 * modalidad=Remoto) la app rompe con:
 *   "This page couldn't load - A server error occurred. Reload to try again. ERROR 3466207270"
 * Console: "Error: An error occurred in the Server Components render..." (Next.js RSC, digest oculto).
 * Sin requests a /api/ antes del error -> posible fallo server-side en el propio render.
 *
 * DISENO: mientras el bug exista este TC FALLA explicitamente (regresion conocida, suite en rojo).
 * Si algun dia PASA (warning en el log), el bug fue corregido: desbloquear TODOs del HAPPY-001
 * y retirar/ajustar este TC.
 */

String caseId = 'TC-ARCHMATCH-AGENDAR-BUGCONFIRMAR-002'
List failures = []
List warnings = []

String baseUrl = 'https://architect-matching-git-testing-daniel-templetios-projects.vercel.app/'
String bypassToken = System.getenv('VERCEL_AUTOMATION_BYPASS_SECRET') ?: ''
String base = baseUrl.replaceAll('/+$', '')
String bypassQS = "x-vercel-protection-bypass=${bypassToken}&x-vercel-set-bypass-cookie=true"

// Datos EXACTOS de la reproduccion original
String cliente = 'Globex Inc'
String kit = 'GTM Kit 1'
String arquitecto = 'Daniel Okoro'

String JS_SET_VALUE = '''
var el = document.querySelector(arguments[0]);
if (!el) { return 'NOT_FOUND'; }
var setter = Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype, 'value').set;
setter.call(el, arguments[1]);
el.dispatchEvent(new Event('input', { bubbles: true }));
el.dispatchEvent(new Event('change', { bubbles: true }));
return 'OK';
'''

try {
    WebUI.openBrowser('')
    WebUI.maximizeWindow()
    WebUI.navigateToUrl("${base}/agendar?${bypassQS}")

    // Paso 1: Cliente
    def cardCliente = findTestObject('ArchitectMatching/Agendar/Paso1_Cliente/card_cliente', ['nombre': cliente])
    WebUI.waitForElementVisible(cardCliente, 15)
    WebUI.click(cardCliente)

    // Paso 2: GTM Kit
    def buscadorKit = findTestObject('ArchitectMatching/Agendar/Paso2_GTMKit/input_buscar_kit')
    WebUI.waitForElementVisible(buscadorKit, 15)
    WebUI.setText(buscadorKit, kit)
    def cardKit = findTestObject('ArchitectMatching/Agendar/Paso2_GTMKit/card_kit', ['nombre': kit])
    WebUI.waitForElementVisible(cardKit, 10)
    WebUI.click(cardKit)

    // Paso 3: defaults + rango de fechas
    WebUI.waitForElementVisible(findTestObject('ArchitectMatching/Agendar/Paso3_CuandoDonde/combo_pais'), 15)
    WebUI.executeJavaScript(JS_SET_VALUE, Arrays.asList('#w-date', new Date().plus(7).format('yyyy-MM-dd')))
    WebUI.executeJavaScript(JS_SET_VALUE, Arrays.asList('#w-date-to', new Date().plus(21).format('yyyy-MM-dd')))
    WebUI.click(findTestObject('ArchitectMatching/Agendar/Paso3_CuandoDonde/btn_buscar_arquitectos'))

    // Paso 4: Agendar con Daniel Okoro
    def btnAgendar = findTestObject('ArchitectMatching/Agendar/Paso4_Resultados/btn_agendar_arquitecto', ['nombre': arquitecto])
    WebUI.waitForElementVisible(btnAgendar, 20)
    WebUI.click(btnAgendar)

    // Paso 5: franja "Dia completo" y modalidad "Remoto" por default -> Confirmar
    def btnConfirmar = findTestObject('ArchitectMatching/Agendar/Paso5_Reservar/btn_confirmar_taller')
    WebUI.waitForElementVisible(btnConfirmar, 15)
    WebUI.click(btnConfirmar)

    // ---------- Assertion sobre el mensaje de error ----------
    boolean serverError = WebUI.verifyElementPresent(
        findTestObject('ArchitectMatching/Agendar/Paso5_Reservar/msg_error_servidor'), 15, FailureHandling.OPTIONAL)
    if (serverError) {
        boolean textoError = WebUI.verifyTextPresent('A server error occurred', false, FailureHandling.OPTIONAL)
        boolean codigoError = WebUI.verifyTextPresent('3466207270', false, FailureHandling.OPTIONAL)
        WebUI.takeScreenshot()
        failures.add("[BUG CONFIRMADO] Server error al Confirmar taller (texto='${textoError}', codigo 3466207270='${codigoError}'). " +
            'Next.js RSC render error, sin requests /api/ previos. FALLO ESPERADO: regresion conocida hasta que se corrija.')
    } else {
        warnings.add('[REGRESION] El bug NO se reprodujo: posible fix desplegado. Verificar manualmente, ' +
            'desbloquear TODOs de TC-ARCHMATCH-AGENDAR-HAPPY-001 y retirar/ajustar este TC.')
    }
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
