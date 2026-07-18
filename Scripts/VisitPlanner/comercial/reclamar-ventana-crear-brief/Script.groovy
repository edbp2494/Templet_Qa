// ─────────────────────────────────────────────────────────────────────────────
// TC: TC-VISITPLAN-COMERCIAL-BRIEF-001
// Plataforma: VisitPlanner | Área: comercial
// Descripción: ESQUELETO (prioridad segun brief 02/07): Comercial reclama ventana de C-Level (min 15 dias) y crea Brief. Selectores pendientes de mapeo DOM en vivo.
// Suites: Platforms/VisitPlanner/Smoke
// Precondiciones: credenciales MS en Include/config/templet-credentials.properties
// ─────────────────────────────────────────────────────────────────────────────
import static com.kms.katalon.core.testobject.ObjectRepository.findTestObject

import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI

/*
 * TC-VISITPLAN-COMERCIAL-BRIEF-001 (ESQUELETO — NO EJECUTABLE AUN)
 * PRIORIDAD ALTA: flujo Comercial, el de mayor complejidad de negocio (brief reunion 02/07, NO verificado en la app).
 *
 * Pasos esperados:
 *  1. Home -> ingresar con perfil "Comercial" ("Agenda de reuniones por cliente")
 *  2. Ver ventanas de disponibilidad de los 3 C-Levels de la region (Alejandro, Harold, Ignacio)
 *  3. Reclamar una ventana valida (regla: minimo 15 dias de anticipacion)
 *  4. Seleccionar cuenta / pais / hora / modalidad / contacto
 *  5. Crear Brief: objetivo, contexto, asistentes, temas, riesgos, proximos pasos
 *  6. Assert: ventana reclamada + brief creado y visible
 */

String caseId = 'TC-VISITPLAN-COMERCIAL-BRIEF-001'
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

    // Paso 1 (unico selector confirmado): entrar como Comercial
    WebUI.click(findTestObject('VisitPlanner/Home/opcion_perfil', ['perfil': 'Comercial']))
    WebUI.delay(2)
    WebUI.takeScreenshot()

    // TODO: selectores pendientes de mapeo DOM en vivo (lista de ventanas de disponibilidad por C-Level: Alejandro, Harold, Ignacio)
    // TODO: selectores pendientes de mapeo DOM en vivo (accion "reclamar ventana" + validacion de la regla >= 15 dias)
    // TODO: selectores pendientes de mapeo DOM en vivo (form cuenta / pais / hora / modalidad / contacto)
    // TODO: selectores pendientes de mapeo DOM en vivo (form Brief: objetivo, contexto, asistentes, temas, riesgos, proximos pasos)
    // TODO: asserts finales — ventana reclamada y brief visible

    warnings.add('[ESQUELETO] Completar cuando se mapee el DOM del flujo Comercial (prioridad segun brief 02/07).')
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
