package com.example.taskmanagement

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TaskViewModel : ViewModel() {

    // Danh sách gốc chứa tất cả công việc
    private val allTasks = mutableListOf<Task>()

    // LiveData quan sát danh sách hiển thị
    private val _tasks = MutableLiveData<List<Task>>()
    val tasks: LiveData<List<Task>> get() = _tasks

    // LiveData quan sát tiến độ (%)
    private val _completionRate = MutableLiveData<Int>()
    val completionRate: LiveData<Int> get() = _completionRate

    init {
        loadSampleTasks()
        updateProgress()
    }

    private fun loadSampleTasks() {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        allTasks.add(Task(1, "Chào mừng đến NoelDo", "Bắt đầu lập kế hoạch ngay", Priority.HIGH, date = today, reminderTime = "08:00"))
        _tasks.value = allTasks
    }

    /**
     * Hàm thêm mới công việc với đầy đủ tính năng thông minh:
     * - 3 mức độ ưu tiên (LOW, MEDIUM, HIGH)
     * - Ngày kế hoạch (từ Lịch)
     * - Giờ nhắc nhở
     * - Lặp lại từ T2 đến CN
     * - Chế độ cả ngày
     */
    fun addNewTask(
        title: String,
        description: String? = null,
        priority: Priority,
        date: String,
        reminderTime: String?,
        repeatDays: List<String>?,
        isAllDay: Boolean
    ) {
        val newId = (allTasks.maxOfOrNull { it.id } ?: 0) + 1
        val newTask = Task(
            id = newId,
            title = title,
            description = description,
            priority = priority,
            date = date,
            reminderTime = if (isAllDay) null else reminderTime,
            repeatDays = repeatDays,
            isAllDay = isAllDay
        )
        allTasks.add(newTask)

        // Mặc định sau khi thêm sẽ hiển thị danh sách của ngày vừa thêm
        filterByDate(date)
        updateProgress()
    }

    // --- Logic Lọc Thông Minh ---

    // Lọc công việc theo ngày được chọn từ Lịch (ô vuông)
    fun filterByDate(date: String) {
        _tasks.value = allTasks.filter { it.date == date }
    }

    fun showCompletedTasks() {
        _tasks.value = allTasks.filter { it.isCompleted }
    }

    fun showPendingTasks() {
        _tasks.value = allTasks.filter { !it.isCompleted }
    }

    fun showAllTasks() {
        _tasks.value = allTasks.toList()
    }

    // --- Logic Trạng thái & Tiến độ ---

    fun toggleTaskStatus(task: Task) {
        val index = allTasks.indexOfFirst { it.id == task.id }
        if (index != -1) {
            allTasks[index].isCompleted = !allTasks[index].isCompleted
            // Cập nhật lại danh sách hiện tại đang hiển thị để UI đổi màu/gạch chân
            _tasks.value = _tasks.value?.toList()
            updateProgress()
        }
    }

    private fun updateProgress() {
        val total = allTasks.size
        if (total == 0) {
            _completionRate.value = 0
            return
        }
        val completed = allTasks.count { it.isCompleted }
        _completionRate.value = (completed * 100) / total
    }
    fun deleteTask(task: Task) {
        allTasks.removeIf { it.id == task.id }
        // Sau khi xóa, lọc lại danh sách theo ngày đang xem để cập nhật UI
        filterByDate(task.date)
        updateProgress()
    }
}