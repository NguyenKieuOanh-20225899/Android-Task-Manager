package com.example.taskmanagement

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

class AddEditFragment : Fragment(R.layout.fragment_add_edit) {

    private val viewModel: TaskViewModel by lazy {
        ViewModelProvider(requireActivity())[TaskViewModel::class.java]
    }

    private var _binding: FragmentAddEditBinding? = null
    private val binding get() = _binding!!

    private var selectedTime: String? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentAddEditBinding.bind(view)

        // 1. Nhận ngày được chọn từ màn hình trước (Calendar hoặc Home)
        val planDate = arguments?.getString("selectedDate") ?: "2025-12-21"
        binding.tvPlanDate.text = "Ngày kế hoạch: $planDate"

        // 2. Xử lý chọn giờ nhắc nhở
        binding.btnPickTime.setOnClickListener {
            val cal = Calendar.getInstance()
            TimePickerDialog(requireContext(), { _, hour, minute ->
                selectedTime = String.format("%02d:%02d", hour, minute)
                binding.btnPickTime.text = selectedTime
            }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show()
        }

        // 3. Xử lý trạng thái All Day (Cả ngày)
        binding.swAllDay.setOnCheckedChangeListener { _, isChecked ->
            binding.btnPickTime.isEnabled = !isChecked
            if (isChecked) binding.btnPickTime.text = "Cả ngày"
            else binding.btnPickTime.text = selectedTime ?: "Chọn giờ"
        }

        // 4. Nút Lưu kế hoạch
        binding.btnSave.setOnClickListener {
            val title = binding.etTitle.text.toString().trim()

            if (title.isNotEmpty()) {
                // Đọc mức độ ưu tiên từ ChipGroup
                val priority = when (binding.cgPriority.checkedChipId) {
                    R.id.chipHigh -> Priority.HIGH
                    R.id.chipMedium -> Priority.MEDIUM
                    else -> Priority.LOW
                }

                // Đọc danh sách các thứ lặp lại (T2, T3...)
                val repeatDays = mutableListOf<String>()
                for (id in binding.cgRepeatDays.checkedChipIds) {
                    val chip = view.findViewById<Chip>(id)
                    repeatDays.add(chip.text.toString())
                }

                // Gọi ViewModel để lưu
                viewModel.addNewTask(
                    title = title,
                    description = "", // Bạn có thể thêm EditText cho mô tả nếu muốn
                    priority = priority,
                    date = planDate,
                    reminderTime = selectedTime,
                    repeatDays = if (repeatDays.isEmpty()) null else repeatDays,
                    isAllDay = binding.swAllDay.isChecked
                )

                Log.d("TaskSave", "Đã lưu Task: $title cho ngày $planDate")
                findNavController().popBackStack() // Quay lại màn hình danh sách
            } else {
                binding.etTitle.error = "Vui lòng nhập tên công việc"
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}