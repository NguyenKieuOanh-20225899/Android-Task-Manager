package com.example.taskmanagement

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.taskmanagement.databinding.FragmentHomeBinding

class HomeFragment : Fragment(R.layout.fragment_home) {

    private lateinit var viewModel: TaskViewModel
    private val TAG = "Lifecycle_Home"

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "onViewCreated: Khởi tạo giao diện")

        val binding = FragmentHomeBinding.bind(view)

        // Slide 8: Lấy ViewModel chung của Activity để dùng chung dữ liệu với AddEditFragment
        viewModel = ViewModelProvider(requireActivity()).get(TaskViewModel::class.java)

        // Khởi tạo Adapter với Lambda callback để xử lý khi nhấn CheckBox (Slide 2 & 8)
        val adapter = TaskAdapter(emptyList()) { task ->
            viewModel.toggleTaskStatus(task) // Cập nhật trạng thái thông qua ViewModel
            Log.d("TaskAction", "Đã thay đổi trạng thái: ${task.title}")
        }

        binding.rvTasks.adapter = adapter

        // Slide 8: Quan sát LiveData - Tự động cập nhật UI khi danh sách thay đổi
        viewModel.tasks.observe(viewLifecycleOwner) { updatedList ->
            adapter.updateData(updatedList)
        }

        // Slide 6: Điều hướng sang màn hình thêm mới
        binding.fabAdd.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_addEditFragment)
        }

        // Slide 2: Logic lọc "Smart" sử dụng Chips
        binding.chipGroupFilter.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isNotEmpty()) {
                when (checkedIds.first()) {
                    R.id.chipAll -> viewModel.showAllTasks()
                    R.id.chipPending -> viewModel.showPendingTasks()
                    R.id.chipCompleted -> viewModel.showCompletedTasks()
                }
            }
        }
    }

    // Slide 7: Ghi Log đầy đủ các trạng thái Lifecycle để theo dõi
    override fun onStart() {
        super.onStart()
        Log.d(TAG, "onStart: Fragment bắt đầu hiển thị")
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume: Người dùng bắt đầu tương tác")
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "onPause: Fragment tạm dừng")
    }

    override fun onStop() {
        super.onStop()
        Log.d(TAG, "onStop: Fragment không còn hiển thị")
    }
}