import static com.kms.katalon.core.testobject.ObjectRepository.findTestObject
import com.kms.katalon.core.model.FailureHandling as FailureHandling
import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import CommonKeywords

String caseId = 'TC-EMAIL-LIST-ACTIONS-MODAL-001'
String startUrl = CommonKeywords.getRequiredGlobal('EMAIL_TEST_URL', 'https://emails-test.templet.io/admin/manager.php')
String clientCss = '#inputGroupSelect01'
String initiativeCss = "select[data-toggle='drop-initiatives']"
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

def countVisibleCards = {
    Integer count = (Integer) WebUI.executeJavaScript("""
        return Array.from(document.querySelectorAll('.document-item')).filter(function(el) {
            var st = window.getComputedStyle(el);
            return st.display !== 'none' && st.visibility !== 'hidden';
        }).length;
    """, null)
    return count ?: 0
}

def hasNoDocumentsMessage = {
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

def clickListToggle = {
    Boolean clicked = (Boolean) WebUI.executeJavaScript("""
        var btn = document.querySelector('a[href="#tabs-2"], .btn-view_list, button[data-view="list"]');
        if (btn) {
            btn.click();
            return true;
        }
        return false;
    """, null)
    return Boolean.TRUE.equals(clicked)
}

def openFirstCardMenu = {
    Boolean opened = (Boolean) WebUI.executeJavaScript("""
        var cards = Array.from(document.querySelectorAll('.document-item')).filter(function(el) {
            if (!(el instanceof HTMLElement)) return false;
            var st = window.getComputedStyle(el);
            return st.display !== 'none' && st.visibility !== 'hidden' && el.offsetParent !== null;
        });
        if (cards.length === 0) return false;

        var card = cards[0];

        // Intentar múltiples selectores de botón de menú (más robustos)
        var selectors = [
            '.dropdown-toggle',
            'button[data-toggle="dropdown"]',
            '.btn-menu',
            'button.dropdown-toggle',
            '[role="button"][aria-haspopup="true"]',
            'a.dropdown-toggle',
            '.card-actions button',
            '.item-actions button'
        ];

        for (var i = 0; i < selectors.length; i++) {
            var menuBtn = card.querySelector(selectors[i]);
            if (menuBtn && window.getComputedStyle(menuBtn).display !== 'none') {
                menuBtn.click();
                return true;
            }
        }

        // Fallback: buscar cualquier botón clickeable en la tarjeta
        var allButtons = card.querySelectorAll('button, a, [role="button"]');
        for (var j = 0; j < allButtons.length; j++) {
            var btn = allButtons[j];
            if (window.getComputedStyle(btn).display !== 'none' && btn.offsetParent !== null) {
                btn.click();
                return true;
            }
        }

        return false;
    """, null)
    return Boolean.TRUE.equals(opened)
}

def countModals = {
    Integer count = (Integer) WebUI.executeJavaScript("""
        return Array.from(document.querySelectorAll('.modal')).filter(function(m) {
            var st = window.getComputedStyle(m);
            return st.display !== 'none' && (m.classList.contains('show') || m.style.display === 'block');
        }).length;
    """, null)
    return count ?: 0
}

def countSubwindows = {
    Integer handles = (Integer) WebUI.executeJavaScript('return window.open ? 1 : 0;', null)
    try {
        def driver = com.kms.katalon.core.webui.driver.DriverFactory.getWebDriver()
        handles = driver.getWindowHandles().size()
    } catch (Exception ignored) {}
    return handles ?: 1
}

def closeTransientUi = {
    WebUI.executeJavaScript("""
        var modals = Array.from(document.querySelectorAll('.modal.show, .modal[style*="display: block"]'));
        modals.forEach(function(m) {
            var closeBtn = m.querySelector('.close, [data-dismiss="modal"], .btn-close');
            if (closeBtn) closeBtn.click();
        });
        document.body.click();
    """, null)
    WebUI.delay(1)
}

def getCurrentUrlSafe = {
    try {
        def url = WebUI.getUrl()
        return url ?: ''
    } catch (Exception ignored) {
        return ''
    }
}

def returnToListingView = {
    try {
        WebUI.back(FailureHandling.OPTIONAL)
        WebUI.delay(2)
    } catch (Exception ignored) {}

    WebUI.executeJavaScript("""
        var listBtn = document.querySelector('a[href="#tabs-2"], .btn-view_list, button[data-view="list"]');
        if (listBtn) listBtn.click();
    """, null)
    WebUI.delay(1)
}

def clickActionEntry = { List<String> labels, String actionId ->
    Boolean clicked = false
    for (String label : labels) {
        clicked = (Boolean) WebUI.executeJavaScript("""
            var items = Array.from(document.querySelectorAll('.dropdown-item, .menu-item, a, button'));
            var labelLower = String(arguments[0]).toLowerCase();
            var target = items.find(function(el) {
                var text = el.textContent.trim().toLowerCase();
                return text === labelLower;
            });
            if (target && window.getComputedStyle(target).display !== 'none') {
                target.click();
                return true;
            }
            return false;
        """, [label])
        if (Boolean.TRUE.equals(clicked)) break
    }
    return clicked
}

try {
    // Abrir browser y login
    boolean browserAlreadyOpen = false
    try {
        browserAlreadyOpen = WebUI.verifyElementPresent(findTestObject('Email/Filters/section_dashboard'), 5, FailureHandling.OPTIONAL)
    } catch (Exception ignored) {}

    if (!browserAlreadyOpen) {
        CustomKeywords.'TempletPortalKeywords.openBrowserAndLoginWithMicrosoft'(startUrl)
    }
    WebUI.waitForPageLoad(10)

    // Seleccionar Client
    Map selectedClient = CommonKeywords.selectPreferredOption('Email/Filters/select_client', clientCss, 'BRAVA', 12)
    if (!selectedClient?.value) {
        failures.add('[SETUP] No fue posible seleccionar client BRAVA')
        KeywordUtil.markFailed(failures.join(' | '))
    }

    // Buscar initiative con contenido
    List<Map> initiatives = CommonKeywords.readSelectOptionsWhenReady(initiativeCss, 12, 1)
    List<Map> candidates = initiatives.findAll { it['value'] != null && !it['value'].toString().trim().isEmpty() }

    Map selectedInitiative = null
    for (Map candidate : candidates) {
        String iValue = candidate['value'].toString()
        String iText  = candidate['text']?.toString() ?: iValue
        if (!setSelectValueByCss(initiativeCss, iValue)) continue
        WebUI.waitForPageLoad(5)
        WebUI.delay(2)
        if (!hasNoDocumentsMessage() && countVisibleCards() > 0) {
            selectedInitiative = [value: iValue, text: iText]
            KeywordUtil.logInfo("[SETUP] Initiative con documentos: ${iText}")
            break
        }
    }

    if (selectedInitiative == null) {
        failures.add('[SETUP] Ninguna initiative tiene documentos visibles')
        KeywordUtil.markFailed(failures.join(' | '))
    }

    // Cambiar a List View
    def listViewIcon = findTestObject('Email/Objects/ListView/icon_list_view')
    def listShape = findTestObject('Email/Objects/ListView/icon_list_shape')

    WebUI.waitForElementVisible(listViewIcon, 4, FailureHandling.OPTIONAL)

    boolean listViewReady = false
    for (int attempt = 1; attempt <= 3; attempt++) {
        WebUI.click(listViewIcon, FailureHandling.OPTIONAL)
        WebUI.click(listShape, FailureHandling.OPTIONAL)
        clickListToggle()
        WebUI.delay(1)
        int cardsAfter = countVisibleCards()
        if (cardsAfter > 0) {
            listViewReady = true
            break
        }
    }

    if (!listViewReady) {
        failures.add('[SETUP] No se pudo activar List View (0 cards visibles después de toggle)')
        KeywordUtil.markFailed(failures.join(' | '))
    }

    KeywordUtil.logInfo("[${caseId}] List View activado con ${countVisibleCards()} documentos")

    // Validar que cada acción abre modal o subwindow
    def actions = [
        [id: 'edit',      labels: ['Edit', 'Editar']],
        [id: 'rename',    labels: ['Rename', 'Renombrar']],
        [id: 'url',       labels: ['URL', 'URL Email', 'Copy URL', 'Copiar URL']],
        [id: 'duplicate', labels: ['Duplicate', 'Duplicar']],
        [id: 'download',  labels: ['Download', 'Descargar', 'Export', 'Exportar']],
        [id: 'move',      labels: ['Move', 'Mover']],
        [id: 'delete',    labels: ['Delete', 'Eliminar']]
    ]

    actions.each { Map action ->
        String actionId = action.id
        List<String> labels = action.labels as List<String>

        // Abrir menú de la primera card (con reintentos)
        boolean menuOpened = false
        for (int menuAttempt = 1; menuAttempt <= 3; menuAttempt++) {
            if (openFirstCardMenu()) {
                menuOpened = true
                break
            }
            if (menuAttempt < 3) WebUI.delay(1)
        }

        if (!menuOpened) {
            failures.add("[${actionId}] No se pudo abrir menú dropdown en 3 intentos")
            return
        }
        WebUI.delay(1)
        CustomKeywords.'TempletPortalKeywords.captureCaseScreenshot'(caseId, "action_${actionId}_menu")

        // Leer estado ANTES del click
        int modalsBefore = countModals()
        int subwindowsBefore = countSubwindows()
        String urlBefore = getCurrentUrlSafe()

        // Click en la acción
        boolean clicked = clickActionEntry(labels, actionId)
        if (!clicked) {
            failures.add("[${actionId}] No se encontró opción con labels: ${labels.join('/')}")
            closeTransientUi()
            return
        }

        WebUI.delay(2)

        // Leer estado DESPUÉS del click
        int modalsAfter = countModals()
        int subwindowsAfter = countSubwindows()
        String urlAfter = getCurrentUrlSafe()

        CustomKeywords.'TempletPortalKeywords.captureCaseScreenshot'(caseId, "action_${actionId}_after_click")

        boolean modalOpened = modalsAfter > modalsBefore
        boolean subwindowOpened = subwindowsAfter > subwindowsBefore
        boolean sameTabNavigation = (urlBefore && urlAfter && urlBefore != urlAfter)

        KeywordUtil.logInfo("[${actionId}] modalsBefore=${modalsBefore} modalsAfter=${modalsAfter} subwindowsBefore=${subwindowsBefore} subwindowsAfter=${subwindowsAfter}")

        if (!modalOpened && !subwindowOpened && !sameTabNavigation) {
            failures.add("[${actionId}] NO abrió modal/subwindow/navegación. Esperado: modal, nueva ventana o cambio de URL. Real: modalsBefore=${modalsBefore} modalsAfter=${modalsAfter}, subwindowsBefore=${subwindowsBefore} subwindowsAfter=${subwindowsAfter}, urlBefore='${urlBefore}', urlAfter='${urlAfter}'")
        } else {
            String responseType = modalOpened ? 'modal abierto' : (subwindowOpened ? 'subwindow abierto' : 'navegación misma pestaña')
            KeywordUtil.logInfo("[${actionId}] ✅ Respuesta OK: ${responseType}")
        }

        // Limpiar UI antes de siguiente acción
        closeTransientUi()
        if (sameTabNavigation) {
            returnToListingView()
        }
        WebUI.delay(1)
    }

    // Screenshot final
    CustomKeywords.'TempletPortalKeywords.captureCaseScreenshot'(caseId, 'final')

    // Reporte final
    if (failures.isEmpty()) {
        KeywordUtil.markPassed("${caseId} OK. Todas las acciones abren modal o subwindow.")
    } else {
        KeywordUtil.markFailed("${caseId} FALLÓ. Failures: ${failures.join(' | ')}")
    }

} finally {
    try {
        WebUI.closeBrowser()
    } catch (Exception ignored) {}
}
