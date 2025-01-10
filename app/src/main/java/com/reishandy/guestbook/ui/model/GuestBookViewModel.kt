package com.reishandy.guestbook.ui.model

import android.app.Application
import android.widget.Toast
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.reishandy.guestbook.data.GuestBookPreferenceManager
import com.reishandy.guestbook.network.CheckInResponse
import com.reishandy.guestbook.network.ConnectionResponse
import com.reishandy.guestbook.network.ErrorResponse
import com.reishandy.guestbook.network.GuestBookApiService
import com.reishandy.guestbook.network.RetrofitFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.text.Regex

data class DialogUiState(
    val isShowing: Boolean = false,
    val icon: ImageVector? = null,
    val title: String = "",
    val message: String = "",
    val confirmText: String = "",
    val dismissText: String = "",
    val onConfirm: () -> Unit = {},
    val onDismiss: () -> Unit = {}
)

data class GuestBookUiState(
    val isConnecting: Boolean = false,
    val isErrorConnecting: Boolean = false,
    val connectionError: String = "",
    val isCheckingIn: Boolean = false,
    val dialogUiState: DialogUiState = DialogUiState(),
    val isQrScannerPaused: Boolean = false
)

class GuestBookViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(GuestBookUiState())
    val uiState: StateFlow<GuestBookUiState> = _uiState.asStateFlow()

    private var _guestBookApiService: GuestBookApiService? = null

    // TEXT FIELD
    var apiBaseUrl by mutableStateOf("")
        private set

    var manualEntry by mutableStateOf("")
        private set

    fun updateApiBaseUrl(newApiBaseUrl: String) {
        apiBaseUrl = newApiBaseUrl
    }

    fun updateManualEntry(newManualEntry: String) {
        manualEntry = newManualEntry
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

                    // Store API base URL to SharedPreferences
                    GuestBookPreferenceManager.setBaseUrl(getApplication(), apiBaseUrl)

                    onSuccess()
                } else {
                    throw IOException("Not the expected response")
                }
            } catch (e: IOException) {
                _uiState.value = GuestBookUiState(
                    isErrorConnecting = true,
                    connectionError = "Failed to connect"
                )
            }
        }
    }

    // CHECK-IN
    fun checkIn() {
        // Check if manual entry is empty
        if (manualEntry.isEmpty()) {
            showToast("Id is empty")
            return
        }

        viewModelScope.launch {
            _uiState.value = GuestBookUiState(isCheckingIn = true, isQrScannerPaused = true)

            try {
                val response: CheckInResponse? = _guestBookApiService?.checkIn(manualEntry)

                // Check if response is empty
                if (response == null) {
                    throw IOException("Empty response")
                }

                // Format the time and show success dialog
                val time = formatToLocalTime(response.time)
                showDialog(
                    message = "Check-in successful at $time",
                    isError = false
                )
            } catch (e: retrofit2.HttpException) {
                // Parse retrofit HTTP error response
                val errorBody = e.response()?.errorBody()?.string()
                val errorResponse = Gson().fromJson(errorBody, ErrorResponse::class.java)

                // Check if response code is 404 or 500, and show error dialog
                if (e.code() == 404 || e.code() == 500) {
                    showDialog(
                        message = "Failed to check-in: ${errorResponse.message}",
                        isError = true
                    )
                } else {
                    throw IOException(errorBody)
                }
            } catch (e: IOException) {
                showDialog(
                    message = "Failed to check-in: ${e.message}",
                    isError = true
                )
            } finally {
                manualEntry = ""
                _uiState.update { it.copy(isCheckingIn = false) }
            }
        }
    }

    // RESET
    fun resetDoubleCheck() {
        _uiState.update {
            it.copy(
                dialogUiState = DialogUiState(
                    isShowing = true,
                    icon = Icons.Default.Warning,
                    title = "Warning",
                    message = "Are you sure you want to reset?",
                    confirmText = "Confirm",
                    dismissText = "Cancel",
                    onConfirm = {
                        _uiState.value = GuestBookUiState()
                        reset()
                    },
                    onDismiss = { _uiState.value = GuestBookUiState() }
                )
            )
        }
    }

    private fun reset() {
        viewModelScope.launch {
            try {
                val response = _guestBookApiService?.reset()

                // Check if response is empty
                if (response == null) {
                    throw IOException("Empty response")
                }

                // Show success dialog
                showDialog(
                    message = "Reset successful: ${response.rows} rows affected",
                    isError = false
                )
            } catch (e: retrofit2.HttpException) {
                // Parse retrofit HTTP error response
                val errorBody = e.response()?.errorBody()?.string()
                val errorResponse = Gson().fromJson(errorBody, ErrorResponse::class.java)

                // Check if response code is 404 or 500, and show error dialog
                if (e.code() == 404 || e.code() == 500) {
                    showDialog(
                        message = "Failed to reset: ${errorResponse.message}",
                        isError = true
                    )
                } else {
                    throw IOException(errorBody)
                }
            } catch (e: IOException) {
                showDialog(
                    message = "Failed to reset: ${e.message}",
                    isError = true
                )
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

    fun formatToLocalTime(dateTimeString: String): String {
        // Define the input and output date-time formats
        val inputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSS")
        val outputFormatter = DateTimeFormatter.ofPattern("HH:mm:ss - yyyy-MM-dd")

        // Parse the input date-time string
        val localDateTime = LocalDateTime.parse(dateTimeString, inputFormatter)

        // Convert to the local time zone
        val zonedDateTime = localDateTime.atZone(ZoneId.of("UTC")).withZoneSameInstant(ZoneId.systemDefault())

        // Format the date-time to the desired pattern
        return outputFormatter.format(zonedDateTime)
    }

    private fun showToast(message: String) {
        Toast.makeText(
            getApplication(),
            message,
            Toast.LENGTH_LONG
        ).show()
    }

    private fun showDialog(
        message: String,
        isError: Boolean
    ) {
        _uiState.update {
            it.copy(
                dialogUiState = DialogUiState(
                    isShowing = true,
                    icon = if (isError) Icons.Default.Warning else Icons.Default.CheckCircle,
                    title = if (isError) "Error" else "Success",
                    message = message,
                    confirmText = "Ok",
                    dismissText = "Dismiss",
                    onConfirm = { _uiState.value = GuestBookUiState(isQrScannerPaused = false) },
                    onDismiss = { _uiState.value = GuestBookUiState(isQrScannerPaused = false) }
                )
            )
        }
    }
}