// ─────────────────────────────────────────────────────────────────────────────
// TC: TC-VISITPLAN-COMERCIAL-15DIAS-002
// Plataforma: VisitPlanner | Área: comercial
// Descripción: ESQUELETO: regla de negocio - ventana NO reclamable si faltan menos de 15 dias.
// Suites: Platforms/VisitPlanner/Smoke
// Precondiciones: credenciales MS en Include/config/templet-credentials.properties
// ─────────────────────────────────────────────────────────────────────────────
import com.kms.katalon.core.util.KeywordUtil

/*
 * TC-VISITPLAN-COMERCIAL-15DIAS-002 (ESQUELETO — NO EJECUTABLE AUN)
 * Regla de negocio: una ventana NO es reclamable si faltan MENOS de 15 dias para su inicio.
 *
 * Pasos esperados:
 *  1. Ingresar como Comercial
 *  2. Ubicar una ventana cuyo inicio este a menos de 15 dias de hoy
 *  3. Intentar reclamarla
 *  4. Assert: accion bloqueada (boton deshabilitado o mensaje de validacion)
 */

String caseId = 'TC-VISITPLAN-COMERCIAL-15DIAS-002'
List failures = []
List warnings = []

// TODO: selectores pendientes de mapeo DOM en vivo (todo el flujo)
warnings.add('[ESQUELETO] Regla "minimo 15 dias de anticipacion" — completar cuando se mapee el DOM.')

CustomKeywords.'CommonKeywords.logCaseSummary'(caseId, failures, warnings)
KeywordUtil.logInfo("[${caseId}] PASSED con ${warnings.size()} warnings (esqueleto pendiente)")
