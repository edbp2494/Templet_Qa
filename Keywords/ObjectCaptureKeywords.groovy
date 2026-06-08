import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import com.kms.katalon.core.model.FailureHandling
import groovy.json.JsonBuilder
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardOpenOption

/**
 * Keyword para capturar automáticamente selectores de elementos encontrados en la página
 * y generar archivos de especificación de objetos para futuros test cases
 */

def captureElementSelectors(String platform, String sectionName) {
    try {
        Map<String, Object> capturedElements = (Map<String, Object>) WebUI.executeJavaScript('''
            var visible = function(el) {
                if (!(el instanceof HTMLElement)) return false;
                var st = window.getComputedStyle(el);
                return st.display !== 'none' && st.visibility !== 'hidden' && el.offsetParent !== null;
            };

            var getXPath = function(element) {
                if (element.id !== '')
                    return "//*[@id='" + element.id + "']";
                if (element === document.body)
                    return "/body";

                var ix = 0;
                var siblings = element.parentNode.childNodes;
                for (var i = 0; i < siblings.length; i++) {
                    var sibling = siblings[i];
                    if (sibling === element)
                        return getXPath(element.parentNode) + "/" + element.tagName.toLowerCase() + "[" + (ix + 1) + "]";
                    if (sibling.nodeType === 1 && sibling.tagName.toLowerCase() === element.tagName.toLowerCase())
                        ix++;
                }
            };

            var getCSSSelector = function(el) {
                if (el.id) return '#' + el.id;
                if (el.className) {
                    var classStr = el.className;
                    if (typeof classStr === 'object' && classStr.baseVal) classStr = classStr.baseVal;
                    if (classStr && classStr.trim()) {
                        var normalized = classStr;
                        normalized = normalized.split('\t').join(' ');
                        normalized = normalized.split('\n').join(' ');
                        normalized = normalized.split('\r').join(' ');
                        var parts = normalized.split(' ').filter(function(part) {
                            return part && part.trim().length > 0;
                        });
                        return el.tagName.toLowerCase() + '.' + parts.join('.');
                    }
                }
                return el.tagName.toLowerCase();
            };

            // Capturar elementos interactivos importantes
            var targets = {
                filters: Array.from(document.querySelectorAll('select, input[type="checkbox"], input[type="radio"], [data-toggle]')),
                menuTriggers: Array.from(document.querySelectorAll('[data-toggle="dropdown"], .dropdown-toggle, [aria-haspopup="true"]')),
                listItems: Array.from(document.querySelectorAll('table tbody tr, .list-group-item, .thumbnail-box, .card')),
                modals: Array.from(document.querySelectorAll('.modal, [role="dialog"]')),
                buttons: Array.from(document.querySelectorAll('button, a[role="button"]'))
            };

            var captured = {};
            for (var category in targets) {
                captured[category] = targets[category].filter(visible).slice(0, 5).map(function(el) {
                    return {
                        tag: el.tagName.toLowerCase(),
                        id: el.id || null,
                        classes: el.className ? (typeof el.className === 'string' ? el.className : el.className.baseVal) : null,
                        text: (el.textContent || '').trim().substring(0, 50),
                        xpath: getXPath(el),
                        css: getCSSSelector(el),
                        attributes: {
                            type: el.getAttribute('type'),
                            'data-toggle': el.getAttribute('data-toggle'),
                            'aria-label': el.getAttribute('aria-label'),
                            'title': el.getAttribute('title')
                        }
                    };
                });
            }
            return captured;
        ''', null)

        // Guardar especificación capturada
        String reportPath = System.getProperty('user.dir') + '/Reports/CapturedObjectSpecs'
        new File(reportPath).mkdirs()

        String filename = "${reportPath}/${platform}-${sectionName}-${System.currentTimeMillis()}.json"
        String jsonContent = new JsonBuilder(capturedElements).toPrettyString()
        
        Files.write(Paths.get(filename), jsonContent.bytes, StandardOpenOption.CREATE, StandardOpenOption.WRITE)
        
        KeywordUtil.logInfo("[OBJECT-CAPTURE] Especificación guardada: ${filename}")
        KeywordUtil.logInfo("[OBJECT-CAPTURE] Categorías capturadas: ${capturedElements.keySet().join(', ')}")
        
        return capturedElements
    } catch (Exception e) {
        KeywordUtil.logWarning("[OBJECT-CAPTURE] Error al capturar: ${e.message}")
        return [:]
    }
}

def generateObjectRepositoryXML(String platform, String category, Map elementData) {
    try {
        String name = elementData.id ? elementData.id : "${category}-${System.currentTimeMillis()}"
        String description = "${platform} - ${category} (Auto-captured)"
        
        String xpath = elementData.xpath ?: "//*[@id='${elementData.id}']"
        String css = elementData.css ?: "#${elementData.id}"

        String xml = """<?xml version="1.0" encoding="UTF-8"?>
<WebElementEntity>
   <description>${description}</description>
   <name>${name}</name>
   <tag>${platform.toLowerCase()},${category.toLowerCase()},auto-captured</tag>
   <elementGuidId>${UUID.randomUUID().toString()}</elementGuidId>
   <selectorCollection>
      <entry>
         <key>XPATH</key>
         <value>${xpath}</value>
      </entry>
      <entry>
         <key>CSS</key>
         <value>${css}</value>
      </entry>
   </selectorCollection>
   <selectorMethod>XPATH</selectorMethod>
   <smartLocatorEnabled>false</smartLocatorEnabled>
   <useRalativeImagePath>true</useRalativeImagePath>
   <webElementProperties>
      <isSelected>false</isSelected>
      <matchCondition>equals</matchCondition>
      <name>tag</name>
      <type>Main</type>
      <value>${elementData.tag}</value>
      <webElementGuid>${UUID.randomUUID().toString()}</webElementGuid>
   </webElementProperties>
</WebElementEntity>
"""
        
        String objRepoPath = System.getProperty('user.dir') + "/Object Repository/${platform.capitalize()}/Auto-Captured/${category}"
        new File(objRepoPath).mkdirs()
        
        String filename = "${objRepoPath}/${name}.rs"
        Files.write(Paths.get(filename), xml.bytes, StandardOpenOption.CREATE, StandardOpenOption.WRITE)
        
        KeywordUtil.logInfo("[OBJECT-REPO] Creado: ${filename}")
        return true
    } catch (Exception e) {
        KeywordUtil.logWarning("[OBJECT-REPO] Error: ${e.message}")
        return false
    }
}

def logActionSummary(String platform, String actionId, boolean subwindowOpened, Map beforeSnap, Map afterSnap) {
    String status = subwindowOpened ? "✓ SUBWINDOW OPENED" : "✗ NO SUBWINDOW"
    String summary = """
    ╔════════════════════════════════════════════════════════════╗
    ║ [${platform.toUpperCase()}] ACTION: ${actionId.toUpperCase()}
    ║ Status: ${status}
    ║ Modal Before: ${beforeSnap.modalLike ?: 0} → After: ${afterSnap.modalLike ?: 0}
    ║ Toast Before: ${beforeSnap.toastLike ?: 0} → After: ${afterSnap.toastLike ?: 0}
    ║ Rows Before: ${beforeSnap.rowCount ?: 0} → After: ${afterSnap.rowCount ?: 0}
    ║ URL Changed: ${(afterSnap.url ?: '') != (beforeSnap.url ?: '')}
    ╚════════════════════════════════════════════════════════════╝
    """
    KeywordUtil.logInfo(summary)
}
