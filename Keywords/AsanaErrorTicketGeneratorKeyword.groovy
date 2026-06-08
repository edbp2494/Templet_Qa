import com.kms.katalon.core.annotation.Keyword
import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import internal.GlobalVariable as GlobalVariable
import groovy.json.JsonOutput
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Keyword ejecutado post-suite para procesar errores de Builders Tracking
 * y generar tickets JSON para Asana.
 * 
 * Uso: CustomKeywords.'AsanaErrorTicketGenerator.processBuildersTrackingErrors'()
 */
class AsanaErrorTicketGeneratorKeyword {

	@Keyword
	static void processBuildersTrackingErrors() {
		String suiteStartTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern('yyyy-MM-dd HH:mm:ss'))
		
		KeywordUtil.logInfo("""
╔════════════════════════════════════════════════════════════╗
║  POST-SUITE: Generando Tickets Asana para Builders Tracking║
╔════════════════════════════════════════════════════════════╗
Inicio: ${suiteStartTime}
""")

		try {
			// Ruta del directorio de snapshots
			String projectDir = System.getProperty('user.dir')
			String snapshotDir = projectDir + '/Reports/Tracking/snapshots'
			String ticketsOutputDir = projectDir + '/Reports/asana_tickets'
			
			// Escanear snapshots recientes (últimas 2 horas)
			List<Map<String, Object>> allErrors = AsanaErrorTicketGenerator.scanSnapshotDirectory(snapshotDir, 120)
			
			if (allErrors.isEmpty()) {
				KeywordUtil.logInfo('[ASANA] ✓ No se encontraron errores. Suite exitosa.')
				return
			}
			
			// Generar payload de tickets
			Map<String, Object> payload = AsanaErrorTicketGenerator.generateTicketPayload(allErrors)
			
			// Exportar a JSON
			String exportedFile = AsanaErrorTicketGenerator.exportTicketPayload(payload, ticketsOutputDir)
			
			// Log resumen
			String summary = AsanaErrorTicketGenerator.summarizeTickets(payload)
			KeywordUtil.logInfo(summary)
			
			// Si hay tickets HIGH severity, marcar warning
			int highCount = payload.tickets.count { it.severity == 'HIGH' }
			if (highCount > 0) {
				KeywordUtil.markWarning('[ASANA] Se detectaron ' + highCount + ' errores BLOQUEANTES. Revisar: ' + exportedFile)
			}
			
		} catch (Exception e) {
			KeywordUtil.logInfo('[ERROR] Fallo en procesamiento de tickets: ' + e.message)
			e.printStackTrace()
		}
		
		KeywordUtil.logInfo('══════════════════════════════════════════════════════════════')
	}

	@Keyword
	static void createAsanaTicketFromError(Map<String, Object> errorData) {
		// Este keyword permite crear un ticket Asana desde un error específico
		// Ejemplo: CustomKeywords.'AsanaErrorTicketGenerator.createAsanaTicketFromError'([ title: '...', description: '...' ])
		
		try {
			String asanaProjectGid = GlobalVariable.asana_project_gid
			String asanaApiKey = GlobalVariable.asana_api_key
			String asanaWorkspaceGid = GlobalVariable.asana_workspace_gid
			
			if (!asanaProjectGid || !asanaApiKey) {
				KeywordUtil.markWarning('[ASANA] Credenciales no configuradas. Ticket NO se creó.')
				return
			}
			
			String title = (errorData.title ?: 'Error en Test Case').toString()
			String description = (errorData.description ?: '').toString()
			String severity = (errorData.severity ?: 'MEDIUM').toString()
			
			KeywordUtil.logInfo('[ASANA] Preparando ticket: ' + title)
			// La creación real se hace desde el agente que maneja Asana API
			// Aquí solo preparamos la estructura
			
		} catch (Exception e) {
			KeywordUtil.logInfo('[ERROR] Error creando ticket: ' + e.message)
		}
	}
}
