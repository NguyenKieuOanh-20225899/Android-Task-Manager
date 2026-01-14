package com.example.taskmanagement

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class SyncWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            // Lấy repository từ lớp TaskApplication (Điều khiển trung tâm)
            // Lưu ý: Bạn phải đảm bảo đã khai báo TaskApplication trong AndroidManifest.xml
            val repository = (applicationContext as TaskApplication).repository

            // Thực hiện quét và đẩy các task chưa đồng bộ lên MockAPI
            repository.syncUnsyncedTasks()

            Log.d("SyncWorker", "Đồng bộ dữ liệu thành công")
            Result.success()
        } catch (e: Exception) {
            Log.e("SyncWorker", "Lỗi đồng bộ dữ liệu: ${e.message}")

            // Nếu có lỗi (như lỗi mạng), yêu cầu WorkManager thử lại sau
            Result.retry()
        }
    }
}