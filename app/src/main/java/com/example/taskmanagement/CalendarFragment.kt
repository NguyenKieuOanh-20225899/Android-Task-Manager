package com.example.taskmanagement

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.taskmanagement.databinding.FragmentCalendarBinding
import java.text.SimpleDateFormat
import java.util.*

class CalendarFragment : Fragment(R.layout.fragment_calendar) {

    private var selectedDate: String = ""

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentCalendarBinding.bind(view)

        // 1. Khởi tạo ngày mặc định là ngày hiện tại
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        selectedDate = sdf.format(Date())
        binding.tvSelectedDateDisplay.text = "Ngày đã chọn: $selectedDate"

        // 2. Lắng nghe sự kiện khi người dùng chọn ngày khác trên Lịch
        // CalendarView hỗ trợ chọn Tháng/Năm trực tiếp ở thanh tiêu đề
        binding.calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            // Lưu ý: month trong CalendarView bắt đầu từ 0 (tháng 1 là 0)
            val calendar = Calendar.getInstance()
            calendar.set(year, month, dayOfMonth)
            selectedDate = sdf.format(calendar.time)

            binding.tvSelectedDateDisplay.text = "Ngày đã chọn: $selectedDate"
        }

        // 3. Nút xác nhận để chuyển sang HomeFragment
        binding.btnConfirmDate.setOnClickListener {
            val bundle = Bundle().apply {
                putString("selectedDate", selectedDate)
            }
            // Điều hướng và truyền ngày đã chọn
            findNavController().navigate(R.id.action_calendarFragment_to_homeFragment, bundle)
        }
    }
}