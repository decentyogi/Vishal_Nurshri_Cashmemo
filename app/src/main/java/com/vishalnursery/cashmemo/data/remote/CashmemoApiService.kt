package com.vishalnursery.cashmemo.data.remote

import com.vishalnursery.cashmemo.data.remote.dto.BackupPayloadDto
import com.vishalnursery.cashmemo.data.remote.dto.BillDto
import com.vishalnursery.cashmemo.data.remote.dto.CustomerDto
import com.vishalnursery.cashmemo.data.remote.dto.GenericSuccessDto
import com.vishalnursery.cashmemo.data.remote.dto.PaymentRequestDto
import com.vishalnursery.cashmemo.data.remote.dto.PlantRateDto
import com.vishalnursery.cashmemo.data.remote.dto.SaveBillResponseDto
import com.vishalnursery.cashmemo.data.remote.dto.SettingsDto
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

/**
 * Retrofit contract matching Cashmemo_AI_Web (app.py) REST endpoints.
 */
interface CashmemoApiService {

    @GET("api/settings")
    suspend fun getSettings(): SettingsDto

    @POST("api/settings")
    suspend fun saveSettings(@Body settings: SettingsDto): GenericSuccessDto

    @GET("api/bills")
    suspend fun listBills(
        @Query("q") query: String = "",
        @Query("filter") filter: String = "ALL"
    ): List<BillDto>

    @POST("api/bills")
    suspend fun saveBill(@Body bill: BillDto): SaveBillResponseDto

    @DELETE("api/bills/{id}")
    suspend fun deleteBill(@Path("id") id: String): GenericSuccessDto

    @GET("api/customers")
    suspend fun listCustomers(): List<CustomerDto>

    @POST("api/customers")
    suspend fun saveCustomer(@Body customer: CustomerDto): GenericSuccessDto

    @POST("api/customers/{id}/pay")
    suspend fun recordCustomerPayment(
        @Path("id") id: String,
        @Body request: PaymentRequestDto
    ): GenericSuccessDto

    @DELETE("api/customers/{id}")
    suspend fun deleteCustomer(@Path("id") id: String): GenericSuccessDto

    @GET("api/rates")
    suspend fun listRates(): List<PlantRateDto>

    @POST("api/rates")
    suspend fun saveRate(@Body rate: PlantRateDto): GenericSuccessDto

    @DELETE("api/rates/{id}")
    suspend fun deleteRate(@Path("id") id: String): GenericSuccessDto

    @GET("api/backup")
    suspend fun exportBackup(): BackupPayloadDto

    @POST("api/backup")
    suspend fun restoreBackup(@Body payload: BackupPayloadDto): GenericSuccessDto
}

object DynamicBaseUrlClient {

    private var cachedBaseUrl: String? = null
    private var cachedService: CashmemoApiService? = null

    private val okHttpClient: OkHttpClient by lazy {
        val logger = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        OkHttpClient.Builder()
            .connectTimeout(6, TimeUnit.SECONDS)
            .readTimeout(12, TimeUnit.SECONDS)
            .writeTimeout(12, TimeUnit.SECONDS)
            .addInterceptor(logger)
            .build()
    }

    @Synchronized
    fun getService(rawBaseUrl: String): CashmemoApiService? {
        val normalized = normalizeBaseUrl(rawBaseUrl) ?: return null
        if (cachedService != null && cachedBaseUrl == normalized) {
            return cachedService
        }
        val retrofit = Retrofit.Builder()
            .baseUrl(normalized)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        return retrofit.create(CashmemoApiService::class.java).also {
            cachedBaseUrl = normalized
            cachedService = it
        }
    }

    fun normalizeBaseUrl(raw: String): String? {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return null
        val withScheme = if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            trimmed
        } else {
            "http://$trimmed"
        }
        return if (withScheme.endsWith("/")) withScheme else "$withScheme/"
    }
}
