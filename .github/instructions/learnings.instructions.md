---
description: Reglas aprendidas en sesiones anteriores para evitar errores recurrentes en este proyecto Katalon
---

# Learnings - Katalon Studio / Sheets Project

## CRITICO: Nunca editar scripts en el worktree

- El CWD del agente es `Sheet.worktrees\agents-*\` pero Katalon ejecuta desde `Sheet\`
- **Siempre editar en:** `C:\Users\e2494\Katalon Studio\Templet\Editores\Sheet\`
- **Nunca editar en:** `C:\Users\e2494\Katalon Studio\Templet\Editores\Sheet.worktrees\*`

## CRITICO: Nunca usar Set-Content para archivos .groovy

- PowerShell `Set-Content -Encoding UTF8` agrega BOM (EF BB BF) al inicio
- Groovy NO soporta BOM: falla con `Unexpected character` en linea 1
- **Siempre usar:**
  `[System.IO.File]::WriteAllText($path, $content, [System.Text.UTF8Encoding]::new($false))`
- Verificar: primeros bytes deben ser 105 109 112 (imp), NO 239 187 191

## NO subir nada a Git

- Usuario logueado con cuenta de trabajo (Rappi) - proyecto personal
- Nunca hacer git add, commit, push en este repo