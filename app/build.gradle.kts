plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("com.google.devtools.ksp")
    id("kotlin-parcelize")
}

android {
    namespace = "com.example.taskmanagement"
    compileSdk = 34 // Đã sửa nhẹ từ release(36) để đảm bảo chuẩn Gradle, bạn có thể chỉnh lại nếu cần

    defaultConfig {
        applicationId = "com.example.taskmanagement"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        dataBinding = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // ViewModel và LiveData (Slide 8)
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.2")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.6.2")
    implementation("com.airbnb.android:lottie:6.1.0")

    // Navigation Component (Slide 6)
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("androidx.navigation:navigation-fragment-ktx:2.7.5")
    implementation("androidx.navigation:navigation-ui-ktx:2.7.5")

    // Room Database
    val room_version = "2.6.1"
    implementation("androidx.room:room-runtime:$room_version")
    implementation("androidx.room:room-ktx:$room_version")
    ksp("androidx.room:room-compiler:$room_version")

    // === PHẦN THÊM MỚI ĐỂ CHẠY ONLINE (RETROFIT & OKHTTP) ===
    // Thư viện kết nối API
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    // Bộ chuyển đổi JSON sang Object tự động
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    // Log dữ liệu API gửi/nhận để dễ debug
    implementation("com.squareup.okhttp3:logging-interceptor:4.11.0")
    implementation("androidx.work:work-runtime-ktx:2.8.1")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
}