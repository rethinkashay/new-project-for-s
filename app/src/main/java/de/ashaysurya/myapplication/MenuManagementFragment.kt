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
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import de.ashaysurya.myapplication.databinding.FragmentMenuManagementBinding
import java.io.File

class MenuManagementFragment : Fragment() {

    private val menuViewModel: MenuViewModel by viewModels()
    private var _binding: FragmentMenuManagementBinding? = null
    private val binding get() = _binding!!

    private var tempImageUri: Uri? = null

    // Launcher for getting an image from the gallery
    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { processImage(it) }
    }

    // Launcher for taking a picture with the camera
    private val takePictureLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success: Boolean ->
        if (success) {
            tempImageUri?.let { processImage(it) }
        }
    }

    // Launcher for requesting camera permission
    private val requestCameraPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
        if (isGranted) {
            launchCamera()
        } else {
            Toast.makeText(requireContext(), "Camera permission is required to take a picture", Toast.LENGTH_SHORT).show()
        }
    }

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

        binding.fabScan.setOnClickListener {
            showImageSourceDialog()
        }
    }

    private fun showImageSourceDialog() {
        val options = arrayOf("Take Photo", "Choose from Gallery")
        AlertDialog.Builder(requireContext())
            .setTitle("Scan a Menu")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    1 -> pickImageLauncher.launch("image/*")
                }
            }
            .show()
    }

    private fun launchCamera() {
        val tmpFile = File.createTempFile("temp_image", ".jpg", requireContext().cacheDir).apply {
            createNewFile()
            deleteOnExit()
        }

        tempImageUri = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.provider",
            tmpFile
        )
        takePictureLauncher.launch(tempImageUri)
    }

    private fun processImage(uri: Uri) {
        try {
            val image = InputImage.fromFilePath(requireContext(), uri)
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    // UPDATED: Instead of just showing the text, we now parse it.
                    val parsedItems = parseMenuText(visionText.text)
                    if (parsedItems.isNotEmpty()) {
                        showConfirmItemsDialog(parsedItems)
                    } else {
                        Toast.makeText(requireContext(), "Could not find any menu items.", Toast.LENGTH_LONG).show()
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(requireContext(), "Text recognition failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(requireContext(), "Failed to process image.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun parseMenuText(text: String): List<MenuItem> {
        val menuItems = mutableListOf<MenuItem>()
        // A more robust regex to find prices, including those with commas.
        val priceRegex = Regex("(?<=\\s|^)[₹]?[\\s]?(\\d{2,4}(?:[.,]\\d{2})?)(?=\\s|$)")
        val lines = text.split("\n").filter { it.isNotBlank() }

        val potentialItems = mutableMapOf<Int, String>() // Line number to potential item name
        val potentialPrices = mutableMapOf<Int, Double>() // Line number to price

        // First pass: Identify all potential names and prices by line number
        lines.forEachIndexed { index, line ->
            val priceMatch = priceRegex.find(line)
            // A line is a potential price if it contains a price and not too much other text
            if (priceMatch != null && line.length < 20) {
                priceMatch.groups[1]?.value?.toDoubleOrNull()?.let {
                    potentialPrices[index] = it
                }
            }
            // A line is a potential name if it's mostly letters and not just a short number
            if (line.any { it.isLetter() } && !line.matches(Regex("^\\d{1,3}$"))) {
                potentialItems[index] = line.substringBefore("Rs").trim()
            }
        }

        // Second pass: Associate prices with the nearest item name above them
        potentialPrices.forEach { (lineNum, price) ->
            // Look for the item name on the same line, or the closest one on a line above
            var bestItemLine = -1
            for (itemLineNum in potentialItems.keys) {
                if (itemLineNum <= lineNum && itemLineNum > bestItemLine) {
                    bestItemLine = itemLineNum
                }
            }

            if (bestItemLine != -1) {
                val name = potentialItems[bestItemLine]
                if (name != null && name.isNotEmpty()) {
                    menuItems.add(MenuItem(name = name, price = price, category = "Scanned"))
                }
            }
        }

        // Return only unique items
        return menuItems.distinctBy { it.name }
    }

    // NEW: This function shows the user the items we found and asks for confirmation.
    private fun showConfirmItemsDialog(items: List<MenuItem>) {
        val itemStrings = items.map { "${it.name} - ₹${it.price}" }.toTypedArray()

        AlertDialog.Builder(requireContext())
            .setTitle("Found ${items.size} Menu Items")
            .setItems(itemStrings, null) // Display items, but don't do anything on click
            .setPositiveButton("Add to Menu") { _, _ ->
                items.forEach { menuViewModel.insert(it) }
                Toast.makeText(requireContext(), "${items.size} items added to your menu.", Toast.LENGTH_LONG).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }


    // --- Your original dialog functions are below ---

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