import static com.kms.katalon.core.testobject.ObjectRepository.findTestObject
import com.kms.katalon.core.model.FailureHandling as FailureHandling
import com.kms.katalon.core.testobject.TestObject
import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import CommonKeywords
import internal.GlobalVariable as GlobalVariable

// Use required global with fallback
String caseId = 'TC-SHEETS-LIST-VIEW-CLICKABLE-002'
String startUrl = CustomKeywords.'CommonKeywords.getRequiredGlobal'('SHEETS_TEST_URL', 'https://sheets-test.templet.io/admin/manager.php')
String clientCss = '#inputGroupSelect01'
String initiativeCss = "select[data-toggle='drop-initiatives']"
List<String> failures = []
List<String> warnings = []

// helper: polling wait for cards
def waitForCards = { int timeoutSec = 12 ->
    long end = System.currentTimeMillis() + (timeoutSec * 1000)
    while (System.currentTimeMillis() < end) {
        int c = (Integer) countVisibleCards()
        if (c > 0 && !hasNoDocumentsMessage()) return c
        Thread.sleep(500)
    }
    return 0
}

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

// improved JS click that tries to synthesize pointer events
def clickListToggle = {
    Boolean clicked = (Boolean) WebUI.executeJavaScript('''
        var listCandidates = Array.from(document.querySelectorAll(
            '#icon-list-view, [id*="list"] [class*="icon"], [id*="list"][id*="view"], .icon-list, [data-view="list"], [aria-label*="List"], [title*="List"]'
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
        try {
            el.focus();
            var rect = el.getBoundingClientRect();
            var ev = new MouseEvent('click', { bubbles: true, cancelable: true, view: window, clientX: rect.left + 1, clientY: rect.top + 1 });
            el.dispatchEvent(ev);
            return true;
        } catch(e) {
            return false;
        }
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
        try {
            trigger.focus();
            var rect = trigger.getBoundingClientRect();
            trigger.dispatchEvent(new MouseEvent('click', { bubbles: true, clientX: rect.left+1, clientY: rect.top+1 }));
            return true;
        } catch(e) { return false; }
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

List<Map<String, Object>> listChecks = [
    [path: 'Sheets/Objects/Filters/select_client_initiative', label: 'Filtro client/initiative', clickable: true],
    [path: 'Sheets/Objects/Filters/select_sort_field', label: 'Filtro sort', clickable: false]
]

// Main flow with better waits and error handling
try {
    KeywordUtil.logInfo("[UPDATED-SCRIPT-RUN] [CASE START] ${caseId} - ${new Date().toString()}")

    boolean browserAlreadyOpen = false
    try { browserAlreadyOpen = WebUI.verifyElementPresent(findTestObject('Sheets/Filters/section_dashboard'), 5, FailureHandling.OPTIONAL) } catch (e) { /* ignore */ }

    if (!browserAlreadyOpen) {
        CustomKeywords.'TempletPortalKeywords.openBrowserAndLoginWithMicrosoft'(startUrl)
    }
    WebUI.waitForPageLoad(15)

    // select client - critical
    Map selectedClient = CommonKeywords.selectPreferredOption('Sheets/Filters/select_client', clientCss, 'BRAVA', 12)
    if (!selectedClient?.value) {
        CustomKeywords.'TempletPortalKeywords.captureCaseScreenshot'(caseId, 'client_select_failed')
        KeywordUtil.markFailedAndStop('[CLIENT] No fue posible seleccionar client para la validación de List view')
    }

    List<Map> initiatives = CommonKeywords.readSelectOptionsWhenReady(initiativeCss, 12, 1)
    List<Map> candidates = initiatives.findAll { it['value'] != null && !it['value'].toString().trim().isEmpty() }
    if (!candidates || candidates.isEmpty()) {
        CustomKeywords.'TempletPortalKeywords.captureCaseScreenshot'(caseId, 'no_initiatives')
        KeywordUtil.markFailedAndStop('[INITIATIVE] No hay initiatives disponibles después de seleccionar client')
    }

    Map selectedInitiative = null
    for (Map opt : candidates) {
        String value = opt['value'].toString()
        String text = opt['text']?.toString() ?: value
        if (!setSelectValueByCss(initiativeCss, value)) {
            continue
        }
        WebUI.waitForPageLoad(5)
        int cards = waitForCards(12)
        if (!hasNoDocumentsMessage() && cards > 0) {
            selectedInitiative = [value: value, text: text]
            break
        }
    }

    if (selectedInitiative == null) {
        CustomKeywords.'TempletPortalKeywords.captureCaseScreenshot'(caseId, 'no_initiative_with_content')
        KeywordUtil.markFailedAndStop('[INITIATIVE] No se encontró initiative con contenido para validar List view')
    }

    KeywordUtil.logInfo("[SETUP] Client seleccionado: ${selectedClient.text}; Initiative con contenido: ${selectedInitiative.text}")

    TestObject listShape = findTestObject('Sheets/Objects/ListView/icon_list_shape')
    TestObject listViewIcon = findTestObject('Sheets/Objects/ListView/icon_list_view')

    // Iterate Sort select options and validate after toggling to list view
    List<Map> sortOptions = CommonKeywords.readSelectOptionsWhenReady('#sortField-alpha', 10, 1)
    if (sortOptions && !sortOptions.isEmpty()) {
        KeywordUtil.logInfo("[SORT] Opciones encontradas: ${sortOptions.collect{ it.text }.join(', ')}")
        sortOptions.each { Map opt ->
            String val = (opt?.get('value') ?: '').toString()
            String txt = (opt?.get('text') ?: val).toString()

            try {
                // Select option by value using repository test object for sort
                WebUI.selectOptionByValue(findTestObject('Sheets/Objects/Filters/select_sort_field'), val, false)
                WebUI.waitForPageLoad(5)
                KeywordUtil.logInfo("[SORT] Seleccionado: ${txt} (${val})")

                // Toggle to list view (try native click, fallback to JS)
                if (WebUI.waitForElementClickable(listViewIcon, 5, FailureHandling.OPTIONAL)) {
                    boolean clickedListIcon = CustomKeywords.'RobustClicks.robustClick'(listViewIcon, 12, caseId)
                    if (!clickedListIcon) { failures.add('[CLICK] listViewIcon no respondió al click (12s)') }
                } else {
                    clickListToggle()
                }

                if (WebUI.waitForElementClickable(listShape, 4, FailureHandling.OPTIONAL)) {
                    boolean clickedShape = CustomKeywords.'RobustClicks.robustClick'(listShape, 12, caseId)
                    if (!clickedShape) { failures.add('[CLICK] listShape no respondió al click (12s)') }
                } else {
                    clickListToggle()
                }

                // wait small moment for UI to update
                Thread.sleep(700)

                // Validate sort persisted after list toggle
                String currentVal = WebUI.getAttribute(findTestObject('Sheets/Objects/Filters/select_sort_field'), 'value')
                if (currentVal != val) {
                    failures.add("[SORT] Cambio inesperado tras toggle List: esperado ${txt} (${val}), obtenido ${currentVal}")
                } else {
                    KeywordUtil.logInfo("[SORT] Persistente tras List: ${txt} (${val})")
                }
            } catch (Exception e) {
                failures.add("[SORT] Error iterando opción ${txt} (${val}): ${e.message}")
            }
        }
    } else {
        KeywordUtil.logInfo('[SORT] No se encontraron opciones de sort para iterar')
    }

    // Ensure clickable before clicking primary toggle
    if (!WebUI.waitForElementClickable(listViewIcon, 10, FailureHandling.OPTIONAL)) {
        failures.add('listViewIcon no es clickeable inmediatamente; se usará fallback JS')
    } else {
        boolean ok = CustomKeywords.'RobustClicks.robustClick'(listViewIcon, 12, caseId)
        if (!ok) failures.add('[CLICK] listViewIcon no respondió al click (12s)')
    }

    // attempt secondary click to guarantee state change
    try {
        if (WebUI.waitForElementClickable(listShape, 6, FailureHandling.OPTIONAL)) {
            boolean ok2 = CustomKeywords.'RobustClicks.robustClick'(listShape, 12, caseId)
            if (!ok2) failures.add('[CLICK] listShape no respondió al click (12s)')
        } else {
            clickListToggle()
        }
    } catch (e) {
        clickListToggle()
    }

    // wait for list view to reflect
    int cardsBeforeToggle = countVisibleCards()
    int cardsAfterToggle = waitForCards(10)
    if (cardsAfterToggle == 0 && cardsBeforeToggle == 0) {
        CustomKeywords.'TempletPortalKeywords.captureCaseScreenshot'(caseId, 'list_toggle_failed')
        KeywordUtil.markFailedAndStop('[LIST] No fue posible activar List View o no hay tarjetas visibles')
    }

    // validate elements
    listChecks.each { item ->
        TestObject obj = findTestObject(item.path as String)
        WebUI.scrollToElement(obj, 6)

        boolean visible = WebUI.waitForElementVisible(obj, 8, FailureHandling.OPTIONAL)
        if (!visible) {
            failures.add("No visible tras click List: ${item.label} (Continue...]