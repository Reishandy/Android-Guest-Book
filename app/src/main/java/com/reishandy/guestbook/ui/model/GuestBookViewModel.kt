package com.reishandy.guestbook.ui.model

import android.app.Application
import android.net.Uri
import android.util.Log
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
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import java.io.File
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
    val isLoading: Boolean = false,
    val dialogUiState: DialogUiState = DialogUiState(),
    val isQrScannerPaused: Boolean = false
)

class GuestBookViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(GuestBookUiState())
    val uiState: StateFlow<GuestBookUiState> = _uiState.asStateFlow()

    private var _guestBookApiService: GuestBookApiService? = null

    private var _selectedFileUri by mutableStateOf<Uri>(Uri.EMPTY)

    fun updateSelectedFileUri(newUri: Uri) {
        _selectedFileUri = newUri
    }

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
            showToast("ID is empty")
            return
        }

        viewModelScope.launch {
            _uiState.value = GuestBookUiState(isLoading = true, isQrScannerPaused = true)

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
                _uiState.update { it.copy(isLoading = false) }
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
            _uiState.value = GuestBookUiState(isLoading = true, isQrScannerPaused = true)

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
                    message = "Failed to reset : ${e.message}",
                    isError = true
                )
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    // IMPORT CSV
    fun importCSV() {
        viewModelScope.launch {
            _uiState.value = GuestBookUiState(isLoading = true, isQrScannerPaused = true)

            try {
                val contentResolver = getApplication<Application>().contentResolver
                val inputStream = contentResolver.openInputStream(_selectedFileUri)
                val file = File(getApplication<Application>().cacheDir, "imported_file.csv")
                inputStream?.use { input ->
                    file.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }

                val requestBody = RequestBody.create("text/csv".toMediaTypeOrNull(), file)
                val multipartBody = MultipartBody.Part.createFormData("file", file.name, requestBody)

                val response = _guestBookApiService?.importCSV(multipartBody)

                if (response != null) {
                    showDialog(
                        message = "Import successful: ${response.rows} rows affected",
                        isError = false
                    )
                } else {
                    throw IOException("Empty response")
                }
            } catch (e: retrofit2.HttpException) {
                // Parse retrofit HTTP error response
                val errorBody = e.response()?.errorBody()?.string()
                val errorResponse = Gson().fromJson(errorBody, ErrorResponse::class.java)

                // Check if response code is 404 or 500, and show error dialog
                when (e.code()) {
                    400 -> {
                        showDialog(
                            message = "Failed to import CSV: File is not CSV",
                            isError = true
                        )
                    }
                    500 -> {
                        showDialog(
                            message = "Failed to import CSV: ${errorResponse.message}",
                            isError = true
                        )
                    }
                    else -> {
                        throw IOException(errorBody)
                    }
                }
            } catch (e: Exception) {
                Log.e("GuestBookViewModel", "$e")
                showDialog(
                    message = "Failed to import CSV: ${e.message}",
                    isError = true
                )
            } finally {
                _selectedFileUri = Uri.EMPTY
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    // EXPORT CSV
    fun exportCSV(uri: Uri) {
        viewModelScope.launch {
            try {
                val responseBody = _guestBookApiService?.getCsvData()
                responseBody?.let { body ->
                    val contentResolver = getApplication<Application>().contentResolver
                    contentResolver.openOutputStream(uri)?.use { output ->
                        body.byteStream().copyTo(output)
                    }
                    showToast("CSV exported successfully")
                } ?: run {
                    showToast("Failed to export CSV: Empty response")
                }
            } catch (e: Exception) {
                showToast("Failed to export CSV: ${e.message}")
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

    fun showToast(message: String) {
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