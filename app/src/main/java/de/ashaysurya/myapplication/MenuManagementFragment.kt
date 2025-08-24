package de.ashaysurya.myapplication

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import de.ashaysurya.myapplication.databinding.FragmentMenuManagementBinding

class MenuManagementFragment : Fragment() {

    private val menuViewModel: MenuViewModel by viewModels()
    private var _binding: FragmentMenuManagementBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMenuManagementBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = MenuListAdapter { menuItem ->
            showOptionsDialog(menuItem)
        }
        binding.recyclerView.adapter = adapter
        binding.recyclerView.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(requireContext())

        menuViewModel.allMenuItems.observe(viewLifecycleOwner) { items ->
            items?.let { adapter.submitList(it) }
        }

        binding.fab.setOnClickListener {
            showAddItemDialog()
        }
    }

    private fun showOptionsDialog(menuItem: MenuItem) {
        val options = arrayOf("Edit", "Delete")
        AlertDialog.Builder(requireContext())
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
        val builder = AlertDialog.Builder(requireContext())
        val dialogLayout = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_item, null)
        val editTextName = dialogLayout.findViewById<EditText>(R.id.editTextName)
        val editTextPrice = dialogLayout.findViewById<EditText>(R.id.editTextPrice)
        val editTextCategory = dialogLayout.findViewById<EditText>(R.id.editTextCategory)

        editTextName.setText(menuItem.name)
        editTextPrice.setText(menuItem.price.toString())
        editTextCategory.setText(menuItem.category)

        builder.setTitle("Edit Menu Item")
            .setView(dialogLayout)
            .setPositiveButton("Save") { dialog, _ ->
                val name = editTextName.text.toString().trim()
                val priceString = editTextPrice.text.toString().trim()
                val category = editTextCategory.text.toString().trim()

                if (name.isNotEmpty() && priceString.isNotEmpty() && category.isNotEmpty()) {
                    val price = priceString.toDoubleOrNull()
                    if (price != null) {
                        val updatedItem = menuItem.copy(name = name, price = price, category = category)
                        menuViewModel.update(updatedItem)
                        dialog.dismiss()
                    } else {
                        Toast.makeText(requireContext(), "Please enter a valid price", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(requireContext(), "Please fill out all fields", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showDeleteConfirmationDialog(menuItem: MenuItem) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Item")
            .setMessage("Are you sure you want to delete '${menuItem.name}'?")
            .setPositiveButton("Delete") { _, _ ->
                menuViewModel.delete(menuItem)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showAddItemDialog() {
        val builder = AlertDialog.Builder(requireContext())
        val dialogLayout = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_item, null)
        val editTextName = dialogLayout.findViewById<EditText>(R.id.editTextName)
        val editTextPrice = dialogLayout.findViewById<EditText>(R.id.editTextPrice)
        val editTextCategory = dialogLayout.findViewById<EditText>(R.id.editTextCategory)

        builder.setTitle("Add New Menu Item")
            .setView(dialogLayout)
            .setPositiveButton("Add") { dialog, _ ->
                val name = editTextName.text.toString().trim()
                val priceString = editTextPrice.text.toString().trim()
                val category = editTextCategory.text.toString().trim()

                if (name.isNotEmpty() && priceString.isNotEmpty() && category.isNotEmpty()) {
                    val price = priceString.toDoubleOrNull()
                    if (price != null) {
                        val newItem = MenuItem(name = name, price = price, category = category)
                        menuViewModel.insert(newItem)
                        dialog.dismiss()
                    } else {
                        Toast.makeText(requireContext(), "Please enter a valid price", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(requireContext(), "Please fill out all fields", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}