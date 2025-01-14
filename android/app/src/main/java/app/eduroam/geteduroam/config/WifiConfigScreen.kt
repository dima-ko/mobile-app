package app.eduroam.geteduroam.config

import android.content.Intent
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.ExperimentalLifecycleComposeApi
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.eduroam.geteduroam.EduTopAppBar
import app.eduroam.geteduroam.R
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLifecycleComposeApi::class)
@Composable
fun WifiConfigScreen(
    viewModel: WifiConfigViewModel, snackbarHostState: SnackbarHostState = SnackbarHostState()
) {
    val hostState = remember(snackbarHostState) { snackbarHostState }
    Scaffold(
        topBar = {
            EduTopAppBar(stringResource(R.string.name))
        },
        snackbarHost = { SnackbarHost(hostState) },
    ) { paddingValues ->
        val launch by viewModel.launch.collectAsStateWithLifecycle(null)
        val processing by viewModel.processing.collectAsStateWithLifecycle(true)
        val message by viewModel.progressMessage.collectAsStateWithLifecycle("")
        val suggestionIntent by viewModel.intentWithSuggestions.collectAsStateWithLifecycle(null)
        val askNetworkPermission by viewModel.requestChangeNetworkPermission.collectAsStateWithLifecycle(
            false
        )
        val context = LocalContext.current
        launch?.let {
            LaunchedEffect(it) {
                viewModel.launchConfiguration(context)
            }
        }
        val activityLauncher = rememberLauncherForSuggestionIntent(snackbarHostState, viewModel)
        suggestionIntent?.let { intent ->
            LaunchedEffect(intent) {
                viewModel.consumeSuggestionIntent()
                activityLauncher.launch(intent)
            }
        }

        Column(
            Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = stringResource(id = R.string.configuration_progress),
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(8.dp))
            if (processing) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Text(
                text = stringResource(id = R.string.configuration_logs),
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(8.dp))

            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
            )
            if (askNetworkPermission) {
                AskForWiFiPermissions { viewModel.handleAndroid10WifiConfig(context) }
            }
        }
    }
}

@Composable
private fun rememberLauncherForSuggestionIntent(
    snackbarHostState: SnackbarHostState, viewModel: WifiConfigViewModel
): ManagedActivityResultLauncher<Intent, WifiConfigResponse> {
    val coroutineScope = rememberCoroutineScope()
    val cancel = stringResource(R.string.configuration_progress)
    val completed = stringResource(R.string.configuration_completed)
    return rememberLauncherForActivityResult(
        contract = WifiConfigResult(),
    ) { result ->
        when (result) {
            WifiConfigResponse.Canceled -> {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(cancel)
                }
            }
            is WifiConfigResponse.Success -> {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(completed)
                }
            }
        }
        viewModel.markAsComplete()
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun AskForWiFiPermissions(
    onPermissionGranted: () -> Unit
) {
    val multiplePermissionsState = rememberMultiplePermissionsState(
        listOf(
            android.Manifest.permission.CHANGE_WIFI_STATE,
            android.Manifest.permission.ACCESS_WIFI_STATE,
        )
    )
    if (multiplePermissionsState.allPermissionsGranted) {
        onPermissionGranted()
    } else {
        Column {
            val textToShow = getTextToShowGivenPermissions(
                multiplePermissionsState.revokedPermissions,
                multiplePermissionsState.shouldShowRationale
            )
            Text(textToShow)
            Button(onClick = { multiplePermissionsState.launchMultiplePermissionRequest() }) {
                Text("Grant permission")
            }
        }
    }
}


@OptIn(ExperimentalPermissionsApi::class)
private fun getTextToShowGivenPermissions(
    permissions: List<PermissionState>, shouldShowRationale: Boolean
): String {
    val revokedPermissionsSize = permissions.size
    if (revokedPermissionsSize == 0) return ""

    val textToShow = StringBuilder().apply {
        append("The ")
    }

    for (i in permissions.indices) {
        textToShow.append(permissions[i].permission)
        when {
            revokedPermissionsSize > 1 && i == revokedPermissionsSize - 2 -> {
                textToShow.append(", and ")
            }
            i == revokedPermissionsSize - 1 -> {
                textToShow.append(" ")
            }
            else -> {
                textToShow.append(", ")
            }
        }
    }
    textToShow.append(if (revokedPermissionsSize == 1) "permission is" else "permissions are")
    textToShow.append(
        if (shouldShowRationale) {
            " important. Please grant all of them for the app to function properly."
        } else {
            " denied. The app cannot function without them."
        }
    )
    return textToShow.toString()
}
