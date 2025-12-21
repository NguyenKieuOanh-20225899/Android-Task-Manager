package com.example.taskmanagement

/**
 * Slide 3: Enum Class để quản lý độ ưu tiên
 */
// File: app/src/main/java/com/example/taskmanagement/Task.kt
enum class Priority { LOW, MEDIUM, HIGH, URGENT } // Bổ sung mức độ khẩn cấp



data class Task(
    val id: Int,
    val title: String,
    val description: String? = null,
    val priority: Priority = Priority.LOW,
    var isCompleted: Boolean = false,
    val category: String = "Cá nhân",
    val date: String,            // Ngày lên kế hoạch (yyyy-MM-dd)
    val reminderTime: String?,   // Giờ nhắc nhở (HH:mm)
    val repeatDays: List<String>? = null, // Danh sách "T2", "T3",..., "CN"
    val isAllDay: Boolean = false // Thiết lập công việc cả ngày
)