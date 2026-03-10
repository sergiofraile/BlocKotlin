package dev.bloc.sample.examples.lorcana

import dev.bloc.sample.examples.lorcana.models.LorcanaCard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

interface LorcanaNetworkServiceProtocol {
    suspend fun fetchAllCards(page: Int = 1, pageSize: Int = 100): List<LorcanaCard>
    suspend fun searchCards(query: String, page: Int = 1, pageSize: Int = 100): List<LorcanaCard>
    suspend fun fetchCardsFromSet(setName: String, page: Int = 1, pageSize: Int = 200): List<LorcanaCard>
}

class LorcanaNetworkService : LorcanaNetworkServiceProtocol {

    private val json = Json { ignoreUnknownKeys = true }
    private val baseUrl = "https://api.lorcana-api.com"

    override suspend fun fetchAllCards(page: Int, pageSize: Int): List<LorcanaCard> =
        withContext(Dispatchers.IO) {
            val url = URL("$baseUrl/cards/all?page=$page&pagesize=$pageSize")
            getJson(url)
        }

    override suspend fun searchCards(query: String, page: Int, pageSize: Int): List<LorcanaCard> =
        withContext(Dispatchers.IO) {
            val encoded = URLEncoder.encode("name~$query", "UTF-8")
            val url = URL("$baseUrl/cards/fetch?search=$encoded&page=$page&pagesize=$pageSize")
            getJson(url)
        }

    override suspend fun fetchCardsFromSet(setName: String, page: Int, pageSize: Int): List<LorcanaCard> =
        withContext(Dispatchers.IO) {
            val encoded = URLEncoder.encode("set_name=$setName", "UTF-8")
            val url = URL("$baseUrl/cards/fetch?search=$encoded&page=$page&pagesize=$pageSize")
            getJson(url)
        }

    private fun getJson(url: URL): List<LorcanaCard> {
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.setRequestProperty("Accept", "application/json")
        connection.connectTimeout = 15_000
        connection.readTimeout    = 15_000
        return try {
            val text = connection.inputStream.bufferedReader().readText()
            json.decodeFromString<List<LorcanaCard>>(text)
        } finally {
            connection.disconnect()
        }
    }
}
