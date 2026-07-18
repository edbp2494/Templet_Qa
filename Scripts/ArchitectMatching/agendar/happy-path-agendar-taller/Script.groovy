// ─────────────────────────────────────────────────────────────────────────────
// TC: TC-ARCHMATCH-AGENDAR-HAPPY-001
// Plataforma: ArchitectMatching | Área: agendar
// Descripción: Happy path del wizard de agendamiento: Cliente - GTM Kit - Cuando y donde - Resultados - Reservar - Confirmar. Incluye TODOs de persistencia/edicion/borrado bloqueados por bug conocido.
// Suites: Platforms/ArchitectMatching/Agendar-Regression
// Precondiciones: credenciales MS en Include/config/templet-credentials.properties
// ─────────────────────────────────────────────────────────────────────────────
import static com.kms.katalon.core.testobject.ObjectRepository.findTestObject

import com.kms.katalon.core.model.FailureHandling
import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI

/*
 * TC-ARCHMATCH-AGENDAR-HAPPY-001 — Happy path del wizard de agendamiento (Architect Matching, TESTING).
 * Flujo: Cliente -> GTM Kit -> Cuando y donde -> Resultados -> Reservar -> Confirmar.
 *
 * >>> BLOQUEANTE CONOCIDO <<<
 * "Confirmar taller" rompe con server error ERROR 3466207270 (ver TC-ARCHMATCH-AGENDAR-BUGCONFIRMAR-002).
 * Mientras exista, este TC falla en la confirmacion y la verificacion de persistencia tras reload,
 * edicion y borrado queda BLOQUEADA (TODOs al final).
 *
 * Bypass de Vercel: GlobalVariable VERCEL_BYPASS_TOKEN (o env VERCEL_AUTOMATION_BYPASS_SECRET) via
 * query param x-vercel-protection-bypass + x-vercel-set-bypass-cookie=true en la 1ra navegacion.
 * El token NUNCA va hardcodeado.
 */

String caseId = 'TC-ARCHMATCH-AGENDAR-HAPPY-001'
List failures = []
List warnings = []

String baseUrl = 'https://architect-matching-git-testing-daniel-templetios-projects.vercel.app/'
String bypassToken = System.getenv('VERCEL_AUTOMATION_BYPASS_SECRET') ?: ''
String base = baseUrl.replaceAll('/+$', '')
String bypassQS = "x-vercel-protection-bypass=${bypassToken}&x-vercel-set-bypass-cookie=true"

// Datos de prueba relevados en vivo
String cliente = 'Globex Inc'
String kit = 'GTM Kit 1'
String arquitecto = 'Daniel Okoro'

// Setter React-safe para inputs type=date (setText no dispara el estado de React)
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

    // ---------- Paso 1: Cliente ----------
    def cardCliente = findTestObject('ArchitectMatching/Agendar/Paso1_Cliente/card_cliente', ['nombre': cliente])
    WebUI.waitForElementVisible(cardCliente, 15)
    WebUI.click(cardCliente)

    // ---------- Paso 2: GTM Kit (seleccion por texto EXACTO — ver KITMATCH-003) ----------
    def buscadorKit = findTestObject('ArchitectMatching/Agendar/Paso2_GTMKit/input_buscar_kit')
    WebUI.waitForElementVisible(buscadorKit, 15)
    WebUI.setText(buscadorKit, kit)
    def cardKit = findTestObject('ArchitectMatching/Agendar/Paso2_GTMKit/card_kit', ['nombre': kit])
    WebUI.waitForElementVisible(cardKit, 10)
    WebUI.click(cardKit)

    // ---------- Paso 3: Cuando y donde (defaults: Cualquier pais/ciudad, Remoto) ----------
    WebUI.waitForElementVisible(findTestObject('ArchitectMatching/Agendar/Paso3_CuandoDonde/combo_pais'), 15)
    String desde = new Date().plus(7).format('yyyy-MM-dd')
    String hasta = new Date().plus(21).format('yyyy-MM-dd')
    WebUI.executeJavaScript(JS_SET_VALUE, Arrays.asList('#w-date', desde))
    WebUI.executeJavaScript(JS_SET_VALUE, Arrays.asList('#w-date-to', hasta))
    WebUI.click(findTestObject('ArchitectMatching/Agendar/Paso3_CuandoDonde/btn_buscar_arquitectos'))

    // ---------- Paso 4: Resultados ----------
    def btnAgendar = findTestObject('ArchitectMatching/Agendar/Paso4_Resultados/btn_agendar_arquitecto', ['nombre': arquitecto])
    WebUI.waitForElementVisible(btnAgendar, 20)
    WebUI.click(btnAgendar)

    // ---------- Paso 5: Reservar / Confirma el taller ----------
    def btnConfirmar = findTestObject('ArchitectMatching/Agendar/Paso5_Reservar/btn_confirmar_taller')
    WebUI.waitForElementVisible(btnConfirmar, 15)
    if (!WebUI.verifyTextPresent(cliente, false, FailureHandling.OPTIONAL)) {
        failures.add("[RESUMEN] Cliente '${cliente}' no visible en el resumen del paso 5")
    }
    if (!WebUI.verifyTextPresent(kit, false, FailureHandling.OPTIONAL)) {
        failures.add("[RESUMEN] Kit '${kit}' no visible en el resumen (posible mismatch — ver TC-ARCHMATCH-AGENDAR-KITMATCH-003)")
    }
    if (!WebUI.verifyTextPresent(arquitecto, false, FailureHandling.OPTIONAL)) {
        failures.add("[RESUMEN] Arquitecto '${arquitecto}' no visible en el resumen del paso 5")
    }
    // Franja "Dia completo" (#w-slot) y modalidad "Remoto" (#w-bmodality) quedan por default
    WebUI.setText(findTestObject('ArchitectMatching/Agendar/Paso5_Reservar/textarea_notas'),
        'Reserva creada por Katalon - Templet QA (TC-ARCHMATCH-AGENDAR-HAPPY-001)')
    WebUI.click(btnConfirmar)
    WebUI.delay(2)

    // ---------- Deteccion del bug conocido ----------
    boolean serverError = WebUI.verifyElementPresent(
        findTestObject('ArchitectMatching/Agendar/Paso5_Reservar/msg_error_servidor'), 10, FailureHandling.OPTIONAL)
    if (serverError) {
        WebUI.takeScreenshot()
        failures.add('[BLOQUEANTE] Bug conocido ERROR 3466207270 al Confirmar taller (ver TC-ARCHMATCH-AGENDAR-BUGCONFIRMAR-002). ' +
            'Persistencia/edicion/borrado sin cobertura hasta el fix.')
    } else {
        // ---------- Post-confirmacion (desbloquear cuando se corrija el bug) ----------
        // TODO: assert de exito real (la UI post-confirmacion nunca se pudo relevar por el bug)
        WebUI.refresh()
        if (!WebUI.verifyTextPresent(cliente, false, FailureHandling.OPTIONAL)) {
            warnings.add('[PERSISTENCIA] Datos no visibles tras reload — validar si aplica: app solo frontend (datos maqueta, sin BD real)')
        }
        // TODO: edicion del taller agendado (UI no relevada — bloqueada por el bug)
        // TODO: borrado del taller agendado (UI no relevada — bloqueada por el bug)
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
