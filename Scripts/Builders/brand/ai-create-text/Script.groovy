// ─────────────────────────────────────────────────────────────────────────────
// TC: TC-BUILDERS-BRAND-AICREATE-TEXT-001
// Plataforma: Builders | Área: brand
// Descripción: Valida "Create TEXT" en Builders /brand (ESCALA): abre drafts y por cada metodo de input (Uploading source files / Sharing website links / Filling out a form) selecciona el radio, entra, captura el paso, hace scroll, da "Go Back" y valida el retorno a la pantalla de inputs. Cierra navegador.
// Suites: Platforms/Builders/Brand/AI-Create-PerType
// Precondiciones: credenciales MS en Include/config/templet-credentials.properties
// ─────────────────────────────────────────────────────────────────────────────
// TC-BUILDERS-BRAND-AICREATE-TEXT-001
// Valida el flujo "Create TEXT" -> pantalla "We need some inputs first!" en drafts:
// para cada metodo de input (Uploading / Sharing links / Filling form) selecciona
// el radio, entra (continuar), captura el paso, hace scroll, "Go Back" y valida que
// vuelva la pantalla anterior. Cierra el navegador al final.
CustomKeywords.'BrandAiKeywords.validateCreateInputMethods'([
	caseId: 'TC-BUILDERS-BRAND-AICREATE-TEXT-001',
	type  : 'text',
	label : 'Create TEXT'
])
