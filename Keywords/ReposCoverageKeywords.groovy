import com.kms.katalon.core.annotation.Keyword
import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.driver.DriverFactory
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import org.openqa.selenium.By
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Fase 2 - Cobertura generada desde los repos frontend (Templet-Product-Team).
 * Valida pantallas a partir de element-maps JSON (Include/config/element-maps/*.json)
 * producidos por el skill katalon-element-mapper.
 *
 * Cada failure/warning sigue el formato accionable obligatorio:
 *   [TC][PASO n: desc][SELECTOR primario | fallback] Esperado vs Encontrado
 *   ARCHIVO FUENTE: ruta en el repo frontend
 *   ACCION SUGERIDA: selector en Katalon vs bug en la app
 *
 * Escribe snapshot JSON en Reports/Tracking/snapshots/ para que
 * AsanaErrorTicketGenerator clasifique HIGH (failures) / MEDIUM (warnings).
 */
class ReposCoverageKeywords {

	static int countByXPath(String xpath) {
		try {
			return DriverFactory.getWebDriver().findElements(By.xpath(xpath)).size()
		} catch (Exception ignored) {
			return 0
		}
	}

	static String deriveUrlRoot(String url) {
		int idx = url.indexOf('/admin/')
		if (idx > 0) return url.substring(0, idx + 1)
		return url.endsWith('/') ? url : url + '/'
	}

	static String buildTargetUrl(String baseUrl, String path) {
		if (!path) return baseUrl
		return (baseUrl.endsWith('/') ? baseUrl : baseUrl + '/') + path
	}

	static void gotoTarget(String url) {
		WebUI.navigateToUrl(url)
		WebUI.waitForPageLoad(30)
	}

	static String buildIssue(String caseId, int paso, String pasoDesc, String selectorDesc, String esperado, String encontrado, String sourceRepo, String sourceFile, String accion) {
		String src = (!sourceRepo || sourceFile.startsWith(sourceRepo)) ? sourceFile : (sourceRepo + '/' + sourceFile)
		return "[${caseId}][PASO ${paso}: ${pasoDesc}][SELECTOR ${selectorDesc}] Esperado: ${esperado}. Encontrado: ${encontrado}.\n" +
			"ARCHIVO FUENTE: ${src}\n" +
			"ACCION SUGERIDA: ${accion}"
	}

	@Keyword
	static void validateScreenFromElementMap(Map config) {
		String caseId = (config.caseId ?: 'TC-UNKNOWN').toString()
		String projectDir = System.getProperty('user.dir')
		String mapPath = projectDir + '/' + config.mapRelativePath
		List failures = []
		List warnings = []

		Map map = TempletPortalKeywords.readJsonIfExists(mapPath)
		if (!map || !map.meta) {
			KeywordUtil.markFailedAndStop("[${caseId}][PASO 0: cargar element-map][${mapPath}] Esperado: JSON valido. Encontrado: archivo faltante o invalido.\nARCHIVO FUENTE: Include/config/element-maps\nACCION SUGERIDA: regenerar el mapa con el skill katalon-element-mapper.")
			return
		}
		Map meta = (Map) map.meta
		String baseUrl = CommonKeywords.getRequiredGlobal(meta.environment_variable.toString(), meta.fallback_url).toString()
		String targetUrl = buildTargetUrl(baseUrl, (meta.direct_url_path ?: '').toString())
		String urlRoot = deriveUrlRoot(baseUrl)
		String sourceRepo = (meta.source_repo ?: '').toString()

		// Sesion: 1 login por suite (isReuseDriver=true) - reusar driver vivo si existe
		if (!TempletPortalKeywords.isBrowserSessionAlive()) {
			TempletPortalKeywords.openBrowserAndLoginWithMicrosoft(targetUrl)
			// FIX 2026-06-12: tras el SSO el browser puede quedar en home;
			// re-navegar SIEMPRE a la pantalla objetivo antes de validar.
			gotoTarget(targetUrl)
		} else {
			gotoTarget(targetUrl)
			if (!TempletPortalKeywords.isValidAppSession()) {
				TempletPortalKeywords.openBrowserAndLoginWithMicrosoft(targetUrl)
				gotoTarget(targetUrl)
			}
		}
		WebUI.delay(2)

		int paso = 0
		((Map) map.groups).each { groupName, groupElements ->
			((List) groupElements).each { Object rawEl ->
				paso++
				processElement(caseId, paso, (Map) rawEl, sourceRepo, urlRoot, failures, warnings)
			}
		}

		String screenshotPath = ''
		if (!failures.isEmpty()) {
			screenshotPath = TempletPortalKeywords.captureCaseScreenshot(caseId, 'failure')
		}
		String slug = (config.snapshotSlug ?: caseId.toLowerCase().replaceAll('[^a-z0-9]+', '_')).toString()
		String snapshotPath = projectDir + '/Reports/Tracking/snapshots/repos_coverage_' + slug + '_latest.json'
		TempletPortalKeywords.writeJsonSnapshot(snapshotPath, [
			caseId    : caseId,
			platform  : meta.platform.toString() + ' TEST - ' + meta.screen.toString(),
			tab       : meta.screen.toString(),
			timestamp : LocalDateTime.now().format(DateTimeFormatter.ofPattern('yyyy-MM-dd HH:mm:ss')),
			url       : TempletPortalKeywords.currentUrlSafe(),
			screenshot: screenshotPath,
			sourceFile: sourceRepo,
			failures  : failures,
			warnings  : warnings
		])

		CommonKeywords.logCaseSummary(caseId, failures, warnings)
		if (!failures.isEmpty()) {
			KeywordUtil.markFailedAndStop("[${caseId}] ${failures.size()} failure(s):\n" + failures.join('\n---\n'))
		} else {
			KeywordUtil.logInfo("[${caseId}] PASSED" + (warnings ? " con ${warnings.size()} warnings" : ''))
		}
	}

	static void processElement(String caseId, int paso, Map el, String sourceRepo, String urlRoot, List failures, List warnings) {
		String name = (el.name ?: 'element').toString()
		String action = (el.action ?: 'verify_present').toString()
		String primary = (el.xpath_primary ?: '').toString()
		String fallback = (el.xpath_fallback ?: '').toString()
		boolean hard = 'HARD'.equalsIgnoreCase((el.assertion_type ?: 'SOFT').toString())
		String sourceFile = (el.source_file ?: 'desconocido').toString()
		String selectorDesc = primary + (fallback ? ' | fallback: ' + fallback : '')

		// Navegacion previa opcional (chequeos multi-URL, ej. admin/info.php, admin/403.php)
		if (el.url_path) {
			try {
				WebUI.navigateToUrl(urlRoot + el.url_path.toString())
				WebUI.waitForPageLoad(20)
				WebUI.delay(1)
			} catch (Exception e) {
				warnings.add(buildIssue(caseId, paso, name + ' (navegacion)', selectorDesc,
					'navegar a ' + el.url_path, 'error: ' + e.message, sourceRepo, sourceFile,
					'Verificar disponibilidad de la ruta en el ambiente TEST.'))
				return
			}
		}

		switch (action) {
			case 'verify_present':
			case 'verify_text':
				boolean found = primary ? TempletPortalKeywords.verifyXPathPresent(name, primary, 10) : false
				boolean viaFallback = false
				if (!found && fallback) {
					found = TempletPortalKeywords.verifyXPathPresent(name + '_fallback', fallback, 5)
					viaFallback = found
				}
				if (!found) {
					String expectedDesc = 'elemento presente' + (el.expected_value ? " con texto '${el.expected_value}'" : '')
					String issue = buildIssue(caseId, paso, name + ' (' + action + ')', selectorDesc,
						expectedDesc, 'NO encontrado con selector primario ni fallback', sourceRepo, sourceFile,
						'Revisar el componente en el repo: si el texto/estructura cambio, actualizar element-map y selector en Katalon; si el elemento desaparecio de la UI, posible bug de la app.')
					(hard ? failures : warnings).add(issue)
				} else {
					if (viaFallback) {
						warnings.add(buildIssue(caseId, paso, name + ' (selector)', selectorDesc,
							'match con selector primario', 'solo el fallback funciono', sourceRepo, sourceFile,
							'Agregar data-testid en la app (ver Include/config/SELECTORS-REVIEW.md) y actualizar el selector primario del element-map.'))
					}
					if (action == 'verify_text' && el.expected_value) {
						boolean textOk = TempletPortalKeywords.verifyXPathText(name + '_text', (viaFallback ? fallback : primary), el.expected_value.toString(), 5)
						if (!textOk) {
							String issue = buildIssue(caseId, paso, name + ' (verify_text)', selectorDesc,
								"texto exacto '${el.expected_value}'", 'texto distinto al esperado', sourceRepo, sourceFile,
								'Confirmar el copy actual en el archivo fuente; si cambio legitimamente, actualizar expected_value en el element-map.')
							(hard ? failures : warnings).add(issue)
						}
					}
				}
				break

			case 'verify_count':
				int count = countByXPath(primary)
				if (count == 0 && fallback) count = countByXPath(fallback)
				int minExpected = 1
				try {
					String digits = (el.expected_value ?: '>=1').toString().replaceAll('[^0-9]', '')
					minExpected = (digits ?: '1') as int
				} catch (Exception ignored) {}
				if (count < minExpected) {
					String issue = buildIssue(caseId, paso, name + ' (verify_count)', selectorDesc,
						">=${minExpected} elemento(s)", "${count} elemento(s)", sourceRepo, sourceFile,
						'Si el ambiente TEST no tiene data es un tema de datos (warning); si deberia haber elementos, revisar el render del listado en el componente.')
					(hard ? failures : warnings).add(issue)
				}
				break

			case 'click':
				boolean clicked = TempletPortalKeywords.clickXPathAndKeepValidSession(name, primary, 10)
				if (!clicked && fallback) {
					clicked = TempletPortalKeywords.clickXPathAndKeepValidSession(name + '_fallback', fallback, 5)
				}
				if (!clicked) {
					String issue = buildIssue(caseId, paso, name + ' (click)', selectorDesc,
						'click exitoso manteniendo sesion valida', 'elemento no clickeable o no encontrado', sourceRepo, sourceFile,
						'Verificar visibilidad/habilitacion del control en el componente; si el selector quedo desactualizado, corregir el element-map.')
					(hard ? failures : warnings).add(issue)
				}
				break

			case 'verify_absent':
				boolean present = TempletPortalKeywords.verifyXPathPresent(name, primary, 3)
				if (present) {
					String issue = buildIssue(caseId, paso, name + ' (verify_absent)', selectorDesc,
						(el.expected_value ?: 'elemento ausente').toString(), 'elemento PRESENTE', sourceRepo, sourceFile,
						(el.notes ?: 'Eliminar o proteger el recurso expuesto.').toString())
					(hard ? failures : warnings).add(issue)
				}
				break

			case 'verify_absent_after_wait':
				boolean stillPresent = true
				for (int i = 0; i < 6; i++) {
					// primera pasada corta: si el loading ya desaparecio no pagamos timeout completo
					if (!TempletPortalKeywords.verifyXPathPresent(name, primary, (i == 0 ? 2 : 5))) {
						stillPresent = false
						break
					}
					WebUI.delay(5)
				}
				if (stillPresent) {
					String issue = buildIssue(caseId, paso, name + ' (loading)', selectorDesc,
						'estado de carga desaparece tras fetch async (<60s)', 'spinner/loading sigue visible', sourceRepo, sourceFile,
						'Revisar el fetch de datos del componente o la latencia del backend en TEST.')
					(hard ? failures : warnings).add(issue)
				}
				break

			default:
				warnings.add("[${caseId}][PASO ${paso}: ${name}] action '${action}' no soportada por ReposCoverageKeywords - elemento omitido.")
		}
	}
}
