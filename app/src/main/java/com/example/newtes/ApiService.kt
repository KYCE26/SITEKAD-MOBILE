package com.example.newtes

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*

// Interface untuk mendefinisikan endpoint API
interface ApiService {

    @Multipart
    @POST("api/lembur/start")
    suspend fun startLembur(
        @Header("Authorization") token: String,
        @Part splFile: MultipartBody.Part,
        @Part("latitude") latitude: RequestBody,
        @Part("longitude") longitude: RequestBody,
        @Part("android_id") androidId: RequestBody,
        @Part("kodeqr") kodeqr: RequestBody
    ): Response<LemburResponse>

    // --- TAMBAHKAN FUNGSI BARU INI UNTUK CUTI ---
    @Multipart
    @POST("api/cuti") // Sesuaikan path-nya jika berbeda
    suspend fun submitCuti(
        @Header("Authorization") token: String,
        @Part("alasan") alasan: RequestBody,
        @Part("tanggal_mulai") tanggalMulai: RequestBody,
        @Part("tanggal_selesai") tanggalSelesai: RequestBody,
        @Part("keterangan") keterangan: RequestBody,
        @Part suket: MultipartBody.Part? // Dibuat nullable, karena file 'suket' opsional
    ): Response<LemburResponse> // Kita pakai ulang LemburResponse karena strukturnya mirip
}

// Object untuk membuat instance Retrofit (singleton) - PORT 11084 (Lama)
object RetrofitClient {
    private const val BASE_URL = "http://202.138.248.93:11084/v1/"

    val instance: ApiService by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        retrofit.create(ApiService::class.java)
    }
}

// --- TAMBAHKAN OBJECT BARU INI UNTUK CUTI (PORT 10084) ---
object RetrofitClientCuti {
    // Pastikan IP dan /v1/ nya sudah benar
    private const val BASE_URL = "http://202.138.248.93:11084/v1/"

    val instance: ApiService by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        retrofit.create(ApiService::class.java)
    }
}