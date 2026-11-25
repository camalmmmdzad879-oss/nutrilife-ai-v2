package com.example.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class OpenFoodFactsResponse(
    val status: Int = 0,
    val product: OpenFoodFactsProduct? = null
)

@JsonClass(generateAdapter = true)
data class OpenFoodFactsProduct(
    @Json(name = "product_name") val productName: String? = null,
    @Json(name = "brands") val brands: String? = null,
    @Json(name = "serving_size") val servingSize: String? = null,
    @Json(name = "nutriments") val nutriments: OpenFoodFactsNutriments? = null
)

@JsonClass(generateAdapter = true)
data class OpenFoodFactsNutriments(
    @Json(name = "energy-kcal_100g") val energyKcal100g: Double? = null,
    @Json(name = "proteins_100g") val proteins100g: Double? = null,
    @Json(name = "carbohydrates_100g") val carbohydrates100g: Double? = null,
    @Json(name = "fat_100g") val fat100g: Double? = null,
    @Json(name = "fiber_100g") val fiber100g: Double? = null,
    @Json(name = "sugars_100g") val sugars100g: Double? = null,
    @Json(name = "sodium_100g") val sodiumG100g: Double? = null,
    @Json(name = "calcium_100g") val calciumG100g: Double? = null,
    @Json(name = "iron_100g") val ironG100g: Double? = null
)

interface OpenFoodFactsApi {
    @GET("api/v2/product/{barcode}.json")
    suspend fun getProduct(@Path("barcode") barcode: String): OpenFoodFactsResponse
}

object OpenFoodFactsClient {
    private val moshi: Moshi by lazy {
        Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()
    }

    private val okHttp: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(7, TimeUnit.SECONDS)
            .readTimeout(8, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("User-Agent", "NutriLifeAI-Android/1.0")
                    .build()
                chain.proceed(request)
            }
            .build()
    }

    val api: OpenFoodFactsApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://world.openfoodfacts.org/")
            .client(okHttp)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(OpenFoodFactsApi::class.java)
    }
}
