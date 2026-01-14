package com.example.taskmanagement

import android.app.Application
import androidx.lifecycle.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import java.util.Calendar
import android.os.Build
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

class TaskViewModel(application: Application) : AndroidViewModel(application) {

    // === PHẦN THAY ĐỔI: LẤY REPOSITORY TỪ APPLICATION ===
    private val repository: TaskRepository

    // Danh sách gốc quan sát từ repository (Room + Flow)
    private val allTasksFromDb: LiveData<List<Task>>

    init {
        // Lấy repository duy nhất từ TaskApplication thay vì khởi tạo mới tại đây
        repository = (application as TaskApplication).repository
        allTasksFromDb = repository.allTasks.asLiveData()
    }
    // ====================================================

    // LiveData dùng cho UI
    private val _tasks = MediatorLiveData<List<Task>>()
    val tasks: LiveData<List<Task>> get() = _tasks

    private val _completionRate = MutableLiveData<Int>()
    val completionRate: LiveData<Int> get() = _completionRate

    private var currentFilteringDate: String = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    init {
        // Tự động cập nhật danh sách hiển thị mỗi khi database thay đổi
        _tasks.addSource(allTasksFromDb) { list ->
            applyFilter(list, currentFilteringDate)
        }
        // Tự động đồng bộ dữ liệu khi mở app nếu có mạng
        refreshDataFromRemote()
    }

    // Hàm kiểm tra internet nhanh
    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = getApplication<Application>().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false
        return activeNetwork.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    // Đồng bộ dữ liệu Online/Offline thông minh
    fun refreshDataFromRemote() {
        if (isNetworkAvailable()) {
            viewModelScope.launch(Dispatchers.IO) {
                // 1. Đẩy các task tạo lúc offline lên server trước để tránh mất dữ liệu
                repository.syncUnsyncedTasks()
                // 2. Sau đó mới lấy dữ liệu mới từ server về cập nhật vào Room
                repository.refreshTasks()
            }
        }
    }

    // Thêm Task mới
    fun addNewTask(
        title: String,
        description: String? = null,
        priority: Priority,
        date: String,
        reminderTime: String?,
        repeatDays: List<String>?,
        isAllDay: Boolean
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val newTask = Task(
                id = 0,
                title = title,
                description = description,
                priority = priority,
                date = date,
                reminderTime = if (isAllDay) null else reminderTime,
                repeatDays = repeatDays?.joinToString(","),
                isAllDay = isAllDay,
                isSynced = false
            )

            // Lưu qua Repository
            val generatedId = repository.insert(newTask, isNetworkAvailable()).toInt()

            // Lập lịch thông báo
            val taskWithId = newTask.copy(id = generatedId)
            scheduleNotification(taskWithId)
        }
    }

    fun toggleTaskStatus(task: Task) {
        viewModelScope.launch(Dispatchers.IO) {
            val updatedTask = task.copy(isCompleted = !task.isCompleted, isSynced = false)
            repository.update(updatedTask, isNetworkAvailable())

            if (updatedTask.isCompleted) {
                cancelNotification(updatedTask)
            } else {
                scheduleNotification(updatedTask)
            }
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch(Dispatchers.IO) {
            cancelNotification(task)
            repository.delete(task, isNetworkAvailable())
        }
    }

    fun updateTask(
        id: Int,
        title: String,
        description: String? = null,
        priority: Priority,
        date: String,
        reminderTime: String?,
        repeatDays: List<String>?,
        isAllDay: Boolean,
        isCompleted: Boolean
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val updatedTask = Task(
                id = id,
                title = title,
                description = description,
                priority = priority,
                date = date,
                reminderTime = if (isAllDay) null else reminderTime,
                repeatDays = repeatDays?.joinToString(","),
                isAllDay = isAllDay,
                isCompleted = isCompleted,
                isSynced = false
            )

            repository.update(updatedTask, isNetworkAvailable())

            cancelNotification(updatedTask)
            if (!isCompleted && !isAllDay && reminderTime != null) {
                scheduleNotification(updatedTask)
            }
        }
    }

    // === CÁC LOGIC LỌC VÀ THÔNG BÁO GIỮ NGUYÊN HOÀN TOÀN ===

    fun filterByDate(date: String) {
        try {
            val parsedDate = dateFormat.parse(date)
            currentFilteringDate = dateFormat.format(parsedDate!!)
            allTasksFromDb.value?.let { applyFilter(it, currentFilteringDate) }
        } catch (e: Exception) {
            Log.e("TaskViewModel", "Định dạng ngày không hợp lệ: $date")
        }
    }

    private fun applyFilter(allList: List<Task>, date: String) {
        val selectedDayOfWeek = getDayOfWeek(date)
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        val filtered = allList.filter { task ->
            val isSameDate = task.date == date
            val daysList = task.repeatDays?.split(",") ?: emptyList()
            val isRepeatingToday = daysList.contains(selectedDayOfWeek) && date >= task.date
            val isPendingFromPast = date == todayStr && task.date < todayStr && !task.isCompleted

            isSameDate || isRepeatingToday || isPendingFromPast
        }
        _tasks.postValue(filtered)
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
        _completionRate.postValue(if (total == 0) 0 else (currentList.count { it.isCompleted } * 100) / total)
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

    private fun scheduleNotification(task: Task) {
        if (task.reminderTime == null || task.isCompleted) return
        val alarmManager = getApplication<Application>().getSystemService(Context.ALARM_SERVICE) as AlarmManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) return
        }

        val intent = Intent(getApplication(), NotificationReceiver::class.java).apply {
            putExtra("TASK_ID", task.id)
            putExtra("TASK_TITLE", task.title)
            putExtra("TASK_DESC", task.description)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            getApplication(), task.id, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance()
        try {
            val dateParts = task.date.split("-")
            val timeParts = task.reminderTime!!.split(":")
            calendar.set(dateParts[0].toInt(), dateParts[1].toInt() - 1, dateParts[2].toInt(), timeParts[0].toInt(), timeParts[1].toInt(), 0)

            if (calendar.timeInMillis > System.currentTimeMillis()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
            }
        } catch (e: Exception) { }
    }

    private fun cancelNotification(task: Task) {
        val alarmManager = getApplication<Application>().getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(getApplication(), NotificationReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            getApplication(), task.id, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }
}