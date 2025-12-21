package com.example.taskmanagement

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class TaskViewModel : ViewModel() {

    // Danh sách gốc chứa tất cả công việc (Private để bảo mật dữ liệu)
    private val allTasks = mutableListOf<Task>()

    // Slide 8: LiveData để UI có thể quan sát (Observe) sự thay đổi
    private val _tasks = MutableLiveData<List<Task>>()
    val tasks: LiveData<List<Task>> get() = _tasks

    init {
        // Tạo dữ liệu mẫu để bạn dễ dàng kiểm tra giao diện khi mới chạy
        loadSampleTasks()
    }

    private fun loadSampleTasks() {
        allTasks.add(Task(1, "Học Android Slide 8", "Tìm hiểu về ViewModel và LiveData", Priority.HIGH))
        allTasks.add(Task(2, "Làm bài tập Kotlin", "Thực hành Filter và Lambda", Priority.MEDIUM))
        _tasks.value = allTasks
    }

    // Hàm thêm công việc mới
    // Trong TaskViewModel.kt
    fun addNewTask(title: String, description: String?, priority: Priority) {
        val newId = (allTasks.maxOfOrNull { it.id } ?: 0) + 1
        val newTask = Task(newId, title, description, priority)
        allTasks.add(newTask)
        _tasks.value = allTasks.toList() // Kích hoạt LiveData để HomeFragment cập nhật
    }

    // Slide 2: Sử dụng Filter và Lambda để lọc công việc "Smart"
    fun showCompletedTasks() {
        _tasks.value = allTasks.filter { it.isCompleted }
    }

    fun showPendingTasks() {
        _tasks.value = allTasks.filter { !it.isCompleted }
    }

    fun showAllTasks() {
        _tasks.value = allTasks
    }

    // Hàm cập nhật trạng thái hoàn thành
    fun toggleTaskStatus(task: Task) {
        val index = allTasks.indexOfFirst { it.id == task.id }
        if (index != -1) {
            allTasks[index].isCompleted = !allTasks[index].isCompleted
            _tasks.value = allTasks // Thông báo cho UI cập nhật lại
        }
    }
}