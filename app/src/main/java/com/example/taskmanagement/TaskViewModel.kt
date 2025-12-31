package com.example.taskmanagement

import android.app.Application
import androidx.lifecycle.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class TaskViewModel(application: Application) : AndroidViewModel(application) {

    // Khởi tạo Database và DAO (Tham khảo Lab 9.3)
    private val taskDao = TaskDatabase.getDatabase(application).taskDao()

    // Danh sách gốc quan sát từ database
    private val allTasksFromDb: LiveData<List<Task>> = taskDao.getAllTasks().asLiveData()

    // LiveData dùng cho UI, giữ nguyên tên để tương thích với Fragment
    private val _tasks = MediatorLiveData<List<Task>>()
    val tasks: LiveData<List<Task>> get() = _tasks

    private val _completionRate = MutableLiveData<Int>()
    val completionRate: LiveData<Int> get() = _completionRate

    private var currentFilteringDate: String = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    init {
        // Tự động cập nhật danh sách hiển thị mỗi khi database thay đổi
        _tasks.addSource(allTasksFromDb) { list ->
            applyFilter(list, currentFilteringDate)
        }
    }

    // Giữ nguyên tham số truyền vào để không lỗi AddEditFragment
    fun addNewTask(
        title: String,
        description: String? = null,
        priority: Priority,
        date: String,
        reminderTime: String?,
        repeatDays: List<String>?,
        isAllDay: Boolean
    ) {
        // Sử dụng viewModelScope để chạy tác vụ database ngầm (Lab 9.3)
        viewModelScope.launch(Dispatchers.IO) {
            val newTask = Task(
                id = 0, // Room tự tăng ID
                title = title,
                description = description,
                priority = priority,
                date = date,
                reminderTime = if (isAllDay) null else reminderTime,
                // Chuyển List thành String để lưu vào SQLite đơn giản
                repeatDays = repeatDays?.joinToString(","),
                isAllDay = isAllDay
            )
            taskDao.insert(newTask)
        }
    }

    // Giữ nguyên logic đảo trạng thái Task
    fun toggleTaskStatus(task: Task) {
        viewModelScope.launch(Dispatchers.IO) {
            val updatedTask = task.copy(isCompleted = !task.isCompleted)
            taskDao.update(updatedTask)
        }
    }

    // Giữ nguyên logic xóa Task
    fun deleteTask(task: Task) {
        viewModelScope.launch(Dispatchers.IO) {
            taskDao.delete(task)
        }
    }

    // Logic lọc giữ nguyên như cũ để tương thích với giao diện
    fun filterByDate(date: String) {
        currentFilteringDate = date
        allTasksFromDb.value?.let { applyFilter(it, date) }
    }

    private fun applyFilter(allList: List<Task>, date: String) {
        val selectedDayOfWeek = getDayOfWeek(date)
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        val filtered = allList.filter { task ->
            val isSameDate = task.date == date
            // Chuyển ngược từ String sang List để kiểm tra lặp lại
            val daysList = task.repeatDays?.split(",") ?: emptyList()
            val isRepeatingToday = daysList.contains(selectedDayOfWeek) && date >= task.date
            val isPendingFromPast = date == todayStr && task.date < todayStr && !task.isCompleted

            isSameDate || isRepeatingToday || isPendingFromPast
        }
        _tasks.value = filtered
        updateProgress()
    }

    fun showCompletedTasks() {
        val selectedDayOfWeek = getDayOfWeek(currentFilteringDate)
        _tasks.value = allTasksFromDb.value?.filter { task ->
            val daysList = task.repeatDays?.split(",") ?: emptyList()
            val isRelevant = task.date == currentFilteringDate ||
                    (daysList.contains(selectedDayOfWeek) && currentFilteringDate >= task.date)
            isRelevant && task.isCompleted
        }
    }

    fun showPendingTasks() {
        val selectedDayOfWeek = getDayOfWeek(currentFilteringDate)
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        _tasks.value = allTasksFromDb.value?.filter { task ->
            val daysList = task.repeatDays?.split(",") ?: emptyList()
            val isRelevant = task.date == currentFilteringDate ||
                    (daysList.contains(selectedDayOfWeek) && currentFilteringDate >= task.date) ||
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
        } catch (e: Exception) { "" }
    }
}