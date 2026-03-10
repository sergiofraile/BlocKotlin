package dev.bloc.sample.examples.suvs

import dev.bloc.sample.examples.suvs.models.SuvActiveDirectoryUser
import dev.bloc.sample.examples.suvs.models.SuvErrorResponse
import dev.bloc.sample.examples.suvs.models.SuvInstance
import dev.bloc.sample.examples.suvs.models.SuvifyError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class SUVNetworkService {

    private val json = Json { ignoreUnknownKeys = true }

    private companion object {
        const val AUTH_BASE_URL = "https://narada.ark.gowday.com"
        const val SUV_BASE_URL  = "https://suv-api.ark.gowday.com/instances"
        const val CLIENT_ID     = "SUVify"
        const val TIMEOUT_MS    = 30_000
    }

    // ── Authentication ────────────────────────────────────────────────────────

    suspend fun login(
        username: String,
        password: String,
        clientKey: String,
    ): SuvActiveDirectoryUser = withContext(Dispatchers.IO) {
        val url = buildUrl(
            base = "$AUTH_BASE_URL/token",
            params = mapOf("clientId" to CLIENT_ID, "clientKey" to clientKey),
        )
        val body = json.encodeToString(mapOf("username" to username, "password" to password))
        performRequest(url, method = "POST", body = body)
    }

    // ── Instance operations ───────────────────────────────────────────────────

    suspend fun fetchInstances(
        username: String,
        authToken: String,
    ): List<SuvInstance> = withContext(Dispatchers.IO) {
        val url = buildUrl(
            base = "$SUV_BASE_URL/users/${encode(username)}",
            params = mapOf("clientId" to CLIENT_ID, "authtoken" to authToken),
        )
        performRequest(url, method = "GET")
    }

    suspend fun extendInstance(
        instanceId: String,
        newStopTime: String,
        authToken: String,
    ): SuvInstance = withContext(Dispatchers.IO) {
        val url = buildUrl(
            base = "$SUV_BASE_URL/${encode(instanceId)}",
            params = mapOf("clientId" to CLIENT_ID, "authtoken" to authToken),
        )
        val body = json.encodeToString(mapOf("wdAutoStopTime" to newStopTime))
        performRequest(url, method = "PUT", body = body)
    }

    // ── Generic request ───────────────────────────────────────────────────────

    private inline fun <reified T> performRequest(
        url: URL,
        method: String,
        body: String? = null,
    ): T {
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod    = method
            connectTimeout   = TIMEOUT_MS
            readTimeout      = TIMEOUT_MS
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            setRequestProperty("Accept", "application/json; charset=utf-8")
            if (body != null) {
                doOutput = true
                outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            }
        }

        return try {
            val statusCode   = conn.responseCode
            val responseText = when {
                statusCode in 200..299 -> conn.inputStream.bufferedReader().readText()
                else                   -> conn.errorStream?.bufferedReader()?.readText() ?: ""
            }

            when (statusCode) {
                401  -> {
                    val err = runCatching { json.decodeFromString<SuvErrorResponse>(responseText) }
                        .getOrDefault(SuvErrorResponse("Unauthorized", "Invalid or expired token", ""))
                    throw SuvifyError.Unauthorized(err)
                }
                404  -> throw SuvifyError.UserNotFound
                in 200..299 -> {}
                else -> {
                    val err = runCatching { json.decodeFromString<SuvErrorResponse>(responseText) }
                        .getOrNull()
                    throw if (err != null) SuvifyError.ErrorResponse(err) else SuvifyError.SomethingWentWrong
                }
            }

            runCatching { json.decodeFromString<T>(responseText) }
                .getOrElse { throw SuvifyError.DecodingError(it.message ?: "Unknown parse error") }
        } finally {
            conn.disconnect()
        }
    }

    // ── URL helpers ───────────────────────────────────────────────────────────

    private fun buildUrl(base: String, params: Map<String, String>): URL {
        val query = params.entries.joinToString("&") { (k, v) -> "$k=${encode(v)}" }
        return URL("$base?$query")
    }

    private fun encode(value: String): String =
        URLEncoder.encode(value, "UTF-8")
}
