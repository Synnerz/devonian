package com.github.synnerz.devonian.api

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.await
import kotlinx.coroutines.withContext
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
    val ioScope = CoroutineScope(Dispatchers.IO)

    suspend fun get(
        url: String
    ): String = withContext(Dispatchers.IO) {
        val request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .GET()
            .build()

        val response = httpClient.sendAsync(request, BodyHandlers.ofString()).await()
        if (response.statusCode() !in 200..299)
            throw Exception("WebRequests #GET Error ${response.statusCode()}: ${response.body()}")

        response.body()
    }

    suspend fun post(
        url: String,
        body: String,
        contentType: String = "application/json"
    ): String = withContext(Dispatchers.IO) {
        val request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .headers("Content-Type", contentType)
            .POST(BodyPublishers.ofString(body))
            .build()

        val response = httpClient.sendAsync(request, BodyHandlers.ofString()).await()
        if (response.statusCode() !in 200..299)
            throw Exception("WebRequests #POST Error ${response.statusCode()}: ${response.body()}")

        response.body()
    }
}