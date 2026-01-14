package com.example.taskmanagement

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * Interface TaskDao: Định nghĩa các phương thức để truy vấn và thao tác với dữ liệu.
 * Room sẽ tự động tạo mã thực thi (Implementation) cho interface này.
 */
@Dao
interface TaskDao {
    /**
     * Lấy danh sách tất cả công việc từ bảng 'task_table'.
     * ORDER BY date ASC: Sắp xếp danh sách theo ngày tăng dần.
     * Flow<List<Task>>: Cho phép quan sát dữ liệu thực tế (Real-time).
     * UI sẽ tự động cập nhật mỗi khi dữ liệu trong Database thay đổi.
     */
    @Query("SELECT * FROM task_table ORDER BY date ASC")
    fun getAllTasks(): Flow<List<Task>>

    /**
     * Thêm một công việc mới vào cơ sở dữ liệu.
     * onConflict = OnConflictStrategy.REPLACE: Nếu ID đã tồn tại, nó sẽ ghi đè dữ liệu mới lên.
     * suspend: Hàm chạy trong luồng phụ (Coroutine), tránh làm đơ giao diện người dùng.
     * @return: Trả về ID của hàng vừa được chèn (kiểu Long).
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: Task): Long

    @Update
    suspend fun update(task: Task)

    @Delete
    suspend fun delete(task: Task)

    @Query("SELECT * FROM task_table WHERE id = :taskId")
    suspend fun getTaskById(taskId: Int): Task?

    // === PHẦN BỔ SUNG ĐỂ HỖ TRỢ ĐỒNG BỘ ONLINE/OFFLINE ===

    /**
     * Lấy danh sách các công việc chưa được đồng bộ với server (isSynced = false).
     * Được sử dụng bởi Repository để đẩy dữ liệu lên MockAPI khi có mạng trở lại.
     */
    @Query("SELECT * FROM task_table WHERE isSynced = 0")
    suspend fun getUnsyncedTasks(): List<Task>

    /**
     * Xóa tất cả các công việc đã được đồng bộ thành công (isSynced = true).
     * Hàm này cực kỳ quan trọng để dọn dẹp dữ liệu cũ tại máy cục bộ,
     * giúp đồng bộ "gương soi" chính xác với Server MockAPI.
     */
    @Query("DELETE FROM task_table WHERE isSynced = 1")
    suspend fun deleteSyncedTasks()
}