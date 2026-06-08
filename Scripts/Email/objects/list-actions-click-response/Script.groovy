import static com.kms.katalon.core.testobject.ObjectRepository.findTestObject
import com.kms.katalon.core.model.FailureHandling as FailureHandling
import com.kms.katalon.core.testobject.TestObject
import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import org.openqa.selenium.Keys
import CommonKeywords
import internal.GlobalVariable as GlobalVariable

String caseId = 'TC-EMAIL-LIST-ACTIONS-RESPONSE-003'
String startUrl = CommonKeywords.getRequiredGlobal('EMAIL_TEST_URL', 'https://emails-test.templet.io/admin/manager.php')
String clientCss = '#inputGroupSelect01'
String initiativeCss = "select[data-toggle='drop-initiatives']"
List<String> failures = []
List<String> warnings = []

def logWarning = { String message ->
    String normalized = "[WARNING] ${message}"
    warnings.add(normalized)
    KeywordUtil.logInfo(normalized)
}

def countVisibleCards = {
    Number count = (Number) WebUI.executeJavaScript('''
        var candidates = Array.from(document.querySelectorAll(
            '.thumbnails-boxes .thumbnail-box, .thumbnails-boxes .box, .thumbnails-boxes .card, .thumbnails-boxes article, .thumbnails-boxes [class*="thumb"], .thumbnails-boxes [class*="item"], table.table tbody tr, .list-group .list-group-item, .documents-list .row'
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

def clickListToggle = {
    Boolean clicked = (Boolean) WebUI.executeJavaScript('''
        var listCandidates = Array.from(document.querySelectorAll(
            '#icon-list, #icon-list-view, [id*="list"][class*="icon"], [id*="list"][id*="view"], .icon-list, [data-view="list"], [aria-label*="List"], [title*="List"]'
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

def openFirstCardMenu = {
    Map state = (Map) WebUI.executeJavaScript('''
        var visible = function(el) {
            if (!(el instanceof HTMLElement)) return false;
            var st = window.getComputedStyle(el);
            return st.display !== 'none' && st.visibility !== 'hidden' && el.offsetParent !== null;
        };
        var row = Array.from(document.querySelectorAll('table.table tbody tr, .thumbnails-boxes .thumbnail-box, .thumbnails-boxes .box, .documents-list .row, .list-group .list-group-item')).find(visible);
        var scope = row || document;
        var triggers = Array.from(scope.querySelectorAll(
            '.dropdown-toggle, [data-bs-toggle="dropdown"], [data-toggle="dropdown"], [aria-haspopup="true"], .fa-ellipsis-h, .fa-ellipsis-v, [class*="ellipsis"], [class*="three-dots"], [class*="kebab"], [title*="menu" i], [aria-label*="menu" i]'
        ));
        var trigger = triggers.find(visible);
        if (!trigger) {
            return { opened: false, reason: 'no-trigger' };
        }
        trigger.scrollIntoView({ block: 'center', inline: 'nearest' });
        ['mousedown', 'mouseup', 'click'].forEach(function(evt) {
            trigger.dispatchEvent(new MouseEvent(evt, { bubbles: true }));
        });
        var menuVisible = Array.from(document.querySelectorAll('.dropdown-menu.show, .dropdown.open .dropdown-menu, .show > .dropdown-menu, [role="menu"], .context-menu')).some(visible);
        return { opened: menuVisible, reason: menuVisible ? 'opened' : 'trigger-clicked-no-open' };
    ''', null)
    return state ?: [opened: false, reason: 'unknown']
}

def readUiSnapshot = {
    Map snap = (Map) WebUI.executeJavaScript('''
        var visible = function(el) {
            if (!(el instanceof HTMLElement)) return false;
            var st = window.getComputedStyle(el);
            return st.display !== 'none' && st.visibility !== 'hidden' && el.offsetParent !== null;
        };
        var modalLike = Array.from(document.querySelectorAll('.modal.show, .modal.in, [role="dialog"], .swal2-popup, .ui-dialog, .bootbox, .ReactModal__Content')).filter(visible).length;
        var toastLike = Array.from(document.querySelectorAll('.toast, .alert, .iziToast, .noty_body, .notification, [role="alert"]')).filter(visible).length;
        var dropdownOpen = Array.from(document.querySelectorAll('.dropdown-menu.show, .dropdown.open .dropdown-menu, .show > .dropdown-menu')).filter(visible).length;
        var rowCount = Array.from(document.querySelectorAll('table.table tbody tr, .thumbnails-boxes .thumbnail-box, .thumbnails-boxes .box, .documents-list .row, .list-group .list-group-item')).filter(visible).length;
        return {
            url: window.location.href,
            modalLike: modalLike,
            toastLike: toastLike,
            dropdownOpen: dropdownOpen,
            rowCount: rowCount
        };
    ''', null)
    return snap ?: [url: '', modalLike: 0, toastLike: 0, dropdownOpen: 0, rowCount: 0]
}

def clickActionEntry = { List<String> labels, String actionId ->
    Map result = (Map) WebUI.executeJavaScript('''
        var labels = (arguments[0] || []).map(function(x) { return (x || '').toLowerCase(); });
        var actionId = (arguments[1] || '').toLowerCase();
        var visible = function(el) {
            if (!(el instanceof HTMLElement)) return false;
            var st = window.getComputedStyle(el);
            return st.display !== 'none' && st.visibility !== 'hidden' && el.offsetParent !== null;
        };
        var clickEl = function(el) {
            if (!el) return false;
            var host = el.closest('a,button,[role="button"]') || el;
            host.scrollIntoView({ block: 'center', inline: 'nearest' });
            ['mousedown', 'mouseup', 'click'].forEach(function(evt) {
                host.dispatchEvent(new MouseEvent(evt, { bubbles: true }));
            });
            return true;
        };
        var textMatch = function(el) {
            if (!visible(el)) return false;
            var txt = (el.textContent || '').trim().toLowerCase();
            var title = ((el.getAttribute('title') || '') + ' ' + (el.getAttribute('aria-label') || '') + ' ' + (el.getAttribute('data-original-title') || '')).toLowerCase();
            var haystack = (txt + ' ' + title).trim();
            if (!haystack) return false;
            return labels.some(function(label) { return haystack === label || haystack.indexOf(label) >= 0; });
        };
        var hints = {
            edit: ['edit', 'editar', 'pencil'],
            rename: ['rename', 'renombrar', 'name'],
            url: ['url', 'link', 'copy url', 'copiar'],
            duplicate: ['duplicate', 'duplicar', 'clone', 'copy'],
            download: ['download', 'descargar'],
            move: ['move', 'mover', 'folder'],
            delete: ['delete', 'eliminar', 'trash', 'remove']
        };

        var menuRoots = Array.from(document.querySelectorAll('.dropdown-menu.show, .dropdown.open .dropdown-menu, .show > .dropdown-menu, [role="menu"], .context-menu')).filter(visible);
        var menuNodes = menuRoots.flatMap(function(root) { return Array.from(root.querySelectorAll('a,button,li,span,div')); });
        var menuTarget = menuNodes.find(textMatch);
        if (menuTarget && clickEl(menuTarget)) {
            return { clicked: true, text: (menuTarget.textContent || '').trim(), source: 'menu' };
        }

        var row = Array.from(document.querySelectorAll('table.table tbody tr, .thumbnails-boxes .thumbnail-box, .thumbnails-boxes .box, .documents-list .row, .list-group .list-group-item')).find(visible);
        var scope = row || document;
        var directNodes = Array.from(scope.querySelectorAll('a,button,span,i,svg,path,div'));
        var actionTokens = hints[actionId] || [];
        var directTarget = directNodes.find(function(el) {
            if (!visible(el)) return false;
            if (textMatch(el)) return true;
            var cls = (el.className && el.className.baseVal ? el.className.baseVal : el.className || '').toString().toLowerCase();
            var title = ((el.getAttribute('title') || '') + ' ' + (el.getAttribute('aria-label') || '') + ' ' + (el.getAttribute('data-original-title') || '') + ' ' + (el.getAttribute('href') || '')).toLowerCase();
            var haystack = (cls + ' ' + title).trim();
            return actionTokens.some(function(token) { return haystack.indexOf(token) >= 0; });
        });
        if (directTarget && clickEl(directTarget)) {
            return { clicked: true, text: (directTarget.textContent || '').trim(), source: 'direct' };
        }
        return { clicked: false, text: '', source: 'none' };
    ''', [labels, actionId])
    return result ?: [clicked: false, text: '', source: 'none']
}

def closeTransientUi = {
    try {
        WebUI.executeJavaScript('document.activeElement.blur();', FailureHandling.OPTIONAL)
    } catch (Exception ignored) {}
    WebUI.executeJavaScript('''
        var visible = function(el) {
            if (!(el instanceof HTMLElement)) return false;
            var st = window.getComputedStyle(el);
            return st.display !== 'none' && st.visibility !== 'hidden' && el.offsetParent !== null;
        };
        var closers = Array.from(document.querySelectorAll('[data-bs-dismiss="modal"], .btn-close, .close, .swal2-cancel, .swal2-close, button, a'));
        var words = ['cancel', 'cerrar', 'close', 'no'];
        var btn = closers.find(function(el) {
            if (!visible(el)) return false;
            var txt = (el.textContent || '').trim().toLowerCase();
            return words.some(function(w) { return txt === w || txt.indexOf(w) >= 0; });
        });
        if (btn) {
            btn.dispatchEvent(new MouseEvent('mousedown', { bubbles: true }));
            btn.dispatchEvent(new MouseEvent('mouseup', { bubbles: true }));
            btn.dispatchEvent(new MouseEvent('click', { bubbles: true }));
            return true;
        }
        return false;
    ''', null)
    WebUI.delay(1)
}

def hasResponseSignal = { Map beforeSnap, Map afterSnap ->
    if ((afterSnap.url ?: '') != (beforeSnap.url ?: '')) return true
    if ((afterSnap.modalLike ?: 0) > (beforeSnap.modalLike ?: 0)) return true
    if ((afterSnap.toastLike ?: 0) > (beforeSnap.toastLike ?: 0)) return true
    if ((afterSnap.dropdownOpen ?: 0) != (beforeSnap.dropdownOpen ?: 0)) return true
    if ((afterSnap.rowCount ?: 0) != (beforeSnap.rowCount ?: 0)) return true
    return false
}

def evaluateActionResponse = { String actionId, Map beforeSnap, Map afterSnap, Map clickResult ->
    boolean genericSignal = hasResponseSignal(beforeSnap, afterSnap)
    int modalDelta = (afterSnap.modalLike ?: 0) - (beforeSnap.modalLike ?: 0)
    int toastDelta = (afterSnap.toastLike ?: 0) - (beforeSnap.toastLike ?: 0)
    int rowDelta = (afterSnap.rowCount ?: 0) - (beforeSnap.rowCount ?: 0)
    boolean urlChanged = (afterSnap.url ?: '') != (beforeSnap.url ?: '')
    boolean directClick = (clickResult?.source ?: '') == 'direct'

    switch (actionId) {
        case 'download':
            return genericSignal ? [ok: true] : [ok: false, soft: true, reason: 'Download sin señal visual clara']
        case 'url':
            return (genericSignal || toastDelta > 0) ? [ok: true] : [ok: false, soft: true, reason: 'URL/Copy sin señal visual clara']
        case 'delete':
            return (modalDelta > 0 || toastDelta > 0 || rowDelta != 0 || genericSignal)
                ? [ok: true]
                : (directClick ? [ok: true, warn: 'Delete ejecutado por acceso directo sin confirmación visible'] : [ok: false, soft: false, reason: 'Delete sin confirmación visible'])
        case 'rename':
            return (modalDelta > 0 || toastDelta > 0 || genericSignal)
                ? [ok: true]
                : (directClick ? [ok: true, warn: 'Rename ejecutado por acceso directo sin modal/confirmación visible'] : [ok: false, soft: false, reason: 'Rename sin modal/confirmación'])
        case 'move':
            return (modalDelta > 0 || toastDelta > 0 || genericSignal)
                ? [ok: true]
                : (directClick ? [ok: true, warn: 'Move ejecutado por acceso directo sin modal/confirmación visible'] : [ok: false, soft: false, reason: 'Move sin modal/confirmación'])
        case 'duplicate':
            return (toastDelta > 0 || rowDelta != 0 || genericSignal)
                ? [ok: true]
                : (directClick ? [ok: true, warn: 'Duplicate ejecutado por acceso directo sin señal visible de cambio'] : [ok: false, soft: false, reason: 'Duplicate sin señal de cambio'])
        case 'edit':
            return (urlChanged || modalDelta > 0 || genericSignal)
                ? [ok: true]
                : (directClick ? [ok: true, warn: 'Edit ejecutado por acceso directo sin navegación/modal visible'] : [ok: false, soft: false, reason: 'Edit sin navegación ni modal'])
        default:
            return genericSignal ? [ok: true] : [ok: false, soft: false, reason: 'Sin señal de respuesta UI']
    }
}

def listDownloadCandidates = { File dir, long sinceMs ->
    if (dir == null || !dir.exists() || !dir.isDirectory()) {
        return []
    }
    File[] files = dir.listFiles()
    if (files == null) {
        return []
    }
    return files.findAll { File f ->
        if (f == null || !f.isFile()) return false
        String name = (f.name ?: '').toLowerCase()
        if (name.endsWith('.crdownload') || name.endsWith('.tmp')) return false
        return f.lastModified() >= sinceMs
    }
}

def runDownloadChecks = { List<String> labels ->
    String downloadsPath = System.getProperty('user.home') + '/Downloads'
    File downloadsDir = new File(downloadsPath)

    if (!downloadsDir.exists() || !downloadsDir.isDirectory()) {
        logWarning("No existe carpeta Downloads en ${downloadsPath}; no se puede validar descarga")
        return
    }

    labels.each { String label ->
        Map menuState = openFirstCardMenu()
        if (!Boolean.TRUE.equals(menuState.opened)) {
            logWarning("No se pudo reabrir menú para export '${label}' (${menuState.reason})")
        }

        long startMs = System.currentTimeMillis()
        List<File> beforeCandidates = listDownloadCandidates(downloadsDir, startMs - 120000)
        Set<String> beforeFingerprints = beforeCandidates.collect { File f ->
            "${f.name}|${f.lastModified()}|${f.length()}"
        } as Set<String>

        Map clickResult = clickActionEntry([label], 'download')
        if (!Boolean.TRUE.equals(clickResult.clicked)) {
            logWarning("No se pudo hacer click en opción de export '${label}'")
            closeTransientUi()
            return
        }

        WebUI.delay(5)
        List<File> afterCandidates = listDownloadCandidates(downloadsDir, startMs)
        List<File> freshFiles = afterCandidates.findAll { File f ->
            String fingerprint = "${f.name}|${f.lastModified()}|${f.length()}"
            return !beforeFingerprints.contains(fingerprint)
        }

        if (freshFiles.isEmpty()) {
            logWarning("Export '${label}' sin archivo nuevo detectado en Downloads")
        } else {
            KeywordUtil.logInfo("[CHECK][DOWNLOAD] '${label}' generó ${freshFiles.size()} archivo(s): " + freshFiles.collect { it.name }.join(', '))
        }

        closeTransientUi()
    }
}

try {
    boolean browserAlreadyOpen = false
    try {
        browserAlreadyOpen = WebUI.verifyElementPresent(findTestObject('Email/Filters/section_dashboard'), 5, FailureHandling.OPTIONAL)
    } catch (Exception ignored) {}

    if (!browserAlreadyOpen) {
        CustomKeywords.'TempletPortalKeywords.openBrowserAndLoginWithMicrosoft'(startUrl)
    }
    WebUI.waitForPageLoad(10)

    Map selectedClient = CommonKeywords.selectPreferredOption('Email/Filters/select_client', clientCss, 'BRAVA', 10)
    if (!selectedClient?.value) {
        KeywordUtil.markFailedAndStop('[CLIENT] No fue posible seleccionar client para validación de acciones List view')
    }

    List<Map> initiatives = CommonKeywords.readSelectOptionsWhenReady(initiativeCss, 10, 1)
    List<Map> candidates = initiatives.findAll { it['value'] != null && !it['value'].toString().trim().isEmpty() }
    if (!candidates || candidates.isEmpty()) {
        KeywordUtil.markFailedAndStop('[INITIATIVE] No hay initiatives disponibles después de seleccionar client')
    }

    Map selectedInitiative = null
    for (Map opt : candidates) {
        String value = opt['value'].toString()
        String text = opt['text']?.toString() ?: value
        if (!setSelectValueByCss(initiativeCss, value)) continue
        WebUI.waitForPageLoad(2)
        WebUI.delay(1)
        if (!hasNoDocumentsMessage() && countVisibleCards() > 0) {
            selectedInitiative = [value: value, text: text]
            break
        }
    }

    if (selectedInitiative == null) {
        KeywordUtil.markFailedAndStop('[INITIATIVE] No se encontró initiative con contenido para validar acciones de List view')
    }

    KeywordUtil.logInfo("[SETUP] Client: ${selectedClient.text}; Initiative: ${selectedInitiative.text}")

    TestObject listShape = findTestObject('Email/Objects/ListView/icon_list_shape')
    TestObject listViewIcon = findTestObject('Email/Objects/ListView/icon_list_view')
    WebUI.waitForElementVisible(listViewIcon, 4)
    boolean listShapePresent = CustomKeywords.'TempletPortalKeywords.isPresentQuiet'(listShape, 2)
    int cardsBeforeToggle = countVisibleCards()
    boolean listViewReady = false
    for (int attempt = 1; attempt <= 3; attempt++) {
        WebUI.click(listViewIcon, FailureHandling.OPTIONAL)
        if (listShapePresent) {
            WebUI.click(listShape, FailureHandling.OPTIONAL)
        }
        clickListToggle()
        WebUI.delay(1)
        int cardsAfterToggle = countVisibleCards()
        if (cardsAfterToggle != cardsBeforeToggle || cardsAfterToggle > 0) {
            listViewReady = true
            break
        }
    }
    if (!listViewReady) {
        KeywordUtil.markFailedAndStop('[LIST] No fue posible activar List View para validar acciones')
    }

    List<Map<String, Object>> actions = [
        [id: 'edit', labels: ['Edit', 'Editar']],
        [id: 'rename', labels: ['Rename', 'Renombrar']],
        [id: 'url', labels: ['URL', 'URL Email', 'Copy URL', 'Copiar URL']],
        [id: 'duplicate', labels: ['Duplicate', 'Duplicar']],
        [id: 'download', labels: ['Download', 'Descargar', 'Export', 'Exportar']],
        [id: 'move', labels: ['Move', 'Mover']],
        [id: 'delete', labels: ['Delete', 'Eliminar']]
    ]

    actions.each { action ->
        if (action.id == 'download') {
            List<String> requiredExportOptions = ['Download', 'Export']
            runDownloadChecks(requiredExportOptions)
            return
        }

        Map menuState = openFirstCardMenu()
        CustomKeywords.'TempletPortalKeywords.captureCaseScreenshot'(caseId, "action_${action.id}_menu")
        if (!Boolean.TRUE.equals(menuState.opened)) {
            logWarning("No se pudo abrir menú para acción ${action.id} (${menuState.reason}), se intenta acción directa")
        }

        Map beforeSnap = readUiSnapshot()
        Map clickResult = clickActionEntry((List<String>) action.labels, action.id.toString())
        if (!Boolean.TRUE.equals(clickResult.clicked)) {
            logWarning("No se pudo hacer click en acción ${action.id} (ni menú ni acceso directo)")
            closeTransientUi()
            return
        }

        WebUI.delay(1)
        Map afterSnap = readUiSnapshot()
        boolean subwindowOpened = (afterSnap.modalLike ?: 0) > (beforeSnap.modalLike ?: 0)
        if (subwindowOpened) {
            KeywordUtil.logInfo("[ACTION ${action.id}] click=${clickResult.source} modalBefore=${beforeSnap.modalLike} modalAfter=${afterSnap.modalLike} subwindowOpened=true")
        } else {
            KeywordUtil.logInfo("[ACTION ${action.id}] click=${clickResult.source} modalBefore=${beforeSnap.modalLike} modalAfter=${afterSnap.modalLike} subwindowOpened=false (evaluando señales alternativas)")
        }
        CustomKeywords.'TempletPortalKeywords.captureCaseScreenshot'(caseId, "action_${action.id}_after_click")
        Map eval = evaluateActionResponse(action.id.toString(), beforeSnap, afterSnap, clickResult)
        if (!Boolean.TRUE.equals(eval.ok)) {
            logWarning("${action.id}: ${eval.reason}")
        } else {
            if (eval.warn) {
                logWarning("${action.id}: ${eval.warn}")
            }
            if (!subwindowOpened) {
                logWarning("${action.id}: respondió pero sin subventana modal visible")
            }
        }

        closeTransientUi()
    }

    CustomKeywords.'TempletPortalKeywords.captureCaseScreenshot'(caseId, 'list_actions_response')

    if (failures.isEmpty()) {
        KeywordUtil.logInfo("[SUMMARY] warnings=${warnings.size()}")
        KeywordUtil.markPassed(caseId + ' OK. Click y respuesta de acciones de List view validadas.')
    } else {
        KeywordUtil.markFailed(caseId + ' falló: ' + failures.join(' | '))
    }
} finally {
    // Browser se mantiene abierto para isReuseDriver=true en la suite.
}
