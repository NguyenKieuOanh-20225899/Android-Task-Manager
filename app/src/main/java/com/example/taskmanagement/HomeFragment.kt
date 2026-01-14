package com.example.taskmanagement

import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
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

        // --- 1. XỬ LÝ NGÀY THÁNG ---
        val selectedDate = arguments?.getString("selectedDate") ?: getCurrentDate()
        binding.tvCurrentDateHeader.text = "Kế hoạch ngày: $selectedDate"
        viewModel.filterByDate(selectedDate)

        // --- 2. THIẾT LẬP ADAPTER ---
        val adapter = TaskAdapter(
            emptyList(),
            onTaskChecked = { task -> viewModel.toggleTaskStatus(task) },
            onTaskLongClick = { task -> showDeleteDialog(task) },
            onEditClick = { task ->
                val bundle = Bundle().apply {
                    putParcelable("task_to_edit", task) // Truyền toàn bộ đối tượng Task sang màn hình sửa
                }
                findNavController().navigate(R.id.action_homeFragment_to_addEditFragment, bundle)
            }
        )
        binding.rvTasks.adapter = adapter

        // --- 3. THIẾT LẬP MENU TOOLBAR ---
        setupToolbarMenu()

        // --- 4. QUAN SÁT DỮ LIỆU (OBSERVERS) ---
        viewModel.tasks.observe(viewLifecycleOwner) { updatedList ->
            adapter.updateData(updatedList)
            if (updatedList.isEmpty()) {
                Log.d(TAG, "Không có nhiệm vụ cho ngày $selectedDate")
            }
        }

        viewModel.completionRate.observe(viewLifecycleOwner) { rate ->
            binding.pbCompletion.progress = rate
        }

        // --- 5. SỰ KIỆN CLICK ---
        binding.fabAdd.setOnClickListener {
            val bundle = Bundle().apply {
                putString("selectedDate", selectedDate)
            }
            findNavController().navigate(R.id.action_homeFragment_to_addEditFragment, bundle)
        }

        binding.chipGroupFilter.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isNotEmpty()) {
                when (checkedIds.first()) {
                    R.id.chipAll -> viewModel.filterByDate(selectedDate)
                    R.id.chipPending -> viewModel.showPendingTasks()
                    R.id.chipCompleted -> viewModel.showCompletedTasks()
                }
            }
        }
    }

    private fun setupToolbarMenu() {
        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.home_menu, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.menu_calendar -> {
                        findNavController().navigate(R.id.calendarFragment)
                        true
                    }
                    R.id.menu_welcome -> {
                        findNavController().navigate(R.id.homeBeforeFragment)
                        true
                    }
                    R.id.menu_home -> {
                        // Nếu đã ở Home rồi thì chỉ cần cuộn lên đầu hoặc làm mới
                        viewModel.filterByDate(getCurrentDate())
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun getCurrentDate(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    private fun showDeleteDialog(task: Task) {
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Xóa kế hoạch")
            .setMessage("Bạn có chắc chắn muốn xóa '${task.title}' không?")
            .setPositiveButton("Xóa") { _, _ ->
                viewModel.deleteTask(task)
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume: Người dùng đang xem danh sách công việc")
    }
}