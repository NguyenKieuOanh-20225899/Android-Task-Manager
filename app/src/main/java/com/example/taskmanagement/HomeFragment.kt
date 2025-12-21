package com.example.taskmanagement

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.taskmanagement.databinding.FragmentHomeBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HomeFragment : Fragment(R.layout.fragment_home) {

    private lateinit var viewModel: TaskViewModel
    private val TAG = "Lifecycle_Home"

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentHomeBinding.bind(view)

        // Khởi tạo ViewModel dùng chung Activity
        viewModel = ViewModelProvider(requireActivity()).get(TaskViewModel::class.java)

        // 1. Lấy ngày được chọn từ CalendarFragment truyền sang
        // Nếu không có (mở app lần đầu), mặc định lấy ngày hiện tại
        val selectedDate = arguments?.getString("selectedDate") ?: getCurrentDate()
        binding.tvCurrentDateHeader.text = "Kế hoạch ngày: $selectedDate"

        // 2. Tự động lọc danh sách theo ngày này ngay khi vào màn hình
        viewModel.filterByDate(selectedDate)

        // Thiết lập Adapter cho RecyclerView
        val adapter = TaskAdapter(
            emptyList(),
            onTaskChecked = { task -> viewModel.toggleTaskStatus(task) },
            onTaskLongClick = { task ->
                showDeleteDialog(task)
            }
        )
        binding.rvTasks.adapter = adapter

        // 3. Quan sát danh sách công việc (đã được lọc theo ngày)
        viewModel.tasks.observe(viewLifecycleOwner) { updatedList ->
            adapter.updateData(updatedList)
            // Hiển thị thông báo nếu ngày này không có task nào
            if (updatedList.isEmpty()) {
                Log.d(TAG, "Không có nhiệm vụ cho ngày $selectedDate")
            }
        }

        // 4. Quan sát tiến độ hoàn thành (%) để cập nhật ProgressBar
        viewModel.completionRate.observe(viewLifecycleOwner) { rate ->
            binding.pbCompletion.progress = rate
        }

        // 5. Điều hướng sang màn hình Thêm mới (Truyền kèm ngày hiện tại để làm mặc định)
        binding.fabAdd.setOnClickListener {
            val bundle = Bundle().apply {
                putString("selectedDate", selectedDate)
            }
            findNavController().navigate(R.id.action_homeFragment_to_addEditFragment, bundle)
        }

        // 6. Logic lọc trạng thái (Tất cả, Chưa xong, Đã xong) trong ngày đó
        binding.chipGroupFilter.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isNotEmpty()) {
                when (checkedIds.first()) {
                    R.id.chipAll -> viewModel.filterByDate(selectedDate) // Lọc lại theo ngày
                    R.id.chipPending -> viewModel.showPendingTasks()
                    R.id.chipCompleted -> viewModel.showCompletedTasks()
                }
            }
        }
    }

    private fun getCurrentDate(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    // Các hàm Lifecycle để theo dõi log
    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume: Người dùng đang xem danh sách công việc")
    }
    private fun showDeleteDialog(task: Task) {
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Xóa kế hoạch")
            .setMessage("Bạn có chắc chắn muốn xóa '${task.title}' không?")
            .setPositiveButton("Xóa") { _, _ ->
                viewModel.deleteTask(task) // Cần thêm hàm này vào ViewModel
            }
            .setNegativeButton("Hủy", null)
            .show()
    }
}