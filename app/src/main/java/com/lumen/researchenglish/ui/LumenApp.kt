package com.lumen.researchenglish.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController

private data class Destination(
    val route: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
)

private val destinations = listOf(
    Destination("library", "Library", Icons.Outlined.AutoStories),
    Destination("bair", "BAIR", Icons.Outlined.Science),
    Destination("chat", "Chat", Icons.Outlined.ChatBubbleOutline),
    Destination("review", "Review", Icons.Outlined.TaskAlt),
    Destination("settings", "Settings", Icons.Outlined.Settings),
)

@Composable
fun LumenApp(viewModel: AppViewModel = viewModel()) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val route = backStackEntry?.destination?.route
    val isReader = route?.startsWith("reader/") == true
    val snackbarHostState = remember { SnackbarHostState() }
    val error by viewModel.error.collectAsStateWithLifecycle()
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    val availableUpdate by viewModel.availableUpdate.collectAsStateWithLifecycle()
    val updateDownloading by viewModel.updateDownloading.collectAsStateWithLifecycle()
    val updateDownloadProgress by viewModel.updateDownloadProgress.collectAsStateWithLifecycle()
    val downloadedUpdateReady by viewModel.downloadedUpdateReady.collectAsStateWithLifecycle()
    val updateStatus by viewModel.updateStatus.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.checkForUpdates()
    }

    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (route in destinations.map { it.route }) {
                NavigationBar {
                    destinations.forEach { destination ->
                        NavigationBarItem(
                            selected = route == destination.route,
                            onClick = {
                                navController.navigate(destination.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(destination.icon, contentDescription = destination.label)
                            },
                            label = { androidx.compose.material3.Text(destination.label) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(if (isReader) Modifier else Modifier.padding(padding)),
        ) {
            NavHost(
                navController = navController,
                startDestination = "library",
            ) {
                composable("library") {
                    LibraryScreen(
                        viewModel = viewModel,
                        onOpenDocument = { navController.navigate("reader/$it") },
                    )
                }
                composable("bair") { BairScreen(viewModel) }
                composable("chat") { ChatScreen(viewModel) }
                composable("review") { ReviewScreen(viewModel) }
                composable("settings") { SettingsScreen(viewModel) }
                composable("reader/{documentId}") { entry ->
                    ReaderScreen(
                        documentId = entry.arguments?.getString("documentId").orEmpty(),
                        viewModel = viewModel,
                        onBack = { navController.popBackStack() },
                    )
                }
            }
            if (busy) {
                CircularProgressIndicator(Modifier.align(Alignment.Center))
            }
        }
    }

    availableUpdate?.let { update ->
        AlertDialog(
            onDismissRequest = {
                if (!updateDownloading) viewModel.dismissUpdate()
            },
            title = {
                Text(
                    when {
                        updateDownloading -> "Downloading Lumen ${update.versionName}"
                        downloadedUpdateReady -> "Lumen ${update.versionName} is ready"
                        else -> "Lumen ${update.versionName} is available"
                    },
                )
            },
            text = {
                Column {
                    Text(
                        when {
                            updateDownloading -> "The update is downloading inside Lumen."
                            downloadedUpdateReady ->
                                "The APK has been verified. Tap Install to open Android's system confirmation."
                            else -> "Would you like to download and install this update?"
                        },
                    )
                    if (updateDownloading) {
                        Spacer(Modifier.height(10.dp))
                        val fraction = updateDownloadProgress?.fraction
                        if (fraction == null) {
                            LinearProgressIndicator(Modifier.fillMaxWidth())
                        } else {
                            LinearProgressIndicator(
                                progress = { fraction },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        updateDownloadProgress?.let { progress ->
                            Spacer(Modifier.height(6.dp))
                            val downloadedMb = progress.bytesDownloaded / (1024f * 1024f)
                            val totalMb = progress.totalBytes?.let { it / (1024f * 1024f) }
                            Text(
                                if (totalMb == null) {
                                    "%.1f MB downloaded".format(downloadedMb)
                                } else {
                                    "%.1f / %.1f MB".format(downloadedMb, totalMb)
                                },
                            )
                        }
                    }
                    if (updateStatus.isNotBlank() && !updateDownloading) {
                        Spacer(Modifier.height(10.dp))
                        Text(updateStatus)
                    }
                    if (update.notes.isNotBlank()) {
                        Spacer(Modifier.height(10.dp))
                        Text(update.notes)
                    }
                }
            },
            dismissButton = {
                if (!updateDownloading) {
                    TextButton(onClick = viewModel::dismissUpdate) { Text("Later") }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !updateDownloading,
                    onClick = {
                        if (downloadedUpdateReady) {
                            viewModel.installDownloadedUpdate()
                        } else {
                            viewModel.downloadAndInstallUpdate()
                        }
                    },
                ) {
                    Text(
                        when {
                            updateDownloading -> "Downloading"
                            downloadedUpdateReady -> "Install"
                            else -> "Download"
                        },
                    )
                }
            },
        )
    }

}
