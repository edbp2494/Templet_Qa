import com.kms.katalon.core.annotation.Keyword
import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI

import javax.imageio.ImageIO
import java.awt.Color
import java.awt.image.BufferedImage
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * VisualKeywords — Comparación pixel a pixel entre ambientes TEST y PROD.
 *
 * Capacidades:
 *   - saveBaseline()         → captura imagen de referencia (PROD)
 *   - compareWithBaseline()  → calcula % de diferencia respecto al baseline
 *   - captureSection()       → screenshot de una región CSS/XPath específica
 *   - compareScreenshots()   → compara dos imágenes ya guardadas
 *   - generateDiffImage()    → genera imagen con diferencias resaltadas en rojo
 *   - generateHtmlReport()   → reporte HTML visual con thumbnails y resultados
 *
 * Uso típico:
 *   1. Ejecutar con ENV_PROD → saveBaseline('SHEETS', 'dashboard')
 *   2. Ejecutar con ENV_TEST → compareWithBaseline('SHEETS', 'dashboard', 3.0)
 */
class VisualKeywords {

	// ─── Constantes de configuración ───────────────────────────────────────────

	/** Color usado para resaltar diferencias en la imagen diff (rojo semitransparente) */
	private static final Color DIFF_COLOR = new Color(255, 0, 0, 160)

	// ─── Rutas base ────────────────────────────────────────────────────────────

	static String basePath() {
		return System.getProperty('user.dir')
	}

	static String baselinesDir() {
		return basePath() + '/Reports/Visual/Baselines'
	}

	static String screenshotsDir() {
		return basePath() + '/Reports/Visual/Screenshots'
	}

	static String diffsDir() {
		return basePath() + '/Reports/Visual/Diffs'
	}

	static String reportsDir() {
		return basePath() + '/Reports/Visual/Reports'
	}

	static void ensureDirs() {
		[baselinesDir(), screenshotsDir(), diffsDir(), reportsDir()].each {
			new File(it).mkdirs()
		}
	}

	static String timestamp() {
		return LocalDateTime.now().format(DateTimeFormatter.ofPattern('yyyyMMdd_HHmmss'))
	}

	// ─── Captura ────────────────────────────────────────────────────────────────

	/**
	 * Toma un screenshot completo de la página actual.
	 * @param label  Nombre descriptivo para el archivo (ej: 'SHEETS_PROD_dashboard')
	 * @param dir    Directorio donde guardar (usa screenshotsDir() si null)
	 * @return       Ruta al archivo generado
	 */
	@Keyword
	static String captureFullPage(String label, String dir = null) {
		ensureDirs()
		String targetDir = dir ?: screenshotsDir()
		String path = targetDir + '/' + label + '_' + timestamp() + '.png'
		WebUI.takeScreenshot(path)
		KeywordUtil.logInfo('[VISUAL] Captura: ' + path)
		return path
	}

	/**
	 * Guarda una captura como baseline (referencia de producción).
	 * Sobreescribe el baseline anterior si existe.
	 * @param platform  'SHEETS', 'DECKS' o 'EMAIL'
	 * @param section   Nombre de la sección (ej: 'dashboard', 'header', 'tabla')
	 * @return          Ruta al baseline guardado
	 */
	@Keyword
	static String saveBaseline(String platform, String section) {
		ensureDirs()
		String path = baselinesDir() + '/' + platform.toUpperCase() + '_' + section + '_baseline.png'
		WebUI.takeScreenshot(path)
		KeywordUtil.logInfo('[VISUAL] Baseline guardado: ' + path)
		return path
	}

	// ─── Comparación ────────────────────────────────────────────────────────────

	/**
	 * Compara la pantalla actual con el baseline guardado.
	 * @param platform   'SHEETS', 'DECKS' o 'EMAIL'
	 * @param section    Nombre de la sección a comparar
	 * @param threshold  % máximo de diferencia permitido (default: 3.0)
	 * @return           Map con: diffPercent, passed, baselinePath, currentPath, diffPath
	 */
	@Keyword
	static Map<String, Object> compareWithBaseline(String platform, String section, double threshold = 3.0) {
		ensureDirs()
		String baselinePath = baselinesDir() + '/' + platform.toUpperCase() + '_' + section + '_baseline.png'
		File baselineFile = new File(baselinePath)

		if (!baselineFile.exists()) {
			KeywordUtil.logInfo('[VISUAL] No existe baseline para ' + platform + '/' + section + '. Guardando captura actual como baseline.')
			saveBaseline(platform, section)
			return [diffPercent: 0.0, passed: true, message: 'Baseline creado — sin comparación previa', baselinePath: baselinePath, currentPath: baselinePath, diffPath: null]
		}

		String currentPath = screenshotsDir() + '/' + platform.toUpperCase() + '_' + section + '_current_' + timestamp() + '.png'
		WebUI.takeScreenshot(currentPath)

		return compareScreenshots(baselinePath, currentPath, threshold, platform + '_' + section)
	}

	/**
	 * Compara dos imágenes ya guardadas en disco.
	 * @param path1      Ruta imagen de referencia (baseline / PROD)
	 * @param path2      Ruta imagen actual (TEST)
	 * @param threshold  % máximo permitido
	 * @param label      Nombre para el archivo diff y el log
	 * @return           Map con: diffPercent, passed, path1, path2, diffPath
	 */
	@Keyword
	static Map<String, Object> compareScreenshots(String path1, String path2, double threshold = 3.0, String label = 'comparison') {
		ensureDirs()
		try {
			BufferedImage img1 = ImageIO.read(new File(path1))
			BufferedImage img2 = ImageIO.read(new File(path2))

			// Normalizar tamaños: usar el menor común denominador
			int width  = Math.min(img1.width, img2.width)
			int height = Math.min(img1.height, img2.height)

			BufferedImage diffImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
			long differentPixels = 0
			long totalPixels = (long)(width * height)

			for (int y = 0; y < height; y++) {
				for (int x = 0; x < width; x++) {
					int rgb1 = img1.getRGB(x, y)
					int rgb2 = img2.getRGB(x, y)

					if (rgb1 != rgb2) {
						differentPixels++
						diffImage.setRGB(x, y, DIFF_COLOR.getRGB())
					} else {
						// Pixel idéntico: mostrar oscurecido en el diff para contexto
						int gray = dimPixel(rgb1)
						diffImage.setRGB(x, y, gray)
					}
				}
			}

			double diffPercent = (differentPixels / (double) totalPixels) * 100.0
			boolean passed = diffPercent <= threshold

			// Guardar imagen diff
			String diffPath = diffsDir() + '/' + label + '_diff_' + timestamp() + '.png'
			ImageIO.write(diffImage, 'PNG', new File(diffPath))

			String status = passed ? 'PASSED' : 'FAILED'
			KeywordUtil.logInfo(String.format('[VISUAL] %s %s — diff=%.2f%% (umbral=%.1f%%) diff=%s', status, label, diffPercent, threshold, diffPath))

			return [
				diffPercent : Math.round(diffPercent * 100) / 100.0,
				passed      : passed,
				threshold   : threshold,
				path1       : path1,
				path2       : path2,
				diffPath    : diffPath,
				label       : label,
				message     : String.format('%.2f%% diferencia (%s del umbral %.1f%%)', diffPercent, passed ? 'dentro' : 'FUERA', threshold)
			]

		} catch (Exception e) {
			KeywordUtil.logInfo('[VISUAL] Error comparando imágenes: ' + e.message)
			return [diffPercent: -1.0, passed: false, message: 'Error: ' + e.message, path1: path1, path2: path2, diffPath: null]
		}
	}

	// ─── Reporte HTML ───────────────────────────────────────────────────────────

	/**
	 * Genera un reporte HTML con los resultados de comparaciones visuales.
	 * Incluye thumbnails de baseline, actual y diff, con resultado por sección.
	 *
	 * @param results   Lista de Maps retornados por compareWithBaseline / compareScreenshots
	 * @param suiteName Nombre de la suite para el título del reporte
	 * @return          Ruta al HTML generado
	 */
	@Keyword
	static String generateHtmlReport(List<Map<String, Object>> results, String suiteName = 'Visual QA Report') {
		ensureDirs()
		String ts = timestamp()
		String reportPath = reportsDir() + '/visual_report_' + ts + '.html'

		int total  = results.size()
		int passed = results.count { it.passed == true }
		int failed = total - passed

		StringBuilder html = new StringBuilder()
		html << """<!DOCTYPE html>
<html lang="es">
<head>
  <meta charset="UTF-8"/>
  <title>${suiteName}</title>
  <style>
    * { box-sizing: border-box; margin: 0; padding: 0; }
    body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif; background: #f5f5f5; color: #333; }
    header { background: #1a1a2e; color: #fff; padding: 24px 32px; }
    header h1 { font-size: 22px; }
    header p  { font-size: 13px; color: #aaa; margin-top: 4px; }
    .summary { display: flex; gap: 16px; padding: 20px 32px; background: #fff; border-bottom: 1px solid #e0e0e0; }
    .badge { padding: 10px 20px; border-radius: 8px; font-weight: bold; font-size: 18px; }
    .badge.total  { background: #e8eaf6; color: #3949ab; }
    .badge.pass   { background: #e8f5e9; color: #2e7d32; }
    .badge.fail   { background: #ffebee; color: #c62828; }
    .badge small  { display: block; font-size: 11px; font-weight: normal; }
    .cases { padding: 24px 32px; display: flex; flex-direction: column; gap: 20px; }
    .case { background: #fff; border-radius: 10px; box-shadow: 0 1px 4px rgba(0,0,0,.08); overflow: hidden; }
    .case-header { padding: 14px 20px; display: flex; justify-content: space-between; align-items: center; }
    .case-header.pass { border-left: 5px solid #43a047; }
    .case-header.fail { border-left: 5px solid #e53935; }
    .case-title { font-size: 15px; font-weight: 600; }
    .pill { padding: 4px 12px; border-radius: 20px; font-size: 12px; font-weight: bold; }
    .pill.pass { background: #e8f5e9; color: #2e7d32; }
    .pill.fail { background: #ffebee; color: #c62828; }
    .case-meta { padding: 10px 20px; font-size: 12px; color: #666; border-bottom: 1px solid #f0f0f0; }
    .imgs { display: grid; grid-template-columns: repeat(3, 1fr); gap: 12px; padding: 16px 20px; }
    .img-box { text-align: center; }
    .img-box span { display: block; font-size: 11px; color: #888; margin-bottom: 6px; font-weight: 600; text-transform: uppercase; letter-spacing: .5px; }
    .img-box img { width: 100%; border-radius: 6px; border: 1px solid #e0e0e0; cursor: pointer; }
    .img-box img:hover { transform: scale(1.02); transition: .2s; }
    footer { text-align: center; padding: 20px; font-size: 12px; color: #aaa; }
  </style>
</head>
<body>
<header>
  <h1>🔍 ${suiteName}</h1>
  <p>Generado: ${LocalDateTime.now().format(DateTimeFormatter.ofPattern('dd/MM/yyyy HH:mm:ss'))}</p>
</header>
<div class="summary">
  <div class="badge total"><small>Total</small>${total}</div>
  <div class="badge pass"><small>✅ Passed</small>${passed}</div>
  <div class="badge fail"><small>❌ Failed</small>${failed}</div>
</div>
<div class="cases">
"""

		results.each { r ->
			String status = r.passed ? 'pass' : 'fail'
			String icon   = r.passed ? '✅' : '❌'
			String label  = (r.label ?: 'comparison').toString()
			String msg    = (r.message ?: '').toString()
			double pct    = r.diffPercent instanceof Number ? (double) r.diffPercent : -1.0
			double thr    = r.threshold instanceof Number ? (double) r.threshold : 3.0

			String img1Tag  = imgTag(r.path1?.toString())
			String img2Tag  = imgTag(r.path2?.toString())
			String diffTag  = imgTag(r.diffPath?.toString())

			html << """
  <div class="case">
    <div class="case-header ${status}">
      <span class="case-title">${icon} ${label}</span>
      <span class="pill ${status}">${r.passed ? 'PASSED' : 'FAILED'}</span>
    </div>
    <div class="case-meta">
      Diferencia: <strong>${pct >= 0 ? String.format('%.2f', pct) + '%' : 'N/A'}</strong> | Umbral: ${thr}% | ${msg}
    </div>
    <div class="imgs">
      <div class="img-box"><span>Baseline (PROD)</span>${img1Tag}</div>
      <div class="img-box"><span>Actual (TEST)</span>${img2Tag}</div>
      <div class="img-box"><span>Diferencias</span>${diffTag}</div>
    </div>
  </div>
"""
		}

		html << """
</div>
<footer>Reporte generado automáticamente por VisualKeywords — Katalon Templet QA</footer>
</body>
</html>"""

		new File(reportPath).text = html.toString()
		KeywordUtil.logInfo('[VISUAL] Reporte HTML: ' + reportPath)
		return reportPath
	}

	// ─── Helpers privados ───────────────────────────────────────────────────────

	private static int dimPixel(int rgb) {
		int r = (int)(((rgb >> 16) & 0xFF) * 0.4)
		int g = (int)(((rgb >> 8)  & 0xFF) * 0.4)
		int b = (int)((rgb         & 0xFF) * 0.4)
		return (0xFF << 24) | (r << 16) | (g << 8) | b
	}

	private static String imgTag(String path) {
		if (!path || !new File(path).exists()) {
			return '<span style="color:#bbb;font-size:12px;">Sin imagen</span>'
		}
		// Convertir a Base64 para incrustar en HTML (portable, sin rutas absolutas)
		try {
			byte[] bytes = new File(path).bytes
			String b64 = bytes.encodeBase64().toString()
			return "<img src=\"data:image/png;base64,${b64}\" alt=\"${new File(path).name}\" title=\"${path}\"/>"
		} catch (Exception e) {
			return '<span style="color:#bbb;font-size:12px;">Error cargando imagen</span>'
		}
	}
}
