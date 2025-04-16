package com.veggievision.lokatani.view

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraXThreads.TAG
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.veggievision.lokatani.R
import com.veggievision.lokatani.databinding.ActivityMainBinding


class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("main", "Uncaught exception in thread ${thread.name}: ${throwable.message}", throwable)
        }
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)
//        clearTemporaryCapturedFiles()
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.button.setOnClickListener {
            val intent = Intent(this, CameraActivity::class.java)
            startActivity(intent) }
    }
    private fun clearTemporaryCapturedFiles() {
        val cacheDir = cacheDir
        cacheDir.listFiles()?.forEach { file ->
            if (file.name.startsWith("captured_image_uri")) {
                file.delete()
            }
        }
    }
}