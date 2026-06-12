import com.kms.katalon.core.util.KeywordUtil
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Genera tickets en Asana basado en errores de test cases.
 * Lee snapshots JSON de ejecuciones fallidas y crea un ticket por fallo.
 */
class AsanaErrorTicketGenerator {

	/**
	 * Lee un archivo de snapshot y extrae los errores/failures.
	 * Retorna una lista de mapas con detalles de cada error.
	 */
	static List<Map<String, Object>> extractErrorsFromSnapshot(File snapshotFile) {
		List<Map<String, Object>> errors = []
		
		if (!snapshotFile || !snapshotFile.exists()) {
			return errors
		}
		
		try {
			def json = new JsonSlurper().parse(snapshotFile)
			List<String> failures = (List) (json.failures ?: [])
			List<String> warnings = (List) (json.warnings ?: [])
			
			failures.each { String failure ->
				errors.add([
					type: 'FAILURE',
					severity: 'HIGH',
					message: failure,
					caseId: json.caseId,
					platform: json.platform,
					tab: json.tab,
					timestamp: json.timestamp,
					url: json.url,
					screenshot: json.screenshot,
					sourceFile: json.sourceFile
				])
			}
			
			// Warnings también se registran pero con severidad menor
			warnings.each { String warning ->
				errors.add([
					type: 'WARNING',
					severity: 'MEDIUM',
					message: warning,
					caseId: json.caseId,
					platform: json.platform,
					tab: json.tab,
					timestamp: json.timestamp,
					url: json.url,
					screenshot: json.screenshot,
					sourceFile: json.sourceFile
				])
			}
		} catch (Exception e) {
			KeywordUtil.logInfo('[ERROR] No se pudo parsear snapshot: ' + snapshotFile.name + ' - ' + e.message)
		}
		
		return errors
	}

	/**
	 * Escanea el directorio de snapshots y extrae todos los errores de archivos recientes.
	 * Retorna lista consolidada de errores.
	 */
	static List<Map<String, Object>> scanSnapshotDirectory(String snapshotDir, int maxAgeMinutes = 120) {
		List<Map<String, Object>> allErrors = []
		File dir = new File(snapshotDir)
		
		if (!dir.exists() || !dir.isDirectory()) {
			KeywordUtil.logInfo('[ASANA] Directorio de snapshots no existe: ' + snapshotDir)
			return allErrors
		}
		
		try {
			long now = System.currentTimeMillis()
			long maxAgeMillis = maxAgeMinutes * 60 * 1000L
			
			dir.listFiles()?.each { File file ->
				if (file.name.endsWith('.json')) {
					// Filtrar solo archivos recientes
					if ((now - file.lastModified()) > maxAgeMillis) {
						return
					}
					
					List<Map<String, Object>> fileErrors = extractErrorsFromSnapshot(file)
					allErrors.addAll(fileErrors)
				}
			}
		} catch (Exception e) {
			KeywordUtil.logInfo('[ERROR] Error escaneando directorio: ' + e.message)
		}
		
		return allErrors
	}

	/**
	 * Genera un JSON consolidado de errores para procesar en Asana.
	 * Estructura: { tickets: [ { title, description, severity, fields } ] }
	 */
	static Map<String, Object> generateTicketPayload(List<Map<String, Object>> errors) {
		List<Map<String, Object>> tickets = []
		
		errors.each { Map<String, Object> error ->
			String severity = (error.severity ?: 'MEDIUM').toString()
			String caseId = (error.caseId ?: 'UNKNOWN').toString()
			String platform = (error.platform ?: 'Unknown').toString()
			String tab = (error.tab ?: '').toString()
			String message = (error.message ?: '').toString()
			String url = (error.url ?: '').toString()
			String timestamp = (error.timestamp ?: '').toString()
			String screenshot = (error.screenshot ?: '').toString()
			String sourceFile = (error.sourceFile ?: '').toString()
			
			// Determinar plataforma/componente
			String component = 'Builders'
			if (platform.contains('Sheet')) component = 'Sheets'
			if (platform.contains('Deck')) component = 'Decks'
			if (platform.contains('Email')) component = 'Email'
			if (platform.contains('Scheduler')) component = 'Schedulers'
			
			// Título del ticket
			String tabLabel = tab ? " [${tab}]" : ''
			String title = "[${component}] ${caseId}${tabLabel} - ${severity}".take(100)
			
			// Descripción detallada
			String description = """
**Test Case:** ${caseId}
**Plataforma:** ${platform}
${tab ? "**Tab:** ${tab}" : ''}
**Timestamp:** ${timestamp}
**URL:** ${url}
**Severity:** ${severity}

**Error:**
${message}

${sourceFile ? "**Source File (repo frontend):** ${sourceFile}" : ''}
${screenshot ? "**Screenshot:** ${screenshot}" : ''}

---
_Generado automáticamente por Katalon - Builders Tracking Suite_
""".stripIndent()
			
			Map<String, Object> ticket = [
				title: title,
				description: description,
				severity: severity,
				caseId: caseId,
				component: component,
				platform: platform,
				tab: tab,
				timestamp: timestamp,
				url: url,
				screenshot: screenshot,
				sourceFile: sourceFile
			]
			
			// Agrupar por case + error para evitar duplicados
			boolean isDuplicate = tickets.any { t ->
				t.caseId == caseId && t.message == message
			}
			
			if (!isDuplicate) {
				tickets.add(ticket)
			}
		}
		
		return [
			generated: LocalDateTime.now().format(DateTimeFormatter.ofPattern('yyyy-MM-dd HH:mm:ss')),
			totalErrors: errors.size(),
			totalTickets: tickets.size(),
			tickets: tickets
		]
	}

	/**
	 * Exporta el payload de tickets a un archivo JSON para revisión/procesamiento posterior.
	 */
	static String exportTicketPayload(Map<String, Object> payload, String outputDir) {
		try {
			new File(outputDir).mkdirs()
			String filename = outputDir + '/asana_tickets_' + LocalDateTime.now().format(DateTimeFormatter.ofPattern('yyyyMMdd_HHmmss')) + '.json'
			new File(filename).text = JsonOutput.prettyPrint(JsonOutput.toJson(payload))
			KeywordUtil.logInfo('[ASANA] Tickets exportados: ' + filename + ' (total=' + payload.tickets.size() + ')')
			return filename
		} catch (Exception e) {
			KeywordUtil.logInfo('[ERROR] No se pudo exportar tickets: ' + e.message)
			return ''
		}
	}

	/**
	 * Retorna un resumen de tickets en formato readable para logs.
	 */
	static String summarizeTickets(Map<String, Object> payload) {
		List<Map> tickets = (List) (payload.tickets ?: [])
		int highCount = tickets.count { it.severity == 'HIGH' }
		int mediumCount = tickets.count { it.severity == 'MEDIUM' }
		int lowCount = tickets.count { it.severity == 'LOW' }
		
		return """
═══════════════════════════════════════════════════════════
RESUMEN DE TICKETS ASANA
═══════════════════════════════════════════════════════════
Total Errores Detectados: ${payload.totalErrors}
Total Tickets Generados: ${payload.totalTickets}
  • HIGH (Bloqueantes): ${highCount}
  • MEDIUM (Importantes): ${mediumCount}
  • LOW (Menores): ${lowCount}

Componentes Afectados:
${tickets.groupBy { it.component }.collect { component, list ->
	"  • ${component}: ${list.size()} tickets"
}.join('\n')}

═══════════════════════════════════════════════════════════
Archivo de tickets: Consultar Reports/asana_tickets_*.json
═══════════════════════════════════════════════════════════
""".stripIndent()
	}
}
