// ─────────────────────────────────────────────────────────────────────────────
// TC: TC-BUILDERS-BRAND-AITEXT-001
// Plataforma: Builders | Área: brand
// Descripción: Valida el flujo "Create TEXT" (la otra IA: ai.templet.io/drafts) desde Builders /brand: carga de pantallas (Active Brands -> brand detail -> tab Layouts), abrir y cerrar la pestaña Create TEXT 2 veces, intento de mensaje vacio (boton deshabilitado -> warning, no fail) y creacion de un mensaje con marcador generico capturando la respuesta de la IA.
// Suites: Platforms/Builders/Brand/AI-Text-Generation-Flow
// Precondiciones: credenciales MS en Include/config/templet-credentials.properties
// ─────────────────────────────────────────────────────────────────────────────
import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import com.kms.katalon.core.model.FailureHandling as FailureHandling
import com.kms.katalon.core.testobject.TestObject
import com.kms.katalon.core.testobject.ConditionType
import com.kms.katalon.core.webui.driver.DriverFactory
import org.openqa.selenium.WebDriver

// TC-BUILDERS-BRAND-AITEXT-001
// /brand -> brand ESCALA -> tab Layouts -> seleccionar iniciativa "Attendance-short"
// -> "..." de un asset -> Create TEXT (abre ai.templet.io/drafts en pestaña nueva).
// Abre/cierra esa pestaña 2 veces. 2da apertura (drafts): selecciona iniciativa,
// mensaje vacio (boton disabled -> warning), mensaje con marcador -> envia y captura.

String caseId = 'TC-BUILDERS-BRAND-AITEXT-001'
List failures = []
List warnings = []

String marker = 'QA-KATALON-AITEXT-' + new Date().format('yyyyMMdd-HHmmss')
KeywordUtil.logInfo("[${caseId}] Marcador de mensaje AI: ${marker}")
String initiativeName = 'Attendance-short'

// Brand ESCALA por defecto (hardcode de respaldo; tambien en BUILDERS_BRAND_FIXTURE_URL)
String escalaBrandUrl = 'https://testing-templet-builders.vercel.app/brand/3ac229a2-3bcd-ef11-b8e9-6045bd034dc5'
String buildersUrl = CustomKeywords.'CommonKeywords.getRequiredGlobal'('BUILDERS_TEST_URL', 'https://testing-templet-builders.vercel.app/')
String brandListUrl = buildersUrl.replaceAll('/+$', '') + '/brand'
String fixtureUrl = CustomKeywords.'CommonKeywords.getRequiredGlobal'('BUILDERS_BRAND_FIXTURE_URL', escalaBrandUrl)
boolean hasFixture = (fixtureUrl != null && fixtureUrl.trim().length() > 0)

// ── Login / reuso de sesion ─────────────────────────────────────────────────────
if (!CustomKeywords.'TempletPortalKeywords.ensureAuthenticatedSession'(buildersUrl, 25)) {
	failures.add('[AUTH] Sesion no valida — la URL apunta a Microsoft login')
	CustomKeywords.'CommonKeywords.logCaseSummary'(caseId, failures, warnings)
	KeywordUtil.markFailedAndStop("[${caseId}] Failures: ${failures}")
}

// ── 1a. Navegar al brand (fixture ESCALA directo, o toolbar + lista) ─────────────
if (hasFixture) {
	WebUI.navigateToUrl(fixtureUrl.trim())
	WebUI.waitForPageLoad(20)
} else {
	TestObject navBrandCss = new TestObject('nav_brand_css')
	navBrandCss.addProperty('css', ConditionType.EQUALS,
		"body > div.flex.h-screen.bg-gray-50 > div > div > div.flex-1.p-4.space-y-6.overflow-y-auto > div:nth-child(2) > div.space-y-2 > a:nth-child(2) > button")
	TestObject navBrandBtn = CustomKeywords.'TempletPortalKeywords.xpathObject'('nav_brand_btn', "//a[@href='/brand']//button")
	TestObject navBrandLink = CustomKeywords.'TempletPortalKeywords.xpathObject'('nav_brand_link', "//a[@href='/brand']")
	boolean navClicked = CustomKeywords.'TempletPortalKeywords.clickFirstPresent'([navBrandCss, navBrandBtn, navBrandLink], 12)
	if (!navClicked) {
		WebUI.navigateToUrl(brandListUrl)
	}
	WebUI.waitForPageLoad(20)
	TestObject brandRow = CustomKeywords.'TempletPortalKeywords.xpathObject'('brand_row_link', "(//table//a[contains(@href,'/brand/')])[1]")
	boolean intoBrand = false
	for (int attempt = 1; attempt <= 6; attempt++) {
		intoBrand = CustomKeywords.'TempletPortalKeywords.clickFirstPresent'([brandRow], 5)
		if (intoBrand) { break }
		WebUI.delay(2)
	}
	if (!intoBrand) {
		failures.add('[NAV] No se pudo entrar a un brand. Revisa BUILDERS_BRAND_FIXTURE_URL.')
		CustomKeywords.'CommonKeywords.logCaseSummary'(caseId, failures, warnings)
		KeywordUtil.markFailedAndStop("[${caseId}] Failures: ${failures}")
	}
	WebUI.waitForPageLoad(20)
}

// ── 1b. Brand detail: Definition ─────────────────────────────────────────────────
boolean brandDetail = false
for (int attempt = 1; attempt <= 6; attempt++) {
	brandDetail = CustomKeywords.'TempletPortalKeywords.verifyXPathPresent'('section_definition', "//h2[normalize-space(.)='Definition']", 4)
	if (brandDetail) { break }
	WebUI.delay(2)
}
if (!brandDetail) {
	failures.add('[SCREEN] brand detail no cargo (seccion "Definition" ausente)')
}

// Tab Layouts (por defecto activa; click defensivo)
TestObject layoutsTab = CustomKeywords.'TempletPortalKeywords.xpathObject'('tab_layouts', "//*[@role='tab'][normalize-space(.)='Layouts']")
CustomKeywords.'TempletPortalKeywords.clickIfPresent'(layoutsTab, 6)
WebUI.delay(2)

// ── 1c. Seleccionar filtro/iniciativa "Attendance-short" en Layouts (best-effort) ─
// Se hace ANTES de buscar los assets, por si los layouts dependen de esta seleccion.
TestObject initOptBrand = CustomKeywords.'TempletPortalKeywords.xpathObject'('init_opt_brand',
	"//option[normalize-space(.)='${initiativeName}'] | //*[@role='option'][normalize-space(.)='${initiativeName}'] | //li[normalize-space(.)='${initiativeName}']")
boolean initSelBrand = false
for (int s = 1; s <= 6; s++) {
	TestObject combo = CustomKeywords.'TempletPortalKeywords.xpathObject'("brand_combo_${s}", "(//button[@role='combobox'])[${s}]")
	if (!CustomKeywords.'TempletPortalKeywords.isPresentQuiet'(combo, 2)) { break }
	try { WebUI.click(combo) } catch (Throwable ignore) { }
	WebUI.delay(1)
	if (CustomKeywords.'TempletPortalKeywords.isPresentQuiet'(initOptBrand, 2)) {
		try { WebUI.click(initOptBrand); initSelBrand = true } catch (Throwable ignore) { }
		KeywordUtil.logInfo("[${caseId}] Filtro/iniciativa '${initiativeName}' seleccionada en Layouts")
		WebUI.delay(2)
		break
	}
	try { WebUI.click(combo) } catch (Throwable ignore) { }
	WebUI.delay(1)
}
if (!initSelBrand) {
	warnings.add("[INITIATIVE] No se encontro la iniciativa '${initiativeName}' en un filtro de Layouts (puede no aplicar aqui).")
}

// ── 1d. Menus "..." de assets ─────────────────────────────────────────────────────
boolean assetMenus = CustomKeywords.'TempletPortalKeywords.verifyXPathPresent'('asset_menu_trigger', "(//button[@aria-haspopup='menu'])[1]", 10)
if (!assetMenus) {
	warnings.add('[LAYOUTS] No se encontraron menus "..." de assets en Layouts (brand sin layouts visibles?).')
}
CustomKeywords.'TempletPortalKeywords.captureCaseScreenshot'(caseId, 'brand_layouts')

// ── Descubrir un asset cuyo "Create TEXT" este HABILITADO (siempre se intenta) ───
WebDriver driver = DriverFactory.getWebDriver()
int chosenIndex = -1
for (int k = 1; k <= 12; k++) {
	TestObject trig = CustomKeywords.'TempletPortalKeywords.xpathObject'("menu_trig_disc_${k}", "(//button[@aria-haspopup='menu'])[${k}]")
	if (!CustomKeywords.'TempletPortalKeywords.isPresentQuiet'(trig, 2)) { break }
	try { WebUI.click(trig) } catch (Throwable ignore) { }
	WebUI.delay(1)
	TestObject createTextEnabled = CustomKeywords.'TempletPortalKeywords.xpathObject'('create_text_enabled', "//a[normalize-space(.)='Create TEXT']")
	if (CustomKeywords.'TempletPortalKeywords.isPresentQuiet'(createTextEnabled, 2)) {
		chosenIndex = k
		try { WebUI.click(trig) } catch (Throwable ignore) { }
		WebUI.delay(1)
		break
	}
	try { WebUI.click(trig) } catch (Throwable ignore) { }
	WebUI.delay(1)
}
if (chosenIndex < 1) {
	warnings.add('[FIXTURE] Ningun asset (primeros 12) tiene "Create TEXT" habilitado en este brand.')
}

// ── Abrir/cerrar la pestaña "Create TEXT" 2 veces; 2da = interactuar IA ───────────
if (chosenIndex >= 1) {
	String mainHandle = driver.getWindowHandle()
	for (int i = 1; i <= 2; i++) {
		TestObject trig = CustomKeywords.'TempletPortalKeywords.xpathObject'("menu_trig_${i}", "(//button[@aria-haspopup='menu'])[${chosenIndex}]")
		boolean menuOpened = CustomKeywords.'TempletPortalKeywords.clickFirstPresent'([trig], 8)
		if (!menuOpened) {
			failures.add("[MENU] No se abrio el menu del asset (intento ${i})")
			break
		}
		WebUI.delay(1)

		TestObject createText = CustomKeywords.'TempletPortalKeywords.xpathObject'("create_text_${i}", "//a[normalize-space(.)='Create TEXT']")
		TestObject createTextFb = CustomKeywords.'TempletPortalKeywords.xpathObject'("create_text_fb_${i}", "//a[contains(normalize-space(.),'Create TEXT')]")

		List<String> beforeHandles = new ArrayList<String>(driver.getWindowHandles())
		boolean clickedCreate = CustomKeywords.'TempletPortalKeywords.clickFirstPresent'([createText, createTextFb], 8)
		if (!clickedCreate) {
			failures.add("[CREATE-TEXT] Opcion 'Create TEXT' no clickable (intento ${i})")
			break
		}

		boolean switched = CustomKeywords.'TempletPortalKeywords.switchToNewestTabIfPresent'(beforeHandles, 8)
		if (!switched) {
			failures.add("[TAB] No se abrio la pestaña ai.templet.io/drafts (intento ${i})")
			WebUI.delay(1)
			continue
		}

		WebUI.waitForPageLoad(20)
		String draftsUrl = CustomKeywords.'TempletPortalKeywords.currentUrlSafe'()
		boolean draftsOk = draftsUrl.contains('drafts') || draftsUrl.contains('ai.templet')
		if (!draftsOk) {
			warnings.add("[TAB] URL inesperada en pestaña Create TEXT (intento ${i}): ${draftsUrl}")
		}
		CustomKeywords.'TempletPortalKeywords.captureCaseScreenshot'(caseId, "create_text_tab_${i}")

		if (i == 2 && draftsOk) {
			WebUI.delay(3)

			// Seleccionar iniciativa "Attendance-short" en drafts (best-effort)
			TestObject dInitOpt = CustomKeywords.'TempletPortalKeywords.xpathObject'('drafts_init_opt',
				"//option[normalize-space(.)='${initiativeName}'] | //*[@role='option'][normalize-space(.)='${initiativeName}'] | //li[normalize-space(.)='${initiativeName}'] | //button[normalize-space(.)='${initiativeName}']")
			boolean initSelDrafts = false
			if (CustomKeywords.'TempletPortalKeywords.isPresentQuiet'(dInitOpt, 3)) {
				try { WebUI.click(dInitOpt); initSelDrafts = true } catch (Throwable ignore) { }
				WebUI.delay(1)
			}
			if (!initSelDrafts) {
				for (int s = 1; s <= 4; s++) {
					TestObject dcombo = CustomKeywords.'TempletPortalKeywords.xpathObject'("drafts_combo_${s}", "((//button[@role='combobox']) | (//*[@aria-haspopup='listbox']) | (//select))[${s}]")
					if (!CustomKeywords.'TempletPortalKeywords.isPresentQuiet'(dcombo, 2)) { break }
					try { WebUI.click(dcombo) } catch (Throwable ignore) { }
					WebUI.delay(1)
					if (CustomKeywords.'TempletPortalKeywords.isPresentQuiet'(dInitOpt, 2)) {
						try { WebUI.click(dInitOpt); initSelDrafts = true } catch (Throwable ignore) { }
						WebUI.delay(1)
						break
					}
				}
			}
			if (initSelDrafts) {
				KeywordUtil.logInfo("[${caseId}] Iniciativa '${initiativeName}' seleccionada en drafts")
			} else {
				warnings.add("[AI-INIT] No se pudo seleccionar la iniciativa '${initiativeName}' en drafts — verifica el selector real")
			}
			WebUI.delay(2)

			TestObject fTextarea = CustomKeywords.'TempletPortalKeywords.xpathObject'('ai_msg_textarea', "//textarea")
			TestObject fEditable = CustomKeywords.'TempletPortalKeywords.xpathObject'('ai_msg_editable', "//div[@contenteditable='true']")
			TestObject fInput = CustomKeywords.'TempletPortalKeywords.xpathObject'('ai_msg_input', "//input[@type='text']")

			TestObject msgField = null
			boolean editableField = false
			if (CustomKeywords.'TempletPortalKeywords.isPresentQuiet'(fTextarea, 8)) {
				msgField = fTextarea
			} else if (CustomKeywords.'TempletPortalKeywords.isPresentQuiet'(fEditable, 3)) {
				msgField = fEditable
				editableField = true
			} else if (CustomKeywords.'TempletPortalKeywords.isPresentQuiet'(fInput, 3)) {
				msgField = fInput
			}

			if (msgField == null) {
				warnings.add('[AI-IFRAME] Campo de mensaje no encontrado en drafts — pedir data-testid')
			} else {
				String alpha = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ'
				String lower = 'abcdefghijklmnopqrstuvwxyz'
				List sendCandidates = [
					CustomKeywords.'TempletPortalKeywords.xpathObject'('ai_btn_send',     "//button[contains(translate(normalize-space(.),'${alpha}','${lower}'),'send')]"),
					CustomKeywords.'TempletPortalKeywords.xpathObject'('ai_btn_create',   "//button[contains(translate(normalize-space(.),'${alpha}','${lower}'),'create')]"),
					CustomKeywords.'TempletPortalKeywords.xpathObject'('ai_btn_generate', "//button[contains(translate(normalize-space(.),'${alpha}','${lower}'),'generate')]"),
					CustomKeywords.'TempletPortalKeywords.xpathObject'('ai_btn_submit',   "//button[@type='submit']")
				]
				TestObject sendBtn = null
				for (TestObject c : sendCandidates) {
					if (CustomKeywords.'TempletPortalKeywords.isPresentQuiet'(c, 3)) {
						sendBtn = c
						break
					}
				}

				// 3a. MENSAJE VACIO: el boton crear deberia estar deshabilitado
				try { WebUI.clearText(msgField) } catch (Throwable ignore) { }
				WebUI.delay(1)
				if (sendBtn == null) {
					warnings.add('[AI-EMPTY][TICKET] Boton crear/enviar no encontrado para el caso vacio — pedir data-testid')
				} else {
					String disabledAttr = WebUI.getAttribute(sendBtn, 'disabled', FailureHandling.OPTIONAL)
					String ariaDisabled = WebUI.getAttribute(sendBtn, 'aria-disabled', FailureHandling.OPTIONAL)
					boolean isDisabled = (disabledAttr != null) || (ariaDisabled != null && ariaDisabled.trim().toLowerCase() == 'true')
					if (isDisabled) {
						warnings.add('[AI-EMPTY][TICKET] Esperado OK: con mensaje vacio el boton crear esta DESHABILITADO. Warning informativo para ticket.')
					} else {
						warnings.add('[AI-EMPTY][TICKET] El boton crear esta HABILITADO con mensaje vacio — deberia deshabilitarse. Genera ticket (no bloqueante).')
					}
				}
				CustomKeywords.'TempletPortalKeywords.captureCaseScreenshot'(caseId, 'ai_empty_message')

				// 3b. MENSAJE CON MARCADOR: escribir, enviar y capturar respuesta
				boolean wrote = false
				try {
					if (editableField) {
						WebUI.sendKeys(msgField, marker)
					} else {
						WebUI.clearText(msgField)
						WebUI.setText(msgField, marker)
					}
					wrote = true
				} catch (Throwable t) {
					try { WebUI.sendKeys(msgField, marker); wrote = true } catch (Throwable t2) {
						warnings.add('[AI-MSG] No se pudo escribir el marcador: ' + t2.getMessage())
					}
				}
				WebUI.delay(1)

				boolean sent = false
				if (wrote && sendBtn != null) {
					try { WebUI.click(sendBtn); sent = true } catch (Throwable t) {
						warnings.add('[AI-MSG] No se pudo click en enviar: ' + t.getMessage())
					}
				} else if (wrote) {
					warnings.add('[AI-MSG] Sin boton enviar identificado — no se envio el marcador')
				}

				String responseText = ''
				if (sent) {
					WebUI.delay(8)
					List respXpaths = [
						"(//*[contains(@class,'assistant')])[last()]",
						"(//*[contains(@class,'response')])[last()]",
						"(//*[contains(@class,'message')])[last()]",
						"(//*[contains(@class,'bubble')])[last()]"
					]
					for (String xp : respXpaths) {
						TestObject ro = CustomKeywords.'TempletPortalKeywords.xpathObject'('ai_resp', xp)
						if (CustomKeywords.'TempletPortalKeywords.isPresentQuiet'(ro, 4)) {
							try { responseText = WebUI.getText(ro, FailureHandling.OPTIONAL) } catch (Throwable ignore) { }
							if (responseText != null && responseText.trim().length() > 0) { break }
						}
					}
					if (responseText == null || responseText.trim().length() == 0) {
						warnings.add('[AI-RESP] No se pudo capturar la respuesta de la IA — pedir data-testid')
						responseText = ''
					} else {
						int previewLen = Math.min(300, responseText.length())
						KeywordUtil.logInfo("[${caseId}] Respuesta IA (${responseText.length()} chars): " + responseText.substring(0, previewLen))
					}
				}
				CustomKeywords.'TempletPortalKeywords.captureCaseScreenshot'(caseId, 'ai_response')

				Map snap = [
					caseId            : caseId,
					marker            : marker,
					initiative        : initiativeName,
					timestamp         : new Date().format("yyyy-MM-dd'T'HH:mm:ss"),
					messageSent       : sent,
					aiResponseCaptured: (responseText.trim().length() > 0),
					aiResponseExcerpt : (responseText.length() > 1000 ? responseText.substring(0, 1000) : responseText),
					warnings          : warnings
				]
				String snapPath = System.getProperty('user.dir') + '/Reports/Builders/snapshots/brand_ai_text_latest.json'
				try {
					CustomKeywords.'TempletPortalKeywords.writeJsonSnapshot'(snapPath, snap)
				} catch (Throwable t) {
					warnings.add('[SNAPSHOT] No se pudo escribir snapshot: ' + t.getMessage())
				}
			}
		}

		// Cerrar la pestaña "Create TEXT" y volver al brand
		try { driver.close() } catch (Throwable ignore) { }
		try {
			driver.switchTo().window(mainHandle)
		} catch (Throwable t) {
			CustomKeywords.'TempletPortalKeywords.closeExtraTabsKeepCurrent'('post_create_text')
		}
		WebUI.delay(2)
	}
}

CustomKeywords.'TempletPortalKeywords.captureCaseScreenshot'(caseId, 'brand_ai_text_final')

if (failures) {
	CustomKeywords.'CommonKeywords.logCaseSummary'(caseId, failures, warnings)
	KeywordUtil.markFailedAndStop("[${caseId}] Failures: ${failures}")
} else {
	CustomKeywords.'CommonKeywords.logCaseSummary'(caseId, failures, warnings)
	KeywordUtil.logInfo("[${caseId}] PASSED" + (warnings ? " con ${warnings.size()} warnings" : ""))
}
