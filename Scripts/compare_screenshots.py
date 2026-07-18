#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Comparador de capturas: Builders vieja vs nueva
Detecta diferencias visuales, componentes y genera reporte HTML
"""

import sys
import os
sys.stdout.reconfigure(encoding='utf-8')

import cv2
import numpy as np
from pathlib import Path
import json
from datetime import datetime
import glob

# Configuración
BASE_DIR = Path(r"C:\Users\e2494\Katalon Studio\Templet\Reports\Builders\Compare")
OUTPUT_DIR = BASE_DIR / "analysis"
OUTPUT_DIR.mkdir(exist_ok=True)

# Ancho (px) del sidebar izquierdo a descartar del diff. El rediseno del nav
# es un cambio intencional de la migracion y de lo contrario infla el diff%
# marcando modulos como CRITICO por ruido. Poner 0 para comparar la imagen completa.
IGNORE_LEFT_PX = 260

# Modulos alineados a los 7 BCs del objetivo (ids = prefijo de los .png)
MODULES = {
    "01": "Home / dashboard",
    "02": "BC-01 Brand Properties — lista",
    "03": "BC-01 Brand detail (Layouts)",
    "04": "BC-01 Brand — Colors",
    "05": "BC-01 Brand — Samples",
    "06": "BC-01 Brand — Techtionary",
    "07": "BC-02 Brand Layouts — lista",
    "08": "BC-02 Layout detail",
    "09": "BC-03 Templates — lista",
    "10": "BC-03 Template detail",
    "11": "BC-04 Task Creation — One-Off",
    "12": "BC-04 Task Creation — Non-Standard",
    "13": "BC-08 Layout Generation — create",
    "14": "BC-08 Layout Generation — upload",
    "15": "BC-09 Initiatives — blueprint manager",
    "16": "BC-09 Initiatives — blueprint detail",
    "17": "BC-09 Initiatives — requests",
    "18": "BC-10 File Delivery — convert",
    "19": "Contexto — Project Schedule",
    "20": "Contexto — Work in Progress",
    "21": "Contexto — Current Spend",
}

def get_file_quality_status(filepath):
    """Detecta si un archivo es 404/loading basado en tamaño"""
    try:
        size = Path(filepath).stat().st_size
        size_kb = size / 1024
        
        if size_kb < 20:
            return "🔴 PROBLEMA (posible 404/loading)", "FAILED"
        elif size_kb < 50:
            return "⚠️ REVISAR (muy pequeño)", "WARNING"
        else:
            return "✅ OK", "OK"
    except:
        return "❌ ERROR al leer archivo", "ERROR"

def compare_images(old_path, new_path, module_id):
    """
    Compara dos imágenes y retorna:
    - % de píxeles diferentes
    - Heat map de diferencias
    - Resumen de cambios
    """
    print(f"  🔍 Analizando módulo {module_id}...", end=" ", flush=True)
     
    # Guardar nombres reales de archivos
    old_filename = Path(old_path).name
    new_filename = Path(new_path).name
     
    # Verificar calidad de archivos
    old_status, old_quality = get_file_quality_status(old_path)
    new_status, new_quality = get_file_quality_status(new_path)
     
    if old_quality == "FAILED" or new_quality == "FAILED":
        print(f"❌ (archivo problemático: vieja={old_quality}, nueva={new_quality})")
        return {
            "module_id": module_id,
            "module_name": MODULES.get(module_id, "Unknown"),
            "old_filename": old_filename,
            "new_filename": new_filename,
            "diff_percent": 0,
            "severity": "ERROR",
            "status_old": old_status,
            "status_new": new_status,
            "issue": "No se pudo analizar - archivos problemáticos",
            "changed_pixels": 0,
            "total_pixels": 0,
            "changed_regions": [],
            "num_regions": 0,
            "heatmap": None,
            "annotated": None
        }
    
    try:
        old_img = cv2.imread(old_path)
        new_img = cv2.imread(new_path)
        
        if old_img is None or new_img is None:
            print("❌ (imagen no encontrada)")
            return None
        
        # Asegurar mismo tamaño
        h, w = old_img.shape[:2]
        new_img = cv2.resize(new_img, (w, h))

        # Descartar la franja del sidebar izquierdo (rediseño intencional)
        if IGNORE_LEFT_PX > 0:
            cut = min(IGNORE_LEFT_PX, w)
            old_img[:, :cut] = 0
            new_img[:, :cut] = 0

        # Calcular diferencia
        diff = cv2.absdiff(old_img, new_img)
        
        # Detectar cambios por color
        gray_diff = cv2.cvtColor(diff, cv2.COLOR_BGR2GRAY)
        _, thresh = cv2.threshold(gray_diff, 20, 255, cv2.THRESH_BINARY)
        
        # Dilatación para conectar píxeles cercanos
        kernel = cv2.getStructuringElement(cv2.MORPH_ELLIPSE, (5, 5))
        dilated = cv2.dilate(thresh, kernel, iterations=2)
        
        # Estadísticas
        total_pixels = w * h
        changed_pixels = np.count_nonzero(dilated)
        diff_percent = round((changed_pixels / total_pixels) * 100, 2)
        
        # Detectar contornos (componentes que cambiaron)
        contours, _ = cv2.findContours(dilated, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)
        
        changed_regions = []
        for cnt in contours:
            x, y, w_cnt, h_cnt = cv2.boundingRect(cnt)
            area = w_cnt * h_cnt
            if area > 100:  # Filtrar ruido pequeño
                changed_regions.append({
                    "x": int(x),
                    "y": int(y),
                    "width": int(w_cnt),
                    "height": int(h_cnt),
                    "area": int(area)
                })
        
        # Generar heat map visual
        heatmap = cv2.applyColorMap(gray_diff, cv2.COLORMAP_JET)
        
        # Dibujar rectángulos en cambios detectados
        annotated = new_img.copy()
        for region in changed_regions[:10]:  # Top 10
            cv2.rectangle(
                annotated,
                (region["x"], region["y"]),
                (region["x"] + region["width"], region["y"] + region["height"]),
                (0, 0, 255),
                2
            )
        
        # Guardar visualizaciones
        heatmap_path = OUTPUT_DIR / f"{module_id}-heatmap.png"
        annotated_path = OUTPUT_DIR / f"{module_id}-annotated.png"
        cv2.imwrite(str(heatmap_path), heatmap)
        cv2.imwrite(str(annotated_path), annotated)
        
        # Clasificar severidad
        if diff_percent > 15:
            severity = "CRÍTICO"
            icon = "🔴"
        elif diff_percent > 5:
            severity = "MEDIO"
            icon = "🟡"
        else:
            severity = "BAJO"
            icon = "🟢"
        
        print(f"{icon} {diff_percent}% ({len(changed_regions)} regiones)")
        
        return {
            "module_id": module_id,
            "module_name": MODULES.get(module_id, "Unknown"),
            "old_filename": old_filename,
            "new_filename": new_filename,
            "diff_percent": diff_percent,
            "severity": severity,
            "status_old": old_status,
            "status_new": new_status,
            "changed_pixels": int(changed_pixels),
            "total_pixels": total_pixels,
            "changed_regions": sorted(changed_regions, key=lambda x: x["area"], reverse=True)[:5],
            "num_regions": len(changed_regions),
            "heatmap": str(heatmap_path.name),
            "annotated": str(annotated_path.name)
        }
    
    except Exception as e:
        print(f"❌ Error: {str(e)}")
        return None

def generate_html_report(results):
    """Genera reporte HTML interactivo con sliders"""
    html = """<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Comparison Report - Builders Old vs New</title>
    <style>
        * { margin: 0; padding: 0; box-sizing: border-box; }
        body {
            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            padding: 20px;
            min-height: 100vh;
        }
        .container {
            max-width: 1200px;
            margin: 0 auto;
            background: white;
            border-radius: 12px;
            overflow: hidden;
            box-shadow: 0 20px 60px rgba(0,0,0,0.3);
        }
        .header {
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            color: white;
            padding: 30px;
            text-align: center;
        }
        .header h1 { font-size: 2.5em; margin-bottom: 10px; }
        .header p { font-size: 1.1em; opacity: 0.9; }
        
        .summary {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
            gap: 20px;
            padding: 30px;
            background: #f8f9fa;
        }
        .summary-card {
            background: white;
            padding: 20px;
            border-radius: 8px;
            border-left: 4px solid #667eea;
            box-shadow: 0 2px 8px rgba(0,0,0,0.1);
        }
        .summary-card.critical { border-left-color: #dc3545; }
        .summary-card.medium { border-left-color: #fd7e14; }
        .summary-card.low { border-left-color: #28a745; }
        .summary-card h3 { margin-bottom: 10px; color: #666; font-size: 0.9em; }
        .summary-card .number { font-size: 2em; font-weight: bold; color: #333; }
        
        .modules {
            padding: 30px;
        }
        .module {
            background: #f8f9fa;
            border-radius: 8px;
            margin-bottom: 30px;
            overflow: hidden;
            border: 1px solid #e9ecef;
        }
        .module-header {
            background: white;
            padding: 20px;
            border-bottom: 2px solid #e9ecef;
            display: flex;
            justify-content: space-between;
            align-items: center;
        }
        .module-header h3 {
            margin: 0;
            color: #333;
        }
        .module-header .badge {
            padding: 6px 12px;
            border-radius: 20px;
            font-size: 0.85em;
            font-weight: bold;
            color: white;
        }
        .badge.critical { background: #dc3545; }
        .badge.medium { background: #fd7e14; }
        .badge.low { background: #28a745; }
        
        .module-body {
            padding: 20px;
        }
        .comparison-section {
            margin-bottom: 20px;
        }
        .comparison-section h4 {
            color: #666;
            margin-bottom: 12px;
            font-size: 0.95em;
        }
        
        .slider-container {
            position: relative;
            width: 100%;
            background: black;
            border-radius: 4px;
            overflow: hidden;
            margin-bottom: 15px;
        }
        .slider-img {
            display: block;
            width: 100%;
            max-height: 500px;
            object-fit: contain;
            background: #f0f0f0;
        }
        .comparison-slider {
            position: absolute;
            top: 0;
            left: 50%;
            width: 50%;
            height: 100%;
            overflow: hidden;
            cursor: col-resize;
        }
        .comparison-slider img {
            width: 200%;
            height: 100%;
            object-fit: contain;
            margin-left: -50%;
        }
        .slider-handle {
            position: absolute;
            top: 0;
            left: 50%;
            width: 3px;
            height: 100%;
            background: white;
            transform: translateX(-50%);
            cursor: col-resize;
        }
        .slider-handle::before {
            content: "⟨  ⟩";
            position: absolute;
            top: 50%;
            left: 50%;
            transform: translate(-50%, -50%);
            color: white;
            font-weight: bold;
            white-space: nowrap;
        }
        
        .regions {
            background: white;
            padding: 15px;
            border-radius: 4px;
            font-size: 0.9em;
        }
        .regions h5 {
            margin-bottom: 10px;
            color: #333;
        }
        .region-item {
            padding: 8px;
            background: #f8f9fa;
            margin: 5px 0;
            border-left: 3px solid #667eea;
            border-radius: 2px;
        }
        .region-item strong { color: #667eea; }
        
        .heatmap-grid {
            display: grid;
            grid-template-columns: 1fr 1fr;
            gap: 15px;
            margin-bottom: 15px;
        }
        .heatmap-grid img {
            width: 100%;
            border-radius: 4px;
            border: 1px solid #e9ecef;
        }
        
        .stats {
            display: grid;
            grid-template-columns: repeat(4, 1fr);
            gap: 15px;
            margin-top: 15px;
        }
        .stat {
            background: white;
            padding: 12px;
            border-radius: 4px;
            text-align: center;
            border: 1px solid #e9ecef;
        }
        .stat-label { font-size: 0.85em; color: #666; }
        .stat-value { font-size: 1.5em; font-weight: bold; color: #333; }
        
        footer {
            background: #f8f9fa;
            padding: 20px;
            text-align: center;
            color: #666;
            font-size: 0.9em;
            border-top: 1px solid #e9ecef;
        }
    </style>
</head>
<body>
    <div class="container">
        <div class="header">
            <h1>🔍 Comparison Report</h1>
            <p>Builders: Old App vs New App</p>
        </div>
        
        <div class="summary" id="summary"></div>
        <div class="modules" id="modules"></div>
        
        <footer>
            <p>Generated: """ + datetime.now().strftime("%Y-%m-%d %H:%M:%S") + (f" · sidebar izq. excluido: {IGNORE_LEFT_PX}px" if IGNORE_LEFT_PX > 0 else "") + """</p>
        </footer>
    </div>
    
    <script>
        const results = """ + json.dumps(results) + """;
        
        // Renderizar resumen
        const critical = results.filter(r => r.severity === 'CRÍTICO').length;
        const medium = results.filter(r => r.severity === 'MEDIO').length;
        const low = results.filter(r => r.severity === 'BAJO').length;
        
        document.getElementById('summary').innerHTML = `
            <div class="summary-card critical">
                <h3>🔴 Críticos</h3>
                <div class="number">${critical}</div>
            </div>
            <div class="summary-card medium">
                <h3>🟡 Medios</h3>
                <div class="number">${medium}</div>
            </div>
            <div class="summary-card low">
                <h3>🟢 Bajos</h3>
                <div class="number">${low}</div>
            </div>
            <div class="summary-card">
                <h3>📊 Promedio</h3>
                <div class="number">${(results.reduce((a, b) => a + b.diff_percent, 0) / results.length).toFixed(1)}%</div>
            </div>
        `;
        
        // Renderizar módulos
        document.getElementById('modules').innerHTML = results.map(r => `
            <div class="module">
                <div class="module-header">
                    <h3>${r.module_id} - ${r.module_name}</h3>
                    <span class="badge ${r.severity.toLowerCase()}">${r.severity} (${r.diff_percent}%)</span>
                </div>
                <div class="module-body">
                    <div class="comparison-section">
                        <h4>📸 Visual Comparison (arrastra para comparar)</h4>
                        <div class="slider-container" onmousemove="handleSlider(event, this)" ontouchstart="handleSlider(event, this)" ontouchmove="handleSlider(event, this)">
                            <img src="${r.old_filename}" class="slider-img" alt="Old">
                            <div class="comparison-slider">
                                <img src="${r.new_filename}" alt="New">
                            </div>
                            <div class="slider-handle"></div>
                        </div>
                    </div>
                    
                    <div class="heatmap-grid">
                        <div>
                            <h4>🌡️ Heat Map</h4>
                            <img src="${r.heatmap}" alt="Heat map">
                        </div>
                        <div>
                            <h4>📍 Cambios Detectados</h4>
                            <img src="${r.annotated}" alt="Annotated">
                        </div>
                    </div>
                    
                    <div class="stats">
                        <div class="stat">
                            <div class="stat-label">Píxeles Cambiados</div>
                            <div class="stat-value">${(r.changed_pixels / 1000).toFixed(1)}K</div>
                        </div>
                        <div class="stat">
                            <div class="stat-label">% Total</div>
                            <div class="stat-value">${r.diff_percent}%</div>
                        </div>
                        <div class="stat">
                            <div class="stat-label">Regiones</div>
                            <div class="stat-value">${r.num_regions}</div>
                        </div>
                        <div class="stat">
                            <div class="stat-label">Top Cambios</div>
                            <div class="stat-value">${r.changed_regions.length}</div>
                        </div>
                    </div>
                    
                    ${r.changed_regions.length > 0 ? `
                    <div class="regions">
                        <h5>🎯 Top Regiones Modificadas:</h5>
                        ${r.changed_regions.map((reg, i) => `
                            <div class="region-item">
                                <strong>Región ${i+1}:</strong> 
                                Posición (${reg.x}, ${reg.y}) | 
                                Tamaño ${reg.width}×${reg.height}px | 
                                Área ${(reg.area / 1000).toFixed(1)}K px²
                            </div>
                        `).join('')}
                    </div>
                    ` : ''}
                </div>
            </div>
        `).join('');
        
        // Slider interactivo
        function handleSlider(e, container) {
            const slider = container.querySelector('.comparison-slider');
            const handle = container.querySelector('.slider-handle');
            const rect = container.getBoundingClientRect();
            let x = e.clientX - rect.left;
            if (e.touches) x = e.touches[0].clientX - rect.left;
            
            x = Math.max(0, Math.min(x, rect.width));
            const percent = (x / rect.width) * 100;
            
            slider.style.left = percent + '%';
            handle.style.left = percent + '%';
        }
    </script>
</body>
</html>"""
    
    return html

def main():
    print("\n" + "="*60)
    print("🚀 COMPARADOR DE CAPTURAS: Builders Old vs New")
    print("="*60 + "\n")
    
    results = []
    
    for module_id in sorted(MODULES.keys()):
        old_pattern = BASE_DIR / f"vieja-{module_id}-*.png"
        new_pattern = BASE_DIR / f"nueva-{module_id}-*.png"
        
        old_files = list(BASE_DIR.glob(f"vieja-{module_id}-*.png"))
        new_files = list(BASE_DIR.glob(f"nueva-{module_id}-*.png"))
        
        # Tomar la captura MAS RECIENTE (evita mezclar con runs archivados)
        old_path = max(old_files, key=lambda p: p.stat().st_mtime) if old_files else None
        new_path = max(new_files, key=lambda p: p.stat().st_mtime) if new_files else None
        
        if not old_path or not new_path:
            print(f"  ⚠️  [{module_id}] Archivos no encontrados")
            continue
        
        result = compare_images(old_path, new_path, module_id)
        if result:
            results.append(result)
    
    print("\n" + "="*60)
    print("📊 RESULTADOS:")
    print("="*60)
    
    critical = [r for r in results if r["severity"] == "CRÍTICO"]
    medium = [r for r in results if r["severity"] == "MEDIO"]
    low = [r for r in results if r["severity"] == "BAJO"]
    
    print(f"\n🔴 CRÍTICOS ({len(critical)}):")
    for r in critical:
        print(f"   [{r['module_id']}] {r['module_name']:30} → {r['diff_percent']}%")
    
    print(f"\n🟡 MEDIOS ({len(medium)}):")
    for r in medium:
        print(f"   [{r['module_id']}] {r['module_name']:30} → {r['diff_percent']}%")
    
    print(f"\n🟢 BAJOS ({len(low)}):")
    for r in low:
        print(f"   [{r['module_id']}] {r['module_name']:30} → {r['diff_percent']}%")
    
    avg_diff = sum(r["diff_percent"] for r in results) / len(results)
    print(f"\n📈 Diferencia promedio: {avg_diff:.2f}%")
    
    # Guardar JSON
    json_path = OUTPUT_DIR / "comparison-results.json"
    with open(json_path, "w", encoding="utf-8") as f:
        json.dump(results, f, indent=2, ensure_ascii=False)
    print(f"\n✅ JSON guardado: {json_path}")
    
    # Generar HTML
    html = generate_html_report(results)
    html_path = OUTPUT_DIR / "COMPARISON-REPORT.html"
    with open(html_path, "w", encoding="utf-8") as f:
        f.write(html)
    print(f"✅ HTML guardado: {html_path}")
    
    print("\n" + "="*60)
    print(f"✨ Análisis completado: {len(results)} módulos")
    print(f"📁 Resultados en: {OUTPUT_DIR}")
    print("="*60 + "\n")

if __name__ == "__main__":
    main()
