package com.example.taskmanagement

import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController

class HomeBeforeFragment : Fragment(R.layout.fragment_home_before) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnStart = view.findViewById<Button>(R.id.btnStart)
        btnStart.setOnClickListener {
            // Điều hướng đến HomeFragment (màn hình danh sách công việc)
            findNavController().navigate(R.id.action_homeBeforeFragment_to_homeFragment)
        }
    }
}