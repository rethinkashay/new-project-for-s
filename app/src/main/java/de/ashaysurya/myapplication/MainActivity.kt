package de.ashaysurya.myapplication

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.updatePadding
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.navigation.NavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Add this line to call the new function
        setupEdgeToEdge()

        val drawerLayout = findViewById<DrawerLayout>(R.id.drawer_layout)
        val navView = findViewById<NavigationView>(R.id.nav_view)
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)

        setSupportActionBar(toolbar)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_pos, R.id.nav_dashboard, R.id.nav_menu_management
            ), drawerLayout
        )

        toolbar.setupWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
    }

    // NEW FUNCTION
    private fun setupEdgeToEdge() {
        // This tells the system to let our app draw behind the status and navigation bars.
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Find your main content layout
        val mainLayout = findViewById<DrawerLayout>(R.id.drawer_layout)
        ViewCompat.setOnApplyWindowInsetsListener(mainLayout) { view, windowInsets ->
            val insets = windowInsets.getInsets(
                androidx.core.view.WindowInsetsCompat.Type.systemBars()
            )
            // Apply padding to the top and bottom of the view
            view.updatePadding(top = insets.top, bottom = insets.bottom)
            // Consume the insets so they aren't applied twice
            androidx.core.view.WindowInsetsCompat.CONSUMED
        }
    }
}