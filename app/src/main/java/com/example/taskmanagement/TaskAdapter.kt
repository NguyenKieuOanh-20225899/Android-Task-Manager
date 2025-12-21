package com.example.taskmanagement

import android.graphics.Color
import android.graphics.Paint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.taskmanagement.databinding.ItemTaskBinding

class TaskAdapter(
    private var tasks: List<Task>,
    private val onTaskChecked: (Task) -> Unit,
    private val onTaskLongClick: (Task) -> Unit
) : RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {

    class TaskViewHolder(val binding: ItemTaskBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val binding = ItemTaskBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TaskViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val currentTask = tasks[position]
        val binding = holder.binding

        // Gán dữ liệu cho DataBinding (Khớp với biến <variable name="task"> trong XML)
        binding.task = currentTask

        // 1. Hiển thị thông tin thời gian & lặp lại
        val timeDisplay = if (currentTask.isAllDay) "Cả ngày" else currentTask.reminderTime ?: "Không hẹn giờ"
        val repeatDisplay = currentTask.repeatDays?.joinToString(", ") ?: ""
        binding.tvTaskInfo.text = if (repeatDisplay.isNotEmpty()) "$timeDisplay | Lặp lại: $repeatDisplay" else timeDisplay

        // 2. Xử lý màu sắc vạch ưu tiên (Priority Indicator)
        val priorityColor = when (currentTask.priority) {
            Priority.HIGH -> Color.parseColor("#D32F2F")   // Đỏ
            Priority.MEDIUM -> Color.parseColor("#FBC02D") // Vàng
            Priority.LOW -> Color.parseColor("#388E3C")    // Xanh lá
            else -> Color.LTGRAY
        }
        binding.priorityIndicator.setBackgroundColor(priorityColor)

        // 3. Hiệu ứng gạch ngang tên Task khi hoàn thành
        if (currentTask.isCompleted) {
            binding.tvTitle.paintFlags = binding.tvTitle.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            binding.root.alpha = 0.6f
        } else {
            binding.tvTitle.paintFlags = binding.tvTitle.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            binding.root.alpha = 1.0f
        }

        // 4. Xử lý sự kiện click CheckBox
        // Lưu ý: Dùng setOnClickListener thay vì OnCheckedChangeListener để tránh lỗi lặp khi cuộn
        binding.cbCompleted.setOnClickListener {
            onTaskChecked(currentTask)
        }
        binding.root.setOnLongClickListener {
            onTaskLongClick(currentTask)
            true // Trả về true để báo hiệu đã xử lý sự kiện
        }

        binding.executePendingBindings()
    }

    override fun getItemCount() = tasks.size

    fun updateData(newTasks: List<Task>) {
        tasks = newTasks
        notifyDataSetChanged()
    }
}