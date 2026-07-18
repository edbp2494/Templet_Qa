// ─────────────────────────────────────────────────────────────────────────────
// TC: TC-QA-CYCLE-REPORT-001
// Plataforma: QA | Área: (raíz)
// Descripción: Genera el dashboard de ciclo QA (24->24) corriendo qa-metrics/generate_cycle_report.py. Salida en docs/qa-cycles/latest.html + carpeta del ciclo. No hace push (el CI lo hace el dia 24).
// Suites: QA/Generate-Cycle-Report
// Precondiciones: credenciales MS en Include/config/templet-credentials.properties
// ─────────────────────────────────────────────────────────────────────────────
import com.kms.katalon.core.util.KeywordUtil

// TC-QA-CYCLE-REPORT-001 — Genera el dashboard de ciclo QA (docs/qa-cycles/latest.html)
// Corre el generador Python del proyecto. NO hace push (eso lo hace el CI el dia 24).
String repo = System.getProperty('user.dir')
String script = repo + File.separator + 'qa-metrics' + File.separator + 'generate_cycle_report.py'

List<List<String>> candidates = [
	['python', script, '--repo', repo],
	['py', '-3', script, '--repo', repo],
	['python3', script, '--repo', repo],
]

boolean ok = false
String out = ''
String tried = ''
for (List<String> cmd : candidates) {
	tried += cmd[0] + ' '
	try {
		Process p = new ProcessBuilder(cmd).redirectErrorStream(true).directory(new File(repo)).start()
		out = p.getInputStream().getText('UTF-8')
		p.waitFor()
		if (p.exitValue() == 0) { ok = true; break }
	} catch (Throwable t) {
		// probar siguiente interprete
	}
}

if (out) { KeywordUtil.logInfo(out.trim()) }
if (ok) {
	KeywordUtil.markPassed('[QA-CYCLE] Reporte generado: ' + repo + '\\docs\\qa-cycles\\latest.html')
} else {
	KeywordUtil.markFailed('[QA-CYCLE] No se pudo generar el reporte. Interpretes probados: ' + tried.trim() + '. Verifica que Python este instalado y en PATH.')
}
