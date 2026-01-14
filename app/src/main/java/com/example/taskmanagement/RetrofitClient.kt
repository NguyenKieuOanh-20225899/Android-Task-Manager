package com.example.taskmanagement

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    // Thay URL này bằng địa chỉ Server API của bạn (ví dụ: http://10.0.2.2:3000/ cho local emulator)
    private const val BASE_URL = "https://69674bbcbbe157c088b177e1.mockapi.io/"
    val apiService: TaskApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(TaskApiService::class.java)
    }
}