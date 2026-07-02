import static com.kms.katalon.core.testobject.ObjectRepository.findTestObject
import com.kms.katalon.core.model.FailureHandling as FailureHandling
import com.kms.katalon.core.testobject.TestObject
import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import CommonKeywords
import internal.GlobalVariable as GlobalVariable

String caseId = 'TC-SHEETS-LIST-VIEW-CLICKABLE-002'
String startUrl = CommonKeywords.getRequiredGlobal('SHEETS_TEST_URL', 'https://sheets-test.templet.io/admin/manager.php')
String clientCss = '#inputGroupSelect01'
String initiativeCss = "select[data-toggle='drop-initiatives']"
String sortCss = '#sortField-alpha'
List<String> failures = []
List<String> warnings = []

def scrollDown = {
    WebUI.executeJavaScript('window.scrollTo(0, document.body.scrollHeight / 2);', null)
    WebUI.delay(1)
}

def scrollTop = {
    WebUI.executeJavaScript('window.scrollTo(0, 0);', null)
    WebUI.delay(1)
}

def closeVisibleModal = {
    Boolean closed = (Boolean) WebUI.executeJavaScript("""
        var closeBtn = document.querySelector('.modal.show .close, .modal.show [data-dismiss="modal"], .modal.show .btn-close, .modal.show button[aria-label="Close"]');
        if (closeBtn) { closeBtn.click(); return true; }
        var backdrop = document.querySelector('.modal-backdrop.show');
        if (backdrop) { document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape', keyCode: 27 })); return true; }
        return false;
    """, null)
    if (Boolean.TRUE.equals(closed)) WebUI.delay(1)
    return Boolean.TRUE.equals(closed)
}

def waitForModalVisible = { int timeoutSec ->
    for (int i = 0; i < timeoutSec; i++) {
        Boolean visible = (Boolean) WebUI.executeJavaScript("""
            var modal = document.querySelector('.modal.show, .modal[style*="display: block"]');
            return modal !== null;
        """, null)
        if (Boolean.TRUE.equals(visible)) return true
        WebUI.delay(1)
    }
    return false
}

def setSelectValueByCss = { String cssSelector, String value ->
    Boolean ok = (Boolean) WebUI.executeJavaScript("""
        var sel = document.querySelector(arguments[0]);
        if (!sel) return false;
        sel.value = arguments[1];
        sel.dispatchEvent(new Event('input', { bubbles: true }));
        sel.dispatchEvent(new Event('change', { bubbles: true }));
        return true;
    """, [cssSelector, value])
    return Boolean.TRUE.equals(ok)
}

def isSortEnabled = {
    Boolean enabled = (Boolean) WebUI.executeJavaScript('var sel = document.querySelector("#sortField-alpha"); return sel && !sel.disabled;', null)
    return Boolean.TRUE.equals(enabled)
}

def getSortOptions = {
    List options = (List) WebUI.executeJavaScript("""
        var sel = document.querySelector('#sortField-alpha');
        if (!sel) return [];
        return Array.from(sel.options).map(function(o) { return { value: o.value, text: o.textContent.trim() }; });
    """, null)
    return options ?: []
}

def getVisibleDocumentTitles = {
    List titles = (List) WebUI.executeJavaScript("""
        return Array.from(document.querySelectorAll('.document-item h6.title-card[data-title]'))
            .map(function(el) {
                var text = el.getAttribute('data-title') || '';
                return text.trim();
            })
            .filter(function(text) { return text.length > 0; });
    """, null)
    return titles ?: []
}

def isAlphabeticalAsc = { List<String> values ->
    if (values == null || values.size() <= 1) return true
    List<String> normalized = values.collect { (it ?: '').toLowerCase() }
    List<String> sorted = new ArrayList<String>(normalized)
    Collections.sort(sorted)
    return normalized == sorted
}

def findSortOptionByLabel = { List options, String label ->
    String expected = (label ?: '').trim().toLowerCase()
    return options.find { Map opt ->
        String text = opt?.text?.toString()?.trim()?.toLowerCase()
        return text == expected
    }
}

def applySortAndReadTitles = { String optionLabel ->
    List sortOptions = getSortOptions()
    Map selected = findSortOptionByLabel(sortOptions, optionLabel)
    if (!selected?.value) {
        return [ok: false, titles: [], error: "Opcion '${optionLabel}' no disponible en Sort"]
    }
    boolean applied = setSelectValueByCss(sortCss, selected.value.toString())
    WebUI.delay(2)
    if (!applied) {
        return [ok: false, titles: [], error: "No fue posible aplicar Sort '${optionLabel}'"]
    }
    List<String> titles = getVisibleDocumentTitles()
    if (titles.isEmpty()) {
        return [ok: false, titles: [], error: "Sin titulos visibles tras Sort '${optionLabel}'"]
    }
    return [ok: true, titles: titles, error: null]
}

def hasNoDocuments = {
    Boolean found = (Boolean) WebUI.executeJavaScript("""
        return Array.from(document.querySelectorAll('body *')).some(function(el) {
            if (!(el instanceof HTMLElement)) return false;
            var st = window.getComputedStyle(el);
            if (st.display === 'none' || st.visibility === 'hidden') return false;
            return el.textContent.trim().indexOf('No hay documentos disponibles') >= 0;
        });
    """, null)
    return Boolean.TRUE.equals(found)
}

try {
    KeywordUtil.logInfo("[UPDATED-SCRIPT-RUN] [CASE START] caseId - ${caseId} - ${new Date()}")

    // STEP 1: Abrir browser y login
    boolean browserAlreadyOpen = false
    try {
        browserAlreadyOpen = WebUI.verifyElementPresent(findTestObject('Sheets/Filters/section_dashboard'), 5, FailureHandling.OPTIONAL)
    } catch (Exception ignored) {}
    if (!browserAlreadyOpen) {
        WebUI.delay(3)  // Buffer para liberar Chrome profile del TC anterior
        CustomKeywords.'TempletPortalKeywords.openBrowserAndLoginWithMicrosoft'(startUrl)
    }
    WebUI.waitForPageLoad(10)
    KeywordUtil.logInfo("[STEP 1] Browser abierto y login completado")

    // STEP 2: Setear Client
    Map selectedClient = CommonKeywords.selectPreferredOption('Sheets/Filters/select_client', clientCss, 'BRAVA', 12)
    if (!selectedClient?.value) {
        failures.add('[CLIENT] No fue posible seleccionar client')
    } else {
        KeywordUtil.logInfo("[STEP 2] Client seleccionado: ${selectedClient.text}")
    }

    // STEP 3: Iterar initiatives hasta encontrar una con contenido y Sort habilitado
    List<Map> initiatives = CommonKeywords.readSelectOptionsWhenReady(initiativeCss, 12, 1)
    List<Map> candidates = initiatives.findAll { it['value'] != null && !it['value'].toString().trim().isEmpty() }

    Map selectedInitiative = null
    for (Map candidate : candidates) {
        String iValue = candidate['value'].toString()
        String iText  = candidate['text']?.toString() ?: iValue
        if (!setSelectValueByCss(initiativeCss, iValue)) continue
        WebUI.waitForPageLoad(5)
        WebUI.delay(2)
        if (hasNoDocuments()) {
            KeywordUtil.logInfo("[STEP 3] Initiative '${iText}' sin documentos, probando siguiente...")
            continue
        }
        if (isSortEnabled()) {
            selectedInitiative = [value: iValue, text: iText]
            KeywordUtil.logInfo("[STEP 3] Initiative valida encontrada: ${iText}")
            break
        }
        KeywordUtil.logInfo("[STEP 3] Initiative '${iText}' sin Sort habilitado, probando siguiente...")
    }
    if (selectedInitiative == null) {
        failures.add('[INITIATIVE] Ninguna initiative tiene contenido con Sort habilitado')
    }

    // Validar que el filtro de initiative cambia el contenido visible
    if (selectedInitiative != null && candidates.size() > 1) {
        int cardsBase = getVisibleDocumentTitles().size()
        Map secondInit = candidates.find { Map c -> c['value']?.toString() != selectedInitiative.value }
        if (secondInit) {
            setSelectValueByCss(initiativeCss, secondInit['value'].toString())
            WebUI.waitForPageLoad(3); WebUI.delay(1)
            int cardsOther = getVisibleDocumentTitles().size()
            if (cardsBase != cardsOther || hasNoDocuments()) {
                KeywordUtil.logInfo("[INITIATIVE-FILTER] OK - '${selectedInitiative.text}' titulos=${cardsBase} vs '${secondInit['text']}' titulos=${cardsOther}")
            } else {
                warnings.add("[INITIATIVE-FILTER] Cambio de initiative no modifico contenido (ambas: ${cardsBase} items)")
            }
            // Volver a la initiative original
            setSelectValueByCss(initiativeCss, selectedInitiative.value.toString())
            WebUI.waitForPageLoad(3); WebUI.delay(1)
        }
    } else if (candidates.size() <= 1) {
        warnings.add('[INITIATIVE-FILTER] Solo hay una initiative disponible, no se pudo comparar')
    }

    // Helper: aplica Sort y valida orden/reordenamiento usando titulos visibles
    def applySortOptions = { String viewLabel ->
        def logSortWarning = { String msg ->
            warnings.add("[SORT ${viewLabel}] ${msg}")
            KeywordUtil.logInfo("[SORT ${viewLabel}][WARNING] ${msg}")
        }

        if (!isSortEnabled()) {
            logSortWarning('Sort select no se habilito')
            return
        }
        List sortOptions = getSortOptions()
        List<String> expected = ['Newest', 'Oldest', 'A to Z', 'Z to A']
        List<String> missing = expected.findAll { label -> !findSortOptionByLabel(sortOptions, label) }
        if (!missing.isEmpty()) {
            logSortWarning("Faltan opciones esperadas: ${missing.join(', ')}")
            return
        }

        Map newest = applySortAndReadTitles('Newest')
        Map oldest = applySortAndReadTitles('Oldest')
        Map atoz = applySortAndReadTitles('A to Z')
        Map ztoa = applySortAndReadTitles('Z to A')

        List<Map> results = [
            [label: 'Newest', data: newest],
            [label: 'Oldest', data: oldest],
            [label: 'A to Z', data: atoz],
            [label: 'Z to A', data: ztoa]
        ]
        for (Map result : results) {
            if (!result.data.ok) {
                logSortWarning(result.data.error.toString())
            } else {
                KeywordUtil.logInfo("[SORT ${viewLabel}] ${result.label} aplicado con ${result.data.titles.size()} titulos")
                CustomKeywords.'TempletPortalKeywords.captureCaseScreenshot'(caseId, "sort_${viewLabel.replace(' ','_')}_${result.label.replace(' ','_')}")
            }
        }
        boolean allSortReadsOk = results.every { Map r -> Boolean.TRUE.equals(r.data.ok) }
        if (allSortReadsOk) {
            List<String> newestTitles = newest.titles as List<String>
            List<String> oldestTitles = oldest.titles as List<String>
            List<String> azTitles = atoz.titles as List<String>
            List<String> zaTitles = ztoa.titles as List<String>
            // Verificar que sorts distintos producen listas distintas
            if (newestTitles == azTitles) {
                logSortWarning('Newest y A-to-Z devuelven el mismo orden - sort puede no estar aplicando')
            }

            String newestFirst = newestTitles.first()
            String newestLast = newestTitles.last()
            String oldestFirst = oldestTitles.first()
            String oldestLast = oldestTitles.last()
            if (newestFirst == oldestLast && newestLast == oldestFirst) {
                KeywordUtil.logInfo("[SORT ${viewLabel}] Validacion Newest/Oldest OK")
            } else {
                logSortWarning('Newest/Oldest no invierten primer/ultimo item')
            }

            String azFirst = azTitles.first()
            String azLast = azTitles.last()
            String zaFirst = zaTitles.first()
            String zaLast = zaTitles.last()
            if (azFirst == zaLast && azLast == zaFirst) {
                KeywordUtil.logInfo("[SORT ${viewLabel}] Validacion A to Z / Z to A OK")
            } else {
                logSortWarning('A to Z / Z to A no invierten primer/ultimo item')
            }

            if (isAlphabeticalAsc(azTitles)) {
                KeywordUtil.logInfo("[SORT ${viewLabel}] A to Z en orden alfabetico ascendente")
            } else {
                logSortWarning('A to Z no esta en orden alfabetico ascendente')
            }
        }
    }

    // STEP 4: Sort en View Grid (vista inicial)
    applySortOptions('View Grid')

    // STEP 5: Click View List
    TestObject viewListBtn = findTestObject('Tc1/Page_sheets.templet.  Admin/a_View List')
    WebUI.waitForElementVisible(viewListBtn, 10)
    WebUI.waitForElementClickable(viewListBtn, 10)
    WebUI.click(viewListBtn)
    WebUI.waitForPageLoad(5)
    WebUI.delay(2)
    KeywordUtil.logInfo("[STEP 5] View List clickeado")

    // STEP 6: Sort en View List
    applySortOptions('View List')

    // STEP 7: Volver a View Grid
    scrollTop()
    Boolean gridClicked = (Boolean) WebUI.executeJavaScript("""
        var btn = document.querySelector("a[href='#tabs-1'], .btn-view_cards");
        if (btn) { btn.click(); return true; }
        return false;
    """, null)
    if (Boolean.TRUE.equals(gridClicked)) {
        WebUI.waitForPageLoad(5)
        KeywordUtil.logInfo("[STEP 7] View Grid clickeado")
    } else {
        failures.add('[VIEW GRID] No se encontro boton View Grid')
    }

    // STEP 8: Export - validar PPTX e IDML
    TestObject exportBtn = findTestObject('Tc1/Page_sheets.templet.  Admin/button_Exportar')
    WebUI.waitForElementVisible(exportBtn, 10)
    WebUI.waitForElementClickable(exportBtn, 10)
    WebUI.click(exportBtn)
    WebUI.delay(1)
    KeywordUtil.logInfo("[STEP 8] Export clickeado")

    long preExportTimestamp = System.currentTimeMillis()

    Boolean pptxVisible = (Boolean) WebUI.executeJavaScript("""
        var el = document.querySelector('#btPPTX') ||
            Array.from(document.querySelectorAll('.dropdown-item')).find(function(e) { return e.textContent.trim().toUpperCase() === 'PPTX'; });
        if (!el) return false;
        var st = window.getComputedStyle(el);
        return st.display !== 'none' && st.visibility !== 'hidden';
    """, null)

    Boolean idmlVisible = (Boolean) WebUI.executeJavaScript("""
        var el = document.querySelector('#btZIP') ||
            Array.from(document.querySelectorAll('.dropdown-item')).find(function(e) { return e.textContent.trim().toUpperCase() === 'IDML'; });
        if (!el) return false;
        var st = window.getComputedStyle(el);
        return st.display !== 'none' && st.visibility !== 'hidden';
    """, null)

    if (Boolean.TRUE.equals(pptxVisible)) {
        KeywordUtil.logInfo("[STEP 8.1] PPTX visible en dropdown Export")
    } else {
        failures.add('[EXPORT] PPTX no visible en dropdown')
    }
    if (Boolean.TRUE.equals(idmlVisible)) {
        KeywordUtil.logInfo("[STEP 8.2] IDML visible en dropdown Export")
    } else {
        failures.add('[EXPORT] IDML no visible en dropdown')
    }

    def clickExportOption = { String label, String domId ->
        Boolean clicked = (Boolean) WebUI.executeJavaScript("""
            var option = document.querySelector(arguments[1]) ||
                Array.from(document.querySelectorAll('.dropdown-item')).find(function(e) {
                    return e.textContent.trim().toUpperCase() === arguments[0].toUpperCase();
                });
            if (!option) return false;
            option.click();
            return true;
        """, [label, '#' + domId])
        return Boolean.TRUE.equals(clicked)
    }

    if (Boolean.TRUE.equals(pptxVisible)) {
        if (clickExportOption('PPTX', 'btPPTX')) {
            KeywordUtil.logInfo('[STEP 8.3] Click en PPTX ejecutado')
        } else {
            failures.add('[EXPORT] No se pudo clickear opcion PPTX')
        }
    }

    WebUI.click(exportBtn)
    WebUI.delay(1)

    if (Boolean.TRUE.equals(idmlVisible)) {
        if (clickExportOption('IDML', 'btZIP')) {
            KeywordUtil.logInfo('[STEP 8.4] Click en IDML ejecutado')
        } else {
            failures.add('[EXPORT] No se pudo clickear opcion IDML')
        }
    }

    WebUI.delay(5)
    File downloadsDir = new File(System.getProperty('user.home'), 'Downloads')
    List<File> newDownloads = []
    if (downloadsDir.exists() && downloadsDir.isDirectory()) {
        File[] files = downloadsDir.listFiles()
        if (files != null) {
            newDownloads = files.findAll { File f ->
                if (f == null || !f.isFile()) return false
                String name = (f.getName() ?: '').toLowerCase()
                if (name.endsWith('.crdownload') || name.endsWith('.tmp')) return false
                return f.lastModified() > preExportTimestamp
            }
        }
    }
    if (newDownloads.isEmpty()) {
        warnings.add('[STEP 8.5] No se detectaron descargas nuevas en Downloads tras clicks de export')
        KeywordUtil.logInfo('[STEP 8.5][WARNING] No se detectaron descargas nuevas en Downloads tras clicks de export')
    } else {
        String names = newDownloads.collect { it.getName() }.join(', ')
        KeywordUtil.logInfo("[STEP 8.5] Descargas nuevas detectadas: ${names}")
    }

    WebUI.executeJavaScript('document.body.click();', null)
    WebUI.delay(1)

    // STEP 9: Create Document y cerrar modal
    TestObject createDocBtn = findTestObject('Tc1/Page_sheets.templet.  Admin/a_Create Document')
    WebUI.waitForElementVisible(createDocBtn, 10)
    WebUI.waitForElementClickable(createDocBtn, 10)
    WebUI.click(createDocBtn)
    KeywordUtil.logInfo("[STEP 9] Create Document clickeado")
    if (waitForModalVisible(5)) {
        WebUI.delay(1)
        closeVisibleModal()
        KeywordUtil.logInfo("[STEP 9] Modal Create Document cerrado")
    } else {
        failures.add('[CREATE DOC] Modal no aparecio')
    }
    WebUI.delay(1)

    // STEP 10: Create Initiative y cerrar modal
    TestObject createInitBtn = findTestObject('Tc1/Page_sheets.templet.  Admin/a_Create Initiative')
    WebUI.waitForElementVisible(createInitBtn, 10)
    WebUI.waitForElementClickable(createInitBtn, 10)
    WebUI.click(createInitBtn)
    KeywordUtil.logInfo("[STEP 10] Create Initiative clickeado")
    if (waitForModalVisible(5)) {
        WebUI.delay(1)
        closeVisibleModal()
        KeywordUtil.logInfo("[STEP 10] Modal Create Initiative cerrado")
    } else {
        failures.add('[CREATE INIT] Modal no aparecio')
    }
    WebUI.delay(1)

    // STEP 11: Logout
    TestObject logoutBtn = findTestObject('Sheets/Filters/btn_logout')
    WebUI.waitForElementVisible(logoutBtn, 10)
    WebUI.waitForElementClickable(logoutBtn, 10)
    WebUI.click(logoutBtn)
    WebUI.waitForPageLoad(10)
    KeywordUtil.logInfo("[STEP 11] Log Out clickeado")

    CustomKeywords.'TempletPortalKeywords.captureCaseScreenshot'(caseId, 'final')

    CommonKeywords.logCaseSummary(caseId, failures, warnings)

    if (failures.isEmpty()) {
        KeywordUtil.markPassed(caseId + ' OK. Flujo completo ejecutado correctamente.')
    } else {
        KeywordUtil.markFailed(caseId + ' fallo: ' + failures.join(' | '))
    }

} catch (Exception e) {
    KeywordUtil.logInfo("[UPDATED-SCRIPT-RUN] [CASE ERROR] caseId - ${caseId} - ${new Date()} - " + e.message)
    try {
        CustomKeywords.'TempletPortalKeywords.captureCaseScreenshot'(caseId, 'error')
    } catch (Exception ex) {
        KeywordUtil.logInfo("[SCREENSHOT-ERROR] ${ex.message}")
    }
    KeywordUtil.markFailedAndStop(caseId + ' - Error critico: ' + e.message)
} finally {
    // Cerrar Chrome al terminar
    try { WebUI.closeBrowser() } catch (Exception ignored) {}
}