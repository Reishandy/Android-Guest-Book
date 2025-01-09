package com.reishandy.guestbook.ui

import android.app.Application
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.reishandy.guestbook.ui.component.InitScreen
import com.reishandy.guestbook.ui.model.GuestBookViewModel
import com.reishandy.guestbook.ui.model.GuestBookViewModelFactory
import com.reishandy.guestbook.ui.theme.GuestBookTheme

enum class GuestBookNavItems {
    INIT,
    HOME
}

@Composable
fun GuestBookApp() {
    val context = LocalContext.current
    val application = context.applicationContext as Application
    val navController: NavHostController = rememberNavController()

    val guestBookViewModel: GuestBookViewModel = viewModel(factory = GuestBookViewModelFactory(application))
    val guestBookUiState by guestBookViewModel.uiState.collectAsState()

    Surface(modifier = Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = GuestBookNavItems.INIT.name,
            modifier = Modifier.statusBarsPadding()
        ) {
            composable(route = GuestBookNavItems.INIT.name) {
                InitScreen(
                    uiState = guestBookUiState,
                    apiBaseUrl = guestBookViewModel.apiBaseUrl,
                    onApiBaseUrlChange = { guestBookViewModel.updateApiBaseUrl(it) },
                    onConnectToApi = { guestBookViewModel.connectToApi(
                        onSuccess = { navController.navigate(GuestBookNavItems.HOME.name) }
                    ) }
                )
            }

            composable(route = GuestBookNavItems.HOME.name) {
                // TODO: Implement HomeScreen
                Text("it works!")
            }
        }
    }
}

@Preview
@Composable
fun GuestBookAppPreview() {
    GuestBookTheme(darkTheme = false) {
        GuestBookApp()
    }
}
