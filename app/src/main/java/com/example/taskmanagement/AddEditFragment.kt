package com.example.taskmanagement

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.taskmanagement.databinding.FragmentAddEditBinding

class AddEditFragment : Fragment(R.layout.fragment_add_edit) {

    // Slide 8: Sử dụng lazy để tránh lỗi phân tích biến lúc Build
    private val viewModel: TaskViewModel by lazy {
        ViewModelProvider(requireActivity())[TaskViewModel::class.java]
    }

    private var _binding: FragmentAddEditBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentAddEditBinding.bind(view)

        binding.btnSave.setOnClickListener {
            val title = binding.etTitle.text.toString().trim()
            if (title.isNotEmpty()) {
                // Sử dụng Priority mặc định để kiểm tra
                viewModel.addNewTask(title, "Mô tả", Priority.MEDIUM)
                Log.d("Lifecycle_Debug", "Task Added") // Slide 7
                findNavController().popBackStack() // Slide 6
            } else {
                binding.etTitle.error = "Không được để trống"
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // Giải phóng bộ nhớ (Slide 7)
    }
}