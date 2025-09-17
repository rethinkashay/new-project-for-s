package de.ashaysurya.myapplication

import android.Manifest
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.viewModelScope
import de.ashaysurya.myapplication.databinding.FragmentMenuManagementBinding
import de.ashaysurya.myapplication.network.RetrofitInstance
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.InputStream

class MenuManagementFragment : Fragment() {

    private val menuViewModel: MenuViewModel by viewModels()
    private var _binding: FragmentMenuManagementBinding? = null
    private val binding get() = _binding!!

    private var tempImageUri: Uri? = null

    // Launchers for picking/taking photos (no changes here)
    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { processImage(it) }
    }

    private val takePictureLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success: Boolean ->
        if (success) { tempImageUri?.let { processImage(it) } }
    }

    private val requestCameraPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
        if (isGranted) { launchCamera() } else {
            Toast.makeText(requireContext(), "Camera permission is required", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMenuManagementBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val adapter = MenuListAdapter { menuItem -> showOptionsDialog(menuItem) }
        binding.recyclerView.adapter = adapter
        binding.recyclerView.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(requireContext())
        menuViewModel.allMenuItems.observe(viewLifecycleOwner) { items -> items?.let { adapter.submitList(it) } }
        binding.fab.setOnClickListener { showAddItemDialog() }
        binding.fabScan.setOnClickListener { showImageSourceDialog() }
    }

    // --- THIS IS THE NEW CORE LOGIC THAT CALLS YOUR BACKEND ---
    private fun processImage(uri: Uri) {
        val imagePart = uriToMultipartBodyPart(uri)
        if (imagePart == null) {
            Toast.makeText(requireContext(), "Could not process the image file.", Toast.LENGTH_SHORT).show()
            return
        }
        Toast.makeText(requireContext(), "Uploading and processing image...", Toast.LENGTH_SHORT).show()

        menuViewModel.viewModelScope.launch {
            try {
                val response = RetrofitInstance.api.uploadImage(imagePart)
                if (response.isSuccessful && response.body() != null) {
                    val menuItems = response.body()!!
                    if (menuItems.isNotEmpty()) {
                        // Use the new, powerful confirmation dialog
                        showConfirmItemsDialog(menuItems)
                    } else {
                        Toast.makeText(requireContext(), "No menu items found in the image.", Toast.LENGTH_LONG).show()
                    }
                } else {
                    Toast.makeText(requireContext(), "Error: ${response.message()}", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Network Connection Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun showImageSourceDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Scan a Menu")
            .setItems(arrayOf("Take Photo", "Choose from Gallery")) { _, which ->
                when (which) {
                    0 -> requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    1 -> pickImageLauncher.launch("image/*")
                }
            }.show()
    }

    private fun launchCamera() {
        val tmpFile = File.createTempFile("temp_image", ".jpg", requireContext().cacheDir).apply {
            createNewFile()
            deleteOnExit()
        }
        tempImageUri = FileProvider.getUriForFile(requireContext(), "${requireContext().packageName}.provider", tmpFile)
        takePictureLauncher.launch(tempImageUri)
    }

    // --- NEW HELPER FUNCTION TO PREPARE THE FILE FOR UPLOAD ---
    private fun uriToMultipartBodyPart(uri: Uri): MultipartBody.Part? {
        return try {
            val inputStream = requireContext().contentResolver.openInputStream(uri)
            val fileBytes = inputStream?.readBytes()
            inputStream?.close()

            if (fileBytes != null) {
                val requestFile = fileBytes.toRequestBody("image/jpeg".toMediaTypeOrNull())
                MultipartBody.Part.createFormData("image", "image.jpg", requestFile)
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // --- NEW DIALOG TO CONFIRM AND SAVE PARSED ITEMS ---
    private fun showConfirmItemsDialog(items: List<MenuItem>) {
        val itemStrings = items.map { "${it.name} - ₹${it.price}" }.toTypedArray()
        AlertDialog.Builder(requireContext())
            .setTitle("Found ${items.size} Menu Items")
            .setItems(itemStrings, null) // Display items, not clickable
            .setPositiveButton("Add to Menu") { _, _ ->
                items.forEach { menuItem ->
                    // The item from the backend has no category, so we add one.
                    val itemToInsert = menuItem.copy(category = "Scanned")
                    menuViewModel.insert(itemToInsert)
                }
                Toast.makeText(requireContext(), "${items.size} items added.", Toast.LENGTH_LONG).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // --- DIALOG FUNCTIONS FOR MANUAL EDITING (UNCHANGED) ---

    private fun showOptionsDialog(menuItem: MenuItem) {
        AlertDialog.Builder(requireContext())
            .setTitle(menuItem.name)
            .setItems(arrayOf("Edit", "Delete")) { _, which ->
                when (which) {
                    0 -> showEditItemDialog(menuItem)
                    1 -> showDeleteConfirmationDialog(menuItem)
                }
            }.show()
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
            .setPositiveButton("Save") { _, _ ->
                val name = editTextName.text.toString().trim()
                val priceString = editTextPrice.text.toString().trim()
                val category = editTextCategory.text.toString().trim()
                if (name.isNotEmpty() && priceString.isNotEmpty() && category.isNotEmpty()) {
                    val price = priceString.toDoubleOrNull()
                    if (price != null) {
                        val updatedItem = menuItem.copy(name = name, price = price, category = category)
                        menuViewModel.update(updatedItem)
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
            .setPositiveButton("Add") { _, _ ->
                val name = editTextName.text.toString().trim()
                val priceString = editTextPrice.text.toString().trim()
                val category = editTextCategory.text.toString().trim()
                if (name.isNotEmpty() && priceString.isNotEmpty() && category.isNotEmpty()) {
                    val price = priceString.toDoubleOrNull()
                    if (price != null) {
                        val newItem = MenuItem(name = name, price = price, category = category)
                        menuViewModel.insert(newItem)
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