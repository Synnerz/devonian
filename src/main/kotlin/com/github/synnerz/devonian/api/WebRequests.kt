package com.github.synnerz.devonian.api

import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpRequest.BodyPublishers
import java.net.http.HttpResponse.BodyHandlers
import java.time.Duration

object WebRequests {
    private val httpClient = HttpClient
        .newBuilder()
        .connectTimeout(Duration.ofSeconds(20))
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build()
    val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO + CoroutineName("Devonian"))

    suspend fun get(
        url: String
    ): String {
        val request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .headers("User-Agent", "Mozilla/5.0 (Devonian)")
            .GET()
            .build()

        val response = httpClient.sendAsync(request, BodyHandlers.ofString()).await()
        if (response.statusCode() !in 200..299) {
            println("Devonian\$WebRequest(url=\"$url\", mode=\"GET\", status=\"${response.statusCode()}\", body=\"${response.body()}\")")
            throw Exception("WebRequests #GET Error ${response.statusCode()}: ${response.body()}")
        }

        return response.body()
    }

    suspend fun post(
        url: String,
        body: String,
        contentType: String = "application/json"
    ): String {
        val request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .headers("User-Agent", "Mozilla/5.0 (Devonian)")
            .headers("Content-Type", contentType)
            .POST(BodyPublishers.ofString(body))
            .build()

        val response = httpClient.sendAsync(request, BodyHandlers.ofString()).await()
        if (response.statusCode() !in 200..299) {
            println("Devonian\$WebRequest(url=\"$url\", mode=\"POST\", status=\"${response.statusCode()}\", body=\"${response.body()}\")")
            throw Exception("WebRequests #POST Error ${response.statusCode()}: ${response.body()}")
        }

        return response.body()
    }

    /**
     * - Launches a new context with the specified name
     */
    fun withName(name: String, block: suspend CoroutineScope.() -> Unit) = ioScope.launch(CoroutineName(name)) {
        try {
            block()
        } catch (e: Exception) {
            println("Devonian\$WebRequest Error - $name")
            e.printStackTrace()
        }
    }

    fun withName(name: String, block: suspend CoroutineScope.() -> Unit, catch: () -> Unit) = ioScope.launch(CoroutineName(name)) {
        try {
            block()
        } catch (e: Exception) {
            println("Devonian\$WebRequest Error - $name")
            e.printStackTrace()
            catch()
        }
    }
}