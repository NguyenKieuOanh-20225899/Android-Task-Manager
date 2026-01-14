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
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
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

            // 1. Thực hiện chèn và lấy ID thực tế từ Database trả về
            val generatedId = taskDao.insert(newTask).toInt()

            // 2. Tạo bản sao của Task với ID chính xác để lập lịch thông báo
            val taskWithId = newTask.copy(id = generatedId)

            // 3. Gọi hàm lập lịch thông báo (Đảm bảo bạn đã paste hàm scheduleNotification vào ViewModel)
            scheduleNotification(taskWithId)

        }
    }

    // Giữ nguyên logic đảo trạng thái Task
    fun toggleTaskStatus(task: Task) {
        viewModelScope.launch(Dispatchers.IO) {
            val updatedTask = task.copy(isCompleted = !task.isCompleted) // Đảo trạng thái
            taskDao.update(updatedTask)
        }
    }
    private fun cancelNotification(task: Task) {
        val alarmManager = getApplication<Application>().getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(getApplication(), NotificationReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            getApplication(),
            task.id, // Sử dụng ID đồng nhất để tìm đúng báo thức
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.cancel(pendingIntent)
        Log.d("TaskViewModel", "Đã hủy báo thức cho Task: ${task.title}")
    }
    // Giữ nguyên logic xóa Task
    fun deleteTask(task: Task) {
        viewModelScope.launch(Dispatchers.IO) {
            cancelNotification(task)
            taskDao.delete(task)
        }
    }

    // Logic lọc giữ nguyên như cũ để tương thích với giao diện
    fun filterByDate(date: String) {
        try {
            // Parse rồi format lại để đảm bảo luôn là yyyy-MM-dd
            val parsedDate = dateFormat.parse(date)
            currentFilteringDate = dateFormat.format(parsedDate!!)
            allTasksFromDb.value?.let { applyFilter(it, currentFilteringDate) }
        } catch (e: Exception) {
            Log.e("TaskViewModel", "Định dạng ngày không hợp lệ: $date")
        }
    }
    // Trong TaskViewModel.kt
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
                id = id, // Sử dụng ID cũ để ghi đè
                title = title,
                description = description,
                priority = priority,
                date = date,
                reminderTime = if (isAllDay) null else reminderTime,
                repeatDays = repeatDays?.joinToString(","),
                isAllDay = isAllDay,
                isCompleted = isCompleted
            )

            // 1. Cập nhật vào Database
            taskDao.update(updatedTask)

            // 2. Quản lý thông báo: Hủy báo thức cũ và đặt lại nếu cần
            cancelNotification(updatedTask)
            if (!isCompleted && !isAllDay && reminderTime != null) {
                scheduleNotification(updatedTask)
            }
        }
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
    private fun scheduleNotification(task: Task) {
        // Nếu không có thời gian nhắc nhở hoặc đã hoàn thành thì không đặt báo thức
        if (task.reminderTime == null || task.isCompleted) return

        val alarmManager = getApplication<Application>().getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // 1. Kiểm tra quyền cho Android 12+ (API 31+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                Log.e("TaskViewModel", "Không có quyền SCHEDULE_EXACT_ALARM")
                // Tại đây bạn có thể bắn một LiveData để Fragment hiển thị thông báo yêu cầu cấp quyền
                return
            }
        }

        val intent = Intent(getApplication(), NotificationReceiver::class.java).apply {
            putExtra("TASK_ID", task.id)
            putExtra("TASK_TITLE", task.title)
            putExtra("TASK_DESC", task.description)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            getApplication(),
            task.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Phân tích thời gian
        val calendar = Calendar.getInstance()
        try {
            val dateParts = task.date.split("-")
            val timeParts = task.reminderTime!!.split(":")
            calendar.set(Calendar.YEAR, dateParts[0].toInt())
            calendar.set(Calendar.MONTH, dateParts[1].toInt() - 1)
            calendar.set(Calendar.DAY_OF_MONTH, dateParts[2].toInt())
            calendar.set(Calendar.HOUR_OF_DAY, timeParts[0].toInt())
            calendar.set(Calendar.MINUTE, timeParts[1].toInt())
            calendar.set(Calendar.SECOND, 0)

            if (calendar.timeInMillis > System.currentTimeMillis()) {
                // 2. Sử dụng try-catch để tránh crash do SecurityException
                try {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                } catch (e: SecurityException) {
                    Log.e("TaskViewModel", "Lỗi bảo mật khi đặt báo thức: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Log.e("TaskViewModel", "Lỗi định dạng ngày/giờ: ${e.message}")
        }
    }
    // Thêm hàm cập nhật Task
    fun updateTask(task: Task) {
        viewModelScope.launch(Dispatchers.IO) {
            taskDao.update(task)

            // Nếu task đã hoàn thành hoặc "Cả ngày", hủy báo thức cũ
            if (task.isCompleted || task.isAllDay || task.reminderTime == null) {
                cancelNotification(task)
            } else {
                // Nếu thay đổi giờ/ngày, hàm này sẽ tự động ghi đè báo thức cũ
                scheduleNotification(task)
            }
        }
    }

}