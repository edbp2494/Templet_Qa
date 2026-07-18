// ─────────────────────────────────────────────────────────────────────────────
// TC: TC-VISITPLAN-COMERCIAL-FERIADO-003
// Plataforma: VisitPlanner | Área: comercial
// Descripción: ESQUELETO: regla de negocio - alerta si hay feriado dentro de la ventana.
// Suites: Platforms/VisitPlanner/Smoke
// Precondiciones: credenciales MS en Include/config/templet-credentials.properties
// ─────────────────────────────────────────────────────────────────────────────
import com.kms.katalon.core.util.KeywordUtil

/*
 * TC-VISITPLAN-COMERCIAL-FERIADO-003 (ESQUELETO — NO EJECUTABLE AUN)
 * Regla de negocio: la app debe alertar si hay un feriado dentro de la ventana seleccionada.
 *
 * Pasos esperados:
 *  1. Ingresar como Comercial
 *  2. Seleccionar/reclamar una ventana que contenga un feriado
 *  3. Assert: se muestra la alerta de feriado
 *  TODO extra: definir fixture de feriado (fuente de feriados de la app desconocida)
 */

String caseId = 'TC-VISITPLAN-COMERCIAL-FERIADO-003'
List failures = []
List warnings = []

// TODO: selectores pendientes de mapeo DOM en vivo (todo el flujo)
warnings.add('[ESQUELETO] Alerta de feriado en ventana — completar cuando se mapee el DOM.')

CustomKeywords.'CommonKeywords.logCaseSummary'(caseId, failures, warnings)
KeywordUtil.logInfo("[${caseId}] PASSED con ${warnings.size()} warnings (esqueleto pendiente)")
