// ─────────────────────────────────────────────────────────────────────────────
// TC: TC-BUILDERS-COMPARE-001
// Plataforma: Builders | Área: (raíz)
// Descripción: Captura pareada de 16 módulos: app vieja (builder.templet.io) vs app nueva (testing-templet-builder-saas.vercel.app). Genera vieja-XX-modulo.png y nueva-XX-modulo.png en Reports/Builders/Compare/
// Suites: Platforms/Builders/Compare-Old-New
// Precondiciones: credenciales MS en Include/config/templet-credentials.properties
// ─────────────────────────────────────────────────────────────────────────────
import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import com.kms.katalon.core.model.FailureHandling
import com.kms.katalon.core.testobject.TestObject
import com.kms.katalon.core.testobject.ConditionType
import com.kms.katalon.core.webui.driver.DriverFactory
import internal.GlobalVariable
import org.openqa.selenium.WebDriver
import java.io.File
import java.nio.file.Files

// ════════════════════════════════════════════════════════════════════════
// TC-BUILDERS-COMPARE-001 — Captura pareada legacy vs migracion TS
// Recorre los 7 BCs del objetivo de comparacion y guarda vieja-XX / nueva-XX
// en Reports/Builders/Compare/. El diff visual + reporte lo genera despues
// Scripts/compare_screenshots.py.
// ════════════════════════════════════════════════════════════════════════
String caseId = 'TC-BUILDERS-COMPARE-001'
List failures = []
List warnings = []

// URLs de las aplicaciones
String oldAppUrl = 'https://builder.templet.io'                          // legacy
String newAppUrl = 'https://testing-templet-builder-saas.vercel.app'     // migracion TS

// ── Fixtures de detalle (deben ser validos en AMBAS apps; mismo backend/IDs) ──
// Si un ID queda vacio, ese modulo de detalle se omite (solo se avisa).
String BRAND_ID     = '00d8b666-8583-f011-b4cb-00224809ed53'  // BLUESTREAM FIBER
String TEMPLATE_ID  = '24a8a68b-59ca-ed11-b597-002248081d31'
String BLUEPRINT_ID = '06cabb46-d0bf-f011-bbd3-6045bd02007a'
String LAYOUT_ID    = ''                                       // TODO: setear un layout de QA

// Directorio de capturas
String reportsDir = System.getProperty('user.dir') + '/Reports/Builders/Compare'
File reportsFolder = new File(reportsDir)
if (!reportsFolder.exists()) { reportsFolder.mkdirs() }

// Archivar (no borrar) capturas del run anterior para no mezclar pares
try {
	File[] prev = reportsFolder.listFiles({ File f ->
		f.isFile() && f.name.endsWith('.png') && (f.name.startsWith('vieja-') || f.name.startsWith('nueva-'))
	} as java.io.FileFilter)
	if (prev != null && prev.length > 0) {
		File archive = new File(reportsDir + '/_prev_' + new Date().format('yyyyMMdd_HHmmss'))
		archive.mkdirs()
		prev.each { File f -> try { Files.move(f.toPath(), new File(archive, f.name).toPath()) } catch (Exception ig) { } }
		KeywordUtil.logInfo("[${caseId}] Archivadas ${prev.length} capturas previas en ${archive.name}/")
	}
} catch (Exception e) {
	KeywordUtil.logInfo("[${caseId}] No se pudieron archivar capturas previas: ${e.message}")
}

// ── Lista de modulos alineada a los 7 BCs ────────────────────────────────
// [code, path, desc, (tab), (newHint)] — path es IGUAL en ambas apps (rutas migradas)
def modules = []
modules << [code: '01-home',              path: '',                          desc: 'Home / dashboard']
// BC-01 Brand Properties  → /brand
modules << [code: '02-brand-list',        oldPath: 'brand', newPath: 'brand-properties', desc: 'BC-01 Brand Properties — lista']
if (BRAND_ID) {
	modules << [code: '03-brand-detail',      path: 'brand/' + BRAND_ID,                        desc: 'BC-01 Brand detail (tab Layouts)']
	modules << [code: '04-brand-colors',      path: 'brand/' + BRAND_ID, tab: 'Colors',         desc: 'BC-01 Brand — tab Colors']
	modules << [code: '05-brand-samples',     path: 'brand/' + BRAND_ID, tab: 'Samples',        desc: 'BC-01 Brand — tab Samples']
	modules << [code: '06-brand-techtionary', path: 'brand/' + BRAND_ID, tab: 'Techtionary',    desc: 'BC-01 Brand — tab Techtionary']
}
// BC-02 Brand Layouts  → /layout
modules << [code: '07-layout-list',       path: 'layout',                    desc: 'BC-02 Brand Layouts — lista']
if (LAYOUT_ID) {
	modules << [code: '08-layout-detail', path: 'layout/' + LAYOUT_ID,       desc: 'BC-02 Layout detail']
}
// BC-03 Templates  → /template
modules << [code: '09-template-list',     oldPath: 'template', newPath: 'templates', desc: 'BC-03 Templates — lista']
if (TEMPLATE_ID) {
	modules << [code: '10-template-detail', oldPath: 'template/' + TEMPLATE_ID, newPath: 'templates/' + TEMPLATE_ID, desc: 'BC-03 Template detail']
}
// BC-04 Task Creation  → /task-creation/*
modules << [code: '11-task-one-off',      path: 'task-creation/content',      desc: 'BC-04 Task Creation — One-Off']
modules << [code: '12-task-non-standard', path: 'task-creation/non-standard', desc: 'BC-04 Task Creation — Non-Standard']
// BC-08 Layout Generation  → /layout/create + /layout/upload  (posible feature nueva)
modules << [code: '13-layout-create',     path: 'layout/create',              desc: 'BC-08 Layout Generation — create', newHint: true]
modules << [code: '14-layout-upload',     path: 'layout/upload',              desc: 'BC-08 Layout Generation — upload', newHint: true]
// BC-09 Initiative Mgmt  → viven dentro de /blueprint (no hay ruta /initiatives)
modules << [code: '15-blueprint-manager', oldPath: 'blueprint/manager/power-user', newPath: 'blueprints', desc: 'BC-09 Initiatives — blueprints lista']
if (BLUEPRINT_ID) {
	modules << [code: '16-blueprint-detail',   oldPath: 'blueprint/' + BLUEPRINT_ID + '?role=admin', newPath: 'blueprints/' + BLUEPRINT_ID, desc: 'BC-09 Initiatives — blueprint detail']
	modules << [code: '17-blueprint-requests', oldPath: 'blueprint/' + BLUEPRINT_ID + '/requests',   newPath: 'blueprints/' + BLUEPRINT_ID + '/requests', desc: 'BC-09 Initiatives — requests']
}
// BC-10 File Delivery  → /convert (genera + descarga asset final)
modules << [code: '18-file-delivery',     path: 'convert',                    desc: 'BC-10 File Delivery — convert/entrega', newHint: true]
// Contexto extra (rutas que ya funcionaban; utiles como referencia)
modules << [code: '19-project-schedule',  path: 'project-schedule',           desc: 'Contexto — Project Schedule']
modules << [code: '20-work-in-progress',  path: 'work-in-progress',           desc: 'Contexto — Work in Progress']
modules << [code: '21-current-spend',     path: 'current-spend',              desc: 'Contexto — Current Spend / Financial']

// ── Helper: click de tab por texto (robusto a iconos y a variaciones) ─────
def clickTabByText = { String label ->
	List xps = [
		"//*[@role='tab'][normalize-space(.)='" + label + "']",
		"//button[normalize-space(.)='" + label + "']",
		"//*[@role='tab'][contains(normalize-space(.), '" + label + "')]",
		"//button[contains(normalize-space(.), '" + label + "')]",
		"//a[contains(normalize-space(.), '" + label + "')]"
	]
	for (String xp : xps) {
		try {
			TestObject to = new TestObject('tab_' + label)
			to.addProperty('xpath', ConditionType.EQUALS, xp)
			if (WebUI.verifyElementPresent(to, 3, FailureHandling.OPTIONAL)) {
				WebUI.click(to)
				return true
			}
		} catch (Exception e) { }
	}
	warnings.add('[TAB] no encontrado: ' + label)
	return false
}

// ── Helper: capturar todos los modulos de una app ────────────────────────
def captureApp = { String appLabel, String baseUrl, String prefix ->
	KeywordUtil.logInfo("[${caseId}] ▶ Capturando ${appLabel} (${baseUrl})")
	CustomKeywords.'TempletPortalKeywords.openBrowserAndLoginWithMicrosoft'(baseUrl)
	WebUI.waitForPageLoad(15)
	WebDriver driver = DriverFactory.getWebDriver()
	driver.manage().window().maximize()
	WebUI.delay(1)

	int ok = 0
	modules.each { module ->
		String code = module.code
		String path = (prefix == 'vieja' ? (module.oldPath ?: module.path) : (module.newPath ?: module.path))
		try {
			String fullUrl = baseUrl + (path ? '/' + path : '')
			if (module.tab) {
				String tv = ((String) module.tab).toLowerCase()
				fullUrl += (fullUrl.contains('?') ? '&' : '?') + 'tab=' + tv
			}
			KeywordUtil.logInfo("[${prefix}-${code}] → ${module.desc}")

			WebUI.navigateToUrl(fullUrl)
			WebUI.waitForPageLoad(15)
			WebUI.delay(8)                 // render + fetch de datos

			// Tabs client-side: navegar por URL no basta, hay que clickear
			if (module.tab) {
				clickTabByText((String) module.tab)
				WebUI.delay(3)
			}

			// Forzar lazy loading y volver arriba
			WebUI.executeJavaScript('window.scrollTo(0, document.body.scrollHeight);', null)
			WebUI.delay(3)
			WebUI.executeJavaScript('window.scrollTo(0, 0);', null)
			WebUI.delay(2)

			String currentUrl = (WebUI.getUrl() ?: '').toLowerCase()
			if (currentUrl.contains('microsoftonline') || currentUrl.contains('/login')) {
				warnings.add("[${prefix}-${code}] sesion perdida / redirect a login")
				KeywordUtil.markWarning("[${code}] ❌ (${prefix}) sesion/login")
				return
			}
			if (currentUrl.contains('/404') || currentUrl.contains('not-found')) {
				warnings.add("[${prefix}-${code}] 404 Not Found")
				KeywordUtil.markWarning("[${code}] ❌ (${prefix}) 404")
				return
			}

			WebUI.takeScreenshot(reportsDir + '/' + prefix + '-' + code + '.png')
			KeywordUtil.logInfo("  ✓ ${prefix}-${code}.png")
			ok++
		} catch (Exception e) {
			warnings.add("[${prefix}-${code}] ${e.message}")
			KeywordUtil.markWarning("[${code}] ⚠️ (${prefix}) ${e.message}")
		}
	}

	try { WebUI.closeBrowser() } catch (Exception e) {
		KeywordUtil.logInfo("[${caseId}] No se pudo cerrar navegador (${prefix})")
	}
	WebUI.delay(1)
	return ok
}

// ════════════════════════════════════════════════════════════════════════
// EJECUCION
// ════════════════════════════════════════════════════════════════════════
try {
	KeywordUtil.logInfo("[${caseId}] 🚀 Captura pareada de ${modules.size()} modulos (7 BCs)")
	KeywordUtil.logInfo("[${caseId}] 📁 Salida: ${reportsDir}")

	int oldOk = captureApp('APP VIEJA (legacy)', oldAppUrl, 'vieja')
	int newOk = captureApp('APP NUEVA (migracion TS)', newAppUrl, 'nueva')
	int totalSuccess = oldOk + newOk

	KeywordUtil.logInfo("[${caseId}] ✅ Capturas: vieja=${oldOk}, nueva=${newOk}, total=${totalSuccess}")
	CustomKeywords.'CommonKeywords.logCaseSummary'(caseId, failures, warnings)

	if (totalSuccess >= 24) {
		KeywordUtil.logInfo("[${caseId}] PASSED 🎉 — ${totalSuccess} capturas" + (warnings ? " (${warnings.size()} warnings)" : ''))
	} else if (totalSuccess >= 14) {
		KeywordUtil.logInfo("[${caseId}] PASSED ⚠️ — ${totalSuccess} capturas, revisar warnings")
	} else {
		failures.add("Solo ${totalSuccess} capturas (esperado >=14). Posible fallo de login o rutas.")
		KeywordUtil.markFailedAndStop("[${caseId}] FAILED — ${totalSuccess} capturas")
	}

} catch (Exception e) {
	failures.add(e.message)
	CustomKeywords.'CommonKeywords.logCaseSummary'(caseId, failures, warnings)
	KeywordUtil.markFailedAndStop("[${caseId}] Error critico: ${e.message}")
} finally {
	try { WebUI.closeBrowser() } catch (Exception e) { }
}
