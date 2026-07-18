import com.kms.katalon.core.annotation.Keyword
import com.kms.katalon.core.util.KeywordUtil
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import org.apache.http.client.methods.*
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.client.HttpClients
import org.apache.http.util.EntityUtils
import com.kms.katalon.core.webui.driver.DriverFactory
import org.openqa.selenium.Cookie

/**
 * Keywords de API JSON para regresion del Builder SaaS.
 * Usa Apache HttpClient (bundled en Katalon) para soportar PATCH.
 */
public class ApiKeywords {

	/** Cabeceras extra que se inyectan en cada callJson (ej. Cookie de sesion MS). */
	static Map defaultHeaders = [:]

	static HttpRequestBase buildRequest(String method, String url) {
		switch (method.toUpperCase()) {
			case 'GET':    return new HttpGet(url)
			case 'POST':   return new HttpPost(url)
			case 'PATCH':  return new HttpPatch(url)
			case 'PUT':    return new HttpPut(url)
			case 'DELETE': return new HttpDelete(url)
			default: throw new IllegalArgumentException("Metodo no soportado: ${method}")
		}
	}

	/**
	 * Ejecuta una llamada JSON. Retorna [status:int, json:Object, raw:String].
	 */
	@Keyword
	static Map callJson(String method, String url, Map body = null) {
		CloseableHttpClient client = HttpClients.createDefault()
		try {
			HttpRequestBase req = buildRequest(method, url)
			req.setHeader('Content-Type', 'application/json')
			defaultHeaders.each { k, v -> req.setHeader(k.toString(), v.toString()) }
			if (body != null && req instanceof HttpEntityEnclosingRequestBase) {
				((HttpEntityEnclosingRequestBase) req).setEntity(new StringEntity(JsonOutput.toJson(body), 'UTF-8'))
			}
			def resp = client.execute(req)
			int status = resp.getStatusLine().getStatusCode()
			String raw = resp.getEntity() != null ? EntityUtils.toString(resp.getEntity(), 'UTF-8') : ''
			Object json = null
			try { json = new JsonSlurper().parseText(raw) } catch (Exception ignored) { }
			KeywordUtil.logInfo("[API] ${method} ${url} -> ${status}")
			return [status: status, json: json, raw: raw]
		} finally {
			client.close()
		}
	}

	/**
	 * Acumula failure si el status real no esta en la lista esperada.
	 */
	@Keyword
	static void assertStatus(List failures, String label, Map resp, List expected) {
		if (!expected.contains(resp.status)) {
			failures.add("[API][${label}] status=${resp.status} esperado=${expected} body=${(resp.raw ?: '').take(200)}")
		} else {
			KeywordUtil.logInfo("[API][${label}] OK status=${resp.status}")
		}
	}

	/**
	 * Borra un recurso si existe id (limpieza best-effort, no falla el test).
	 */
	@Keyword
	static void cleanupResource(String url) {
		try { callJson('DELETE', url) } catch (Exception e) {
			KeywordUtil.markWarning("[CLEANUP] No se pudo borrar ${url}: ${e.message}")
		}
	}

	/**
	 * Adopta las cookies del navegador actual (incluye HttpOnly de sesion MS) en HttpClient.
	 * Solo captura cookies del dominio actual: llamar estando EN la app tras el login.
	 * Retorna el header Cookie construido.
	 */
	@Keyword
	static String useBrowserSession() {
		Set<Cookie> cookies = DriverFactory.getWebDriver().manage().getCookies()
		String cookieHeader = cookies.collect { "${it.getName()}=${it.getValue()}" }.join('; ')
		defaultHeaders['Cookie'] = cookieHeader
		KeywordUtil.logInfo("[API] Sesion de navegador adoptada: ${cookies.size()} cookies")
		return cookieHeader
	}

	/** Elimina la cookie de sesion de defaultHeaders (fuerza llamadas anonimas). */
	@Keyword
	static void clearSession() {
		defaultHeaders.remove('Cookie')
		KeywordUtil.logInfo('[API] Sesion limpiada de defaultHeaders (modo anonimo)')
	}
}
