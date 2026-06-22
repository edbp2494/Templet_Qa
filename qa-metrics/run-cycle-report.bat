@echo off
REM Genera el dashboard de ciclo QA y lo abre. Uso:
REM   run-cycle-report.bat            -> genera y abre el HTML
REM   run-cycle-report.bat push       -> ademas hace git add/commit/push del reporte
setlocal
cd /d "%~dp0.."
echo [QA-CYCLE] Generando reporte...
python qa-metrics\generate_cycle_report.py --repo "%cd%"
if errorlevel 1 py -3 qa-metrics\generate_cycle_report.py --repo "%cd%"
if /I "%~1"=="push" (
  echo [QA-CYCLE] Commit y push del reporte...
  git add docs/qa-cycles
  git commit -m "chore(qa): reporte de ciclo %date%"
  git push
)
echo [QA-CYCLE] Abriendo dashboard...
start "" "docs\qa-cycles\latest.html"
endlocal
