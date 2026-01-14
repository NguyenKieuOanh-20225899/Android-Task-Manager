package com.example.taskmanagement

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.taskmanagement.databinding.FragmentAddEditBinding
import com.google.android.material.chip.Chip
import java.util.*
import android.os.Build
class AddEditFragment : Fragment(R.layout.fragment_add_edit) {

    private val viewModel: TaskViewModel by lazy {
        ViewModelProvider(requireActivity())[TaskViewModel::class.java]
    }

    private var _binding: FragmentAddEditBinding? = null
    private val binding get() = _binding!!

    private var selectedTime: String? = null
    private var currentPlanDate: String = "" // Biến lưu trữ ngày đang được chọn

    private var editingTaskId: Int = 0
    private var isEditMode = false
    private var isTaskCompleted = false
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentAddEditBinding.bind(view)

        // 1. Nhận ngày ban đầu từ màn hình trước
        currentPlanDate = arguments?.getString("selectedDate") ?: "2025-12-21"
        val taskToEdit = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arguments?.getParcelable("task_to_edit", Task::class.java)
        } else {
            @Suppress("DEPRECATION")
            arguments?.getParcelable<Task>("task_to_edit")
        }

        taskToEdit?.let { task ->
            isEditMode = true
            editingTaskId = task.id
            // Bạn nên tạo biến này ở cấp độ class để lưu trạng thái hoàn thành cũ
             isTaskCompleted = task.isCompleted
            fillTaskData(task)
        }
        updateDateDisplay()

        // 2. LỰA CHỌN 1: Nhấn vào dòng ngày để hiện DatePickerDialog (Chọn nhanh)
        binding.tvPlanDate.setOnClickListener {
            val cal = Calendar.getInstance()
            // Cố gắng phân tích ngày hiện tại để hiển thị lịch đúng vị trí
            val dateParts = currentPlanDate.split("-")
            if (dateParts.size == 3) {
                cal.set(dateParts[0].toInt(), dateParts[1].toInt() - 1, dateParts[2].toInt())
            }

            DatePickerDialog(requireContext(), { _, year, month, day ->
                // Cập nhật lại biến currentPlanDate sau khi chọn
                currentPlanDate = String.format("%04d-%02d-%02d", year, month + 1, day)
                updateDateDisplay()
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
        }

        // 3. LỰA CHỌN 2: Nhấn nút quay về màn hình Calendar ô vuông lớn
        binding.btnBackToCalendar.setOnClickListener {
            // Quay lại màn hình CalendarFragment trong đồ thị điều hướng
            findNavController().popBackStack(R.id.calendarFragment, false)
        }

        // 4. Xử lý chọn giờ nhắc nhở
        binding.btnPickTime.setOnClickListener {
            val cal = Calendar.getInstance()
            TimePickerDialog(requireContext(), { _, hour, minute ->
                selectedTime = String.format("%02d:%02d", hour, minute)
                binding.btnPickTime.text = selectedTime
            }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show()
        }

        // 5. Xử lý trạng thái All Day (Cả ngày)
        binding.swAllDay.setOnCheckedChangeListener { _, isChecked ->
            binding.btnPickTime.isEnabled = !isChecked
            if (isChecked) {
                binding.btnPickTime.text = "Cả ngày"
            } else {
                binding.btnPickTime.text = selectedTime ?: "Chọn giờ"
            }
        }

        // 6. Nút Lưu kế hoạch
        binding.btnSave.setOnClickListener {
            val title = binding.etTitle.text.toString().trim()

            if (title.isNotEmpty()) {
                val priority = when (binding.cgPriority.checkedChipId) {
                    R.id.chipHigh -> Priority.HIGH
                    R.id.chipMedium -> Priority.MEDIUM
                    else -> Priority.LOW
                }

                val repeatDays = mutableListOf<String>()
                for (id in binding.cgRepeatDays.checkedChipIds) {
                    val chip = view.findViewById<Chip>(id)
                    repeatDays.add(chip.text.toString())
                }

                // QUAN TRỌNG: Sử dụng currentPlanDate (ngày có thể đã thay đổi) thay vì planDate cũ
                if (isEditMode) {
                    // Chế độ Sửa: Truyền thêm ID và trạng thái hoàn thành cũ
                    viewModel.updateTask(
                        id = editingTaskId,
                        title = title,
                        description = "", // Có thể bổ sung etDescription.text nếu có
                        priority = priority,
                        date = currentPlanDate,
                        reminderTime = if (binding.swAllDay.isChecked) null else selectedTime,
                        repeatDays = if (repeatDays.isEmpty()) null else repeatDays,
                        isAllDay = binding.swAllDay.isChecked,
                        isCompleted = isTaskCompleted // Giữ nguyên trạng thái hoàn thành
                    )
                    Log.d("TaskUpdate", "Đã cập nhật Task ID: $editingTaskId")
                } else {
                    // Chế độ Thêm mới
                    viewModel.addNewTask(
                        title = title,
                        description = "",
                        priority = priority,
                        date = currentPlanDate,
                        reminderTime = if (binding.swAllDay.isChecked) null else selectedTime,
                        repeatDays = if (repeatDays.isEmpty()) null else repeatDays,
                        isAllDay = binding.swAllDay.isChecked
                    )
                    Log.d("TaskSave", "Đã thêm Task mới: $title")
                }

                //  quay về màn hình HomeFragment để xem danh sách
                findNavController().popBackStack(R.id.homeFragment, false)
            } else {
                binding.etTitle.error = "Vui lòng nhập tên công việc"
            }
        }
    }
    private fun fillTaskData(task: Task) {
        binding.etTitle.setText(task.title)
        currentPlanDate = task.date
        selectedTime = task.reminderTime
        binding.swAllDay.isChecked = task.isAllDay

        when (task.priority) {
            Priority.HIGH -> binding.cgPriority.check(R.id.chipHigh)
            Priority.MEDIUM -> binding.cgPriority.check(R.id.chipMedium)
            Priority.LOW -> binding.cgPriority.check(R.id.chipLow)
            Priority.URGENT -> { /* Check chip tương ứng nếu có */ }
        }

        // Tick lại các thứ lặp lại
        val days = task.repeatDays?.split(",") ?: emptyList()
        for (i in 0 until binding.cgRepeatDays.childCount) {
            val chip = binding.cgRepeatDays.getChildAt(i) as Chip
            if (days.contains(chip.text.toString())) {
                chip.isChecked = true
            }
        }
    }
    // Hàm cập nhật text hiển thị ngày
    private fun updateDateDisplay() {
        binding.tvPlanDate.text = "Ngày kế hoạch: $currentPlanDate"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}