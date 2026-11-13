package cl.belmontespa.myapplication.repository.api

import androidx.annotation.VisibleForTesting
import cl.belmontespa.myapplication.BuildConfig
import cl.belmontespa.myapplication.repository.interceptor.AuthInterceptor
import cl.belmontespa.myapplication.session.SesionStore
import java.util.concurrent.TimeUnit
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

object ApiClient {
    val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    private var actual: Pair<SesionStore, BelmonteApi>? = null

    @Synchronized
    fun de(sesiones: SesionStore): BelmonteApi {
        actual?.takeIf { it.first === sesiones }?.let { return it.second }
        return crear(sesiones, BuildConfig.API_BASE_URL).also { actual = sesiones to it }
    }

    @VisibleForTesting
    fun crear(sesiones: SesionStore, urlBase: String): BelmonteApi {
        val cliente = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .retryOnConnectionFailure(false)
            .addInterceptor(AuthInterceptor(sesiones))
            .apply {
                if (BuildConfig.DEBUG) {
                    addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
                }
            }
            .build()
        return Retrofit.Builder()
            .baseUrl(urlBase + "api/v1/")
            .client(cliente)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(BelmonteApi::class.java)
    }
}
