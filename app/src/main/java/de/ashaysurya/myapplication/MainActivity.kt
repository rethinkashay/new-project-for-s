package de.ashaysurya.myapplication

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton

class MainActivity : AppCompatActivity() {

    private val menuViewModel: MenuViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // --- Setup RecyclerView ---
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        val adapter = MenuListAdapter { menuItem ->
            showOptionsDialog(menuItem)
        }
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        // --- Observe Data ---
        menuViewModel.allMenuItems.observe(this) { items ->
            items?.let { adapter.submitList(it) }
        }

        // --- Setup Floating Action Button ---
        val fab = findViewById<FloatingActionButton>(R.id.fab)
        fab.setOnClickListener {
            showAddItemDialog()
        }

        // --- THIS IS THE MISSING CODE FOR YOUR BUTTONS ---
        val goToPosButton = findViewById<Button>(R.id.buttonGoToPos)
        goToPosButton.setOnClickListener {
            val intent = Intent(this, PosActivity::class.java)
            startActivity(intent)
        }

        val goToDashboardButton = findViewById<Button>(R.id.buttonGoToDashboard)
        goToDashboardButton.setOnClickListener {
            val intent = Intent(this, DashboardActivity::class.java)
            startActivity(intent)
        }
    }

    private fun showOptionsDialog(menuItem: MenuItem) {
        val options = arrayOf("Edit", "Delete")
        AlertDialog.Builder(this)
            .setTitle(menuItem.name)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showEditItemDialog(menuItem)
                    1 -> showDeleteConfirmationDialog(menuItem)
                }
            }
            .show()
    }

    private fun showEditItemDialog(menuItem: MenuItem) {
        val builder = AlertDialog.Builder(this)
        val dialogLayout = LayoutInflater.from(this).inflate(R.layout.dialog_add_item, null)
        val editTextName = dialogLayout.findViewById<EditText>(R.id.editTextName)
        val editTextPrice = dialogLayout.findViewById<EditText>(R.id.editTextPrice)

        editTextName.setText(menuItem.name)
        editTextPrice.setText(menuItem.price.toString())

        builder.setTitle("Edit Menu Item")
            .setView(dialogLayout)
            .setPositiveButton("Save") { dialog, _ ->
                val name = editTextName.text.toString().trim()
                val priceString = editTextPrice.text.toString().trim()
                if (name.isNotEmpty() && priceString.isNotEmpty()) {
                    val price = priceString.toDoubleOrNull()
                    if (price != null) {
                        // We use .copy() to create a new object with the same ID
                        val updatedItem = menuItem.copy(name = name, price = price)
                        menuViewModel.update(updatedItem)
                    }
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showDeleteConfirmationDialog(menuItem: MenuItem) {
        AlertDialog.Builder(this)
            .setTitle("Delete Item")
            .setMessage("Are you sure you want to delete '${menuItem.name}'?")
            .setPositiveButton("Delete") { _, _ ->
                menuViewModel.delete(menuItem)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showAddItemDialog() {
        val builder = AlertDialog.Builder(this)
        val dialogLayout = LayoutInflater.from(this).inflate(R.layout.dialog_add_item, null)
        val editTextName = dialogLayout.findViewById<EditText>(R.id.editTextName)
        val editTextPrice = dialogLayout.findViewById<EditText>(R.id.editTextPrice)

        builder.setTitle("Add New Menu Item")
            .setView(dialogLayout)
            .setPositiveButton("Add") { dialog, _ ->
                val name = editTextName.text.toString().trim()
                val priceString = editTextPrice.text.toString().trim()
                if (name.isNotEmpty() && priceString.isNotEmpty()) {
                    val price = priceString.toDoubleOrNull()
                    if (price != null) {
                        val newItem = MenuItem(name = name, price = price)
                        menuViewModel.insert(newItem)
                    }
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}