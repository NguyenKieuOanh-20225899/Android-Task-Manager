package com.example.taskmanagement

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Chuyển sang AndroidViewModel để sử dụng SharedPreferences thông qua Application context
class TaskViewModel(application: Application) : AndroidViewModel(application) {

    private val sharedPrefs = application.getSharedPreferences("TaskPrefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    // Danh sách gốc chứa tất cả công việc
    private var allTasks = mutableListOf<Task>()

    // LiveData quan sát danh sách hiển thị
    private val _tasks = MutableLiveData<List<Task>>()
    val tasks: LiveData<List<Task>> get() = _tasks

    // LiveData quan sát tiến độ (%)
    private val _completionRate = MutableLiveData<Int>()
    val completionRate: LiveData<Int> get() = _completionRate

    // Biến lưu trữ ngày đang lọc hiện tại để refresh UI chính xác
    private var currentFilteringDate: String = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    init {
        loadTasksFromDisk()
        updateProgress()
    }

    // --- Logic Lưu trữ Bền vững ---

    private fun saveTasksToDisk() {
        val json = gson.toJson(allTasks)
        sharedPrefs.edit().putString("saved_tasks_list", json).apply()
    }

    private fun loadTasksFromDisk() {
        val json = sharedPrefs.getString("saved_tasks_list", null)
        if (json != null) {
            val type = object : TypeToken<MutableList<Task>>() {}.type
            allTasks = gson.fromJson(json, type)
        } else {
            // Nếu lần đầu mở app, có thể nạp task chào mừng
            allTasks = mutableListOf()
        }
        filterByDate(currentFilteringDate)
    }

    // --- Các hàm thao tác dữ liệu ---

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

        saveTasksToDisk() // Lưu vào máy ngay khi thêm mới
        filterByDate(date)
        updateProgress()
    }

    fun toggleTaskStatus(task: Task) {
        val index = allTasks.indexOfFirst { it.id == task.id }
        if (index != -1) {
            allTasks[index].isCompleted = !allTasks[index].isCompleted

            saveTasksToDisk() // Lưu vào máy ngay khi thay đổi trạng thái
            filterByDate(currentFilteringDate)
            updateProgress()
        }
    }

    fun deleteTask(task: Task) {
        allTasks.removeIf { it.id == task.id }

        saveTasksToDisk() // Lưu vào máy ngay khi xóa
        filterByDate(currentFilteringDate)
        updateProgress()
    }

    // --- Logic Lọc & Hiển thị ---

    fun filterByDate(date: String) {
        currentFilteringDate = date
        _tasks.value = allTasks.filter { it.date == date }
    }

    fun showCompletedTasks() {
        _tasks.value = allTasks.filter { it.date == currentFilteringDate && it.isCompleted }
    }

    fun showPendingTasks() {
        _tasks.value = allTasks.filter { it.date == currentFilteringDate && !it.isCompleted }
    }

    private fun updateProgress() {
        // Chỉ tính tiến độ cho ngày đang xem để người dùng không bị áp lực bởi tổng số task
        val tasksToday = allTasks.filter { it.date == currentFilteringDate }
        val total = tasksToday.size
        if (total == 0) {
            _completionRate.value = 0
            return
        }
        val completed = tasksToday.count { it.isCompleted }
        _completionRate.value = (completed * 100) / total
    }
}