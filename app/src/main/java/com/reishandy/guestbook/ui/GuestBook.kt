package com.reishandy.guestbook.ui

import android.app.Application
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.reishandy.guestbook.MainActivity
import com.reishandy.guestbook.R
import com.reishandy.guestbook.ui.component.HomeScreen
import com.reishandy.guestbook.ui.component.InitScreen
import com.reishandy.guestbook.ui.model.GuestBookViewModel
import com.reishandy.guestbook.ui.model.GuestBookViewModelFactory

enum class GuestBookNavItems {
    INIT,
    HOME
}

@Composable
fun GuestBookApp() {
    val context = LocalContext.current
    val activity = context as MainActivity
    val application = context.applicationContext as Application
    val navController: NavHostController = rememberNavController()

    val guestBookViewModel: GuestBookViewModel =
        viewModel(factory = GuestBookViewModelFactory(application))
    val guestBookUiState by guestBookViewModel.uiState.collectAsState()

    val getContent = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            guestBookViewModel.updateSelectedFileUri(it)
            guestBookViewModel.importCSV()
        } ?: run {
            guestBookViewModel.showDialog(
                message = R.string.please_select_a_file_to_import,
                isError = true
            )
        }
    }

    val createDocument = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        uri?.let {
            guestBookViewModel.exportCSV(it)
        } ?: run {
            guestBookViewModel.showDialog(
                message = R.string.please_select_a_location_to_save_the_file,
                isError = true
            )
        }
    }


    Surface(modifier = Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = GuestBookNavItems.INIT.name,
            modifier = Modifier.statusBarsPadding()
        ) {
            composable(route = GuestBookNavItems.INIT.name) {
                // Disable back button, instead close the app
                BackHandler {
                    activity.finish()
                }

                InitScreen(
                    uiState = guestBookUiState,
                    apiBaseUrl = guestBookViewModel.apiBaseUrl,
                    onApiBaseUrlChange = { guestBookViewModel.updateApiBaseUrl(it) },
                    onConnectToApi = {
                        guestBookViewModel.connectToApi(
                            onSuccess = { navController.navigate(GuestBookNavItems.HOME.name) }
                        )
                    }
                )
            }

            composable(route = GuestBookNavItems.HOME.name) {
                // Disable back button, instead close the app
                BackHandler {
                    activity.finish()
                }

                HomeScreen(
                    uiState = guestBookUiState,
                    manualEntry = guestBookViewModel.manualEntry,
                    onManualEntryChange = { guestBookViewModel.updateManualEntry(it) },
                    onManualEntrySubmit = {
                        guestBookViewModel.checkIn()
                    },
                    onImportCSV = {
                        getContent.launch("*/*")
                    },
                    onExportCSV = {
                        createDocument.launch("export.csv")
                    },
                    onReset = {
                        guestBookViewModel.resetDoubleCheck()
                    },
                    onChangeAPI = {
                        navController.navigate(GuestBookNavItems.INIT.name)
                    },
                    onQrCodeScanned = { scannedText ->
                        if (!guestBookUiState.isQrScannerPaused) {
                            guestBookViewModel.updateManualEntry(scannedText)
                            guestBookViewModel.checkIn()
                        }
                    }
                )
            }
        }

        if (guestBookUiState.dialogUiState.isShowing) {
            AlertDialog(
                icon = {
                    Icon(
                        imageVector = guestBookUiState.dialogUiState.icon
                            ?: Icons.Default.Notifications,
                        contentDescription = stringResource(guestBookUiState.dialogUiState.title)
                    )
                },
                title = {
                    Text(
                        text = stringResource(guestBookUiState.dialogUiState.title),
                        style = MaterialTheme.typography.headlineSmall
                    )
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(guestBookUiState.dialogUiState.message, guestBookUiState.dialogUiState.additionalInfo),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                },
                onDismissRequest = { guestBookUiState.dialogUiState.onDismiss() },
                confirmButton = {
                    TextButton(
                        onClick = { guestBookUiState.dialogUiState.onConfirm() },
                        content = {
                            Text(
                                text = stringResource(guestBookUiState.dialogUiState.confirmText),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    )
                },
                dismissButton = {
                    if (guestBookUiState.dialogUiState.dismissText != 0) {
                        TextButton(
                            onClick = { guestBookUiState.dialogUiState.onDismiss() },
                            content = {
                                Text(
                                    text = stringResource(guestBookUiState.dialogUiState.dismissText),
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        )
                    }
                }
            )
        }
    }
}
