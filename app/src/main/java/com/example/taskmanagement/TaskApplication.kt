package com.example.taskmanagement

import android.app.Application

class TaskApplication : Application() {
    // Khởi tạo database và repository duy nhất cho toàn app
    val database by lazy { TaskDatabase.getDatabase(this) }

    val repository by lazy {
        TaskRepository(database.taskDao(), RetrofitClient.apiService)
    }
}