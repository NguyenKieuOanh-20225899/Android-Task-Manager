package com.example.taskmanagement

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.taskmanagement.databinding.ItemTaskBinding

class TaskAdapter(
    private var tasks: List<Task>,
    // Thêm callback để xử lý sự kiện click CheckBox
    private val onTaskChecked: (Task) -> Unit
) : RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {

    class TaskViewHolder(val binding: ItemTaskBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val binding = ItemTaskBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TaskViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val currentTask = tasks[position]
        holder.binding.task = currentTask

        // Bắt sự kiện click vào CheckBox
        holder.binding.cbCompleted.setOnClickListener {
            onTaskChecked(currentTask) // Gọi hàm xử lý từ Fragment/ViewModel
        }

        holder.binding.executePendingBindings()
    }

    override fun getItemCount() = tasks.size

    fun updateData(newTasks: List<Task>) {
        tasks = newTasks
        notifyDataSetChanged()
    }
}