package com.example.taskmanagement

import kotlinx.coroutines.flow.Flow

class TaskRepository(private val taskDao: TaskDao, private val apiService: TaskApiService) {

    // Lấy dữ liệu từ Room để UI hiển thị ngay lập tức (Offline-first)
    val allTasks: Flow<List<Task>> = taskDao.getAllTasks()

    // Hàm đồng bộ: Lấy từ Server về và lưu vào Room
    suspend fun refreshTasks() {
        try {
            // 1. Lấy danh sách mới nhất từ MockAPI
            val remoteTasks = apiService.getAllTasks()

            // 2. Xóa các Task cũ đã từng được đồng bộ ở máy cục bộ
            // Việc này giúp loại bỏ những Task đã bị xóa trực tiếp trên Server
            taskDao.deleteSyncedTasks()

            // 3. Nạp lại danh sách mới từ Server vào Room
            remoteTasks.forEach { task ->
                // Đánh dấu isSynced = true vì dữ liệu này vừa tải từ server về
                taskDao.insert(task.copy(isSynced = true))
            }
        } catch (e: Exception) {
            // Xử lý lỗi khi không có mạng hoặc lỗi server
        }
    }

    /**
     * Hàm tự động đẩy các Task tạo lúc Offline lên Server.
     * Quét tất cả task có isSynced = false trong Room và upload lên MockAPI.
     */
    suspend fun syncUnsyncedTasks() {
        try {
            // 1. Lấy danh sách Task chưa đồng bộ từ Room
            val unsyncedTasks = taskDao.getUnsyncedTasks()

            unsyncedTasks.forEach { task ->
                // 2. Đẩy từng task lên Server (MockAPI)
                apiService.uploadTask(task)
                // 3. Cập nhật trạng thái thành đã đồng bộ (isSynced = true) trong Room
                taskDao.update(task.copy(isSynced = true))
            }
        } catch (e: Exception) {
            // Lỗi kết nối, dữ liệu vẫn an toàn trong Room để thử lại sau
        }
    }

    // SỬA LỖI toInt(): Hàm này phải trả về Long (ID của Task vừa tạo)
    suspend fun insert(task: Task, isOnline: Boolean): Long {
        val id = taskDao.insert(task) // Luôn lưu vào Room trước
        if (isOnline) {
            try {
                // Đẩy dữ liệu lên Server với ID chính xác từ máy
                apiService.uploadTask(task.copy(id = id.toInt()))
                // Sau khi thành công, cập nhật lại trạng thái isSynced thành true
                taskDao.update(task.copy(id = id.toInt(), isSynced = true))
            } catch (e: Exception) {
                // Nếu lỗi mạng, task vẫn lưu ở Room với isSynced = false
            }
        }
        return id
    }

    // SỬA LỖI "update": Bổ sung phương thức cập nhật
    suspend fun update(task: Task, isOnline: Boolean) {
        taskDao.update(task) // Cập nhật local trước
        if (isOnline) {
            try {
                apiService.updateTask(task.id, task)
                taskDao.update(task.copy(isSynced = true))
            } catch (e: Exception) {
                // Đánh dấu isSynced = false nếu không cập nhật được lên server để đồng bộ sau
                taskDao.update(task.copy(isSynced = false))
            }
        }
    }

    // SỬA LỖI "delete": Bổ sung phương thức xóa
    suspend fun delete(task: Task, isOnline: Boolean) {
        taskDao.delete(task) // Xóa ở database local
        if (isOnline) {
            try {
                // Gọi API xóa trên server MockAPI
                apiService.deleteTask(task.id)
            } catch (e: Exception) {
                // Lỗi xóa online không làm ảnh hưởng đến dữ liệu đã xóa offline
            }
        }
    }
}