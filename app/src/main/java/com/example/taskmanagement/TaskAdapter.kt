package com.example.taskmanagement

import android.graphics.Color
import android.graphics.Paint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.taskmanagement.databinding.ItemTaskBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TaskAdapter(
    private var tasks: List<Task>,
    private val onTaskChecked: (Task) -> Unit,
    private val onTaskLongClick: (Task) -> Unit,
    private val onEditClick: (Task) -> Unit
) : RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {

    class TaskViewHolder(val binding: ItemTaskBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val binding = ItemTaskBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TaskViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val currentTask = tasks[position]
        val binding = holder.binding

        // Lấy ngày hiện tại để so sánh trễ hạn
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        // Điều kiện tồn đọng: Ngày cũ hơn hôm nay và CHƯA hoàn thành
        val isOverdue = currentTask.date < today && !currentTask.isCompleted

        // Gán dữ liệu cho DataBinding (Tự động cập nhật ivSyncStatus dựa trên isSynced trong XML)
        binding.task = currentTask

        // 1. Hiển thị thông tin thời gian & lặp lại
        val timeDisplay = if (currentTask.isAllDay) "Cả ngày" else currentTask.reminderTime ?: "Không hẹn giờ"
        val repeatDisplay = currentTask.repeatDays ?: ""
        val baseInfo = if (repeatDisplay.isNotEmpty()) "$timeDisplay | Lặp lại: $repeatDisplay" else timeDisplay

        // Bổ sung logic hiển thị Tồn đọng (Màu sắc & Icon cảnh báo)
        if (isOverdue) {
            binding.tvTitle.setTextColor(Color.parseColor("#F44336")) // Màu đỏ
            binding.tvTaskInfo.setTextColor(Color.parseColor("#F44336"))
            binding.tvTaskInfo.text = "⚠️ Trễ hạn (${currentTask.date}) | $baseInfo"
        } else {
            binding.tvTitle.setTextColor(Color.parseColor("#333333"))
            binding.tvTaskInfo.setTextColor(Color.GRAY)
            binding.tvTaskInfo.text = baseInfo
        }

        // 2. Xử lý màu sắc vạch ưu tiên (Priority Indicator)
        val priorityColor = when (currentTask.priority) {
            Priority.URGENT -> Color.parseColor("#B71C1C") // Đỏ đậm
            Priority.HIGH -> Color.parseColor("#D32F2F")   // Đỏ
            Priority.MEDIUM -> Color.parseColor("#FBC02D") // Vàng
            Priority.LOW -> Color.parseColor("#388E3C")    // Xanh lá
        }
        binding.priorityIndicator.setBackgroundColor(priorityColor)

        // 3. Hiệu ứng gạch ngang và mờ khi hoàn thành
        if (currentTask.isCompleted) {
            binding.tvTitle.paintFlags = binding.tvTitle.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            binding.root.alpha = 0.5f
            binding.tvTitle.setTextColor(Color.GRAY)
            binding.tvTaskInfo.setTextColor(Color.GRAY)
        } else {
            binding.tvTitle.paintFlags = binding.tvTitle.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            // Nếu không trễ hạn thì để hiện rõ 100%
            if (!isOverdue) binding.root.alpha = 1.0f
        }

        // 4. Xử lý sự kiện click
        binding.cbCompleted.setOnClickListener {
            onTaskChecked(currentTask)
        }

        binding.ivEdit.setOnClickListener {
            onEditClick(currentTask)
        }

        binding.root.setOnLongClickListener {
            onTaskLongClick(currentTask)
            true
        }

        // Ép DataBinding cập nhật ngay để hiển thị chính xác trạng thái Sync (Đám mây)
        binding.executePendingBindings()
    }

    override fun getItemCount() = tasks.size

    fun updateData(newTasks: List<Task>) {
        tasks = newTasks
        notifyDataSetChanged()
    }
}