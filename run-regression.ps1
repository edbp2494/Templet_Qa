#!/usr/bin/env powershell
<#
.SYNOPSIS
    Script para ejecutar la colección canónica Email-Sheets-Decks
    
.DESCRIPTION
    Ejecuta automáticamente la colección del grupo Email-Sheets-Decks
    (Email, Sheets y Decks) para smoke, comparación, filtros y regresión full.

.EXAMPLE
    .\run-regression.ps1
    
.NOTES
    Requiere: Katalon Studio instalado y proyecto Sheet abierto
#>

$ErrorActionPreference = "Continue"

# Detectar ruta del proyecto
$projectPath = Get-Location
$kataloneEXE = "C:\Program Files\Katalon Studio\katalon.exe"
$testSuiteCollection = "$projectPath\Test Suites\Collection-Email-Sheets-Decks.tsc"

Write-Host "╔════════════════════════════════════════════════════════════╗" -ForegroundColor Cyan
Write-Host "║  COLECCIÓN CANÓNICA EMAIL-SHEETS-DECKS                   ║" -ForegroundColor Cyan
Write-Host "╚════════════════════════════════════════════════════════════╝" -ForegroundColor Cyan
Write-Host ""

# Validación de requisitos
Write-Host "[PRE-FLIGHT CHECK]" -ForegroundColor Yellow
Write-Host "  ✓ Proyecto: $projectPath"
Write-Host "  ✓ Katalon: $(if (Test-Path $kataloneEXE) { 'ENCONTRADO' } else { 'NO ENCONTRADO' })"
Write-Host "  ✓ Suite: $(if (Test-Path $testSuiteCollection) { 'ENCONTRADO' } else { 'NO ENCONTRADO' })"
Write-Host ""

if (-not (Test-Path $kataloneEXE)) {
    Write-Host "ERROR: Katalon Studio no encontrado en: $kataloneEXE" -ForegroundColor Red
    exit 1
}

if (-not (Test-Path $testSuiteCollection)) {
    Write-Host "ERROR: Suite no encontrada en: $testSuiteCollection" -ForegroundColor Red
    exit 1
}

# Crear carpeta de reportes
$reportsFolder = "$projectPath\Reports"
$capturePath = "$reportsFolder\CapturedObjectSpecs"
if (-not (Test-Path $capturePath)) {
    New-Item -ItemType Directory -Force -Path $capturePath | Out-Null
    Write-Host "[SETUP] Carpeta de reportes creada" -ForegroundColor Green
}

Write-Host ""
Write-Host "[INICIANDO EJECUCIÓN]" -ForegroundColor Green
Write-Host "  Colección: Collection-Email-Sheets-Decks"
Write-Host "  Grupo: Email + Sheets + Decks"
Write-Host "  Incluye: smoke, compare TEST/PROD, filtros y regresión full"
Write-Host "  Mode: SEQUENTIAL (ReuseDriver=true)"
Write-Host "  Timestamp: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')"
Write-Host ""
Write-Host "  ⏱️  TIEMPO ESTIMADO: variable segun ambiente y datos"
Write-Host "  📊 Reportes en: $reportsFolder"
Write-Host ""

# Ejecutar suite
Write-Host "────────────────────────────────────────────────────────────" -ForegroundColor Cyan

$stopwatch = [System.Diagnostics.Stopwatch]::StartNew()

& $kataloneEXE `
    -noSplash `
    -runMode=console `
    "-testSuiteCollectionPath=$testSuiteCollection" `
    "-projectPath=$projectPath" `
    "-reportFolder=$reportsFolder" `
    -browserType=Chrome `
    --config `
    -webui.autoUpdateXpathAndCssLocators=true `
    -webui.engagement=true

$stopwatch.Stop()

Write-Host "────────────────────────────────────────────────────────────" -ForegroundColor Cyan
Write-Host ""

if ($LASTEXITCODE -eq 0) {
    Write-Host "✅ EJECUCIÓN COMPLETADA CON ÉXITO" -ForegroundColor Green
} else {
    Write-Host "⚠️  EJECUCIÓN COMPLETADA CON ERRORES (Exit Code: $LASTEXITCODE)" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "[RESUMEN]" -ForegroundColor Yellow
Write-Host "  Duración total: $([math]::Round($stopwatch.Elapsed.TotalSeconds)) segundos"
Write-Host "  Reporte HTML: $reportsFolder\TIMESTAMP\index.html"
Write-Host "  Objetos capturados: $capturePath\*.json"
Write-Host "  Screenshots: $reportsFolder\Screenshots\functional-smoke\TC-*.png"
Write-Host ""

Write-Host "[PRÓXIMOS PASOS]" -ForegroundColor Cyan
Write-Host "  1. Abre el reporte HTML para ver resultados por plataforma"
Write-Host "  2. Verifica logs buscando 'subwindowOpened=true/false'"
Write-Host "  3. Descarga objetos capturados para crear nuevos test cases"
Write-Host ""

Write-Host "Presiona ENTER para cerrar..." -ForegroundColor Gray
Read-Host
