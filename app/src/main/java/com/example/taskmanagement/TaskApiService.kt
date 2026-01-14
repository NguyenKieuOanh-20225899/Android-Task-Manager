package com.example.taskmanagement

import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface TaskApiService {
    @GET("tasks") // Lấy toàn bộ task từ server
    suspend fun getAllTasks(): List<Task>

    @POST("tasks") // Đẩy một task mới lên server
    suspend fun uploadTask(@Body task: Task): Task

    @PUT("tasks/{id}") // Cập nhật task trên server
    suspend fun updateTask(@Path("id") id: Int, @Body task: Task): Task

    @DELETE("tasks/{id}")
    suspend fun deleteTask(@Path("id") id: Int)
}