package de.ashaysurya.myapplication.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitInstance {

    // The public URL for your deployed backend on Render
    private const val BASE_URL = "https://backend-for-pos.onrender.com/"

    // A logger to see network request details in Logcat (for debugging)
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    // A custom client that includes the logger and longer timeouts
    private val client = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(60, TimeUnit.SECONDS) // NEW: Wait 60 seconds to connect
        .readTimeout(60, TimeUnit.SECONDS)    // NEW: Wait 60 seconds for a response
        .writeTimeout(60, TimeUnit.SECONDS)   // NEW: Wait 60 seconds to send data
        .build()

    // The single Retrofit instance
    private val retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    // The public object that the rest of the app will use to make API calls
    val api: ApiService by lazy {
        retrofit.create(ApiService::class.java)
    }
}