package com.example.vault

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultDashboardScreen(
    viewModel: VaultViewModel,
    onLockVault: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val currentCategory by viewModel.currentCategory.collectAsState()
    val isDecoyMode by viewModel.isDecoyMode.collectAsState()
    val selectedMedia by viewModel.selectedMedia.collectAsState()
    val importing by viewModel.importing.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()

    val photos by viewModel.photos.collectAsState()
    val videos by viewModel.videos.collectAsState()
    val notes by viewModel.notes.collectAsState()
    val trashed by viewModel.trashedMedia.collectAsState()
    val intruderLogs by viewModel.intruderLogs.collectAsState()

    var showFabMenu by remember { mutableStateOf(false) }

    // Pick visual media launchers (zero-permission Android Photo Picker)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            viewModel.importUris(uris, "Default")
        }
    }

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            viewModel.importUris(uris, "Default")
        }
    }

    // Status snackbar handler
    LaunchedEffect(statusMessage) {
        statusMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearStatusMessage()
        }
    }

    // Hardware back press handling
    BackHandler {
        if (selectedMedia != null) {
            viewModel.selectMedia(null)
        } else if (currentCategory != VaultCategory.PHOTOS) {
            viewModel.setCategory(VaultCategory.PHOTOS)
        } else {
            // Relock and return to calculator
            onLockVault()
        }
    }

    // If media detail viewer is active, show fullscreen
    if (selectedMedia != null) {
        val item = selectedMedia!!
        MediaDetailViewer(
            item = item,
            file = viewModel.getFile(item),
            onBack = { viewModel.selectMedia(null) },
            onUnhide = { viewModel.unhideMedia(item) },
            onDelete = { viewModel.moveToTrash(item) }
        )
        return
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color(0xFF0F172A),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (isDecoyMode) "Mughal Calculator (Decoy)" else "Mughal Calculator Vault",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            if (isDecoyMode) {
                                Spacer(modifier = Modifier.size(6.dp))
                                Surface(
                                    color = Color(0xFFEF4444).copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "FAKE MODE",
                                        color = Color(0xFFEF4444),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        Text(
                            text = "${photos.size} Photos • ${videos.size} Videos • ${notes.size} Notes",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = onLockVault,
                        modifier = Modifier.testTag("vault_quick_lock_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Lock Vault",
                            tint = Color(0xFF60A5FA)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1E293B)
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = Color(0xFF1E293B),
                contentColor = Color.White
            ) {
                NavigationBarItem(
                    selected = currentCategory == VaultCategory.PHOTOS,
                    onClick = { viewModel.setCategory(VaultCategory.PHOTOS) },
                    icon = { Icon(Icons.Default.Photo, contentDescription = "Photos") },
                    label = { Text("Photos", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        indicatorColor = Color(0xFF3B82F6),
                        unselectedIconColor = Color(0xFF94A3B8),
                        unselectedTextColor = Color(0xFF94A3B8)
                    ),
                    modifier = Modifier.testTag("nav_photos")
                )

                NavigationBarItem(
                    selected = currentCategory == VaultCategory.VIDEOS,
                    onClick = { viewModel.setCategory(VaultCategory.VIDEOS) },
                    icon = { Icon(Icons.Default.VideoLibrary, contentDescription = "Videos") },
                    label = { Text("Videos", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        indicatorColor = Color(0xFF3B82F6),
                        unselectedIconColor = Color(0xFF94A3B8),
                        unselectedTextColor = Color(0xFF94A3B8)
                    ),
                    modifier = Modifier.testTag("nav_videos")
                )

                NavigationBarItem(
                    selected = currentCategory == VaultCategory.NOTES,
                    onClick = { viewModel.setCategory(VaultCategory.NOTES) },
                    icon = { Icon(Icons.Default.Description, contentDescription = "Notes") },
                    label = { Text("Notes", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        indicatorColor = Color(0xFF3B82F6),
                        unselectedIconColor = Color(0xFF94A3B8),
                        unselectedTextColor = Color(0xFF94A3B8)
                    ),
                    modifier = Modifier.testTag("nav_notes")
                )

                NavigationBarItem(
                    selected = currentCategory == VaultCategory.TRASH,
                    onClick = { viewModel.setCategory(VaultCategory.TRASH) },
                    icon = { Icon(Icons.Default.Delete, contentDescription = "Trash") },
                    label = { Text("Trash", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        indicatorColor = Color(0xFF3B82F6),
                        unselectedIconColor = Color(0xFF94A3B8),
                        unselectedTextColor = Color(0xFF94A3B8)
                    ),
                    modifier = Modifier.testTag("nav_trash")
                )

                NavigationBarItem(
                    selected = currentCategory == VaultCategory.SETTINGS,
                    onClick = { viewModel.setCategory(VaultCategory.SETTINGS) },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                    label = { Text("Settings", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        indicatorColor = Color(0xFF3B82F6),
                        unselectedIconColor = Color(0xFF94A3B8),
                        unselectedTextColor = Color(0xFF94A3B8)
                    ),
                    modifier = Modifier.testTag("nav_settings")
                )
            }
        },
        floatingActionButton = {
            if (currentCategory == VaultCategory.PHOTOS || currentCategory == VaultCategory.VIDEOS) {
                Box {
                    FloatingActionButton(
                        onClick = { showFabMenu = !showFabMenu },
                        containerColor = Color(0xFF3B82F6),
                        contentColor = Color.White,
                        modifier = Modifier.testTag("vault_fab_add")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Hide Media")
                    }

                    DropdownMenu(
                        expanded = showFabMenu,
                        onDismissRequest = { showFabMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Hide Photos") },
                            onClick = {
                                showFabMenu = false
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            leadingIcon = { Icon(Icons.Default.AddPhotoAlternate, contentDescription = null) },
                            modifier = Modifier.testTag("menu_hide_photos")
                        )

                        DropdownMenuItem(
                            text = { Text("Hide Videos") },
                            onClick = {
                                showFabMenu = false
                                videoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                                )
                            },
                            leadingIcon = { Icon(Icons.Default.Videocam, contentDescription = null) },
                            modifier = Modifier.testTag("menu_hide_videos")
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Import progress bar
            if (importing) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFF3B82F6)
                )
            }

            // Main Category Display
            when (currentCategory) {
                VaultCategory.PHOTOS -> {
                    MediaGrid(
                        items = photos,
                        isVideoSection = false,
                        onItemClick = { viewModel.selectMedia(it) },
                        onUnhide = { viewModel.unhideMedia(it) },
                        onDelete = { viewModel.moveToTrash(it) },
                        onImportClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        onCreateSampleClick = { viewModel.createSampleDemoMedia() },
                        getFile = { viewModel.getFile(it) }
                    )
                }

                VaultCategory.VIDEOS -> {
                    MediaGrid(
                        items = videos,
                        isVideoSection = true,
                        onItemClick = { viewModel.selectMedia(it) },
                        onUnhide = { viewModel.unhideMedia(it) },
                        onDelete = { viewModel.moveToTrash(it) },
                        onImportClick = {
                            videoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                            )
                        },
                        onCreateSampleClick = { viewModel.createSampleDemoMedia() },
                        getFile = { viewModel.getFile(it) }
                    )
                }

                VaultCategory.NOTES -> {
                    NotesScreen(
                        notes = notes,
                        onSaveNote = { id, title, content -> viewModel.saveNote(id, title, content) },
                        onDeleteNote = { viewModel.deleteNote(it) }
                    )
                }

                VaultCategory.TRASH -> {
                    TrashScreen(
                        trashedItems = trashed,
                        onRestore = { viewModel.restoreFromTrash(it) },
                        onDeletePermanently = { viewModel.deletePermanently(it) },
                        onEmptyTrash = { viewModel.emptyTrash() },
                        getFile = { viewModel.getFile(it) }
                    )
                }

                VaultCategory.SETTINGS -> {
                    SettingsScreen(
                        securityPreferences = viewModel.securityPrefs,
                        intruderLogs = intruderLogs,
                        onClearIntruders = { viewModel.clearIntruderLogs() },
                        onLockVault = onLockVault
                    )
                }
            }
        }
    }
}
