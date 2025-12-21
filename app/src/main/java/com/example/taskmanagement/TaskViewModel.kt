package com.example.taskmanagement

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class TaskViewModel(application: Application) : AndroidViewModel(application) {

    private val sharedPrefs = application.getSharedPreferences("TaskPrefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    private var allTasks = mutableListOf<Task>()

    private val _tasks = MutableLiveData<List<Task>>()
    val tasks: LiveData<List<Task>> get() = _tasks

    private val _completionRate = MutableLiveData<Int>()
    val completionRate: LiveData<Int> get() = _completionRate

    private var currentFilteringDate: String = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    init {
        loadTasksFromDisk()
    }

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
            allTasks = mutableListOf()
        }
        filterByDate(currentFilteringDate)
    }

    // --- Hàm bổ sung: Lấy thứ trong tuần từ một chuỗi ngày (yyyy-MM-dd) ---
    private fun getDayOfWeek(dateString: String): String {
        return try {
            val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val date = format.parse(dateString)
            val calendar = Calendar.getInstance()
            calendar.time = date!!
            when (calendar.get(Calendar.DAY_OF_WEEK)) {
                Calendar.MONDAY -> "T2"
                Calendar.TUESDAY -> "T3"
                Calendar.WEDNESDAY -> "T4"
                Calendar.THURSDAY -> "T5"
                Calendar.FRIDAY -> "T6"
                Calendar.SATURDAY -> "T7"
                Calendar.SUNDAY -> "CN"
                else -> ""
            }
        } catch (e: Exception) {
            ""
        }
    }

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
        saveTasksToDisk()
        filterByDate(date)
    }

    fun toggleTaskStatus(task: Task) {
        val index = allTasks.indexOfFirst { it.id == task.id }
        if (index != -1) {
            allTasks[index].isCompleted = !allTasks[index].isCompleted
            saveTasksToDisk()
            filterByDate(currentFilteringDate)
        }
    }

    fun deleteTask(task: Task) {
        allTasks.removeIf { it.id == task.id }
        saveTasksToDisk()
        filterByDate(currentFilteringDate)
    }

    // --- Logic Lọc Cải Tiến: Xử lý lặp lại và việc tồn đọng ---
    fun filterByDate(date: String) {
        currentFilteringDate = date
        val selectedDayOfWeek = getDayOfWeek(date)
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        _tasks.value = allTasks.filter { task ->
            // 1. Khớp ngày chính xác
            val isSameDate = task.date == date

            // 2. Kiểm tra lặp lại: Thứ của ngày đang xem có nằm trong danh sách lặp lại không?
            // Lưu ý: Chỉ lặp lại nếu ngày đang xem >= ngày tạo task gốc
            val isRepeatingToday = task.repeatDays?.contains(selectedDayOfWeek) == true && date >= task.date

            // 3. Kiểm tra việc chưa xong từ quá khứ (Overdue):
            // Chỉ hiển thị "việc hôm qua chưa xong" nếu chúng ta đang xem danh sách của ngày "Hôm nay"
            val isPendingFromPast = date == todayStr && task.date < todayStr && !task.isCompleted

            isSameDate || isRepeatingToday || isPendingFromPast
        }
        updateProgress()
    }

    fun showCompletedTasks() {
        val selectedDayOfWeek = getDayOfWeek(currentFilteringDate)
        _tasks.value = allTasks.filter { task ->
            val isRelevant = task.date == currentFilteringDate ||
                    (task.repeatDays?.contains(selectedDayOfWeek) == true && currentFilteringDate >= task.date)
            isRelevant && task.isCompleted
        }
    }

    fun showPendingTasks() {
        val selectedDayOfWeek = getDayOfWeek(currentFilteringDate)
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        _tasks.value = allTasks.filter { task ->
            val isRelevant = task.date == currentFilteringDate ||
                    (task.repeatDays?.contains(selectedDayOfWeek) == true && currentFilteringDate >= task.date) ||
                    (currentFilteringDate == todayStr && task.date < todayStr)
            isRelevant && !task.isCompleted
        }
    }

    private fun updateProgress() {
        val currentList = _tasks.value ?: emptyList()
        val total = currentList.size
        if (total == 0) {
            _completionRate.value = 0
            return
        }
        val completed = currentList.count { it.isCompleted }
        _completionRate.value = (completed * 100) / total
    }
}