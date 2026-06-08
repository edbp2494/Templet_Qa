#!/usr/bin/env powershell
<#!
.SYNOPSIS
    Limpia reportes y evidencias transitorias del proyecto Katalon.

.DESCRIPTION
    Aplica una politica de retencion simple:
    - Evidencia transitoria: 45 dias por defecto.
    - Evidencia importante: 180 dias por defecto.
    - Evidencia protegida de Tracking: se conserva salvo uso explicito de borrado forzado.

    Tambien permite una limpieza inicial agresiva de artefactos transitorios para reducir ruido
    sin tocar snapshots latest, discovery ni baselines visuales.

.EXAMPLE
    .\run-report-retention.ps1

.EXAMPLE
    .\run-report-retention.ps1 -AggressiveTransientCleanup

.EXAMPLE
    .\run-report-retention.ps1 -StandardRetentionDays 45 -ImportantRetentionDays 180 -WhatIf
#>

[CmdletBinding(SupportsShouldProcess = $true)]
param(
    [int]$StandardRetentionDays = 45,
    [int]$ImportantRetentionDays = 180,
    [switch]$AggressiveTransientCleanup,
    [string]$ProjectPath = (Get-Location).Path
)

$ErrorActionPreference = 'Continue'

function Remove-PathSafely {
    param(
        [Parameter(Mandatory = $true)][string]$TargetPath,
        [Parameter(Mandatory = $true)][string]$Reason
    )

    if (-not (Test-Path -LiteralPath $TargetPath)) {
        return $false
    }

    if ($PSCmdlet.ShouldProcess($TargetPath, $Reason)) {
        Remove-Item -LiteralPath $TargetPath -Recurse -Force
        return $true
    }

    return $false
}

function Remove-ChildrenOlderThan {
    param(
        [Parameter(Mandatory = $true)][string]$Root,
        [Parameter(Mandatory = $true)][datetime]$Cutoff,
        [string[]]$ExcludeNames = @()
    )

    if (-not (Test-Path -LiteralPath $Root)) {
        return 0
    }

    $removed = 0
    Get-ChildItem -LiteralPath $Root -Force | ForEach-Object {
        if ($ExcludeNames -contains $_.Name) {
            return
        }

        if ($_.LastWriteTime -lt $Cutoff) {
            if (Remove-PathSafely -TargetPath $_.FullName -Reason "Retencion vencida en $Root") {
                $script:RemovedItems += [pscustomobject]@{
                    Path = $_.FullName
                    Policy = 'retention'
                }
                $removed++
            }
        }
    }

    return $removed
}

function Remove-AllChildren {
    param(
        [Parameter(Mandatory = $true)][string]$Root,
        [string[]]$ExcludeNames = @()
    )

    if (-not (Test-Path -LiteralPath $Root)) {
        return 0
    }

    $removed = 0
    Get-ChildItem -LiteralPath $Root -Force | ForEach-Object {
        if ($ExcludeNames -contains $_.Name) {
            return
        }

        if (Remove-PathSafely -TargetPath $_.FullName -Reason "Limpieza agresiva transitoria en $Root") {
            $script:RemovedItems += [pscustomobject]@{
                Path = $_.FullName
                Policy = 'aggressive'
            }
            $removed++
        }
    }

    return $removed
}

$reportsRoot = Join-Path $ProjectPath 'Reports'
if (-not (Test-Path -LiteralPath $reportsRoot)) {
    Write-Host "No existe carpeta Reports en: $reportsRoot" -ForegroundColor Yellow
    exit 0
}

$standardCutoff = (Get-Date).AddDays(-$StandardRetentionDays)
$importantCutoff = (Get-Date).AddDays(-$ImportantRetentionDays)
$script:RemovedItems = @()

Write-Host "[REPORT RETENTION]" -ForegroundColor Cyan
Write-Host "  Proyecto: $ProjectPath"
Write-Host "  Reports:  $reportsRoot"
Write-Host "  Retencion estandar: $StandardRetentionDays dias"
Write-Host "  Retencion importante: $ImportantRetentionDays dias"
Write-Host "  Limpieza agresiva: $AggressiveTransientCleanup"
Write-Host ""

$timestampFolderPattern = '^20\d{6}_\d{6}$'
Get-ChildItem -LiteralPath $reportsRoot -Directory -Force |
    Where-Object { $_.Name -match $timestampFolderPattern } |
    ForEach-Object {
        if ($AggressiveTransientCleanup -or $_.LastWriteTime -lt $standardCutoff) {
            $reason = if ($AggressiveTransientCleanup) { 'Limpieza agresiva de reportes por corrida' } else { 'Retencion vencida de reportes por corrida' }
            if (Remove-PathSafely -TargetPath $_.FullName -Reason $reason) {
                $script:RemovedItems += [pscustomobject]@{
                    Path = $_.FullName
                    Policy = if ($AggressiveTransientCleanup) { 'aggressive' } else { 'retention' }
                }
            }
        }
    }

$transientRoots = @(
    (Join-Path $reportsRoot 'Screenshots'),
    (Join-Path $reportsRoot 'Smoke-Summary'),
    (Join-Path $reportsRoot 'Self-healing'),
    (Join-Path $reportsRoot 'Visual\Diffs'),
    (Join-Path $reportsRoot 'Visual\Reports'),
    (Join-Path $reportsRoot 'Visual\Screenshots'),
    (Join-Path $reportsRoot 'Tracking\snapshots\history')
)

foreach ($root in $transientRoots) {
    if ($AggressiveTransientCleanup) {
        [void](Remove-AllChildren -Root $root)
    } else {
        [void](Remove-ChildrenOlderThan -Root $root -Cutoff $standardCutoff)
    }
}

$importantRoots = @(
    (Join-Path $reportsRoot 'Important'),
    (Join-Path $reportsRoot 'Tracking\important'),
    (Join-Path $reportsRoot 'asana_tickets'),
    (Join-Path $reportsRoot 'Visual\Baselines')
)

foreach ($root in $importantRoots) {
    [void](Remove-ChildrenOlderThan -Root $root -Cutoff $importantCutoff)
}

$protectedTrackingFiles = @(
    (Join-Path $reportsRoot 'Tracking\snapshots\tracking_all_latest.json'),
    (Join-Path $reportsRoot 'Tracking\snapshots\tracking_blueprint_latest.json'),
    (Join-Path $reportsRoot 'Tracking\snapshots\tracking_task_creation_latest.json'),
    (Join-Path $reportsRoot 'Tracking\snapshots\tracking_login_latest.json')
)

$trackingDiscoveryRoot = Join-Path $reportsRoot 'Tracking\discovery'
$protectedTrackingFiles += Get-ChildItem -LiteralPath $trackingDiscoveryRoot -File -ErrorAction SilentlyContinue | Select-Object -ExpandProperty FullName

Write-Host "[PROTEGIDO]" -ForegroundColor Green
foreach ($path in $protectedTrackingFiles | Where-Object { Test-Path -LiteralPath $_ }) {
    Write-Host "  $path"
}

Write-Host ""
Write-Host "[RESUMEN]" -ForegroundColor Yellow
Write-Host "  Elementos eliminados: $($script:RemovedItems.Count)"

if ($script:RemovedItems.Count -gt 0) {
    $grouped = $script:RemovedItems | Group-Object Policy
    foreach ($group in $grouped) {
        Write-Host "  - $($group.Name): $($group.Count)"
    }
}

Write-Host ""
Write-Host "Sugerencia de uso recurrente: ejecutar este script cada 45 dias." -ForegroundColor Cyan
Write-Host "Para conservar evidencia clave por mas tiempo, moverla a Reports\\Important o Reports\\Tracking\\important." -ForegroundColor Cyan