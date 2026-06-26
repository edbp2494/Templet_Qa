#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Reporte de ciclo QA (24 -> 24) para Templet QA (Katalon).
Dashboard HTML (PO/CEO) + JSON, en docs/qa-cycles/ (versionable).

Diseno:
  - Cada corrida LOCAL ingiere lo nuevo de Reports/Smoke-Summary y Reports/asana_tickets
    y lo agrega a historiales VERSIONADOS (docs/qa-cycles/*-history.json).
  - El dashboard del ciclo se calcula desde esos historiales + git + inventario,
    asi local y CI producen lo mismo (CI solo ve lo versionado).
"""
import os, re, sys, json, glob, html, subprocess, argparse
from datetime import datetime, date

# ---------- fechas / ciclo ----------
def add_month(y, m, delta):
    idx = (y*12 + (m-1)) + delta
    return idx//12, idx%12 + 1

def cycle_window(at):
    if at.day >= 24:
        start = date(at.year, at.month, 24)
        ey, em = add_month(at.year, at.month, 1); end = date(ey, em, 24)
    else:
        sy, sm = add_month(at.year, at.month, -1); start = date(sy, sm, 24)
        end = date(at.year, at.month, 24)
    return start, end

MESES = ["", "enero","febrero","marzo","abril","mayo","junio","julio","agosto","septiembre","octubre","noviembre","diciembre"]

def human_dur(seconds):
    seconds = int(max(0, seconds)); h, r = divmod(seconds, 3600); m, s = divmod(r, 60)
    return (f"{h}h {m}m" if h else (f"{m}m {s}s" if m else f"{s}s"))

def in_window(ts_str, start, end):
    try: ts = datetime.strptime(ts_str, '%Y-%m-%d %H:%M:%S')
    except ValueError: return False
    return datetime.combine(start, datetime.min.time()) <= ts < datetime.combine(end, datetime.min.time())

# ---------- ingestion local (Reports/) ----------
TS_RE = re.compile(r'_(\d{8})_(\d{6})')

def parse_all_executions(repo):
    runs = []
    for path in glob.glob(os.path.join(repo, 'Reports', 'Smoke-Summary', 'smoke_summary_*.txt')):
        m = TS_RE.search(os.path.basename(path))
        if not m: continue
        try: ts = datetime.strptime(m.group(1)+m.group(2), '%Y%m%d%H%M%S')
        except ValueError: continue
        try: txt = open(path, encoding='utf-8', errors='replace').read()
        except OSError: continue
        suite = ''
        ms = re.search(r'SMOKE SUMMARY[^\S\r\n]*[—\-]\s*(.+)', txt)
        if ms: suite = ms.group(1).strip()
        dur = 0
        mi = re.search(r'Inicio:\s*([\d\-: ]+?)\s*\|\s*Fin:\s*([\d\-: ]+)', txt)
        if mi:
            try:
                ini = datetime.strptime(mi.group(1).strip(), '%Y-%m-%d %H:%M:%S')
                fin = datetime.strptime(mi.group(2).strip(), '%Y-%m-%d %H:%M:%S')
                dur = max(0, (fin-ini).total_seconds())
            except ValueError: dur = 0
        tot=pas=fail=err=0
        mc = re.search(r'Total:\s*(\d+)\s*\|\s*Pass:\s*(\d+)\s*\|\s*Fail:\s*(\d+)\s*\|\s*Error:\s*(\d+)', txt)
        if mc: tot,pas,fail,err = (int(x) for x in mc.groups())
        runs.append({'timestamp': ts.strftime('%Y-%m-%d %H:%M:%S'), 'suite': suite,
                     'duration_s': dur, 'total': tot, 'pass': pas, 'fail': fail, 'error': err})
    return runs

def _problema(text):
    t=(text or "").lower()
    if "not interactable" in t or "no interactuable" in t: return "Elemento no interactuable"
    if "not clickable" in t or "click intercepted" in t or "unable to click" in t: return "No se pudo hacer click"
    if "not found" in t or "no encontrado" in t or "webelementnotfound" in t or "selector" in t: return "Elemento no encontrado"
    if "timed out" in t or "timeout" in t: return "Tiempo de espera agotado"
    if "asana_token" in t or "is not set" in t or "config" in t: return "Error de configuracion"
    if "validac" in t or "required" in t or "obligatori" in t or "highlighted fields" in t: return "Validacion faltante"
    if "label" in t or "etiqueta" in t or "incorrect" in t: return "Etiqueta o campo incorrecto"
    return "Falla funcional"

def _humaniza_desc(t):
    lugar = (t.get("tab") or t.get("component") or "la app").strip()
    problema = _problema((t.get("description") or "") + " " + (t.get("title") or ""))
    return f"{problema} en {lugar}"[:72]

def parse_all_tickets(repo):
    items = []
    for path in glob.glob(os.path.join(repo, 'Reports', 'asana_tickets', 'asana_tickets_*.json')):
        m = TS_RE.search(os.path.basename(path))
        if not m: continue
        try: ts = datetime.strptime(m.group(1)+m.group(2), '%Y%m%d%H%M%S')
        except ValueError: continue
        try: data = json.load(open(path, encoding='utf-8'))
        except Exception: continue
        for t in data.get('tickets', []):
            title = t.get('title', '')
            sev = 'OTRO'
            msv = re.search(r'(HIGH|MEDIUM|LOW)', title)
            if msv: sev = msv.group(1)
            desc_raw = t.get('description', '') or ''
            # detalle de reparacion extraido del cuerpo del ticket
            m_err = re.search(r'\*\*Error:\*\*\s*(.+?)(?:\nARCHIVO|\n\*\*Source|\Z)', desc_raw, re.S)
            paso = re.sub(r'\s+', ' ', (m_err.group(1).strip() if m_err else '')).strip()
            m_paso = re.search(r'(PASO\s*\d+:[^\]]+)\]', paso)
            paso_short = m_paso.group(1).strip() if m_paso else paso[:90]
            m_src = re.search(r'ARCHIVO FUENTE:\s*(.+)', desc_raw)
            repo_file = (m_src.group(1).strip() if m_src else t.get('sourceFile', '')) or ''
            m_act = re.search(r'ACCION SUGERIDA:\s*(.+)', desc_raw)
            accion = re.sub(r'\s+', ' ', (m_act.group(1).strip() if m_act else '')).strip()
            items.append({'ts': ts.strftime('%Y-%m-%d %H:%M:%S'), 'title': title[:140], 'sev': sev,
                          'caseId': t.get('caseId',''), 'component': t.get('component',''),
                          'desc': _humaniza_desc(t),
                          'paso': paso_short[:140], 'repo_file': repo_file[:160],
                          'accion': accion[:240], 'screenshot': (t.get('screenshot','') or '')[:240],
                          'url': t.get('url',''),
                          'asana': ('Y' if (t.get('asanaTaskId') or t.get('asanaUrl') or t.get('asana_gid')) else 'N')})
    return items

# ---------- historiales versionados ----------
def load_list(path):
    try: return json.load(open(path, encoding='utf-8'))
    except Exception: return []

def merge_runs(existing, fresh):
    seen = {(r.get('timestamp'), r.get('suite')) for r in existing}
    out = list(existing)
    for r in fresh:
        k = (r.get('timestamp'), r.get('suite'))
        if k not in seen: seen.add(k); out.append(r)
    out.sort(key=lambda r: r.get('timestamp', ''))
    return out

def merge_tickets(existing, fresh):
    seen = {(t.get('ts'), t.get('title')) for t in existing}
    out = list(existing)
    for t in fresh:
        k = (t.get('ts'), t.get('title'))
        if k not in seen: seen.add(k); out.append(t)
    out.sort(key=lambda t: t.get('ts', ''))
    return out

# ---------- git / inventario ----------
def git(repo, args):
    try:
        out = subprocess.run(['git', '-C', repo]+args, capture_output=True, text=True, timeout=60)
        return out.stdout if out.returncode == 0 else ''
    except Exception: return ''

FIX_RE = re.compile(r'\b(fix|fixes|fixed|bug|flak|flaky|estabil|stabil|unblock|desbloque|bloque|resuelve|resuelto|hotfix|regresi)', re.I)

def humanize_commit(subject):
    m = re.match(r"^(\w+)(?:\(([^)]+)\))?!?:\s*(.+)$", subject or "")
    tmap={"feat":"Nueva funcionalidad","fix":"Correccion","chore":"Mantenimiento","refactor":"Refactor",
          "docs":"Documentacion","test":"Pruebas","ci":"CI/CD","perf":"Optimizacion","style":"Estilo","build":"Build","revert":"Revertido"}
    if m:
        tipo=tmap.get(m.group(1).lower(), m.group(1)); scope=(m.group(2) or "").strip(); desc=m.group(3).strip()
        return (f"{tipo} en {scope}: {desc}" if scope else f"{tipo}: {desc}")
    return subject or ""

def parse_git(repo, start, end):
    raw = git(repo, ['log', f'--since={start.isoformat()} 00:00:00', f'--until={end.isoformat()} 00:00:00',
                     '--numstat', '--date=short', '--pretty=__C__%h|%an|%ad|%s'])
    commits=[]; cur=None; files=set(); new_tcs=set(); mod_tcs=set(); new_scripts=set(); new_kw=set(); add=dele=0
    for line in raw.splitlines():
        if line.startswith('__C__'):
            if cur: commits.append(cur)
            parts = (line[5:].split('|',3)+['','','',''])[:4]
            cur = {'hash':parts[0],'author':parts[1],'date':parts[2],'subject':parts[3],'human':humanize_commit(parts[3]),'fix':bool(FIX_RE.search(parts[3]))}
        elif '\t' in line and cur is not None:
            p = line.split('\t')
            if len(p) >= 3:
                a = int(p[0]) if p[0].isdigit() else 0
                d = int(p[1]) if p[1].isdigit() else 0
                f = p[2]; files.add(f)
                added = (d == 0 and a > 0)
                if f.endswith('.tc'): (new_tcs if added else mod_tcs).add(f)
                if f.endswith('Script.groovy') and added: new_scripts.add(f)
                if f.startswith('Keywords/') and f.endswith('.groovy') and added: new_kw.add(f)
                if f.startswith('Scripts/') or f.startswith('Keywords/'): add += a; dele += d
    if cur: commits.append(cur)
    return {'commits':commits,'commit_count':len(commits),'authors':sorted({c['author'] for c in commits}),
            'files_changed':len(files),'new_tcs':sorted(new_tcs),'mod_tcs':sorted(mod_tcs),
            'new_scripts':sorted(new_scripts),'new_keywords':sorted(new_kw),
            'fixes':[c for c in commits if c['fix']],'code_added':add,'code_deleted':dele}

PLATAFORMAS = ['Builders','Sheets','Decks','Email','Schedulers','QA']

def inventory(repo):
    g = lambda pat, base: glob.glob(os.path.join(repo, base, '**', pat), recursive=True)
    inv = {
        'total_tcs': len(g('*.tc','Test Cases')),
        'total_suites': len(g('*.ts','Test Suites')),
        'total_scripts': len(g('Script.groovy','Scripts')),
        'by_platform': {p: len(glob.glob(os.path.join(repo,'Test Cases',p,'**','*.tc'), recursive=True)) for p in PLATAFORMAS},
    }
    return inv

# ---------- agregacion ----------
def aggregate(repo, at, manual_min, exec_hist, ticket_hist):
    start, end = cycle_window(at)
    ex = [r for r in exec_hist if in_window(r.get('timestamp',''), start, end)]
    runs=len(ex); tcs=sum(r['total'] for r in ex); pas=sum(r['pass'] for r in ex)
    fail=sum(r['fail'] for r in ex); err=sum(r['error'] for r in ex); auto=sum(r['duration_s'] for r in ex)
    regress=sum(1 for r in ex if any(k in (r['suite'] or '').lower() for k in ['regression','full','master','regresion']))
    pr = round(100.0*pas/tcs,1) if tcs else 0.0
    manual = tcs*manual_min*60
    g = parse_git(repo, start, end); inv = inventory(repo)
    tks = [t for t in ticket_hist if in_window(t.get('ts',''), start, end)]
    by_sev={}
    for t in tks: by_sev[t['sev']] = by_sev.get(t['sev'],0)+1
    # estado por caseId usando TODO el historial de tickets
    global_last = max((t.get('ts','') for t in ticket_hist), default='')
    last_ts_by_case={}; batches_by_case={}
    for t in ticket_hist:
        cid = t.get('caseId') or (t.get('title','')[:40])
        last_ts_by_case[cid] = max(last_ts_by_case.get(cid,''), t.get('ts',''))
        batches_by_case.setdefault(cid,set()).add(t.get('ts',''))
    cases={}
    for t in tks:
        cid = t.get('caseId') or (t.get('title','')[:40])
        c = cases.setdefault(cid, {'caseId':cid,'desc':t.get('desc',''),'sev':t.get('sev',''),'asana':t.get('asana','N'),'count':0,
                                   'paso':'','repo_file':'','accion':'','screenshot':'','url':''})
        c['count']+=1; c['sev']=t.get('sev',c['sev'])
        if not c.get('desc'): c['desc']=t.get('desc','')
        # quedarse con el detalle de reparacion mas completo encontrado
        for k in ('paso','repo_file','accion','screenshot','url'):
            if t.get(k) and len(t.get(k,'')) > len(c.get(k,'')): c[k]=t[k]
        if t.get('asana')=='Y': c['asana']='Y'
    abiertos=resueltos=0; case_list=[]
    for cid,c in cases.items():
        c['recurrence']=len(batches_by_case.get(cid,set()))
        c['estado']='Abierto' if (global_last and last_ts_by_case.get(cid,'')==global_last) else 'Resuelto'
        if c['estado']=='Abierto': abiertos+=1
        else: resueltos+=1
        case_list.append(c)
    case_list.sort(key=lambda x:-x['count'])
    base_idx = 2026*12 + 5  # mayo 2026 = primer ciclo (meta 60%)
    cyc_idx = start.year*12 + start.month
    target = min(90, max(60, 60 + 10*(cyc_idx - base_idx)))
    metas = [
        {'nombre':f'Tasa de exito >= {target}% (meta del mes)','meta':f'{target}%','valor':f'{pr}%','ok':pr>=target and tcs>0},
        {'nombre':'Regresiones ejecutadas >= 1','meta':'1','valor':str(regress),'ok':regress>=1},
        {'nombre':'Casos nuevos automatizados >= 3','meta':'3','valor':str(len(g['new_tcs'])),'ok':len(g['new_tcs'])>=3},
        {'nombre':'Estabilizaciones/bloqueos resueltos >= 1','meta':'1','valor':str(len(g['fixes'])),'ok':len(g['fixes'])>=1},
    ]
    return {
        'cycle':{'start':start.isoformat(),'end':end.isoformat(),
                 'label':f'24 {MESES[start.month]} {start.year} → 24 {MESES[end.month]} {end.year}'},
        'generated': datetime.now().strftime('%Y-%m-%d %H:%M:%S'),
        'executions':{'runs':runs,'tcs_executed':tcs,'pass':pas,'fail':fail,'error':err,
                      'pass_rate':pr,'regressions':regress,'auto_seconds':auto,'detail':ex},
        'time':{'auto_seconds':auto,'manual_seconds_est':manual,'saved_seconds':max(0,manual-auto),'manual_min_per_tc':manual_min},
        'target_pass':target,
        'git':g,'inventory':inv,'tickets':{'total':len(tks),'unique':len(cases),'abiertos':abiertos,'resueltos':resueltos,'by_severity':by_sev,'cases':case_list,'asana':'Se publican en Asana si hay API key (GlobalVariable asana_api_key); si no, quedan como JSON local en Reports/asana_tickets.'},'metas':metas,
    }

# ---------- HTML ----------
def svg_donut(pas, fail, err):
    total = pas+fail+err
    if total == 0: return '<div class="muted">Sin ejecuciones en el ciclo</div>'
    C = 2*3.14159*52; off=0; parts=[]
    for color,val in [('#16a34a',pas),('#dc2626',fail),('#d97706',err)]:
        frac = val/total
        parts.append(f'<circle cx="70" cy="70" r="52" fill="none" stroke="{color}" stroke-width="22" '
                     f'stroke-dasharray="{frac*C:.2f} {C:.2f}" stroke-dashoffset="{-off*C:.2f}" transform="rotate(-90 70 70)"/>')
        off += frac
    pct = round(100.0*pas/total,1)
    return (f'<svg viewBox="0 0 140 140" width="160" height="160">{"".join(parts)}'
            f'<text x="70" y="64" text-anchor="middle" font-size="26" font-weight="700" fill="#e8eef7">{pct}%</text>'
            f'<text x="70" y="84" text-anchor="middle" font-size="11" fill="#93a1b5">exito</text></svg>')

def svg_bars(pairs, color='#2563eb', unit=''):
    if not pairs: return '<div class="muted">Sin datos</div>'
    mx = max(v for _,v in pairs) or 1; rows=[]
    for label,val in pairs:
        w = int(100.0*val/mx)
        rows.append(f'<div class="bar-row"><div class="bar-label">{html.escape(str(label))}</div>'
                    f'<div class="bar-track"><div class="bar-fill" style="width:{w}%;background:{color}"></div></div>'
                    f'<div class="bar-val">{val}{unit}</div></div>')
    return '<div class="bars">'+''.join(rows)+'</div>'

def kpi(value, label, sub='', color='#e8eef7', win=False):
    sub_html = f'<div class="kpi-sub">{html.escape(sub)}</div>' if sub else ''
    cls = 'kpi win' if win else 'kpi'
    return (f'<div class="{cls}"><div class="kpi-val" style="color:{color}">{html.escape(str(value))}</div>'
            f'<div class="kpi-label">{html.escape(label)}</div>{sub_html}</div>')

def build_html(d):
    ex=d['executions']; t=d['time']; g=d['git']; inv=d['inventory']; tk=d['tickets']
    saved_h=t['saved_seconds']/3600.0; metas_ok=sum(1 for m in d['metas'] if m['ok']); tgt=d.get('target_pass',90)
    kpis=''.join([
        kpi(f"{ex['pass_rate']}%",'Tasa de exito',f"{ex['pass']}/{ex['tcs_executed']} casos (meta {tgt}%)",'#22c55e' if ex['pass_rate']>=tgt else '#fbbf24',win=ex['pass_rate']>=tgt),
        kpi(inv['total_tcs'],'Casos automatizados',f"{inv['total_suites']} suites",'#22c55e',win=True),
        kpi(f"{saved_h:.1f} h",'Tiempo ahorrado','auto vs manual','#22c55e',win=True),
        kpi(len(g['new_tcs']),'Casos nuevos','en el ciclo','#38bdf8'),
        kpi(ex['tcs_executed'],'Casos ejecutados',f"{ex['runs']} corridas"),
        kpi(tk['abiertos'],'Issues abiertos',f"{tk['resueltos']} resueltos",'#fca5a5' if tk['abiertos'] else '#22c55e'),
    ])
    metas_html=''.join(
        f'<div class="meta {"ok" if m["ok"] else "no"}"><span>{"✅" if m["ok"] else "⚠️"}</span>'
        f'<span class="meta-name">{html.escape(m["nombre"])}</span>'
        f'<span class="meta-val">{html.escape(m["valor"])} <small>/ meta {html.escape(m["meta"])}</small></span></div>'
        for m in d['metas'])
    plat_pairs=[(p,v) for p,v in inv['by_platform'].items()]
    _agg={}
    for r in ex['detail']:
        nm=(r['suite'] or '?').split('/')[-1]
        a=_agg.setdefault(nm,{'casos':0,'runs':0}); a['casos']+=r['total']; a['runs']+=1
    suite_pairs=sorted([(f"{nm} ({v['runs']}x)", v['casos']) for nm,v in _agg.items()], key=lambda x:-x[1])[:14]
    commit_rows=''.join(
        f'<tr><td><code>{html.escape(c["hash"])}</code></td><td>{html.escape(c["date"])}</td>'
        f'<td>{html.escape((c.get("human") or c["subject"])[:90])}</td></tr>'
        for c in g['commits'][:25]) or '<tr><td colspan="3" class="muted">Sin commits en el ciclo</td></tr>'
    def _ticket_block(i,c):
        det=[]
        if c.get('repo_file'): det.append(f"<b>Archivo:</b> <code>{html.escape(c['repo_file'])}</code>")
        if c.get('paso'): det.append(f"<b>Fallo:</b> {html.escape(c['paso'])}")
        if c.get('accion'): det.append(f"<b>Reparacion:</b> {html.escape(c['accion'])}")
        if c.get('screenshot'): det.append(f"<b>Screenshot:</b> <code>{html.escape(c['screenshot'])}</code>")
        det_html=('<br>'.join(det)) if det else '<span class="muted">Sin detalle adicional</span>'
        return (
        f'<tr><td style="text-align:center">{i}</td><td><code>{html.escape(c["caseId"][:42])}</code></td>'
        f'<td>{html.escape((c.get("desc") or "")[:60])}</td>'
        f'<td><span class="sev sev-{html.escape(c["sev"])}">{html.escape(c["sev"])}</span></td>'
        f'<td style="text-align:center">{c["count"]}</td>'
        f'<td style="text-align:center">{html.escape(c.get("asana","N"))}</td>'
        f'<td><span class="est est-{("ab" if c["estado"]=="Abierto" else "re")}">{html.escape(c["estado"])}</span></td></tr>'
        f'<tr class="detrow"><td></td><td colspan="6" class="detail">{det_html}</td></tr>')
    ticket_rows=''.join(_ticket_block(i,c) for i,c in enumerate(tk['cases'][:30],1)) or '<tr><td colspan="7" class="muted">Sin tickets en el ciclo</td></tr>'
    auto_h=t['auto_seconds']/3600.0; man_h=t['manual_seconds_est']/3600.0
    time_bars=svg_bars([('Automatizado',round(auto_h,1)),('Manual (est.)',round(man_h,1))],'#7c3aed',' h')
    return f"""<!doctype html><html lang="es"><head><meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>QA Cycle Report - {html.escape(d['cycle']['label'])}</title>
<style>
:root{{--bg:#0a0e1a;--card:#131a2b;--card2:#0f1626;--ink:#e8eef7;--muted:#93a1b5;--line:#26314a;--brand:#22c55e;--accent:#38bdf8}}
*{{box-sizing:border-box}} body{{margin:0;font-family:'Segoe UI',system-ui,Arial,sans-serif;background:radial-gradient(1100px 560px at 72% -12%,#16243f 0%,var(--bg) 55%);color:var(--ink)}}
.wrap{{max-width:1100px;margin:0 auto;padding:28px 20px 60px}}
.head{{display:flex;justify-content:space-between;align-items:flex-end;flex-wrap:wrap;gap:12px;margin-bottom:6px}}
.head h1{{margin:0;font-size:26px}} .head .cycle{{font-size:18px;color:var(--brand);font-weight:700}} .head .gen{{color:var(--muted);font-size:13px}}
.hero{{display:flex;flex-wrap:wrap;gap:18px;align-items:center;justify-content:space-between;background:linear-gradient(135deg,#0f2a1d 0%,#0f1626 70%);border:1px solid #1f7a44;border-radius:18px;padding:20px 24px;margin:16px 0 10px;box-shadow:0 10px 40px rgba(34,197,94,.10)}}
.hero .big{{font-size:46px;font-weight:900;color:var(--brand);line-height:1}} .hero .big small{{display:block;font-size:14px;color:var(--muted);font-weight:600;margin-top:4px}}
.hero .pills{{display:flex;gap:10px;flex-wrap:wrap}}
.hpill{{background:rgba(34,197,94,.10);border:1px solid #1f7a44;color:#bbf7d0;border-radius:14px;padding:10px 14px;font-size:12px;font-weight:700;text-align:center;min-width:96px}} .hpill b{{display:block;font-size:22px;color:#fff;margin-bottom:2px}}
.kpis{{display:grid;grid-template-columns:repeat(auto-fit,minmax(160px,1fr));gap:14px;margin:14px 0}}
.kpi{{background:var(--card);border:1px solid var(--line);border-radius:14px;padding:16px;text-align:center}}
.kpi.win{{border-color:#1f7a44;box-shadow:0 0 0 1px rgba(34,197,94,.20),0 8px 28px rgba(34,197,94,.08)}}
.kpi-val{{font-size:30px;font-weight:800;line-height:1}} .kpi-label{{margin-top:6px;font-size:13px;color:var(--muted)}} .kpi-sub{{font-size:11px;color:#6b7a90;margin-top:2px}}
.grid{{display:grid;grid-template-columns:1fr 1fr;gap:16px}} @media(max-width:760px){{.grid{{grid-template-columns:1fr}}}}
.card{{background:var(--card);border:1px solid var(--line);border-radius:14px;padding:18px;margin-bottom:16px}}
.card h2{{margin:0 0 14px;font-size:14px;text-transform:uppercase;letter-spacing:.05em;color:#9fb0c7}}
.center{{display:flex;align-items:center;justify-content:center;gap:18px;flex-wrap:wrap}}
.legend span{{display:block;font-size:13px;margin:2px 0}} .dot{{width:10px;height:10px;border-radius:50%;display:inline-block;margin-right:6px}}
.bars{{display:flex;flex-direction:column;gap:9px}} .bar-row{{display:grid;grid-template-columns:150px 1fr 70px;align-items:center;gap:10px;font-size:13px}}
.bar-track{{background:#0c1322;border:1px solid #1c2740;border-radius:6px;height:14px;overflow:hidden}} .bar-fill{{height:100%;border-radius:6px}} .bar-val{{text-align:right;color:#aebccf}}
.metas{{display:flex;flex-direction:column;gap:8px}} .meta{{display:flex;align-items:center;gap:10px;padding:10px 12px;border-radius:10px;border:1px solid var(--line);background:var(--card2)}}
.meta.ok{{background:rgba(34,197,94,.10);border-color:#1f7a44}} .meta.no{{background:rgba(217,119,6,.10);border-color:#7c5212}} .meta-name{{flex:1}} .meta-val{{font-weight:700}} .meta-val small{{color:var(--muted);font-weight:400}}
table{{width:100%;border-collapse:collapse;font-size:13px}} th,td{{text-align:left;padding:8px 10px;border-bottom:1px solid var(--line)}} th{{color:var(--muted)}}
code{{background:#0c1322;border:1px solid #1c2740;padding:1px 5px;border-radius:5px;color:#cbd5e1}} .muted{{color:var(--muted)}}
.detrow td{{border-bottom:1px solid var(--line);padding-top:2px}} .detail{{font-size:12px;color:#94a3b8;line-height:1.55;background:#0b1120}} .detail b{{color:#cbd5e1}} .detail code{{font-size:11px}}
.sev{{padding:2px 8px;border-radius:20px;font-size:11px;font-weight:700}} .sev-HIGH{{background:#3b0d0d;color:#fca5a5}} .sev-MEDIUM{{background:#3a2a06;color:#fcd34d}} .sev-LOW{{background:#06283a;color:#7dd3fc}} .sev-OTRO{{background:#1f2937;color:#9ca3af}}
.est{{padding:2px 8px;border-radius:20px;font-size:11px;font-weight:700}} .est-ab{{background:#3b0d0d;color:#fca5a5}} .est-re{{background:#0c2f1c;color:#86efac}}
.foot{{margin-top:24px;color:#6b7a90;font-size:12px;text-align:center}} .pill{{display:inline-block;background:rgba(34,197,94,.12);color:#bbf7d0;border:1px solid #1f7a44;border-radius:20px;padding:3px 10px;font-size:12px;font-weight:700}}
</style></head><body><div class="wrap">
<div class="head"><div><h1>Reporte QA - Templet</h1><div class="cycle">{html.escape(d['cycle']['label'])}</div></div>
<div style="text-align:right"><div class="pill">{metas_ok}/{len(d['metas'])} metas cumplidas</div><div class="gen">Generado {html.escape(d['generated'])}</div></div></div>
<div class="hero">
  <div class="big">{ex['pass_rate']}%<small>tasa de exito del ciclo</small></div>
  <div class="pills">
    <div class="hpill"><b>{inv['total_tcs']}</b>casos automatizados</div>
    <div class="hpill"><b>{saved_h:.0f} h</b>ahorradas vs manual</div>
    <div class="hpill"><b>{len(g['new_tcs'])}</b>casos nuevos</div>
    <div class="hpill"><b>{metas_ok}/{len(d['metas'])}</b>metas cumplidas</div>
  </div>
</div>
<div class="kpis">{kpis}</div>
<div class="grid">
  <div class="card"><h2>Resultado de ejecuciones</h2><div class="center">{svg_donut(ex['pass'],ex['fail'],ex['error'])}
    <div class="legend"><span><span class="dot" style="background:#16a34a"></span>Pass: {ex['pass']}</span>
      <span><span class="dot" style="background:#dc2626"></span>Fail: {ex['fail']}</span>
      <span><span class="dot" style="background:#d97706"></span>Error: {ex['error']}</span>
      <span class="muted">Regresiones: {ex['regressions']} - Corridas: {ex['runs']}</span></div></div>
    <p class="muted" style="font-size:12px;margin:12px 0 0">Tasa de exito = casos en <b>Pass</b> / total de casos ejecutados en el ciclo (<b>{ex['tcs_executed']}</b>). Cuenta cada ejecucion: un caso corrido N veces suma N (incluye corridas de desarrollo).</p></div>
  <div class="card"><h2>Eficiencia: automatizado vs manual</h2>{time_bars}
    <p class="muted" style="font-size:12px;margin:10px 0 0">Manual estimado = {ex['tcs_executed']} casos x {t['manual_min_per_tc']} min.
    Ahorro del ciclo: <b style="color:#16a34a">{human_dur(t['saved_seconds'])}</b>.</p></div>
</div>
<div class="card"><h2>Metas del ciclo</h2><div class="metas">{metas_html}</div></div>
<div class="card"><h2>Casos automatizados por plataforma</h2>{svg_bars(plat_pairs,'#2563eb')}
  <p class="muted" style="font-size:12px;margin-top:10px">Total: {inv['total_tcs']} casos - {inv['total_suites']} suites - {inv['total_scripts']} scripts</p></div>
<div class="card"><h2>Casos ejecutados por suite (agrupado)</h2>{svg_bars(suite_pairs,'#0ea5e9')}
  <p class="muted" style="font-size:12px;margin-top:10px">Cada barra = una suite unica; (Nx) = veces que corrio en el ciclo; el numero = total de casos ejecutados.</p></div>
<div class="card"><h2>Trabajo del ciclo (cambios git)</h2>
  <div class="kpis" style="margin:0 0 14px">{kpi(g['commit_count'],'Commits')}{kpi(len(g['new_tcs']),'Casos nuevos')}{kpi(len(g['mod_tcs']),'Casos modificados')}{kpi(len(g['fixes']),'Estabilizaciones')}{kpi(g['code_added'],'Lineas + codigo','Scripts/Keywords')}</div>
  <table><thead><tr><th>Commit</th><th>Fecha</th><th>Descripcion</th></tr></thead><tbody>{commit_rows}</tbody></table></div>
<div class="card"><h2>Tickets / errores detectados</h2>
  <div class="kpis" style="margin:0 0 12px">{kpi(tk['total'],'Tickets (ocurrencias)')}{kpi(tk['unique'],'Casos unicos')}{kpi(tk['abiertos'],'Abiertos','pendientes','#fca5a5')}{kpi(tk['resueltos'],'Resueltos','no reaparecen','#22c55e')}</div>
  <table><thead><tr><th>ID</th><th>Caso</th><th>Descripcion</th><th>Sev</th><th>Veces</th><th>Asana</th><th>Estado</th></tr></thead><tbody>{ticket_rows}</tbody></table></div>
<div class="foot">Templet QA - Reporte automatico de ciclo (24-24). Datos: ejecuciones Katalon, git, inventario y tickets.</div>
</div></body></html>"""

def build_message(d):
    """Mensaje de periodo (estilo reporte de horas) con data real del ciclo. Horas = estimado editable."""
    c=d['cycle']; g=d['git']; ex=d['executions']; tk=d['tickets']
    new=len(g['new_tcs']); mod=len(g['mod_tcs']); fixes=len(g['fixes']); kw=len(g['new_keywords'])
    runs=ex['runs']; regr=ex['regressions']
    h_auto = round(new*1.5 + mod*0.5 + kw*1.0 + g['code_added']/400.0, 1)
    h_exec = round(runs*0.25 + regr*0.5, 1)
    h_issues = round(tk['unique']*0.5, 1)
    h_reu = 6.0
    total = round(h_auto + h_exec + h_issues + h_reu, 1)
    L = []
    L.append("Randyyyy, todo bien? 🙌 Te comparto el trabajo del periodo:")
    L.append(f"Periodo: {c['start']} a {c['end']}")
    L.append("")
    L.append(f"- Automatizacion y ajustes nuevos ({new} casos nuevos, {mod} modificados, {fixes} estabilizaciones, {kw} keywords): {h_auto} h")
    L.append(f"- Ejecuciones y revalidaciones ({runs} corridas, {regr} regresiones, {ex['tcs_executed']} casos ejecutados): {h_exec} h")
    L.append(f"- Reporte y seguimiento de issues ({tk['unique']} casos / {tk['total']} ocurrencias): {h_issues} h")
    L.append(f"- Reuniones, dailys, refinamientos y demo: {h_reu} h")
    L.append("")
    L.append(f"Total del periodo: {total} h   (estimado — ajusta las horas segun tu registro real)")
    if tk['cases']:
        L.append("")
        L.append("Issues principales del periodo:")
        for ctk in tk['cases'][:6]:
            L.append(f"  * [{ctk['sev']}] {ctk['caseId']} ({ctk.get('component','')}) - {ctk['estado']}")
    return "\n".join(L)

def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--at', default=None)
    ap.add_argument('--manual-min', type=int, default=int(os.environ.get('QA_MANUAL_MIN_PER_TC','10')))
    ap.add_argument('--repo', default=None)
    a = ap.parse_args()
    repo = a.repo or os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    at = datetime.strptime(a.at,'%Y-%m-%d').date() if a.at else date.today()
    base = os.path.join(repo, 'docs', 'qa-cycles'); os.makedirs(base, exist_ok=True)
    exh = os.path.join(base, 'executions-history.json'); tkh = os.path.join(base, 'tickets-history.json')
    exec_hist = merge_runs(load_list(exh), parse_all_executions(repo))
    ticket_hist = merge_tickets(load_list(tkh), parse_all_tickets(repo))
    json.dump(exec_hist, open(exh,'w',encoding='utf-8'), ensure_ascii=False, indent=1)
    json.dump(ticket_hist, open(tkh,'w',encoding='utf-8'), ensure_ascii=False, indent=1)
    data = aggregate(repo, at, a.manual_min, exec_hist, ticket_hist)
    start = data['cycle']['start']; end = data['cycle']['end']
    outdir = os.path.join(base, f'{start}_a_{end}'); os.makedirs(outdir, exist_ok=True)
    json.dump(data, open(os.path.join(outdir,'data.json'),'w',encoding='utf-8'), ensure_ascii=False, indent=2)
    open(os.path.join(outdir,'dashboard.html'),'w',encoding='utf-8').write(build_html(data))
    open(os.path.join(base,'latest.html'),'w',encoding='utf-8').write(build_html(data))
    msg = build_message(data)
    open(os.path.join(outdir,'mensaje.txt'),'w',encoding='utf-8').write(msg)
    open(os.path.join(base,'mensaje-latest.txt'),'w',encoding='utf-8').write(msg)
    print(f"[QA-CYCLE] {data['cycle']['label']}")
    print(f"[QA-CYCLE] runs={data['executions']['runs']} casos={data['executions']['tcs_executed']} "
          f"passrate={data['executions']['pass_rate']}% commits={data['git']['commit_count']} tickets={data['tickets']['total']}")
    print(f"[QA-CYCLE] salida: {outdir}")
    return 0

if __name__ == '__main__':
    sys.exit(main())
