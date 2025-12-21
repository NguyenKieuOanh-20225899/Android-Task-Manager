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

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val calendar = Calendar.getInstance()

        // 1. Thiết lập ngày mặc định (Hôm nay)
        selectedDate = sdf.format(calendar.time)
        binding.tvSelectedDateDisplay.text = "Ngày đã chọn: $selectedDate"

        // 2. Khởi tạo DatePicker
        // init nhận vào: year, month, day và một listener
        binding.datePicker.init(
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ) { _, year, monthOfYear, dayOfMonth ->

            val selectedCal = Calendar.getInstance()
            selectedCal.set(year, monthOfYear, dayOfMonth)
            selectedDate = sdf.format(selectedCal.time)

            binding.tvSelectedDateDisplay.text = "Ngày đã chọn: $selectedDate"
        }

        // 3. Nút xác nhận chuyển sang HomeFragment
        binding.btnConfirmDate.setOnClickListener {
            val bundle = Bundle().apply {
                putString("selectedDate", selectedDate)
            }
            findNavController().navigate(R.id.action_calendarFragment_to_homeFragment, bundle)
        }
    }
}