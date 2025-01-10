package com.reishandy.guestbook.ui

import android.app.Application
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.reishandy.guestbook.MainActivity
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
                        // TODO: Implement import CSV
                    },
                    onExportCSV = {
                        // TODO: Implement export CSV
                    },
                    onReset = {
                        guestBookViewModel.resetDoubleCheck()
                    },
                    onChangeAPI = {
                        navController.navigate(GuestBookNavItems.INIT.name)
                    },
                    onQrCodeScanned = { scannedText ->
                        guestBookViewModel.updateManualEntry(scannedText)
                        if (!guestBookUiState.isQrScannerPaused) {
                            guestBookViewModel.checkIn()
                        }
                    }
                )
            }
        }
    }
}
