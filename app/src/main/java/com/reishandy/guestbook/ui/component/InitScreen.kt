package com.reishandy.guestbook.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.reishandy.guestbook.R
import com.reishandy.guestbook.ui.model.GuestBookUiState
import com.reishandy.guestbook.ui.theme.GuestBookTheme

@Composable
fun InitScreen(
    modifier: Modifier = Modifier,
    uiState: GuestBookUiState,
    apiBaseUrl: String,
    onApiBaseUrlChange: (String) -> Unit,
    onConnectToApi: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(dimensionResource(R.dimen.padding_xlarge)),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.connect_to_api),
            style = MaterialTheme.typography.headlineLarge
        )

        OutlinedTextField(
            value = apiBaseUrl,
            onValueChange = { onApiBaseUrlChange(it) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = dimensionResource(R.dimen.padding_medium)),
            label = {
                if (uiState.isErrorConnecting) {
                    Text(text = stringResource(uiState.connectionError))
                } else {
                    Text(stringResource(R.string.api_base_url))
                }
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = stringResource(R.string.api_base_url)
                )
            },
            isError = uiState.isErrorConnecting,
            singleLine = true
        )

        Button(
            onClick = onConnectToApi,
            modifier = Modifier
                .height(dimensionResource(R.dimen.button_height))
                .width(dimensionResource(R.dimen.button_width))
                .padding(top = dimensionResource(R.dimen.padding_large)),
            content = {
                if (uiState.isConnecting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(dimensionResource(R.dimen.progress_indicator_size)),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(stringResource(R.string.connect))
                }
            }
        )
    }
}


@Preview
@Composable
fun InitScreenPreview() {
    GuestBookTheme(darkTheme = false) {
        Surface(modifier = Modifier.fillMaxSize()) {
            InitScreen(
                apiBaseUrl = "http://localhost:8080",
                uiState = GuestBookUiState(
                    isConnecting = false
                ),
                onApiBaseUrlChange = { },
                onConnectToApi = { }
            )
        }
    }
}