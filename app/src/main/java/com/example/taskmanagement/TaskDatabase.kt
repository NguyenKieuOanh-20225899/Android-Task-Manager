package com.example.taskmanagement

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
/**
 * Khai báo cơ sở dữ liệu Room
 * entities: Danh sách các bảng (ở đây là Task)
 * version: Phiên bản DB (tăng số này khi bạn thay đổi cấu trúc bảng)
 * exportSchema: Không xuất sơ đồ DB ra file ngoài (giảm nhẹ dung lượng)
 */
@Database(entities = [Task::class], version = 1, exportSchema = false)
abstract class TaskDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao // Phương thức trừu tượng để lấy TaskDao

    companion object {
        /**
         * @Volatile: Đảm bảo giá trị của INSTANCE luôn được cập nhật mới nhất
         * cho tất cả các luồng (threads) khác nhau.
         */
        @Volatile
        private var INSTANCE: TaskDatabase? = null
        /**
         * Hàm khởi tạo cơ sở dữ liệu (Singleton Pattern)
         * Giúp đảm bảo chỉ có duy nhất một phiên bản DB được tạo ra trong suốt vòng đời app.
         */
        fun getDatabase(context: Context): TaskDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TaskDatabase::class.java,
                    "task_database"
                ).fallbackToDestructiveMigration().build() //khi thay doi xoa sach db cu va tao moi
                INSTANCE = instance
                instance
            }
        }
    }
}