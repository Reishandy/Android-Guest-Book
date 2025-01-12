package com.reishandy.guestbook.ui.component

import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.LinearLayout
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.reishandy.guestbook.R
import com.reishandy.guestbook.data.startQRCodeAnalyzer
import com.reishandy.guestbook.ui.model.GuestBookUiState
import com.reishandy.guestbook.ui.theme.GuestBookTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    uiState: GuestBookUiState,
    manualEntry: String,
    onManualEntryChange: (String) -> Unit,
    onManualEntrySubmit: () -> Unit,
    onImportCSV: () -> Unit,
    onExportCSV: () -> Unit,
    onReset: () -> Unit,
    onChangeAPI: () -> Unit,
    onQrCodeScanned: (String) -> Unit
) {
    Box(
        modifier = modifier.fillMaxSize()
    ) {
        QRWindow(
            modifier = Modifier.fillMaxSize(),
            onQrCodeScanned = onQrCodeScanned
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimensionResource(R.dimen.padding_xlarge))
                .align(Alignment.BottomCenter),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(dimensionResource(R.dimen.card_elevation))
        ) {
            Column(
                modifier = Modifier
                    .padding(dimensionResource(R.dimen.padding_medium)),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                EntryDivider()

                ManualEntryWindow(
                    manualEntry = manualEntry,
                    onManualEntryChange = onManualEntryChange,
                    onManualEntrySubmit = onManualEntrySubmit,
                    onImportCSV = onImportCSV,
                    onExportCSV = onExportCSV,
                    onReset = onReset,
                    onChangeAPI = onChangeAPI
                )
            }
        }


        if (uiState.isLoading) {
            Dialog(
                onDismissRequest = {},
                properties = DialogProperties(
                    dismissOnBackPress = false,
                    dismissOnClickOutside = false
                )
            ) {
                Card(
                    elevation = CardDefaults.cardElevation(dimensionResource(R.dimen.card_elevation)),
                ) {
                    Column(
                        modifier = Modifier.padding(dimensionResource(R.dimen.padding_large)),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(dimensionResource(R.dimen.progress_indicator_size)),
                            color = MaterialTheme.colorScheme.primary
                        )

                        Text(
                            text = stringResource(R.string.loading),
                            modifier = Modifier.padding(top = dimensionResource(R.dimen.padding_medium)),
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

        }
    }
}

@Composable
fun QRWindow(
    modifier: Modifier = Modifier,
    onQrCodeScanned: (String) -> Unit
) {
    val context = LocalContext.current
    val cameraController = remember {
        LifecycleCameraController(context)
    }

    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { context ->
            PreviewView(context).apply {
                layoutParams = LinearLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT)
                setBackgroundColor(android.graphics.Color.BLACK)
                scaleType = PreviewView.ScaleType.FILL_START
            }.also { previewView ->
                //previewView.controller = cameraController
                //cameraController.bindToLifecycle(liveCycleOwner)

                startQRCodeAnalyzer(context, cameraController, previewView, onQrCodeScanned)
            }
        })
}

@Composable
fun EntryDivider(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            thickness = dimensionResource(R.dimen.divider_thickness),
            color = MaterialTheme.colorScheme.onSurface
        )

        Text(
            text = stringResource(R.string.manual_entry),
            modifier = Modifier.weight(1.5f),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )

        HorizontalDivider(
            modifier = Modifier.weight(1f),
            thickness = dimensionResource(R.dimen.divider_thickness),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun ManualEntryWindow(
    modifier: Modifier = Modifier,
    manualEntry: String,
    onManualEntryChange: (String) -> Unit,
    onManualEntrySubmit: () -> Unit,
    onImportCSV: () -> Unit,
    onExportCSV: () -> Unit,
    onReset: () -> Unit,
    onChangeAPI: () -> Unit
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        OutlinedTextField(
            value = manualEntry,
            onValueChange = { onManualEntryChange(it) },
            modifier = Modifier.fillMaxWidth(),
            label = {
                Text(stringResource(R.string.id))
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.id)
                )
            },
            singleLine = true
        )

        Row(
            modifier = Modifier.padding(top = dimensionResource(R.dimen.padding_small)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = onManualEntrySubmit,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(7f),
                content = {
                    Text(stringResource(R.string.submit))
                }
            )

            OptionDropdown(
                modifier = Modifier.weight(1f),
                onImportCSV = onImportCSV,
                onExportCSV = onExportCSV,
                onReset = onReset,
                onChangeAPI = onChangeAPI
            )
        }
    }
}

@Composable
fun OptionDropdown(
    modifier: Modifier = Modifier,
    onImportCSV: () -> Unit,
    onExportCSV: () -> Unit,
    onReset: () -> Unit,
    onChangeAPI: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box(
        modifier = modifier,
    ) {
        IconButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth(),
            content = {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = stringResource(R.string.options)
                )
            }
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            TextButton(
                onClick = onImportCSV,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(dimensionResource(R.dimen.button_square)),
                content = {
                    Text(stringResource(R.string.import_csv))
                }
            )

            TextButton(
                onClick = onExportCSV,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(dimensionResource(R.dimen.button_square)),
                content = {
                    Text(stringResource(R.string.export_csv))
                }
            )

            TextButton(
                onClick = onReset,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(dimensionResource(R.dimen.button_square)),
                content = {
                    Text(stringResource(R.string.reset))
                }
            )

            TextButton(
                onClick = onChangeAPI,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(dimensionResource(R.dimen.button_square)),
                content = {
                    Text("Change API")
                }
            )
        }
    }
}

@Preview
@Composable
fun HomeScreenPreview() {
    GuestBookTheme(darkTheme = false) {
        Surface(modifier = Modifier.fillMaxSize()) {
            HomeScreen(
                uiState = GuestBookUiState(),
                manualEntry = "12345678",
                onManualEntryChange = {},
                onManualEntrySubmit = {},
                onImportCSV = {},
                onExportCSV = {},
                onReset = {},
                onChangeAPI = {},
                onQrCodeScanned = {}
            )
        }
    }
}