package com.paparazziapps.pretamistapp.presentation.clients

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.paparazziapps.pretamistapp.R
import com.paparazziapps.pretamistapp.databinding.FragmentClientsAddBinding
import com.paparazziapps.pretamistapp.helper.PADialogFactory
import com.paparazziapps.pretamistapp.helper.base.BaseViewModel
import com.paparazziapps.pretamistapp.helper.hideKeyboardActivity
import com.paparazziapps.pretamistapp.helper.isValidEmail
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.util.*

class ClientsAddFragment : Fragment() {

    private val viewModel by viewModel<ClientsAddViewModel>()
    private val LOCATION_REQUEST_CODE = 1001
    private val CAMERA_REQUEST_CODE = 1002

    private var _binding: FragmentClientsAddBinding? = null
    private val binding get() = _binding!!

    private val loadingDialog by lazy {
        PADialogFactory(requireContext()).createLoadingDialog()
    }

    private val generalErrorDialog by lazy {
        PADialogFactory(requireContext()).createGeneralErrorDialog(
            onRetryClick = {
                viewModel.processIntent(ClientsAddViewModel.ClientsAddIntent.None)
            }
        )
    }

    private val generalSuccessDialog by lazy {
        PADialogFactory(requireContext()).createGeneralSuccessDialog(
            successMessage = getString(R.string.operation_success_message),
            buttonTitle = getString(R.string.continue_button_message),
            onConfirmClick = {
                viewModel.processIntent(ClientsAddViewModel.ClientsAddIntent.None)
            }
        )
    }

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var photoUriList = mutableListOf<Uri>()  // Lista para guardar las fotos

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentClientsAddBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        setupButtons()
        setupObservers()
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            viewModel.uiState.flowWithLifecycle(lifecycle).collectLatest { uiState ->
                when (uiState) {
                    is BaseViewModel.UiState.Error -> {
                        loadingDialog.dismiss()
                        generalErrorDialog.show()
                    }
                    BaseViewModel.UiState.GenericError -> {
                        loadingDialog.dismiss()
                        generalErrorDialog.show()
                    }
                    BaseViewModel.UiState.Idle -> {
                        // Do nothing
                    }
                    BaseViewModel.UiState.Loading -> {
                        loadingDialog.show()
                    }
                    is BaseViewModel.UiState.Success<*> -> {
                        loadingDialog.dismiss()
                        generalSuccessDialog.show()
                    }
                }
            }
        }
    }

    private fun setupButtons() {
        binding.saveMessageButton.setOnClickListener {
            hideKeyboardActivity(requireActivity())
            handledSaveMessage()
        }

        binding.gpsButton.setOnClickListener {
            hideKeyboardActivity(requireActivity())
            requestLocationPermission()
        }

        binding.cameraIcon.setOnClickListener {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                takePicture()
            } else {
                ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.CAMERA), CAMERA_REQUEST_CODE)
            }
        }
    }

    private fun handledSaveMessage() {
        val name = binding.nameEditText.text.toString()
        val email = binding.emailEditText.text.toString()
        val lastName = binding.lastNameEditText.text.toString()
        val document = binding.documentEditText.text.toString()
        val phone = binding.phoneEditText.text.toString()
        val latitude = binding.latitudEditText.text.toString()
        val longitude = binding.longitudEditText.text.toString()

        // Validations
        binding.namesLayout.error = when {
            name.isEmpty() -> {
                getString(R.string.error_name_empty)
            }
            else -> null
        }

        binding.emailLayout.error = when {
            email.isEmpty() -> {
                getString(R.string.error_email_empty)
            }
            !isValidEmail(email) -> {
                getString(R.string.error_email_invalid)
            }
            else -> null
        }

        binding.lastNameLayout.error = when {
            lastName.isEmpty() -> {
                getString(R.string.error_last_name_empty)
            }
            else -> null
        }

        binding.documentLayout.error = when {
            document.isEmpty() -> {
                getString(R.string.error_document_empty)
            }
            else -> null
        }

        binding.phoneLayout.error = when {
            phone.isEmpty() -> {
                getString(R.string.error_phone_empty)
            }
            else -> null
        }

        if (name.isNotEmpty() && email.isNotEmpty() && lastName.isNotEmpty() && document.isNotEmpty() && phone.isNotEmpty() && isValidEmail(email)) {
            // Subir las fotos antes de guardar los datos del cliente
            photoUriList.forEach { uri ->
                uploadPhotoToFirebase(uri)
            }

            // Luego de subir las fotos, guardamos el cliente
            viewModel.processIntent(
                ClientsAddViewModel.ClientsAddIntent.SaveClientIntent(
                    document = document,
                    name = name,
                    email = email,
                    phone = phone,
                    lastName = lastName,
                    latitude = latitude,
                    longitude = longitude
                )
            )
        } else {
            Log.d("ClientsAddFragment", "Error in the form")
        }
    }

    private fun requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            // Request permissions
            requestPermissions(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                LOCATION_REQUEST_CODE
            )
        } else {
            startLocationUpdates()
        }
    }

    private fun startLocationUpdates() {
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    binding.latitudEditText.setText(it.latitude.toString())
                    binding.longitudEditText.setText(it.longitude.toString())
                } ?: run {
                    Log.e("ClientsAddFragment", "Location not available")
                }
            }
        } catch (e: SecurityException) {
            Log.e("ClientsAddFragment", "Permission denied: $e")
        }
    }

    private fun takePicture() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        startActivityForResult(intent, CAMERA_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == CAMERA_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            val imageUri = data?.data
            imageUri?.let {
                if (photoUriList.size < 3) {
                    photoUriList.add(it)
                    // Mostrar la imagen o hacer algo con ella
                    Log.d("Camera", "Image URI: $it")
                } else {
                    Log.e("Camera", "You can only upload 3 photos.")
                }
            }
        }
    }

    private fun uploadPhotoToFirebase(photoUri: Uri) {
        val storageRef = FirebaseStorage.getInstance().reference
        val photoRef: StorageReference = storageRef.child("clients_photos/${UUID.randomUUID()}.jpg")

        photoRef.putFile(photoUri)
            .addOnSuccessListener { taskSnapshot ->
                Log.d("Upload", "Upload successful: ${taskSnapshot.metadata?.path}")
                // Puedes guardar la URL de la foto en Firebase Database o en otro lugar
                photoRef.downloadUrl.addOnSuccessListener { uri ->
                    Log.d("Upload", "Photo URL: $uri")
                    // Guardar la URL en tu modelo de cliente si es necesario
                }
            }
            .addOnFailureListener { exception ->
                Log.e("Upload", "Upload failed: ${exception.message}")
            }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == CAMERA_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                takePicture()
            } else {
                Log.e("Camera", "Camera permission denied")
            }
        } else if (requestCode == LOCATION_REQUEST_CODE) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                startLocationUpdates()
            } else {
                Log.e("ClientsAddFragment", "Location permission denied")
            }
        }
    }

    override fun onDestroy() {
        _binding = null
        super.onDestroy()
    }
}
