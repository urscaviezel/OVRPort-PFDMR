package moe.crx.overport.app.composables

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.withContext
import moe.crx.overport.app.model.GithubRelease
import moe.crx.overport.app.model.MainViewModel
import moe.crx.overport.app.theme.OverportTheme
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import overportapp.composeapp.generated.resources.Res
import overportapp.composeapp.generated.resources.overport
import overportapp.composeapp.generated.resources.patcher_nav_item
import java.io.InputStream
import java.io.OutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppContent(
    viewModel: MainViewModel,
    openFile: () -> Unit,
    saveFile: (String) -> Unit,
    openFlow: MutableSharedFlow<Pair<String, InputStream>?>,
    saveFlow: MutableSharedFlow<OutputStream?>,
) {
    OverportTheme(darkTheme = true) {
        val snackbarHostState = remember { SnackbarHostState() }

        val patcherNavItem =
            BottomNavItem(stringResource(Res.string.patcher_nav_item), Icons.Filled.Build, Icons.Outlined.Build) {
                PatcherScreen(viewModel, snackbarHostState, openFile, saveFile, openFlow, saveFlow)
            }

        var versionToUpdate by remember { mutableStateOf<GithubRelease?>(null) }
        val scope = rememberCoroutineScope()
        var checking by remember { mutableStateOf(false) }
        var confirmUpdate by remember { mutableStateOf(false) }
        var updateError by remember { mutableStateOf<String?>(null) }

        LaunchedEffect(Unit) {
            checking = true
            versionToUpdate = withContext(Dispatchers.IO) { runCatching { viewModel.versionToUpdate() }.getOrNull() }
            checking = false
        }
        if (confirmUpdate && versionToUpdate != null) {
            AlertDialog(
                onDismissRequest = { confirmUpdate = false },
                title = { Text("Update to ${versionToUpdate!!.tagName}?") },
                text = { Text("The update will be downloaded and verified, then the app will restart. Settings and signing keys are preserved.") },
                confirmButton = { TextButton(enabled = !viewModel.working && !viewModel.isApkLoaded(), onClick = {
                    confirmUpdate = false
                    scope.launch {
                        try { viewModel.installUpdate(versionToUpdate!!) }
                        catch (e: Exception) { updateError = e.message ?: "Update failed. Please try again." }
                    }
                }) { Text("Update and restart") } },
                dismissButton = { TextButton(onClick = { confirmUpdate = false }) { Text("Later") } },
            )
        }
        updateError?.let { message ->
            AlertDialog(onDismissRequest = { updateError = null }, title = { Text("Update unavailable") },
                text = { Text(message) }, confirmButton = { TextButton(onClick = { updateError = null }) { Text("OK") } })
        }

        DynamicScaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                painterResource(Res.drawable.overport),
                                contentDescription = null,
                                modifier = Modifier.size(112.dp, 24.dp)
                            )
                        }
                    },
                    actions = {
                        FilledTonalButton(
                            modifier = Modifier.padding(8.dp, 0.dp),
                            enabled = !checking && !viewModel.working && !viewModel.isApkLoaded(),
                            onClick = {
                                if (versionToUpdate != null) confirmUpdate = true
                                else scope.launch {
                                    checking = true
                                    try {
                                        versionToUpdate = withContext(Dispatchers.IO) { viewModel.versionToUpdate() }
                                        if (versionToUpdate == null) snackbarHostState.showSnackbar("You are up to date.")
                                        else confirmUpdate = true
                                    } catch (e: Exception) { updateError = e.message ?: "Could not check GitHub for updates." }
                                    finally { checking = false }
                                }
                            }
                        ) {
                            Icon(Icons.Default.CloudDownload, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(if (checking) "Checking..." else if (versionToUpdate != null) "Update ${versionToUpdate!!.tagName}" else "Check for updates")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    ),
                )
            },
            navigationItems = listOf(patcherNavItem),
            itemsEnabled = !viewModel.working && !viewModel.isApkLoaded()
        )
    }
}
