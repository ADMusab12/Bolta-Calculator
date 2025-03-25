package com.example.boltacalculator

import android.util.Log
import com.example.boltacalculator.Global.GEMINI_KEY
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class OpenAIService {
    private val apiKey = GEMINI_KEY

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    //todo for generate content
    suspend fun generateContent(prompt: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val requestBody = JsonObject().apply {
                add("contents", JsonObject().apply {
                    add("parts", JsonObject().apply {
                        addProperty("text", prompt)
                    })
                })
            }

            val jsonBody = requestBody.toString().toRequestBody("application/json".toMediaTypeOrNull())

            val request = Request.Builder()
                .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$apiKey")
                .addHeader("Content-Type", "application/json")
                .post(jsonBody)
                .build()

            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                val responseBody = response.body?.string() ?: "Empty Response Body"
                Log.d("checkobjectdetection", "Response: $responseBody")

                val jsonResponse = Gson().fromJson(responseBody, JsonObject::class.java)
                val candidates = jsonResponse.getAsJsonArray("candidates")
                val firstCandidate = candidates[0].asJsonObject
                val content = firstCandidate.getAsJsonObject("content")
                val parts = content.getAsJsonArray("parts")
                val text = parts[0].asJsonObject.get("text").asString

                Result.success(text)
            } else {
                val errorBody = response.body?.string() ?: "Unknown error"
                Log.e("checkobjectdetection", "Error: ${response.code} - $errorBody")
                Result.failure(Exception("API Error: ${response.code} - $errorBody"))
            }
        } catch (e: Exception) {
            Log.e("checkobjectdetection", "Exception: ${e.message}", e)
            Result.failure(e)
        }
    }
}
