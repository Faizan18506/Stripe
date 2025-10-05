package com.example.stripe

// ===== StripeService.kt =====
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody

import java.io.IOException

class StripeService(private val secretKey: String) {

    private val client = OkHttpClient()
    private val gson = Gson()
    private val baseUrl = "https://api.stripe.com/v1"

    suspend fun createPaymentIntent(amount: Int): Result<String> = withContext(Dispatchers.IO) {
        try {
            // Create payment intent with amount in cents (e.g., 2000 = $20.00)
            val formBody = FormBody.Builder()
                .add("amount", amount.toString())
                .add("currency", "usd")
                .add("payment_method_types[]", "card")
                .build()

            val request = Request.Builder()
                .url("$baseUrl/payment_intents")
                .addHeader("Authorization", "Bearer $secretKey")
                .post(formBody)
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()

            if (response.isSuccessful && responseBody != null) {
                val paymentIntent = gson.fromJson(responseBody, PaymentIntentResponse::class.java)
                Result.success(paymentIntent.client_secret)
            } else {
                Result.failure(Exception("Failed to create payment intent: $responseBody"))
            }
        } catch (e: IOException) {
            Result.failure(e)
        }
    }

    // Data class for parsing response
    data class PaymentIntentResponse(
        val id: String,
        val client_secret: String,
        val amount: Int,
        val currency: String,
        val status: String
    )
}
