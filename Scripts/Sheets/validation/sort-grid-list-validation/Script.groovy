import static com.kms.katalon.core.testobject.ObjectRepository.findTestObject
import com.kms.katalon.core.model.FailureHandling as FailureHandling
import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import CommonKeywords

String caseId = 'TC-SHEETS-SORT-VALIDATION-001'
String startUrl = CommonKeywords.getRequiredGlobal('SHEETS_TEST_URL', 'https://sheets-test.templet.io/admin/manager.php')
String clientCss = '#inputGroupSelect01'
String initiativeCss = "select[data-toggle='drop-initiatives']"
String sortCss = '#sortField-alpha'
List<String> failures = []

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
        // Lee el orden visual (por coordenadas) para no depender del orden DOM.
        var cards = Array.from(document.querySelectorAll('.document-item')).filter(function(card) {
            if (!(card instanceof HTMLElement)) return false;
            var st = window.getComputedStyle(card);
            return st.display !== 'none' && st.visibility !== 'hidden' && card.offsetParent !== null;
        }).map(function(card) {
            var titleEl = card.querySelector('h6.title-card[data-title], h6.title-card, [data-title]');
            if (!titleEl) return null;
            var text = (titleEl.getAttribute('data-title') || titleEl.textContent || '').trim();
            if (!text) return null;
            var rect = card.getBoundingClientRect();
            return { text: text, top: rect.top, left: rect.left };
        }).filter(function(item) { return item !== null; });

        cards.sort(function(a, b) {
            if (Math.abs(a.top - b.top) > 2) return a.top - b.top;
            return a.left - b.left;
        });

        return cards
            .map(function(item) { return item.text; })
            .filter(function(text) { return text.length > 0; });
    """, null)
    return titles ?: []
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

def validateSort = { String viewLabel ->
    KeywordUtil.logInfo("[${caseId}] Validando Sort en ${viewLabel}")
    
    if (!isSortEnabled()) {
        failures.add("[${viewLabel}] Sort select NO está habilitado")
        return
    }
    
    List sortOptions = getSortOptions()
    KeywordUtil.logInfo("[${viewLabel}] Sort opciones disponibles: ${sortOptions.collect { it.text }.join(', ')}")
    
    // Aplicar cada opción de Sort y verificar que se leen títulos
    ['Newest', 'Oldest', 'A to Z', 'Z to A'].each { String sortLabel ->
        Map option = sortOptions.find { it.text?.toString()?.trim()?.equalsIgnoreCase(sortLabel) }
        if (!option?.value) {
            failures.add("[${viewLabel}] Opción Sort '${sortLabel}' NO encontrada en dropdown")
            return
        }
        
        boolean applied = setSelectValueByCss(sortCss, option.value.toString())
        if (!applied) {
            failures.add("[${viewLabel}] Sort '${sortLabel}' NO se pudo aplicar (setSelectValueByCss falló)")
            return
        }
        
        WebUI.delay(2)
        List<String> titles = getVisibleDocumentTitles()
        
        if (titles.isEmpty()) {
            failures.add("[${viewLabel}] Sort '${sortLabel}' aplicado pero SIN TÍTULOS VISIBLES (DOM roto o selector incorrecto)")
        } else {
            KeywordUtil.logInfo("[${viewLabel}] Sort '${sortLabel}' → ${titles.size()} títulos: ${titles.take(3).join(', ')}...")
            CustomKeywords.'TempletPortalKeywords.captureCaseScreenshot'(caseId, "sort_${viewLabel.replace(' ','_')}_${sortLabel.replace(' ','_')}")
        }
    }
    
    // Validar que Newest y Oldest invierten orden
    setSelectValueByCss(sortCss, sortOptions.find { it.text?.toString()?.trim()?.equalsIgnoreCase('Newest') }?.value?.toString())
    WebUI.delay(2)
    List<String> newestTitles = getVisibleDocumentTitles()
    
    setSelectValueByCss(sortCss, sortOptions.find { it.text?.toString()?.trim()?.equalsIgnoreCase('Oldest') }?.value?.toString())
    WebUI.delay(2)
    List<String> oldestTitles = getVisibleDocumentTitles()
    
    // DIAGNÓSTICO: exportar títulos leídos para validar si es bug de app o de lectura JS
    if (!newestTitles.isEmpty() && !oldestTitles.isEmpty()) {
        KeywordUtil.logInfo("[DIAGNÓSTICO] ${viewLabel} Newest primeros 3: ${newestTitles.take(3).join(' | ')}")
        KeywordUtil.logInfo("[DIAGNÓSTICO] ${viewLabel} Newest últimos 3: ${newestTitles.drop(Math.max(0, newestTitles.size()-3)).join(' | ')}")
        KeywordUtil.logInfo("[DIAGNÓSTICO] ${viewLabel} Oldest primeros 3: ${oldestTitles.take(3).join(' | ')}")
        KeywordUtil.logInfo("[DIAGNÓSTICO] ${viewLabel} Oldest últimos 3: ${oldestTitles.drop(Math.max(0, oldestTitles.size()-3)).join(' | ')}")
        KeywordUtil.logInfo("[DIAGNÓSTICO] ${viewLabel} ¿Listas idénticas? ${newestTitles == oldestTitles}")
    }
    
    if (newestTitles.isEmpty() || oldestTitles.isEmpty()) {
        failures.add("[${viewLabel}] No se pudo comparar Newest vs Oldest (uno o ambos sin títulos)")
    } else if (newestTitles == oldestTitles) {
        failures.add("[${viewLabel}] DIAGNÓSTICO: Newest/Oldest devolvieron el MISMO ORDEN (${newestTitles.size()} elementos). Revisar si el sort funciona en la app o si el JS está mal.")
    } else if (newestTitles.first() != oldestTitles.last() || newestTitles.last() != oldestTitles.first()) {
        failures.add("[${viewLabel}] Newest/Oldest NO invierten orden. Newest 1er='${newestTitles.first()}' último='${newestTitles.last()}' | Oldest 1er='${oldestTitles.first()}' último='${oldestTitles.last()}'")
    } else {
        KeywordUtil.logInfo("[${viewLabel}] ✅ Newest/Oldest invierten orden correctamente")
    }
}

try {
    // Abrir browser y login
    boolean browserAlreadyOpen = false
    try {
        browserAlreadyOpen = WebUI.verifyElementPresent(findTestObject('Sheets/Filters/section_dashboard'), 5, FailureHandling.OPTIONAL)
    } catch (Exception ignored) {}
    
    if (!browserAlreadyOpen) {
        CustomKeywords.'TempletPortalKeywords.openBrowserAndLoginWithMicrosoft'(startUrl)
    }
    WebUI.waitForPageLoad(10)
    
    // Seleccionar Client
    Map selectedClient = CommonKeywords.selectPreferredOption('Sheets/Filters/select_client', clientCss, 'BRAVA', 12)
    if (!selectedClient?.value) {
        failures.add('[SETUP] No fue posible seleccionar client BRAVA')
        KeywordUtil.markFailed(failures.join(' | '))
    }
    
    // Buscar initiative con contenido y Sort habilitado
    List<Map> initiatives = CommonKeywords.readSelectOptionsWhenReady(initiativeCss, 12, 1)
    List<Map> candidates = initiatives.findAll { it['value'] != null && !it['value'].toString().trim().isEmpty() }
    
    Map selectedInitiative = null
    for (Map candidate : candidates) {
        String iValue = candidate['value'].toString()
        String iText  = candidate['text']?.toString() ?: iValue
        if (!setSelectValueByCss(initiativeCss, iValue)) continue
        WebUI.waitForPageLoad(5)
        WebUI.delay(2)
        if (hasNoDocuments()) continue
        if (isSortEnabled()) {
            selectedInitiative = [value: iValue, text: iText]
            KeywordUtil.logInfo("[SETUP] Initiative con Sort habilitado: ${iText}")
            break
        }
    }
    
    if (selectedInitiative == null) {
        failures.add('[SETUP] Ninguna initiative tiene contenido con Sort habilitado')
        KeywordUtil.markFailed(failures.join(' | '))
    }
    
    // VALIDAR SORT EN VIEW GRID
    validateSort('View Grid')
    
    // Cambiar a View List
    def viewListBtn = findTestObject('Tc1/Page_sheets.templet.  Admin/a_View List')
    WebUI.waitForElementVisible(viewListBtn, 10)
    WebUI.waitForElementClickable(viewListBtn, 10)
    WebUI.click(viewListBtn)
    WebUI.waitForPageLoad(5)
    WebUI.delay(2)
    KeywordUtil.logInfo("[${caseId}] Cambiado a View List")
    
    // VALIDAR SORT EN VIEW LIST
    validateSort('View List')
    
    // Screenshot final
    CustomKeywords.'TempletPortalKeywords.captureCaseScreenshot'(caseId, 'final')
    
    // Reporte final
    if (failures.isEmpty()) {
        KeywordUtil.markPassed("${caseId} OK. Sort funciona correctamente en Grid y List.")
    } else {
        KeywordUtil.markFailed("${caseId} FALLÓ. Failures: ${failures.join(' | ')}")
    }
    
} finally {
    try {
        WebUI.closeBrowser()
    } catch (Exception ignored) {}
}
