package com.example.taskmanagement

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize
import android.os.Parcelable

/**
 * Slide 3: Enum Class để quản lý độ ưu tiên
 */
// File: app/src/main/java/com/example/taskmanagement/Task.kt
enum class Priority { LOW, MEDIUM, HIGH, URGENT } // Bổ sung mức độ khẩn cấp

/**
 * Data class đại diện cho một bảng trong cơ sở dữ liệu (SQLite/Room)
 * @Parcelize: Cho phép truyền đối tượng Task qua lại giữa các Activity/Fragment
 * @Entity: Khai báo đây là một bảng trong database với tên là "task_table"
 */
@Parcelize
@Entity(tableName = "task_table")
data class Task(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    val description: String? = null,
    val priority: Priority = Priority.LOW,
    var isCompleted: Boolean = false,
    val category: String = "Cá nhân",
    val date: String,            // Ngày lên kế hoạch (yyyy-MM-dd)
    val reminderTime: String?,   // Giờ nhắc nhở (HH:mm)
    val repeatDays: String? = null, // Danh sách "T2", "T3",..., "CN"
    val isAllDay: Boolean = false, // Thiết lập công việc cả ngày

    // === BỔ SUNG CHO TÍNH NĂNG ONLINE/OFFLINE ===
    val isSynced: Boolean = false // Đánh dấu đã đồng bộ với Server hay chưa
): Parcelable