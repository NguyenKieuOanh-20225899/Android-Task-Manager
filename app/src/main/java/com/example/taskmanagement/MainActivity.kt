package com.example.taskmanagement

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupActionBarWithNavController

class MainActivity : AppCompatActivity() {

    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Đảm bảo nạp layout trước khi tìm kiếm các View bên trong
        setContentView(R.layout.activity_main)

        // Tìm NavHostFragment từ layout
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment

        // Khởi tạo NavController
        navController = navHostFragment.navController

        // Thiết lập ActionBar để hiển thị tiêu đề và nút Back tự động
        setupActionBarWithNavController(navController)
    }

    // Quan trọng: Ghi đè phương thức này để nút Back trên Toolbar hoạt động
    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
}