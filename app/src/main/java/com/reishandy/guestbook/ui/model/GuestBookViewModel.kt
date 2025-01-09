package com.reishandy.guestbook.ui.model

import android.app.Application
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.reishandy.guestbook.data.GuestBookPreferenceManager
import com.reishandy.guestbook.network.ConnectionResponse
import com.reishandy.guestbook.network.GuestBookApiService
import com.reishandy.guestbook.network.RetrofitFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.IOException
import kotlin.text.Regex

data class GuestBookUiState(
    val isConnecting: Boolean = false,
    val isErrorConnecting: Boolean = false,
    val connectionError: String = "",
)

class GuestBookViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(GuestBookUiState())
    val uiState: StateFlow<GuestBookUiState> = _uiState.asStateFlow()

    private var _guestBookApiService: GuestBookApiService? = null

    // TEXT FIELD
    var apiBaseUrl by mutableStateOf("")
        private set

    fun updateApiBaseUrl(newApiBaseUrl: String) {
        apiBaseUrl = newApiBaseUrl
    }

    // API BASE URL
    fun connectToApi(
        onSuccess: () -> Unit
    ) {
        // Validate API base URL
        validateApiBaseUrl()

        // Initialize Retrofit
        _guestBookApiService =
            RetrofitFactory.getInstance(apiBaseUrl).create(GuestBookApiService::class.java)

        // Check connection
        viewModelScope.launch {
            _uiState.value = GuestBookUiState(isConnecting = true)

            try {
                val response: ConnectionResponse? = _guestBookApiService?.checkConnection()
                if (response?.message == "ok") {
                    _uiState.value = GuestBookUiState()
                    storeApiBaseUrl() // Store API base URL to SharedPreferences
                    onSuccess()
                } else {
                    throw IOException("Not the expected response")
                }
            } catch (e: IOException) {
                _uiState.value = GuestBookUiState(
                    isErrorConnecting = true,
                    connectionError = "Failed to connect"
                )
                Log.d("GuestBookViewModel", "Failed to connect: ${e.message}")
                showToast("Failed to connect: ${e.message}")
            }
        }
    }

    // HELPER / INIT
    init {
        // Fetch API base URL from SharedPreferences
        val baseUrl = GuestBookPreferenceManager.getBaseUrl(getApplication())
        apiBaseUrl = baseUrl
    }

    private fun validateApiBaseUrl() {
        // Validate API base URL empty
        if (apiBaseUrl.isEmpty()) {
            _uiState.value =
                GuestBookUiState(isErrorConnecting = true, connectionError = "Empty URL")
            return
        }

        // Use regex to validate API base URL
        val regex = Regex("^(https?://)([a-zA-Z0-9.-]+|\\[[0-9a-fA-F:]+])(:\\d{1,5})?(/)?$\n")
        if (!regex.matches(apiBaseUrl)) {
            _uiState.value =
                GuestBookUiState(isErrorConnecting = true, connectionError = "Invalid URL")
            return
        }
    }

    private fun storeApiBaseUrl() {
        GuestBookPreferenceManager.setBaseUrl(getApplication(), apiBaseUrl)
    }

    fun showToast(message: String) {
        Toast.makeText(
            getApplication(),
            message,
            Toast.LENGTH_LONG
        ).show()
    }
}