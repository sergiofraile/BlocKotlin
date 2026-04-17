package dev.bloc.sample.examples.formulaone

import dev.bloc.sample.examples.formulaone.models.DriverChampionship
import dev.bloc.sample.examples.formulaone.models.DriversChampionshipResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL

class FormulaOneNetworkService {

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun fetchDriversChampionship(): List<DriverChampionship> = withContext(Dispatchers.IO) {
        val url = URL("https://f1api.dev/api/current/drivers-championship")
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.setRequestProperty("Accept", "application/json")
        connection.connectTimeout = 10_000
        connection.readTimeout    = 10_000
        try {
            val responseText = connection.inputStream.bufferedReader().readText()
            json.decodeFromString<DriversChampionshipResponse>(responseText).driversChampionship
        } finally {
            connection.disconnect()
        }
    }
}
