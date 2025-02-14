package com.reishandy.guestbook.ui.model

import android.app.Application
import android.net.Uri
import androidx.annotation.StringRes
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
import com.reishandy.guestbook.R
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
    @StringRes val title: Int = 0,
    @StringRes val message: Int = 0,
    val additionalInfo: String = "",
    @StringRes val confirmText: Int = 0,
    @StringRes val dismissText: Int = 0,
    val onConfirm: () -> Unit = {},
    val onDismiss: () -> Unit = {}
)

data class GuestBookUiState(
    val isConnecting: Boolean = false,
    val isErrorConnecting: Boolean = false,
    @StringRes val connectionError: Int = 0,
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
        if (!validateApiBaseUrl()) {
            return
        }

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
                    throw IOException()
                }
            } catch (e: retrofit2.HttpException) {
               if (e.code() == 503) {
                    _uiState.value = GuestBookUiState(
                        isErrorConnecting = true,
                        connectionError = R.string.service_unavailable
                    )
                } else {
                    _uiState.value = GuestBookUiState(
                        isErrorConnecting = true,
                        connectionError = R.string.failed_to_connect
                    )
               }
            } catch (_: IOException) {
                _uiState.value = GuestBookUiState(
                    isErrorConnecting = true,
                    connectionError = R.string.failed_to_connect
                )
            }
        }
    }

    // CHECK-IN
    fun checkIn() {
        // Check if manual entry is empty
        if (manualEntry.isEmpty()) {
            showDialog(
                message = R.string.id_is_empty,
                isError = true
            )
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

                // Show success dialog
                showDialog(
                    message = R.string.check_in_successful_at,
                    additionalInfo = response.time,
                    isError = false
                )
            } catch (e: retrofit2.HttpException) {
                // Parse retrofit HTTP error response
                val errorBody = e.response()?.errorBody()?.string()
                val errorResponse = Gson().fromJson(errorBody, ErrorResponse::class.java)

                // Check if response code is 400, 404 or 500, and show error dialog
                if (e.code() == 400 || e.code() == 404 || e.code() == 500) {
                    showDialog(
                        message = R.string.failed_to_check_in,
                        additionalInfo = errorResponse.message,
                        isError = true
                    )
                } else {
                    throw IOException(errorBody)
                }
            } catch (e: IOException) {
                showDialog(
                    message = R.string.failed_to_check_in,
                    additionalInfo = e.message ?: "",
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
                    title = R.string.warning,
                    message = R.string.are_you_sure_you_want_to_reset,
                    confirmText = R.string.confirm,
                    dismissText = R.string.cancel,
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
                    message = R.string.reset_successful_rows_affected,
                    additionalInfo = response.rows,
                    isError = false
                )
            } catch (e: retrofit2.HttpException) {
                // Parse retrofit HTTP error response
                val errorBody = e.response()?.errorBody()?.string()
                val errorResponse = Gson().fromJson(errorBody, ErrorResponse::class.java)

                // Check if response code is 404 or 500, and show error dialog
                if (e.code() == 404 || e.code() == 500) {
                    showDialog(
                        message = R.string.failed_to_reset,
                        additionalInfo = errorResponse.message,
                        isError = true
                    )
                } else {
                    throw IOException(errorBody)
                }
            } catch (e: IOException) {
                showDialog(
                    message = R.string.failed_to_reset,
                    additionalInfo = e.message ?: "",
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
                val multipartBody =
                    MultipartBody.Part.createFormData("file", file.name, requestBody)

                val response = _guestBookApiService?.importCSV(multipartBody)

                if (response != null) {
                    showDialog(
                        message = R.string.import_successful_rows_affected,
                        additionalInfo = response.rows,
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
                            message = R.string.failed_to_import_csv_file_is_not_csv,
                            isError = true
                        )
                    }

                    500 -> {
                        showDialog(
                            message = R.string.failed_to_import_csv,
                            additionalInfo = errorResponse.message,
                            isError = true
                        )
                    }

                    else -> {
                        throw IOException(errorBody)
                    }
                }
            } catch (e: Exception) {
                showDialog(
                    message = R.string.failed_to_import_csv,
                    additionalInfo = e.message ?: "",
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
                    showDialog(
                        message = R.string.csv_exported_successfully,
                        isError = false
                    )
                } ?: run {
                    showDialog(
                        message = R.string.failed_to_export_csv_empty_response,
                        isError = true
                    )
                }
            } catch (e: Exception) {
                showDialog(
                    message = R.string.failed_to_export_csv,
                    additionalInfo = e.message.toString(),
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

    private fun validateApiBaseUrl(): Boolean {
        // Validate API base URL empty
        if (apiBaseUrl.isEmpty()) {
            _uiState.value = GuestBookUiState(isErrorConnecting = true, connectionError = R.string.empty_url)
            return false
        }

        // Use regex to validate API base URL
        val regex = Regex("^(https?:\\/\\/)([a-zA-Z0-9.-]+|\\[[0-9a-fA-F:]+])(:\\d{1,5})?(\\/)?")
        if (!regex.matches(apiBaseUrl)) {
            _uiState.value = GuestBookUiState(isErrorConnecting = true, connectionError = R.string.invalid_url)
            return false
        }

        return true
    }

    fun showDialog(
        @StringRes message: Int,
        additionalInfo: String = "",
        isError: Boolean
    ) {
        _uiState.update {
            it.copy(
                dialogUiState = DialogUiState(
                    isShowing = true,
                    icon = if (isError) Icons.Default.Warning else Icons.Default.CheckCircle,
                    title = if (isError) R.string.error else R.string.success,
                    message = message,
                    additionalInfo = additionalInfo,
                    confirmText = R.string.ok,
                    onConfirm = { _uiState.value = GuestBookUiState(isQrScannerPaused = false) }
                )
            )
        }
    }
}