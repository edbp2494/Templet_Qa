// ─────────────────────────────────────────────────────────────────────────────
// TC: TC-BUILDERSAAS-FINSUMMARY-CONTENT-002
// Plataforma: BuilderSaas | Área: financial-summary
// Descripción: TC-BUILDERSAAS-FINSUMMARY-CONTENT-002 — [E9 · US-03] Valida el contenido del Financial Summary desde el element-map builder-saas-financial-summary.json (KPIs, Budget Usage, Spend by Category/Initiative, Revenue vs Target, Delivery Accuracy One-Off vs Blueprint) + consistencia aritmetica entre bloques (la data es server-rendered RSC, sin /api/*).
// Suites: Platforms/BuilderSaas/Financial-Summary
// Precondiciones: credenciales MS en Include/config/templet-credentials.properties
// ─────────────────────────────────────────────────────────────────────────────
import com.kms.katalon.core.util.KeywordUtil
import com.kms.katalon.core.webui.keyword.WebUiBuiltInKeywords as WebUI
import groovy.json.JsonSlurper

// TC-BUILDERSAAS-FINSUMMARY-CONTENT-002 — [E9 · US-03] Contenido del Financial Summary Dashboard.
// FASE 1: valida estructura completa desde el element-map (budget vs spend + barra de uso,
//         tabla Spend by Category, Spend/Revenue por iniciativa, Delivery Accuracy One-Off vs Blueprint).
// FASE 2: consistencia aritmetica entre bloques. La app NO expone /api/* (data server-rendered
//         via Next.js RSC, mock en servidor — verificado 2026-07-14): la forma de detectar valores
//         hardcodeados/incoherentes es cruzar los numeros entre bloques:
//         spent + remaining == total | %used == spent/total | %achieved == actual/target |
//         KPI Pending Approvals == cantidad de items de la seccion inferior.
// Element map: Include/config/element-maps/builder-saas-financial-summary.json
String caseId = 'TC-BUILDERSAAS-FINSUMMARY-CONTENT-002'
List failures = []
List warnings = []

// Asegurar navegador abierto ANTES del keyword: con sesion viva, validateScreenFromElementMap
// navega directo (rama sin SSO). El "login" de esta app es el query param del element-map.
boolean sessionAlive = true
try { WebUI.getUrl() } catch (Exception ignored) { sessionAlive = false }
if (!sessionAlive) {
	WebUI.openBrowser('')
	WebUI.maximizeWindow()
}

// ── FASE 1: estructura desde element-map (marca FAILED y corta si falta algo HARD) ──
CustomKeywords.'ReposCoverageKeywords.validateScreenFromElementMap'([
	caseId          : caseId,
	mapRelativePath : 'Include/config/element-maps/builder-saas-financial-summary.json',
	snapshotSlug    : 'tc_buildersaas_finsummary_content_002'
])

// ── FASE 2: consistencia aritmetica (solo corre si la estructura paso) ──────
Object rawJson = WebUI.executeJavaScript('''
var t = function(e){ return (e && e.textContent || '').trim(); };
var leaves = [];
var all = document.querySelectorAll('p,span,div,h1,h2,h3');
for (var i = 0; i < all.length; i++) { if (all[i].children.length === 0) { var s = t(all[i]); if (s) { leaves.push(s); } } }
var findContains = function(sub){ for (var i = 0; i < leaves.length; i++) { if (leaves[i].indexOf(sub) >= 0) { return leaves[i]; } } return ''; };
var cardValue = function(label){
	var nodes = document.querySelectorAll('p');
	for (var i = 0; i < nodes.length; i++) {
		if (t(nodes[i]) === label) {
			var kids = nodes[i].parentElement.querySelectorAll('*');
			for (var j = 0; j < kids.length; j++) {
				if (kids[j].children.length === 0) {
					var s = t(kids[j]);
					if (s && s !== label) {
						var c0 = s.charCodeAt(0);
						if (s.charAt(0) === '$' || (c0 >= 48 && c0 <= 57)) { return s; }
					}
				}
			}
			return '';
		}
	}
	return '';
};
var cardNote = function(label, marker){
	var nodes = document.querySelectorAll('p');
	for (var i = 0; i < nodes.length; i++) {
		if (t(nodes[i]) === label) {
			var kids = nodes[i].parentElement.querySelectorAll('*');
			for (var j = 0; j < kids.length; j++) {
				if (kids[j].children.length === 0) {
					var s = t(kids[j]);
					if (s.indexOf(marker) >= 0) { return s; }
				}
			}
			return '';
		}
	}
	return '';
};
var plusItems = 0;
for (var i = 0; i < leaves.length; i++) { if (leaves[i].indexOf('+$') === 0) { plusItems++; } }
var bars = 0;
var divs = document.querySelectorAll('div');
for (var i = 0; i < divs.length; i++) { var st = divs[i].getAttribute('style') || ''; if (st.indexOf('width:') >= 0 && st.indexOf('%') >= 0) { bars++; } }
return JSON.stringify({
	totalSpend: cardValue('Total Spend'),
	budgetRemaining: cardValue('Budget Remaining'),
	budgetTotalNote: cardNote('Budget Remaining', 'total'),
	revenueAchieved: cardValue('Revenue Achieved'),
	revenueTargetNote: cardNote('Revenue Achieved', 'target'),
	pendingKpi: cardValue('Pending Approvals'),
	usedPct: findContains('% used'),
	achievedPct: findContains('% achieved'),
	pendingItems: plusItems,
	bars: bars
});
''', null)

Map data = [:]
try {
	data = new JsonSlurper().parseText((rawJson ?: '{}').toString()) as Map
} catch (Exception e) {
	failures.add('[FASE 2] No se pudo parsear la extraccion JS: ' + e.getMessage())
}

// '$284k' -> 284 | 'of $400k total' -> 400 | '40% of $1.2M target' -> 1200 (todo en miles)
def moneyToK = { String s ->
	if (!s) { return null }
	int idx = s.indexOf('of ')
	String body = (idx >= 0 ? s.substring(idx + 3) : s)
	String digits = body.replaceAll('[^0-9.]', '')
	if (!digits) { return null }
	double val = Double.parseDouble(digits)
	if (body.contains('M') || body.contains('m')) { val = val * 1000 }
	return val
}
// '71% used' -> 71 | '40% of ...' -> 40
def pctToInt = { String s ->
	if (!s || s.indexOf('%') < 0) { return null }
	String digits = s.substring(0, s.indexOf('%')).replaceAll('[^0-9]', '')
	return (digits ? digits.toInteger() : null)
}

Double spent     = moneyToK((data.totalSpend ?: '').toString())
Double remaining = moneyToK((data.budgetRemaining ?: '').toString())
Double total     = moneyToK((data.budgetTotalNote ?: '').toString())
Double actual    = moneyToK((data.revenueAchieved ?: '').toString())
Double target    = moneyToK((data.revenueTargetNote ?: '').toString())
Integer usedPct     = pctToInt((data.usedPct ?: '').toString())
Integer achievedPct = pctToInt((data.achievedPct ?: '').toString())

if (spent == null || remaining == null || total == null) {
	failures.add('[FASE 2: budget] No se pudieron extraer spent/remaining/total. Crudo: ' +
		data.totalSpend + ' | ' + data.budgetRemaining + ' | ' + data.budgetTotalNote)
} else {
	if (Math.abs(spent + remaining - total) > 1) {
		failures.add('[FASE 2: budget] Esperado: spent + remaining == total. Encontrado: ' +
			spent + 'k + ' + remaining + 'k != ' + total + 'k. Data incoherente entre KPIs (posible hardcode).')
	}
	if (usedPct != null) {
		long calcUsed = Math.round(spent * 100 / total)
		if (Math.abs(calcUsed - usedPct) > 1) {
			failures.add('[FASE 2: budget usage] Esperado: % used == spent/total (' + calcUsed +
				'%). Encontrado: ' + usedPct + '%. La barra de uso no coincide con los KPIs.')
		}
	} else {
		warnings.add('[FASE 2] No se encontro el texto "% used" para el chequeo de la barra.')
	}
}

if (actual == null || target == null) {
	failures.add('[FASE 2: revenue] No se pudieron extraer actual/target. Crudo: ' +
		data.revenueAchieved + ' | ' + data.revenueTargetNote)
} else if (achievedPct != null) {
	long calcAchieved = Math.round(actual * 100 / target)
	if (Math.abs(calcAchieved - achievedPct) > 1) {
		failures.add('[FASE 2: revenue] Esperado: % achieved == actual/target (' + calcAchieved +
			'%). Encontrado: ' + achievedPct + '%. Revenue vs Target incoherente.')
	}
} else {
	warnings.add('[FASE 2] No se encontro el texto "% achieved" para el chequeo de revenue.')
}

String pendingKpiRaw = (data.pendingKpi ?: '').toString().replaceAll('[^0-9]', '')
int pendingItems = ((data.pendingItems ?: 0) as int)
if (pendingKpiRaw) {
	int pendingKpi = pendingKpiRaw.toInteger()
	if (pendingKpi != pendingItems) {
		failures.add('[FASE 2: approvals] Esperado: KPI Pending Approvals (' + pendingKpi +
			') == items "+$..." de la seccion inferior (' + pendingItems + '). Data desincronizada.')
	}
} else {
	warnings.add('[FASE 2] No se pudo extraer el valor de la KPI Pending Approvals.')
}

int bars = ((data.bars ?: 0) as int)
if (bars < 18) {
	warnings.add('[FASE 2: barras] Se esperaban >=18 barras de progreso (1+5+5+5+2); encontradas: ' + bars +
		'. Puede ser cambio del dataset mock.')
}

CustomKeywords.'TempletPortalKeywords.captureCaseScreenshot'(caseId, 'content_consistency')

String stamp = new Date().format('yyyyMMdd_HHmmss')
Map snap = [
	caseId    : caseId,
	timestamp : new Date().format("yyyy-MM-dd'T'HH:mm:ss"),
	fase      : 'consistencia-aritmetica',
	extracted : data,
	failures  : failures,
	warnings  : warnings
]
String dir = System.getProperty('user.dir') + '/Reports/BuilderSaas/snapshots'
try {
	CustomKeywords.'TempletPortalKeywords.writeJsonSnapshot'(dir + '/financial_summary_content_latest.json', snap)
	CustomKeywords.'TempletPortalKeywords.writeJsonSnapshot'(dir + '/history/financial_summary_content_' + stamp + '.json', snap)
} catch (Throwable t) {
	warnings.add('[SNAPSHOT] No se pudo escribir snapshot: ' + t.getMessage())
}

CustomKeywords.'CommonKeywords.logCaseSummary'(caseId, failures, warnings)
if (failures) {
	KeywordUtil.markFailedAndStop('[' + caseId + '] Failures (fase consistencia): ' + failures)
} else {
	KeywordUtil.logInfo('[' + caseId + '] FASE 2 PASSED' + (warnings ? ' con ' + warnings.size() + ' warnings' : ''))
}
