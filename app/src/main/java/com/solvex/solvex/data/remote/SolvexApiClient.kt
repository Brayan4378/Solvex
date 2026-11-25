package com.solvex.solvex.data.remote

import android.content.Context
import com.solvex.solvex.BuildConfig
import com.solvex.solvex.session.SessionManager
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object SolvexApiClient {
    @Volatile private var apiInstance: SolvexApi? = null

    fun get(context: Context): SolvexApi = apiInstance ?: synchronized(this) {
        apiInstance ?: run {
            val sessionManager = SessionManager(context.applicationContext)
            val client = OkHttpClient.Builder()
                .addInterceptor { chain ->
                    val request = chain.request().newBuilder().apply {
                        sessionManager.obtenerToken().takeIf { it.isNotBlank() }?.let {
                            header("Authorization", "Bearer $it")
                        }
                    }.build()
                    chain.proceed(request)
                }.build()
            Retrofit.Builder()
                .baseUrl(BuildConfig.API_BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(SolvexApi::class.java)
                .also { apiInstance = it }
        }
    }
}
