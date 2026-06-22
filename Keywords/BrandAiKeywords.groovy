import com.kms.katalon.core.annotation.Keyword
import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import com.kms.katalon.core.model.FailureHandling as FailureHandling
import com.kms.katalon.core.testobject.TestObject
import com.kms.katalon.core.testobject.ConditionType
import com.kms.katalon.core.webui.driver.DriverFactory
import org.openqa.selenium.WebDriver
import org.openqa.selenium.interactions.Actions
import org.openqa.selenium.Keys

/**
 * Flujo "Create {TYPE}" de Builders /brand + pantalla "We need some inputs first!"
 * de la app de drafts (ai-test.templet.io). Reutilizado por TCs por-tipo (HTML/PPTX/TEXT).
 *
 * Hechos confirmados del DOM de drafts:
 *  - Radios (readonly) con ids: #file (Uploading), #url (Sharing), #form (Filling).
 *  - La opcion se selecciona clickeando su <label> contenedor (la card).
 *  - El boton de continuar es "OK" (button.btn-primary). El "Go Back" aparece en el
 *    paso siguiente.
 */
public class BrandAiKeywords {

	static final String ESCALA_BRAND_URL = 'https://testing-templet-builders.vercel.app/brand/3ac229a2-3bcd-ef11-b8e9-6045bd034dc5'
	static final String GOBACK_CSS = "body > div.bg-white > section > div.min-vh-100.container-fluid.py-3.px-5.draft.d-flex.flex-column.gap-4 > div.mt-auto.d-flex.flex-column.gap-4 > div > div:nth-child(1) > button"

	static TestObject cssObject(String name, String css) {
		TestObject o = new TestObject(name)
		o.addProperty('css', ConditionType.EQUALS, css)
		return o
	}

	static boolean jsClickXpath(String xp) {
		try {
			return Boolean.TRUE.equals(WebUI.executeJavaScript(
				"var r=document.evaluate(arguments[0],document,null,XPathResult.FIRST_ORDERED_NODE_TYPE,null).singleNodeValue;" +
				"if(r){r.scrollIntoView({block:'center'});r.click();return true;}return false;", [xp]))
		} catch (Throwable t) { return false }
	}

	// Clickea el boton VISIBLE (offsetParent != null) y habilitado cuyo texto == t.
	// Evita agarrar botones duplicados ocultos (p.ej. varios "OK" por paso).
	static boolean jsClickVisibleByText(String t) {
		try {
			return Boolean.TRUE.equals(WebUI.executeJavaScript(
				"var t=arguments[0].trim().toLowerCase();" +
				"var bs=Array.from(document.querySelectorAll('button,a,[role=button],input[type=submit],input[type=button],div,span'));" +
				"var b=bs.find(function(x){" +
				"var tx=(x.textContent||x.value||'').trim().toLowerCase();" +
				"if(tx.indexOf(t)<0||tx.length>40)return false;" +
				"if(x.disabled)return false;" +
				"var r=x.getBoundingClientRect();return r.width>0 && r.height>0;});" +
				"if(!b)return false;b.scrollIntoView({block:'center'});b.click();return true;", [t]))
		} catch (Throwable e) { return false }
	}

	// true si existe un boton VISIBLE cuyo texto contiene t y esta habilitado.
	static boolean isBtnEnabledByText(String t) {
		try {
			return Boolean.TRUE.equals(WebUI.executeJavaScript(
				"var t=arguments[0].trim().toLowerCase();" +
				"var bs=Array.from(document.querySelectorAll('button,a,[role=button],input[type=submit],input[type=button],div,span'));" +
				"var b=bs.find(function(x){var tx=(x.textContent||x.value||'').trim().toLowerCase();if(tx.indexOf(t)<0||tx.length>40)return false;var r=x.getBoundingClientRect();return r.width>0&&r.height>0;});" +
				"if(!b)return false;return !b.disabled && b.getAttribute('aria-disabled')!=='true';", [t]))
		} catch (Throwable e) { return false }
	}

	// Click "real": busca el elemento visible con el texto, sube al ancestro clickeable
	// y dispara pointerdown/mousedown/mouseup/click (para handlers tipo React).
	static boolean jsRealClickByText(String t) {
		try {
			return Boolean.TRUE.equals(WebUI.executeJavaScript(
				"var t=arguments[0].trim().toLowerCase();" +
				"var all=Array.from(document.querySelectorAll('button,a,[role=button],div,span'));" +
				"var el=all.find(function(x){var tx=(x.textContent||'').trim().toLowerCase();if(tx.indexOf(t)<0||tx.length>40)return false;var r=x.getBoundingClientRect();return r.width>0&&r.height>0;});" +
				"if(!el)return false;" +
				"var c=el;while(c&&c!==document.body){var cs=getComputedStyle(c);if(c.tagName==='BUTTON'||c.tagName==='A'||c.getAttribute('role')==='button'||c.onclick||cs.cursor==='pointer')break;c=c.parentElement;}" +
				"var target=c||el;target.scrollIntoView({block:'center'});" +
				"var rc=target.getBoundingClientRect();var x=rc.left+rc.width/2,y=rc.top+rc.height/2;" +
				"['pointerdown','mousedown','mouseup','click'].forEach(function(ty){target.dispatchEvent(new MouseEvent(ty,{bubbles:true,cancelable:true,view:window,clientX:x,clientY:y}));});" +
				"return true;", [t]))
		} catch (Throwable e) { return false }
	}

	static String addButtonInfo() {
		try {
			return (WebUI.executeJavaScript(
				"var all=Array.from(document.querySelectorAll('button,a,[role=button],div,span'));" +
				"var el=all.find(function(x){var tx=(x.textContent||'').trim().toLowerCase();if(tx.indexOf('add your inputs')<0||tx.length>40)return false;var r=x.getBoundingClientRect();return r.width>0&&r.height>0;});" +
				"if(!el)return 'not_found';" +
				"var chain=[];var c=el;for(var i=0;i<4&&c;i++){chain.push(c.tagName+'.'+((c.className||'')+'').slice(0,25));c=c.parentElement;}" +
				"return 'txt=['+el.textContent.trim().slice(0,25)+'] '+chain.join(' > ');", null) ?: '?').toString()
		} catch (Throwable e) { return '?' }
	}

	// true si exactamente UN radio esta marcado y es el indice idx (exclusividad).
	static boolean radioExclusive(int idx) {
		try {
			return Boolean.TRUE.equals(WebUI.executeJavaScript(
				"var idx=arguments[0];var rs=document.querySelectorAll(\"input[type='radio']\");" +
				"var c=[];for(var i=0;i<rs.length;i++)if(rs[i].checked)c.push(i);" +
				"return c.length===1 && c[0]===idx;", [idx]))
		} catch (Throwable e) { return false }
	}

	static void closeRadixMenu(WebDriver driver) {
		try { new Actions(driver).sendKeys(Keys.ESCAPE).perform() } catch (Throwable ignore) { }
	}

	// Selecciona la opcion i (0,1,2): sube desde el radio hasta el ancestro (label/card)
	// que contiene el texto de alguna opcion y lo clickea. Devuelve JSON con checked.
	static String selectOptionByIndex(int idx, List labels) {
		try {
			Object r = WebUI.executeJavaScript(
				"var idx=arguments[0];var labels=arguments[1];" +
				"var rs=document.querySelectorAll(\"input[type='radio']\");" +
				"if(idx>=rs.length)return JSON.stringify({ok:false,reason:'no_radio',count:rs.length});" +
				"var el=rs[idx];var card=null;" +
				"while(el){var tx=(el.textContent||'').trim();" +
				"if(labels.some(function(l){return tx.indexOf(l)>=0;})){card=el;break;}el=el.parentElement;}" +
				"if(!card)return JSON.stringify({ok:false,reason:'no_card'});" +
				"card.scrollIntoView({block:'center'});card.click();" +
				"return JSON.stringify({ok:true,checked:rs[idx].checked});", [idx, labels])
			return (r ?: '{}').toString()
		} catch (Throwable t) { return '{err}' }
	}

	static String dumpButtons() {
		try {
			return (WebUI.executeJavaScript(
				"return JSON.stringify(Array.from(document.querySelectorAll('button')).map(function(b){var r=b.getBoundingClientRect();return {t:(b.textContent||'').trim().slice(0,20),d:b.disabled,c:(b.className||'').slice(0,40),y:Math.round(r.y),w:Math.round(r.width),h:Math.round(r.height),html:(b.innerHTML||'').replace(/\\s+/g,' ').slice(0,70)};}));", null) ?: '[]').toString()
		} catch (Throwable t) { return '[]' }
	}

	static String dumpInputsScreen() {
		try {
			return (WebUI.executeJavaScript(
				"var out={};out.iframes=document.querySelectorAll('iframe').length;" +
				"var f=document.querySelector('div.mt-auto');" +
				"out.footer=f?f.outerHTML.replace(/\\s+/g,' ').slice(0,600):'NO_FOOTER';" +
				"var els=Array.from(document.querySelectorAll('a,button,[role=button],input,div[onclick]'));" +
				"out.vis=els.filter(function(e){var r=e.getBoundingClientRect();return r.width>0&&r.height>0;}).slice(0,20).map(function(e){var r=e.getBoundingClientRect();return e.tagName+'|'+((e.textContent||e.value||'').trim().slice(0,18))+'|y'+Math.round(r.y);});" +
				"return JSON.stringify(out);", null) ?: '{}').toString()
		} catch (Throwable t) { return '{err}' }
	}

	// ---- DIAGNOSTICO DE FORMULARIO (paso 1: mapear el DOM, NO adivinar selectores) ----
	// Vuelca inputs/textareas/selects/botones/forms/labels visibles con su texto,
	// tipo, id, name, placeholder, accept, rect, visible y disabled. Retorna JSON.
	static String dumpFormStep() {
		try {
			return (WebUI.executeJavaScript(
				"function vis(e){var r=e.getBoundingClientRect();return r.width>0&&r.height>0;}" +
				"function rect(e){var r=e.getBoundingClientRect();return {x:Math.round(r.x),y:Math.round(r.y),w:Math.round(r.width),h:Math.round(r.height)};}" +
				"var out={url:location.href,title:(document.title||'').slice(0,80)};" +
				"out.inputs=Array.from(document.querySelectorAll('input')).map(function(e){return {type:e.type,id:e.id,name:e.name,placeholder:e.placeholder,accept:e.getAttribute('accept'),required:e.required,value:(e.value||'').slice(0,40),disabled:e.disabled,visible:vis(e),rect:rect(e),cls:(e.className||'').slice(0,60)};});" +
				"out.textareas=Array.from(document.querySelectorAll('textarea')).map(function(e){return {id:e.id,name:e.name,placeholder:e.placeholder,required:e.required,disabled:e.disabled,visible:vis(e),rect:rect(e),cls:(e.className||'').slice(0,60)};});" +
				"out.selects=Array.from(document.querySelectorAll('select')).map(function(e){return {id:e.id,name:e.name,disabled:e.disabled,visible:vis(e),options:Array.from(e.options).map(function(o){return (o.text||'').slice(0,30);}).slice(0,12)};});" +
				"out.buttons=Array.from(document.querySelectorAll('button,a,[role=button],input[type=submit],input[type=button]')).filter(vis).map(function(e){return {tag:e.tagName,text:(e.textContent||e.value||'').trim().slice(0,35),type:e.getAttribute('type'),disabled:e.disabled||e.getAttribute('aria-disabled')==='true',rect:rect(e),cls:(e.className||'').slice(0,55)};}).slice(0,30);" +
				"out.contenteditable=Array.from(document.querySelectorAll('[contenteditable=\"true\"]')).filter(vis).map(function(e){return {tag:e.tagName,text:(e.textContent||'').trim().slice(0,30),rect:rect(e),cls:(e.className||'').slice(0,55)};}).slice(0,10);" +
				"out.forms=Array.from(document.querySelectorAll('form')).map(function(e){return {id:e.id,action:(e.action||'').slice(0,80),cls:(e.className||'').slice(0,60)};});" +
				"out.labels=Array.from(document.querySelectorAll('label')).filter(vis).map(function(e){return {text:(e.textContent||'').trim().slice(0,45),forId:e.getAttribute('for')};}).slice(0,25);" +
				"return JSON.stringify(out);", null) ?: '{}').toString()
		} catch (Throwable t) { return '{"err":"' + (t.message ?: 'dumpFormStep') + '"}' }
	}

	// Persiste el JSON de diagnostico a Reports/Brand/diagnostics/{type}_{slug}.json y lo loguea.
	static void writeDiag(String caseId, String type, String slug, String json) {
		try {
			String dir = System.getProperty('user.dir') + '/Reports/Brand/diagnostics'
			new File(dir).mkdirs()
			String path = dir + '/' + type + '_' + slug + '.json'
			new File(path).write(json, 'UTF-8')
			KeywordUtil.logInfo('[' + caseId + '][DIAG-FORM] ' + type + '/' + slug + ' guardado en ' + path)
		} catch (Throwable t) {
			KeywordUtil.logInfo('[' + caseId + '][DIAG-FORM] no se pudo escribir diag: ' + (t.message ?: t))
		}
	}

	@Keyword
	def validateCreateInputMethods(Map config) {
		String caseId = config.caseId
		String type   = config.type
		String label  = config.label
		List failures = []
		List warnings = []

		String buildersUrl = (String) CommonKeywords.getRequiredGlobal('BUILDERS_TEST_URL', 'https://testing-templet-builders.vercel.app/')
		String fixtureUrl  = (String) CommonKeywords.getRequiredGlobal('BUILDERS_BRAND_FIXTURE_URL', ESCALA_BRAND_URL)
		String brandUrl    = (fixtureUrl != null && fixtureUrl.trim().length() > 0) ? fixtureUrl.trim() : ESCALA_BRAND_URL

		if (!TempletPortalKeywords.isBrowserSessionAlive()) {
			TempletPortalKeywords.openBrowserAndLoginWithMicrosoft(buildersUrl)
			WebUI.waitForPageLoad(25)
		}
		WebUI.navigateToUrl(brandUrl)
		WebUI.waitForPageLoad(20)
		WebDriver driver = DriverFactory.getWebDriver()

		if (!TempletPortalKeywords.isValidAppSession()) {
			failures.add('[AUTH] Sesion no valida — la URL apunta a Microsoft login')
			finish(caseId, failures, warnings); return
		}

		boolean brandDetail = false
		for (int a = 1; a <= 6; a++) {
			brandDetail = TempletPortalKeywords.verifyXPathPresent('section_definition', "//h2[normalize-space(.)='Definition']", 4)
			if (brandDetail) { break }
			WebUI.delay(2)
		}
		if (!brandDetail) { failures.add('[SCREEN] brand detail no cargo (Definition ausente)') }

		TempletPortalKeywords.clickIfPresent(TempletPortalKeywords.xpathObject('tab_layouts', "//*[@role='tab'][normalize-space(.)='Layouts']"), 6)
		WebUI.delay(2)

		if (!TempletPortalKeywords.verifyXPathPresent('asset_menu_trigger', "(//button[@aria-haspopup='menu'])[1]", 10)) {
			failures.add('[LAYOUTS] No se encontraron menus "..." de assets en Layouts.')
			finish(caseId, failures, warnings); return
		}

		int chosenIndex = -1
		for (int k = 1; k <= 12; k++) {
			TestObject trig = TempletPortalKeywords.xpathObject("disc_${k}", "(//button[@aria-haspopup='menu'])[${k}]")
			if (!TempletPortalKeywords.isPresentQuiet(trig, 2)) { break }
			openAssetMenu(k)
			WebUI.delay(1)
			boolean has = TempletPortalKeywords.isPresentQuiet(TempletPortalKeywords.xpathObject('lbl', "//a[normalize-space(.)='${label}']"), 2)
			closeRadixMenu(driver)
			WebUI.delay(1)
			if (has) { chosenIndex = k; break }
		}
		if (chosenIndex < 1) {
			warnings.add("[FIXTURE] Ningun asset (primeros 12) tiene '${label}' habilitado.")
			finish(caseId, failures, warnings); return
		}

		openAssetMenu(chosenIndex)
		WebUI.delay(1)
		List<String> before = new ArrayList<String>(driver.getWindowHandles())
		jsClickXpath("//a[normalize-space(.)='${label}']")
		if (!TempletPortalKeywords.switchToNewestTabIfPresent(before, 8)) {
			failures.add("[${type.toUpperCase()}] '${label}' no abrio la pestaña de drafts")
			finish(caseId, failures, warnings); return
		}
		WebUI.waitForPageLoad(20)
		WebUI.delay(3)
		TempletPortalKeywords.captureCaseScreenshot(caseId, "${type}_inputs_screen")

		TestObject inputsHeading = TempletPortalKeywords.xpathObject('inputs_heading', "//*[contains(normalize-space(.),'We need some inputs first')]")
		if (!TempletPortalKeywords.isPresentQuiet(inputsHeading, 8)) {
			warnings.add("[${type.toUpperCase()}][INPUTS] No se detecto 'We need some inputs first!'.")
		}
		KeywordUtil.logInfo("[${caseId}][DIAG] screen=" + dumpInputsScreen())

		List inputMethods = [
			[idx: 0, name: 'Uploading source files'],
			[idx: 1, name: 'Sharing website links'],
			[idx: 2, name: 'Filling out a form']
		]
		List allLabels = inputMethods.collect { it.name }
		TestObject okBtn      = TempletPortalKeywords.xpathObject('ok_btn', "//button[normalize-space(.)='OK']")
		TestObject okFb       = TempletPortalKeywords.xpathObject('ok_fb', "//button[contains(@class,'btn-primary') and not(contains(normalize-space(.),'Go Back'))]")
		TestObject goBackText = TempletPortalKeywords.xpathObject('goback_txt', "//button[normalize-space(.)='Go Back']")
		TestObject goBackCss  = cssObject('goback_css', GOBACK_CSS)

		inputMethods.each { Map m ->
			int idx = (int) m.idx
			String name = (String) m.name
			String slug = name.toLowerCase().replaceAll('[^a-z0-9]+', '_')

			// Recuperar la pantalla de inputs si no esta (por si quedo en un paso previo)
			if (!TempletPortalKeywords.isPresentQuiet(inputsHeading, 4)) {
				if (!jsClickVisibleByText('Go Back')) { TempletPortalKeywords.clickFirstPresent([goBackText, goBackCss], 5) }
				WebUI.delay(1)
			}

			TestObject card = TempletPortalKeywords.xpathObject("card_${slug}", "(//label[contains(normalize-space(.),'${name}')])[1]")
			boolean sel = TempletPortalKeywords.clickFirstPresent([card], 6)
			KeywordUtil.logInfo("[${caseId}][${name}] cardClicked=" + sel)
			WebUI.delay(2)
			TempletPortalKeywords.captureCaseScreenshot(caseId, "${type}_${slug}_selected")
			if (radioExclusive(idx)) {
				KeywordUtil.logInfo("[${caseId}][${name}] Radio exclusivo OK (solo 1 marcado)")
			} else {
				warnings.add("[${type.toUpperCase()}][${name}] Radio NO exclusivo: quedo mas de un radio marcado (el anterior no se deselecciono).")
			}

			// Avance: en esta pantalla el continuar NO es un boton visible estandar.
			boolean addEnabled = isBtnEnabledByText('Add your inputs')
			if (addEnabled) {
				KeywordUtil.logInfo("[${caseId}][${name}] Boton 'Add your inputs!' ACTIVADO tras seleccionar")
			} else {
				warnings.add("[${type.toUpperCase()}][${name}] El boton 'Add your inputs!' NO se activo tras seleccionar el radio.")
			}
			KeywordUtil.logInfo("[${caseId}][${name}] addInfo=" + addButtonInfo())
			TestObject add1 = TempletPortalKeywords.xpathObject('add1', "//button[contains(normalize-space(.),'Add your inputs')]")
			TestObject add2 = TempletPortalKeywords.xpathObject('add2', "//a[contains(normalize-space(.),'Add your inputs')]")
			TestObject add3 = TempletPortalKeywords.xpathObject('add3', "//*[@role='button'][contains(normalize-space(.),'Add your inputs')]")
			TestObject add4 = TempletPortalKeywords.xpathObject('add4', "//*[contains(@class,'btn')][contains(normalize-space(.),'Add your inputs')]")
			boolean cont = TempletPortalKeywords.clickFirstPresent([add1, add2, add3, add4], 6)
			if (!cont) { cont = jsRealClickByText('Add your inputs') }
			if (!cont) {
				warnings.add("[${type.toUpperCase()}][${name}] No se pudo clickear 'Add your inputs!' tras seleccionar.")
			} else {
				WebUI.waitForPageLoad(15)
				boolean advanced = false
				for (int w = 1; w <= 6; w++) {
					if (!TempletPortalKeywords.isPresentQuiet(inputsHeading, 2)) { advanced = true; break }
					WebUI.delay(1)
				}
				TempletPortalKeywords.captureCaseScreenshot(caseId, "${type}_${slug}_step")
				if (!advanced) {
					warnings.add("[${type.toUpperCase()}][${name}] Tras continuar no avanzo (sigue la pantalla de inputs).")
				} else {
					try { WebUI.executeJavaScript("window.scrollTo(0, document.body.scrollHeight);", null) } catch (Throwable ignore) { }
					WebUI.delay(1)
					TestObject gb1 = TempletPortalKeywords.xpathObject('gb1', "//button[contains(normalize-space(.),'Go Back')]")
					TestObject gb2 = TempletPortalKeywords.xpathObject('gb2', "//a[contains(normalize-space(.),'Go Back')]")
					TestObject gb3 = TempletPortalKeywords.xpathObject('gb3', "//*[@role='button'][contains(normalize-space(.),'Go Back')]")
					TestObject gb4 = TempletPortalKeywords.xpathObject('gb4', "//*[contains(@class,'btn')][contains(normalize-space(.),'Go Back')]")
					boolean back = TempletPortalKeywords.clickFirstPresent([gb1, gb2, gb3, gb4], 6)
					if (!back) { back = jsRealClickByText('Go Back') }
					if (!back) {
						warnings.add("[${type.toUpperCase()}][${name}] No se encontro boton 'Go Back' visible en el paso siguiente.")
					} else {
						WebUI.delay(2)
						if (!TempletPortalKeywords.isPresentQuiet(inputsHeading, 8)) {
							warnings.add("[${type.toUpperCase()}][${name}] Tras 'Go Back' no volvio la pantalla de inputs.")
						} else {
							KeywordUtil.logInfo("[${caseId}] ${name}: continuar + Go Back OK")
						}
					}
				}
			}
		}

		TempletPortalKeywords.captureCaseScreenshot(caseId, "${type}_final")
		try { TempletPortalKeywords.safeCloseBrowser() } catch (Throwable ignore) { }
		finish(caseId, failures, warnings)
	}

	static void openAssetMenu(int idx) {
		try {
			WebUI.executeJavaScript("var b=document.querySelectorAll(\"button[aria-haspopup='menu']\")[arguments[0]];if(b)b.scrollIntoView({block:'center'});", [idx - 1])
		} catch (Throwable ignore) { }
		TempletPortalKeywords.clickIfPresent(TempletPortalKeywords.xpathObject("trig_${idx}", "(//button[@aria-haspopup='menu'])[${idx}]"), 6)
	}

	static void finish(String caseId, List failures, List warnings) {
		CommonKeywords.logCaseSummary(caseId, failures, warnings)
		if (failures) {
			KeywordUtil.markFailedAndStop("[${caseId}] Failures: ${failures}")
		} else {
			KeywordUtil.logInfo("[${caseId}] PASSED" + (warnings ? " con ${warnings.size()} warnings" : ""))
		}
	}
}
