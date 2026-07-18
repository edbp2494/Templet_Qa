// ─────────────────────────────────────────────────────────────────────────────
// TC: TC-ARCHMATCH-AGENDAR-KITMATCH-003
// Plataforma: ArchitectMatching | Área: agendar
// Descripción: Selecciona un GTM Kit por texto exacto y valida que el resumen (paso 5) muestre el mismo kit. Vigila posible bug de mismatch de indice/orden (card GTM Kit 1 mostro GTM Kit 3).
// Suites: Platforms/ArchitectMatching/Agendar-Regression
// Precondiciones: credenciales MS en Include/config/templet-credentials.properties
// ─────────────────────────────────────────────────────────────────────────────
import static com.kms.katalon.core.testobject.ObjectRepository.findTestObject

import com.kms.katalon.core.model.FailureHandling
import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI

/*
 * TC-ARCHMATCH-AGENDAR-KITMATCH-003 — Verificacion de seleccion de GTM Kit.
 * POSIBLE BUG detectado en el relevamiento: al clickear la card visualmente correspondiente a
 * "GTM Kit 1", el resumen (paso 5) mostro "GTM Kit 3". Puede ser mal-click o bug real de indice/orden.
 * Estrategia: filtrar con el buscador, seleccionar por TEXTO EXACTO y validar que el MISMO texto
 * aparece en el resumen final. NO confirma el taller (evita disparar el bug BUGCONFIRMAR-002).
 */

String caseId = 'TC-ARCHMATCH-AGENDAR-KITMATCH-003'
List failures = []
List warnings = []

String baseUrl = 'https://architect-matching-git-testing-daniel-templetios-projects.vercel.app/'
String bypassToken = System.getenv('VERCEL_AUTOMATION_BYPASS_SECRET') ?: ''
String base = baseUrl.replaceAll('/+$', '')
String bypassQS = "x-vercel-protection-bypass=${bypassToken}&x-vercel-set-bypass-cookie=true"

String cliente = 'Globex Inc'
String kitEsperado = 'GTM Kit 1'
String kitSospechoso = 'GTM Kit 3' // el que aparecio por error durante el relevamiento
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

    // Paso 2: filtrar y clickear el kit por texto EXACTO
    def buscadorKit = findTestObject('ArchitectMatching/Agendar/Paso2_GTMKit/input_buscar_kit')
    WebUI.waitForElementVisible(buscadorKit, 15)
    WebUI.setText(buscadorKit, kitEsperado)
    def cardKit = findTestObject('ArchitectMatching/Agendar/Paso2_GTMKit/card_kit', ['nombre': kitEsperado])
    WebUI.waitForElementVisible(cardKit, 10)
    WebUI.click(cardKit)

    // Paso 3: defaults + fechas -> buscar arquitectos
    WebUI.waitForElementVisible(findTestObject('ArchitectMatching/Agendar/Paso3_CuandoDonde/combo_pais'), 15)
    WebUI.executeJavaScript(JS_SET_VALUE, Arrays.asList('#w-date', new Date().plus(7).format('yyyy-MM-dd')))
    WebUI.executeJavaScript(JS_SET_VALUE, Arrays.asList('#w-date-to', new Date().plus(21).format('yyyy-MM-dd')))
    WebUI.click(findTestObject('ArchitectMatching/Agendar/Paso3_CuandoDonde/btn_buscar_arquitectos'))

    // Paso 4: Agendar
    def btnAgendar = findTestObject('ArchitectMatching/Agendar/Paso4_Resultados/btn_agendar_arquitecto', ['nombre': arquitecto])
    WebUI.waitForElementVisible(btnAgendar, 20)
    WebUI.click(btnAgendar)

    // Paso 5: validar el kit en el resumen SIN confirmar
    WebUI.waitForElementVisible(findTestObject('ArchitectMatching/Agendar/Paso5_Reservar/btn_confirmar_taller'), 15)
    WebUI.takeScreenshot()

    // Lectura puntual del valor del resumen (selector heuristico, con fallback a verifyTextPresent)
    String kitEnResumen = ''
    try {
        kitEnResumen = WebUI.getText(findTestObject('ArchitectMatching/Agendar/Paso5_Reservar/valor_resumen', ['label': 'GTM Kit']))
        KeywordUtil.logInfo("[${caseId}] Resumen - GTM Kit: '${kitEnResumen}'")
    } catch (Exception e) {
        warnings.add('[SELECTOR] No se pudo leer el valor puntual del resumen (heuristico) — se usa verifyTextPresent como fallback. Pedir data-testid.')
    }

    boolean kitOk = WebUI.verifyTextPresent(kitEsperado, false, FailureHandling.OPTIONAL)
    boolean kitIncorrectoVisible = WebUI.verifyTextPresent(kitSospechoso, false, FailureHandling.OPTIONAL)

    if (!kitOk || kitIncorrectoVisible || (kitEnResumen && !kitEnResumen.contains(kitEsperado))) {
        failures.add("[KIT-MISMATCH] Se selecciono '${kitEsperado}' por texto exacto y el resumen muestra otro kit " +
            "(leido: '${kitEnResumen}', '${kitSospechoso}' visible: ${kitIncorrectoVisible}). BUG de indice/orden en las cards del Paso 2 CONFIRMADO.")
    } else {
        KeywordUtil.logInfo("[${caseId}] OK: el resumen muestra '${kitEsperado}'. El mismatch del relevamiento habria sido un mal-click.")
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
