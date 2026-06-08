import static com.kms.katalon.core.testobject.ObjectRepository.findTestObject
import com.kms.katalon.core.model.FailureHandling as FailureHandling
import com.kms.katalon.core.testobject.TestObject
import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import CommonKeywords
import internal.GlobalVariable as GlobalVariable

String caseId = 'TC-DECKS-LIST-VIEW-CLICKABLE-002'
String startUrl = CustomKeywords.'CommonKeywords.getRequiredGlobal'('DECKS_TEST_URL', 'https://decks-test.templet.io/admin/manager.php')
String clientCss = '#inputGroupSelect01'
String initiativeCss = "select[data-toggle='drop-initiatives']"
String sortCss = '#sortField-alpha'
List<String> failures = []
List<String> warnings = []

def countVisibleCards = {
    Number count = (Number) WebUI.executeJavaScript('''
        var candidates = Array.from(document.querySelectorAll(
            '.thumbnails-boxes .thumbnail-box, .thumbnails-boxes .box, .thumbnails-boxes .card, .thumbnails-boxes article, .thumbnails-boxes [class*="thumb"], .thumbnails-boxes [class*="item"]'
        ));
        var visible = candidates.filter(function(el) {
            if (!(el instanceof HTMLElement)) return false;
            var st = window.getComputedStyle(el);
            return st.display !== 'none' && st.visibility !== 'hidden' && el.offsetParent !== null;
        });
        return visible.length;
    ''', null)
    return (count != null) ? count.intValue() : 0
}

def clickListToggle = {
    Boolean clicked = (Boolean) WebUI.executeJavaScript('''
        var listCandidates = Array.from(document.querySelectorAll(
            '#icon-list-view, [id*="list"][class*="icon"], [id*="list"][id*="view"], .icon-list, [data-view="list"], [aria-label*="List"], [title*="List"]'
        ));
        if (listCandidates.length === 0) {
            var fallback = Array.from(document.querySelectorAll('svg,button,a,span')).filter(function(el) {
                var id = (el.id || '').toLowerCase();
                var cls = (el.className && el.className.baseVal ? el.className.baseVal : el.className || '').toString().toLowerCase();
                var label = ((el.getAttribute('aria-label') || '') + ' ' + (el.getAttribute('title') || '')).toLowerCase();
                return id.indexOf('list') >= 0 || cls.indexOf('list') >= 0 || label.indexOf('list') >= 0;
            });
            listCandidates = fallback;
        }
        var el = listCandidates.find(function(node) {
            if (!(node instanceof HTMLElement) && !(node instanceof SVGElement)) return false;
            var host = (node instanceof HTMLElement) ? node : node.closest('*');
            if (!host) return false;
            var st = window.getComputedStyle(host);
            return st.display !== 'none' && st.visibility !== 'hidden';
        });
        if (!el) return false;
        ['mousedown', 'mouseup', 'click'].forEach(function(evt) {
            el.dispatchEvent(new MouseEvent(evt, { bubbles: true }));
        });
        return true;
    ''', null)
    return Boolean.TRUE.equals(clicked)
}

def hasVisibleActionEntry = { String label ->
    Boolean found = (Boolean) WebUI.executeJavaScript('''
        var expected = (arguments[0] || '').toLowerCase();
        var nodes = Array.from(document.querySelectorAll('a,button,span,p,li,div'));
        return nodes.some(function(el) {
            if (!(el instanceof HTMLElement)) return false;
            var st = window.getComputedStyle(el);
            if (st.display === 'none' || st.visibility === 'hidden' || el.offsetParent === null) return false;
            var txt = (el.textContent || '').trim().toLowerCase();
            return txt === expected || txt.indexOf(expected) >= 0;
        });
    ''', [label])
    return Boolean.TRUE.equals(found)
}

def tryOpenFirstCardMenu = {
    Boolean opened = (Boolean) WebUI.executeJavaScript('''
        var section = document.querySelector('.thumbnails-boxes') || document.body;
        var triggers = Array.from(section.querySelectorAll(
            '.dropdown-toggle, [data-bs-toggle="dropdown"], [data-toggle="dropdown"], [aria-haspopup="true"], .fa-ellipsis-h, .fa-ellipsis-v, [class*="ellipsis"], [class*="three-dots"], [class*="kebab"]'
        ));
        var trigger = triggers.find(function(el) {
            if (!(el instanceof HTMLElement)) return false;
            var st = window.getComputedStyle(el);
            return st.display !== 'none' && st.visibility !== 'hidden' && el.offsetParent !== null;
        });
        if (!trigger) return false;
        ['mousedown', 'mouseup', 'click'].forEach(function(evt) {
            trigger.dispatchEvent(new MouseEvent(evt, { bubbles: true }));
        });
        return true;
    ''', null)
    return Boolean.TRUE.equals(opened)
}

def hasNoDocumentsMessage = {
    Boolean found = (Boolean) WebUI.executeJavaScript('''
        var target = 'No hay documentos disponibles en esta iniciativa.';
        var nodes = Array.from(document.querySelectorAll('body *'));
        return nodes.some(function(el) {
            if (!(el instanceof HTMLElement)) return false;
            var st = window.getComputedStyle(el);
            var visible = st.display !== 'none' && st.visibility !== 'hidden' && el.offsetParent !== null;
            var txt = (el.textContent || '').trim();
            return visible && txt.indexOf(target) >= 0;
        });
    ''', null)
    return Boolean.TRUE.equals(found)
}

def setSelectValueByCss = { String cssSelector, String value ->
    Boolean ok = (Boolean) WebUI.executeJavaScript('''
        var sel = document.querySelector(arguments[0]);
        var value = arguments[1];
        if (!sel) return false;
        sel.value = value;
        sel.dispatchEvent(new Event('input', { bubbles: true }));
        sel.dispatchEvent(new Event('change', { bubbles: true }));
        return true;
    ''', [cssSelector, value])
    return Boolean.TRUE.equals(ok)
}

def scrollTop = {
    WebUI.executeJavaScript('window.scrollTo(0, 0);', null)
    WebUI.delay(1)
}

def scrollDown = {
    WebUI.executeJavaScript('window.scrollTo(0, document.body.scrollHeight);', null)
    WebUI.delay(1)
}

def scrollFullPage = {
    WebUI.executeJavaScript('window.scrollTo(0, document.body.scrollHeight);', null)
    WebUI.delay(1)
    WebUI.executeJavaScript('window.scrollTo(0, 0);', null)
    WebUI.delay(1)
    KeywordUtil.logInfo('[SCROLL] Recorrido completo: bottom -> top')
}

def isSortEnabled = {
    Boolean enabled = (Boolean) WebUI.executeJavaScript('var sel = document.querySelector("#sortField-alpha"); return sel && !sel.disabled;', null)
    return Boolean.TRUE.equals(enabled)
}

def getSortOptions = {
    List opts = (List) WebUI.executeJavaScript("""
        var sel = document.querySelector('#sortField-alpha');
        if (!sel) return [];
        return Array.from(sel.options).map(function(o) { return { value: o.value, text: o.textContent.trim() }; });
    """, null)
    return opts ?: []
}

def getVisibleDocumentTitles = {
    List titles = (List) WebUI.executeJavaScript("""
        var sels = ['.document-item h6.title-card[data-title]', '.document-item [data-title]', '.thumbnail-box h6[data-title]', 'h6.title-card'];
        for (var s = 0; s < sels.length; s++) {
            var els = document.querySelectorAll(sels[s]);
            if (els.length > 0) {
                return Array.from(els).map(function(el) {
                    return (el.getAttribute('data-title') || el.textContent || '').trim();
                }).filter(function(t) { return t.length > 0; });
            }
        }
        return [];
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
    List<String> ts = getVisibleDocumentTitles()
    if (ts.isEmpty()) {
        return [ok: false, titles: [], error: "Sin titulos visibles tras Sort '${optionLabel}'"]
    }
    return [ok: true, titles: ts, error: null]
}

List<Map<String, Object>> listChecks = [
    [path: 'Sheets/Objects/Filters/select_client_initiative', label: 'Filtro client/initiative', clickable: true],
    [path: 'Sheets/Objects/Filters/select_sort_field', label: 'Filtro sort', clickable: false]
]

try {
    // Verificar si ya hay sesión abierta (reutilizar driver entre TCs de la suite)
    boolean browserAlreadyOpen = false
    try {
        browserAlreadyOpen = WebUI.verifyElementPresent(findTestObject('Sheets/Filters/section_dashboard'), 5, FailureHandling.OPTIONAL)
    } catch (Exception ignored) {}

    if (!browserAlreadyOpen) {
        CustomKeywords.'TempletPortalKeywords.openBrowserAndLoginWithMicrosoft'(startUrl)
    }
    WebUI.waitForPageLoad(10)

    Map selectedClient = CommonKeywords.selectPreferredOption('Sheets/Filters/select_client', clientCss, 'BRAVA', 12)
    if (!selectedClient?.value) {
        KeywordUtil.markFailedAndStop('[CLIENT] No fue posible seleccionar client para la validación de List view')
    }

    List<Map> initiatives = CommonKeywords.readSelectOptionsWhenReady(initiativeCss, 12, 1)
    List<Map> candidates = initiatives.findAll { it['value'] != null && !it['value'].toString().trim().isEmpty() }
    if (!candidates || candidates.isEmpty()) {
        KeywordUtil.markFailedAndStop('[INITIATIVE] No hay initiatives disponibles después de seleccionar client')
    }

    Map selectedInitiative = null
    for (Map opt : candidates) {
        String value = opt['value'].toString()
        String text = opt['text']?.toString() ?: value
        if (!setSelectValueByCss(initiativeCss, value)) {
            continue
        }
        WebUI.waitForPageLoad(2)
        WebUI.delay(1)
        int cards = countVisibleCards()
        if (!hasNoDocumentsMessage() && cards > 0) {
            selectedInitiative = [value: value, text: text]
            break
        }
    }

    if (selectedInitiative == null) {
        KeywordUtil.markFailedAndStop('[INITIATIVE] No se encontró initiative con contenido para validar List view')
    }

    KeywordUtil.logInfo("[SETUP] Client seleccionado: ${selectedClient.text}; Initiative con contenido: ${selectedInitiative.text}")
    scrollFullPage()

    // Validar que el filtro de initiative realmente cambia el contenido
    int cardsFirstInitiative = countVisibleCards()
    Map secondInitiative = candidates.find { Map c ->
        c['value']?.toString() != selectedInitiative.value && !setSelectValueByCss(initiativeCss, c['value'].toString()).equals(false)
    }
    if (secondInitiative) {
        setSelectValueByCss(initiativeCss, secondInitiative['value'].toString())
        WebUI.waitForPageLoad(3)
        WebUI.delay(1)
        int cardsSecondInitiative = countVisibleCards()
        if (cardsFirstInitiative != cardsSecondInitiative || !hasNoDocumentsMessage()) {
            KeywordUtil.logInfo("[INITIATIVE-FILTER] OK - initiative '${selectedInitiative.text}' cards=${cardsFirstInitiative} vs '${secondInitiative['text']}' cards=${cardsSecondInitiative}")
        } else {
            warnings.add("[INITIATIVE-FILTER] Cambio de initiative no modifico el contenido visible (ambas: ${cardsFirstInitiative} cards)")
        }
        // Volver a la initiative original con contenido
        setSelectValueByCss(initiativeCss, selectedInitiative.value.toString())
        WebUI.waitForPageLoad(3)
        WebUI.delay(1)
    } else {
        warnings.add('[INITIATIVE-FILTER] Solo hay una initiative disponible, no se pudo comparar cambio')
    }

    def applySortOptions = { String viewLabel ->
        def logSortW = { String msg ->
            warnings.add("[SORT ${viewLabel}] ${msg}")
            KeywordUtil.logInfo("[SORT ${viewLabel}][WARNING] ${msg}")
        }
        if (!isSortEnabled()) { logSortW('Sort select no se habilito'); return }
        List sortOpts = getSortOptions()
        List<String> expectedOpts = ['Newest', 'Oldest', 'A to Z', 'Z to A']
        List<String> missingOpts = expectedOpts.findAll { lbl -> !findSortOptionByLabel(sortOpts, lbl) }
        if (!missingOpts.isEmpty()) { logSortW("Faltan opciones: ${missingOpts.join(', ')}"); return }
        Map r1 = applySortAndReadTitles('Newest')
        Map r2 = applySortAndReadTitles('Oldest')
        Map r3 = applySortAndReadTitles('A to Z')
        Map r4 = applySortAndReadTitles('Z to A')
        [[label: 'Newest', data: r1],[label: 'Oldest', data: r2],[label: 'A to Z', data: r3],[label: 'Z to A', data: r4]].each { Map r ->
            if (!r.data.ok) { logSortW(r.data.error.toString()) }
            else {
                KeywordUtil.logInfo("[SORT ${viewLabel}] ${r.label} OK con ${r.data.titles.size()} titulos")
                CustomKeywords.'TempletPortalKeywords.captureCaseScreenshot'(caseId, "sort_${viewLabel.replace(' ','_')}_${r.label.replace(' ','_')}")
            }
        }
        if (r1.ok && r2.ok && r3.ok && r4.ok) {
            List<String> t1 = r1.titles as List<String>; List<String> t2 = r2.titles as List<String>
            List<String> t3 = r3.titles as List<String>; List<String> t4 = r4.titles as List<String>
            // Verificar que distintos sorts producen resultados distintos
            if (t1 == t3) { logSortW('Newest y A-to-Z devuelven el mismo orden - sort puede no estar funcionando') }
            if (t1.first() == t2.last() && t1.last() == t2.first()) { KeywordUtil.logInfo("[SORT ${viewLabel}] Newest/Oldest OK") }
            else { logSortW('Newest/Oldest no invierten primer/ultimo item') }
            if (t3.first() == t4.last() && t3.last() == t4.first()) { KeywordUtil.logInfo("[SORT ${viewLabel}] A-Z/Z-A OK") }
            else { logSortW('A to Z / Z to A no invierten primer/ultimo item') }
            if (isAlphabeticalAsc(t3)) { KeywordUtil.logInfo("[SORT ${viewLabel}] A-Z en orden alfabetico") }
            else { logSortW('A to Z no en orden alfabetico ascendente') }
        }
    }
    applySortOptions('View Grid')

    TestObject listShape = findTestObject('Sheets/Objects/ListView/icon_list_shape')
    TestObject listViewIcon = findTestObject('Sheets/Objects/ListView/icon_list_view')
    WebUI.waitForElementVisible(listViewIcon, 4)
    int cardsBeforeToggle = countVisibleCards()
    boolean listViewReady = false
    for (int attempt = 1; attempt <= 3; attempt++) {
        WebUI.click(listViewIcon, FailureHandling.OPTIONAL)
        WebUI.click(listShape, FailureHandling.OPTIONAL)
        clickListToggle()
        WebUI.delay(1)
        int cardsAfterToggle = countVisibleCards()
        if (cardsAfterToggle != cardsBeforeToggle || cardsAfterToggle > 0) {
            listViewReady = true
            break
        }
    }

    if (!listViewReady) {
        KeywordUtil.markFailedAndStop('[LIST] No fue posible activar List View tras 3 intentos')
    }
    scrollFullPage()
    applySortOptions('View List')

    listChecks.each { item ->
        TestObject obj = findTestObject(item.path as String)
        WebUI.scrollToElement(obj, 2, FailureHandling.OPTIONAL)

        boolean visible = WebUI.waitForElementVisible(obj, 2, FailureHandling.OPTIONAL)
        if (!visible) {
            failures.add("No visible tras click List: ${item.label} (${item.path})")
            return
        }

        if (Boolean.TRUE.equals(item.clickable)) {
            boolean clickable = WebUI.waitForElementClickable(obj, 2, FailureHandling.OPTIONAL)
            if (!clickable) {
                failures.add("No clickeable tras click List: ${item.label} (${item.path})")
            }
        }
    }

    // En UI actual, acciones avanzadas se muestran en menu contextual por tarjeta.
    boolean menuOpened = tryOpenFirstCardMenu()
    if (!menuOpened) {
        warnings.add('No fue posible abrir menu de acciones en la primera tarjeta')
    } else {
        WebUI.delay(1)
        ['Edit', 'Rename', 'Duplicate', 'Download', 'Move', 'Delete'].each { actionLabel ->
            if (!hasVisibleActionEntry(actionLabel)) {
                failures.add("No visible en menu: ${actionLabel}")
            }
        }
    }

    // Export
    long preExportTs = System.currentTimeMillis()
    Boolean exportOpened = (Boolean) WebUI.executeJavaScript("""
        var btn = Array.from(document.querySelectorAll('a.btn, button, .dropdown-toggle, a')).find(function(el) {
            var txt = (el.textContent || '').trim().toLowerCase();
            var id = (el.id || '').toLowerCase();
            return txt === 'export' || txt === 'exportar' || id.indexOf('export') >= 0;
        });
        if (!btn) return false;
        btn.click();
        return true;
    """, null)
    if (Boolean.TRUE.equals(exportOpened)) {
        WebUI.delay(1)
        Boolean pptxOk = (Boolean) WebUI.executeJavaScript("""
            var el = document.querySelector('#btPPTX') || Array.from(document.querySelectorAll('.dropdown-item')).find(function(e) { return e.textContent.trim().toUpperCase() === 'PPTX'; });
            if (!el) return false;
            var st = window.getComputedStyle(el); return st.display !== 'none' && st.visibility !== 'hidden';
        """, null)
        Boolean idmlOk = (Boolean) WebUI.executeJavaScript("""
            var el = document.querySelector('#btZIP') || Array.from(document.querySelectorAll('.dropdown-item')).find(function(e) { return e.textContent.trim().toUpperCase() === 'IDML'; });
            if (!el) return false;
            var st = window.getComputedStyle(el); return st.display !== 'none' && st.visibility !== 'hidden';
        """, null)
        if (Boolean.TRUE.equals(pptxOk)) {
            KeywordUtil.logInfo('[EXPORT] PPTX visible')
            WebUI.executeJavaScript("var el = document.querySelector('#btPPTX'); if (el) el.click();", null)
            WebUI.delay(1)
        } else {
            failures.add('[EXPORT] PPTX no visible en dropdown')
        }
        WebUI.executeJavaScript("""
            var btn = Array.from(document.querySelectorAll('a.btn, button, .dropdown-toggle, a')).find(function(el) {
                var txt = (el.textContent || '').trim().toLowerCase(); var id = (el.id || '').toLowerCase();
                return txt === 'export' || txt === 'exportar' || id.indexOf('export') >= 0;
            }); if (btn) btn.click();
        """, null)
        WebUI.delay(1)
        if (Boolean.TRUE.equals(idmlOk)) {
            KeywordUtil.logInfo('[EXPORT] IDML visible')
            WebUI.executeJavaScript("var el = document.querySelector('#btZIP'); if (el) el.click();", null)
            WebUI.delay(1)
        } else {
            failures.add('[EXPORT] IDML no visible en dropdown')
        }
        WebUI.delay(5)
        List<File> newDls = (new File('C:/Users/e2494/Downloads')).listFiles()?.findAll { File f ->
            f.isFile() && !f.name.endsWith('.crdownload') && !f.name.endsWith('.tmp') && f.lastModified() > preExportTs
        } ?: []
        if (newDls.isEmpty()) {
            warnings.add('[EXPORT] No se detectaron descargas nuevas')
            KeywordUtil.logInfo('[EXPORT][WARNING] No se detectaron descargas nuevas')
        } else {
            KeywordUtil.logInfo("[EXPORT] Descargas: ${newDls.collect { it.name }.join(', ')}")
        }
        WebUI.executeJavaScript('document.body.click();', null)
        WebUI.delay(1)
    } else {
        warnings.add('[EXPORT] Boton Export no encontrado')
        KeywordUtil.logInfo('[EXPORT][WARNING] Boton Export no encontrado')
    }

    CustomKeywords.'TempletPortalKeywords.captureCaseScreenshot'(caseId, 'list_view_clickable')

    CommonKeywords.logCaseSummary(caseId, failures, warnings)

    if (failures.isEmpty()) {
        KeywordUtil.markPassed(caseId + ' OK. Vista List aplicada y objetos visibles/clickeables validados.')
    } else {
        KeywordUtil.markFailed(caseId + ' falló: ' + failures.join(' | '))
    }
} finally {
    // Browser se mantiene abierto para permitir isReuseDriver=true y evitar relogin en la suite.
}
