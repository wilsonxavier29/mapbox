package com.paparazziapps.pretamistapp.presentation.clients

import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.paparazziapps.pretamistapp.data.network.PAResult
import com.paparazziapps.pretamistapp.data.repository.PAClientsRepository
import com.paparazziapps.pretamistapp.domain.clients.ClientDomain
import com.paparazziapps.pretamistapp.helper.base.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class ClientsAddViewModel(
    private val clientsRepository: PAClientsRepository
): BaseViewModel() {

    fun processIntent(intent: ClientsAddIntent){
        when(intent){
            is ClientsAddIntent.SaveClientIntent -> {
                saveClient(
                    document = intent.document,
                    name = intent.name,
                    email = intent.email,
                    phone = intent.phone,
                    lastName = intent.lastName,
                    latitude = intent.latitude,
                    longitude = intent.longitude
                )
            }

            ClientsAddIntent.None -> {
                Log.d("ClientsAddViewModel", "None intent")
            }
        }
    }

    private fun saveClient(
        document: String,
        name: String,
        email: String,
        phone: String,
        lastName: String,
        latitude: String,
        longitude: String
    ) = viewModelScope.launch {

        _uiState.value = UiState.Loading

        val domain = ClientDomain(
            id = UUID.randomUUID().toString(),
            name = name,
            lastName = lastName,
            email = email,
            phone = phone,
            document = document,
            latitude = latitude,  // New field for latitude
            longitude = longitude // New field for longitude
        )

        val result = clientsRepository.createClient(domain)

        when(result){
            is PAResult.Error -> {
                _uiState.value = UiState.Error(result.exception)
            }
            is PAResult.Success -> {
                _uiState.value = UiState.Success(result.data)
            }
        }
    }

    sealed class ClientsAddIntent{
        data class SaveClientIntent(
            val document: String,
            val name: String,
            val email: String,
            val phone: String,
            val lastName: String,
            val latitude: String, // New field for latitude
            val longitude: String // New field for longitude
        ): ClientsAddIntent()

        data object None: ClientsAddIntent()
    }
}
