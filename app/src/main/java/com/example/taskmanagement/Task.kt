package com.example.taskmanagement

/**
 * Slide 3: Enum Class để quản lý độ ưu tiên
 */
enum class Priority {
    LOW, MEDIUM, HIGH
}

/**
 * Slide 1 & 3: Data Class cho Task
 */
data class Task(
    val id: Int,
    val title: String,

    // Slide 1: Null Safety cho mô tả công việc
    val description: String? = null,

    // Giá trị mặc định
    val priority: Priority = Priority.LOW,

    // var vì trạng thái này có thể thay đổi
    var isCompleted: Boolean = false
)