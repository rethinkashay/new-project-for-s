package de.ashaysurya.myapplication

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import de.ashaysurya.myapplication.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var navController: NavController
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set our custom toolbar from the layout as the Activity's action bar
        setSupportActionBar(binding.toolbar)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        // Define which screens are "top-level" (should show the hamburger icon)
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_pos, R.id.nav_dashboard, R.id.nav_menu_management
            ), binding.drawerLayout // Link it to the DrawerLayout
        )

        // Connect the action bar (our toolbar) to the NavController
        setupActionBarWithNavController(navController, appBarConfiguration)

        // Connect the NavigationView (the drawer menu) to the NavController
        binding.navView.setupWithNavController(navController)
    }

    // This function is the KEY to making the hamburger/back icon navigate correctly
    override fun onSupportNavigateUp(): Boolean {
        return NavigationUI.navigateUp(navController, appBarConfiguration) || super.onSupportNavigateUp()
    }
}