package com.veggievision.lokatani.view

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.veggievision.lokatani.R
import com.veggievision.lokatani.databinding.ActivityHostBinding

class HostActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityHostBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHostBinding.inflate(layoutInflater)
        setContentView(binding.root)

//        val navController = findNavController(R.id.nav_host_fragment_content_host)

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment_content_host) as NavHostFragment
        val navController = navHostFragment.navController


        // Define which destinations are top-level (no back arrow shown)
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.mainFragment,
                R.id.cameraFragment,
                R.id.historyFragment,
                R.id.nlpFragment
            )
        )

//        setupActionBarWithNavController(navController, appBarConfiguration)
        binding.bottomNav.setupWithNavController(navController)

    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_host)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }
}
