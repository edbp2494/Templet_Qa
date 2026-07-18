#!/usr/bin/env python3
# -*- coding: utf-8 -*-

import os
import cv2
import numpy as np
from pathlib import Path
import json

def detect_404_or_error(image_path):
    """Detecta si una imagen contiene un 404 o página de error."""
    try:
        img = cv2.imread(image_path)
        if img is None:
            return True, "No se puede leer imagen"
        
        # Convertir a escala de grises
        gray = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)
        
        # Detectar características de página de error:
        # 1. Mucho texto blanco/gris (fondo limpio)
        # 2. Pocos píxeles de contenido
        
        height, width = gray.shape
        
        # Contar píxeles claros (200-255)
        bright_pixels = np.sum((gray > 200).astype(int))
        bright_ratio = bright_pixels / (height * width)
        
        # Si >40% es blanco puro, probablemente es error page
        if bright_ratio > 0.4:
            return True, f"Página mayormente blanca ({bright_ratio*100:.1f}%)"
        
        # Analizar varianza (páginas de error tienen baja varianza)
        laplacian_var = cv2.Laplacian(gray, cv2.CV_64F).var()
        if laplacian_var < 50:
            return True, f"Baja varianza de contenido ({laplacian_var:.1f})"
        
        return False, "OK"
    
    except Exception as e:
        return True, str(e)

def analyze_modules():
    """Analiza todas las imágenes y detecta errores."""
    compare_dir = Path("C:/Users/e2494/Katalon Studio/Templet/Reports/Builders/Compare")
    
    results = {
        "timestamp": str(__import__('datetime').datetime.now()),
        "modules": {}
    }
    
    for i in range(1, 17):
        module_id = f"{i:02d}"
        vieja_file = f"vieja-{module_id}-*.png"
        nueva_file = f"nueva-{module_id}-*.png"
        
        # Buscar archivos
        vieja_files = list(compare_dir.glob(f"vieja-{module_id}-*.png"))
        nueva_files = list(compare_dir.glob(f"nueva-{module_id}-*.png"))
        
        vieja_path = str(vieja_files[0]) if vieja_files else None
        nueva_path = str(nueva_files[0]) if nueva_files else None
        
        vieja_error = False
        nueva_error = False
        vieja_reason = ""
        nueva_reason = ""
        
        if vieja_path:
            vieja_error, vieja_reason = detect_404_or_error(vieja_path)
        
        if nueva_path:
            nueva_error, nueva_reason = detect_404_or_error(nueva_path)
        
        results["modules"][module_id] = {
            "vieja": {
                "path": vieja_path,
                "error": vieja_error,
                "reason": vieja_reason
            },
            "nueva": {
                "path": nueva_path,
                "error": nueva_error,
                "reason": nueva_reason
            }
        }
        
        print(f"[{module_id}]")
        status_vieja = "ERROR" if vieja_error else "OK"
        status_nueva = "ERROR" if nueva_error else "OK"
        print(f"  VIEJA: {status_vieja} - {vieja_reason}")
        print(f"  NUEVA: {status_nueva} - {nueva_reason}")
    
    # Guardar resultados
    output_path = compare_dir / "validation_report.json"
    with open(output_path, 'w') as f:
        json.dump(results, f, indent=2)
    
    print(f"\n📊 Reporte guardado: {output_path}")

if __name__ == "__main__":
    analyze_modules()
