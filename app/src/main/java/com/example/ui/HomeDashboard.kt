package com.example.ui

import android.text.format.Formatter
import android.widget.VideoView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.data.*
import com.example.ui.theme.*
import com.example.viewmodel.AppTab
import com.example.viewmodel.MainViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HomeDashboard(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val activeTab by viewModel.activeTab.collectAsStateWithLifecycle()
    val currentTrack by viewModel.currentPlayingTrack.collectAsStateWithLifecycle()

    var activeVideoUrlForPlayer by remember { mutableStateOf<String?>(null) }
    var showSettings by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            Column {
                // Persistent Floating Mini Media Player (bridges current tab controls)
                if (currentTrack != null && activeTab != AppTab.PLAYER) {
                    MiniPlayerFooter(
                        track = currentTrack!!,
                        viewModel = viewModel,
                        onClick = { viewModel.setActiveTab(AppTab.PLAYER) }
                    )
                }

                BottomNavigationBar(
                    activeTab = activeTab,
                    onTabSelected = { viewModel.setActiveTab(it) }
                )
            }
        },
        containerColor = CharcoalDark
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (activeTab) {
                AppTab.HOME -> HomeTabContent(viewModel = viewModel)
                AppTab.QUEUE -> QueueTabContent(viewModel = viewModel)
                AppTab.LIBRARY -> LibraryTabContent(
                    viewModel = viewModel,
                    onPlayVideo = { videoPath -> activeVideoUrlForPlayer = videoPath }
                )
                AppTab.PLAYER -> PlayerTabContent(viewModel = viewModel)
            }

            // Settings gear button
            IconButton(
                onClick = { showSettings = true },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Settings,
                    contentDescription = "Configurações",
                    tint = TextGray,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }

    // Settings dialog
    if (showSettings) {
        SettingsDialog(
            viewModel = viewModel,
            onDismiss = { showSettings = false }
        )
    }

    // Embedive full screen native Video Player Dialog for watching MP4 file formats
    if (activeVideoUrlForPlayer != null) {
        VideoPlayerDialog(
            videoPath = activeVideoUrlForPlayer!!,
            onDismiss = { activeVideoUrlForPlayer = null }
        )
    }
}

@Composable
fun BottomNavigationBar(
    activeTab: AppTab,
    onTabSelected: (AppTab) -> Unit
) {
    NavigationBar(
        containerColor = CharcoalCard,
        tonalElevation = 8.dp,
        windowInsets = WindowInsets.navigationBars
    ) {
        NavigationBarItem(
            selected = activeTab == AppTab.HOME,
            onClick = { onTabSelected(AppTab.HOME) },
            icon = { Icon(Icons.Outlined.Download, contentDescription = "Tab Baixar") },
            label = { Text("Baixar", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color.Black,
                selectedTextColor = AmberPrimary,
                indicatorColor = AmberPrimary,
                unselectedIconColor = TextGray,
                unselectedTextColor = TextGray
            ),
            modifier = Modifier.testTag("nav_tab_home")
        )

        NavigationBarItem(
            selected = activeTab == AppTab.QUEUE,
            onClick = { onTabSelected(AppTab.QUEUE) },
            icon = { Icon(Icons.Outlined.Queue, contentDescription = "Tab Fila") },
            label = { Text("Fila", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color.Black,
                selectedTextColor = AmberPrimary,
                indicatorColor = AmberPrimary,
                unselectedIconColor = TextGray,
                unselectedTextColor = TextGray
            ),
            modifier = Modifier.testTag("nav_tab_queue")
        )

        NavigationBarItem(
            selected = activeTab == AppTab.LIBRARY,
            onClick = { onTabSelected(AppTab.LIBRARY) },
            icon = { Icon(Icons.Outlined.LibraryMusic, contentDescription = "Tab Biblioteca") },
            label = { Text("Arquivos", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color.Black,
                selectedTextColor = AmberPrimary,
                indicatorColor = AmberPrimary,
                unselectedIconColor = TextGray,
                unselectedTextColor = TextGray
            ),
            modifier = Modifier.testTag("nav_tab_library")
        )

        NavigationBarItem(
            selected = activeTab == AppTab.PLAYER,
            onClick = { onTabSelected(AppTab.PLAYER) },
            icon = { Icon(Icons.Outlined.PlayCircle, contentDescription = "Tab Player") },
            label = { Text("Player", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color.Black,
                selectedTextColor = AmberPrimary,
                indicatorColor = AmberPrimary,
                unselectedIconColor = TextGray,
                unselectedTextColor = TextGray
            ),
            modifier = Modifier.testTag("nav_tab_player")
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeTabContent(viewModel: MainViewModel) {
    val clipboardManager = LocalClipboardManager.current
    val urlInput by viewModel.urlInput.collectAsStateWithLifecycle()
    val isAudioOnly by viewModel.isAudioOnly.collectAsStateWithLifecycle()
    val audioFormat by viewModel.audioFormatOption.collectAsStateWithLifecycle()
    val videoQuality by viewModel.videoQualityOption.collectAsStateWithLifecycle()

    var showAudioFormatMenu by remember { mutableStateOf(false) }
    var showVideoQualityMenu by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // App header visual layout
        Spacer(modifier = Modifier.height(20.dp))
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(
                    Brush.verticalGradient(listOf(AmberPrimary, AmberDark)),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Download,
                contentDescription = "Logo Arrow",
                tint = Color.Black,
                modifier = Modifier.size(36.dp)
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "SonicSnap",
            color = TextWhite,
            fontSize = 26.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.5.sp,
            fontFamily = FontFamily.SansSerif
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = "MEDIA DOWNLOADER",
            color = AmberPrimary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Baixe músicas e vídeos offline com alta fidelidade",
            color = TextGray,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(30.dp))

        // Enter URL Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CharcoalCard),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "INSIRA O LINK DO LINK DO YOUTUBE",
                    color = AmberPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = urlInput,
                    onValueChange = { viewModel.setUrlInput(it) },
                    placeholder = { Text("https://www.youtube.com/watch?v=...", color = TextGray) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("url_text_input"),
                    trailingIcon = {
                        if (urlInput.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setUrlInput("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Limpar Campo", tint = TextGray)
                            }
                        } else {
                            Button(
                                onClick = {
                                    val clipText = clipboardManager.getText()?.text
                                    if (!clipText.isNullOrBlank()) {
                                        viewModel.setUrlInput(clipText)
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = CharcoalLight),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                modifier = Modifier
                                    .padding(end = 4.dp)
                                    .testTag("paste_link_button")
                            ) {
                                Text("Colar", color = AmberPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                        focusedBorderColor = AmberPrimary,
                        unfocusedBorderColor = CharcoalLight,
                        focusedContainerColor = Color.Black,
                        unfocusedContainerColor = Color.Black
                    ),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Stream Formats row
                Text(
                    text = "FORMATO DE SAÍDA",
                    color = AmberPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Audio Choice Chips
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { viewModel.setIsAudioOnly(true) },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isAudioOnly) CharcoalLight else Color.Black
                        ),
                        border = BorderStroke(
                            1.dp,
                            if (isAudioOnly) AmberPrimary else Color.Transparent
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Default.MusicNote,
                                contentDescription = "Audio Icon",
                                tint = if (isAudioOnly) AmberPrimary else TextGray,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "Música (MP3)",
                                color = if (isAudioOnly) TextWhite else TextGray,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Video Choice Chips
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { viewModel.setIsAudioOnly(false) },
                        colors = CardDefaults.cardColors(
                            containerColor = if (!isAudioOnly) CharcoalLight else Color.Black
                        ),
                        border = BorderStroke(
                            1.dp,
                            if (!isAudioOnly) AmberPrimary else Color.Transparent
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Default.VideoLibrary,
                                contentDescription = "Video Icon",
                                tint = if (!isAudioOnly) AmberPrimary else TextGray,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "Vídeo (MP4)",
                                color = if (!isAudioOnly) TextWhite else TextGray,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Detail Options
                AnimatedContent(targetState = isAudioOnly, label = "DownloadOptions") { audioOnly ->
                    if (audioOnly) {
                        Column {
                            Text(
                                "Qualidade do Áudio:",
                                color = TextGray,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            ExposedDropdownMenuBox(
                                expanded = showAudioFormatMenu,
                                onExpandedChange = { showAudioFormatMenu = !showAudioFormatMenu }
                            ) {
                                OutlinedButton(
                                    onClick = { showAudioFormatMenu = true },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .menuAnchor(),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextWhite),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, CharcoalLight)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = if (audioFormat == "mp3") "MP3 (Melhor Compatibilidade)" else "Qualidade Original",
                                            fontSize = 13.sp
                                        )
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                    }
                                }

                                DropdownMenu(
                                    expanded = showAudioFormatMenu,
                                    onDismissRequest = { showAudioFormatMenu = false },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(CharcoalCard)
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("MP3 (Melhor Compatibilidade)", color = TextWhite) },
                                        onClick = {
                                            viewModel.setAudioFormat("mp3")
                                            showAudioFormatMenu = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Formato Original (Qualidade Total)", color = TextWhite) },
                                        onClick = {
                                            viewModel.setAudioFormat("best")
                                            showAudioFormatMenu = false
                                        }
                                    )
                                }
                            }
                        }
                    } else {
                        Column {
                            Text(
                                "Resolução do Vídeo:",
                                color = TextGray,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            ExposedDropdownMenuBox(
                                expanded = showVideoQualityMenu,
                                onExpandedChange = { showVideoQualityMenu = !showVideoQualityMenu }
                            ) {
                                OutlinedButton(
                                    onClick = { showVideoQualityMenu = true },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .menuAnchor(),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextWhite),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, CharcoalLight)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        val displayQual = when(videoQuality) {
                                            "1080" -> "1080p Full HD"
                                            "720" -> "720p HD"
                                            "480" -> "480p Clássica"
                                            else -> "${videoQuality}p"
                                        }
                                        Text(text = displayQual, fontSize = 13.sp)
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                    }
                                }

                                DropdownMenu(
                                    expanded = showVideoQualityMenu,
                                    onDismissRequest = { showVideoQualityMenu = false },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(CharcoalCard)
                                ) {
                                    listOf("1080", "720", "480").forEach { q ->
                                        DropdownMenuItem(
                                            text = {
                                                val qLbl = when(q) {
                                                    "1080" -> "1080p Full HD"
                                                    "720" -> "720p HD"
                                                    "480" -> "480p Clássica"
                                                    else -> "${q}p"
                                                }
                                                Text(qLbl, color = TextWhite)
                                            },
                                            onClick = {
                                                viewModel.setVideoQuality(q)
                                                showVideoQualityMenu = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Large Trigger Button
        Button(
            onClick = { viewModel.startDownload(urlInput) },
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag("extract_and_download_button"),
            colors = ButtonDefaults.buttonColors(
                containerColor = AmberPrimary,
                contentColor = Color.Black
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Download, contentDescription = null, tint = Color.Black)
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "BAIXAR AGORA",
                fontWeight = FontWeight.Black,
                fontSize = 15.sp,
                letterSpacing = 0.5.sp
            )
        }

        Spacer(modifier = Modifier.height(36.dp))

        // Quick platform helper references (To add stunning design polish)
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "PLATAFORMAS SUPORTADAS",
                color = TextGray,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                PlatformIconBadge("YouTube", Icons.Default.LiveTv)
                PlatformIconBadge("TikTok", Icons.Default.MusicNote)
                PlatformIconBadge("Instagram", Icons.Default.PhotoCamera)
                PlatformIconBadge("SoundCloud", Icons.Default.QueueMusic)
            }
        }
    }
}

@Composable
fun PlatformIconBadge(name: String, icon: ImageVector) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(CharcoalCard, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = name,
                tint = AmberPrimary,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(name, color = TextGray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun QueueTabContent(viewModel: MainViewModel) {
    val tasks by viewModel.activeTasks.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Fila de Downloads",
                color = TextWhite,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black
            )
            
            if (tasks.any { it.status == DownloadStatus.COMPLETED || it.status == DownloadStatus.FAILED }) {
                TextButton(
                    onClick = { viewModel.clearCompletedTasks() },
                    colors = ButtonDefaults.textButtonColors(contentColor = AmberPrimary)
                ) {
                    Text("Limpar Concluídos", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (tasks.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.CloudDownload,
                        contentDescription = null,
                        tint = CharcoalLight,
                        modifier = Modifier.size(72.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        "Sua fila está vazia",
                        color = TextGray,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Cole um link na tela inicial para iniciar um download",
                        color = TextGray,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(tasks, key = { it.id }) { task ->
                    DownloadTaskCard(task = task, onCancel = { viewModel.removeTask(task.id) })
                }
            }
        }
    }
}

@Composable
fun DownloadTaskCard(
    task: DownloadTask,
    onCancel: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CharcoalCard),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // File type lead icon
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(CharcoalLight, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (task.isAudio) Icons.Default.MusicNote else Icons.Default.Videocam,
                        contentDescription = null,
                        tint = AmberPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Metadata Details
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = task.title,
                        color = TextWhite,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Link: " + task.originalUrl,
                        color = TextGray,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(
                    onClick = onCancel,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Cancelar download", tint = TextGray, modifier = Modifier.size(16.dp))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Dynamic progress indicators depending on status
            when (task.status) {
                DownloadStatus.QUEUED -> {
                    Text("Aguardando...", color = TextGray, fontSize = 11.sp)
                }
                DownloadStatus.RESOLVING -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            color = AmberPrimary,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Processando e extraindo link do áudio...", color = AmberPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
                DownloadStatus.DOWNLOADING -> {
                    Column {
                        // Linear slider track indicator
                        LinearProgressIndicator(
                            progress = { task.progress },
                            color = AmberPrimary,
                            trackColor = ProgressTrack,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                        )
                        
                        Spacer(modifier = Modifier.height(6.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val percentText = (task.progress * 100).toInt().coerceAtLeast(0)
                            Text(
                                "Baixando: $percentText%",
                                color = TextWhite,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            
                            val speedStr = String.format("%.1f", task.speedKbps)
                            val sizeStr = formatBytes(task.downloadedBytes)
                            val totalStr = if (task.totalBytes > 0) formatBytes(task.totalBytes) else "Desconhecido"
                            
                            Text(
                                "$sizeStr de $totalStr ($speedStr KB/s)",
                                color = TextGray,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
                DownloadStatus.COMPLETED -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Download concluído com sucesso!", color = SuccessGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Text(formatBytes(task.totalBytes), color = TextGray, fontSize = 10.sp)
                    }
                }
                DownloadStatus.FAILED -> {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Error, contentDescription = null, tint = ErrorRed, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Falha ao salvar mídia", color = ErrorRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        if (!task.errorMessage.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = task.errorMessage,
                                color = TextGray,
                                fontSize = 10.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LibraryTabContent(
    viewModel: MainViewModel,
    onPlayVideo: (String) -> Unit
) {
    val tracks by viewModel.downloadedTracks.collectAsStateWithLifecycle()
    var searchQuery by remember { mutableStateOf("") }
    var sortBy by remember { mutableStateOf("recent") } // "recent", "title", "size"

    // Filter and sort items dynamically
    val filteredAndSortedTracks = remember(tracks, searchQuery, sortBy) {
        tracks.filter {
            it.title.contains(searchQuery, ignoreCase = true)
        }.sortedWith { a, b ->
            when (sortBy) {
                "title" -> a.title.lowercase().compareTo(b.title.lowercase())
                "size" -> b.fileSize.compareTo(a.fileSize)
                else -> b.downloadTime.compareTo(a.downloadTime) // default: recent first
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            "Biblioteca de Arquivos",
            color = TextWhite,
            fontSize = 18.sp,
            fontWeight = FontWeight.Black
        )
        Text(
            "Ouvir músicas e reproduzir vídeos baixados offline",
            color = TextGray,
            fontSize = 11.sp
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Procurar músicas salvas...", color = TextGray, fontSize = 13.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextGray) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = TextWhite,
                unfocusedTextColor = TextWhite,
                focusedBorderColor = AmberPrimary,
                unfocusedBorderColor = CharcoalLight,
                focusedContainerColor = Color.Black,
                unfocusedContainerColor = Color.Black
            ),
            shape = RoundedCornerShape(10.dp)
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Sorting Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Ordenar por: ", color = TextGray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.width(6.dp))

            listOf("recent" to "Recentes", "title" to "Título", "size" to "Tamanho").forEach { sortOption ->
                val isSelected = sortBy == sortOption.first
                Card(
                    modifier = Modifier
                        .padding(end = 6.dp)
                        .clickable { sortBy = sortOption.first },
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) CharcoalLight else Color.Black
                    ),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, if (isSelected) AmberPrimary else CharcoalLight)
                ) {
                    Text(
                        text = sortOption.second,
                        color = if (isSelected) AmberPrimary else TextGray,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (filteredAndSortedTracks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.MusicVideo,
                        contentDescription = null,
                        tint = CharcoalLight,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        if (searchQuery.isNotEmpty()) "Nenhuma correspondência encontrada" else "Nenhum arquivo baixado",
                        color = TextGray,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                items(filteredAndSortedTracks, key = { it.id }) { track ->
                    LibraryTrackItem(
                        track = track,
                        onPlay = {
                            if (track.format == "mp4") {
                                onPlayVideo(track.localPath)
                            } else {
                                viewModel.playTrack(track)
                            }
                        },
                        onDelete = { viewModel.deleteTrack(track) }
                    )
                }
            }
        }
    }
}

@Composable
fun LibraryTrackItem(
    track: DownloadedTrack,
    onPlay: () -> Unit,
    onDelete: () -> Unit
) {
    var showConfirmDelete by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onPlay() }
            .testTag("track_library_item"),
        colors = CardDefaults.cardColors(containerColor = CharcoalCard)
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Lead Vinyl rotating element or Video elements
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(Color.Black, RoundedCornerShape(6.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (track.format == "mp4") Icons.Default.Videocam else Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = AmberPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Body Detail labels
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = track.title,
                    color = TextWhite,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val sizeStr = formatBytes(track.fileSize)
                    val extLabel = track.format.uppercase()
                    
                    Text(
                        text = "$extLabel • $sizeStr",
                        color = TextGray,
                        fontSize = 11.sp
                    )
                    
                    val dateStr = remember(track.downloadTime) {
                        try {
                            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                            sdf.format(Date(track.downloadTime))
                        } catch (e: Exception) { "" }
                    }
                    if (dateStr.isNotEmpty()) {
                        Text(
                            text = "• $dateStr",
                            color = TextGray,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            // Quick Play Button
            IconButton(onClick = onPlay) {
                Icon(
                    imageVector = if (track.format == "mp4") Icons.Default.PlayArrow else Icons.Default.PlayArrow,
                    contentDescription = "Tocar Mídia",
                    tint = AmberPrimary
                )
            }

            // Trash delete button
            IconButton(onClick = { showConfirmDelete = true }) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Excluir Mídia",
                    tint = TextGray,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }

    if (showConfirmDelete) {
        AlertDialog(
            onDismissRequest = { showConfirmDelete = false },
            title = { Text("Excluir Arquivo", color = TextWhite) },
            text = { Text("Deseja realmente apagar o arquivo permanentemente? Esta ação liberará armazenamento interno do dispositivo.", color = TextGray) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showConfirmDelete = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = ErrorRed)
                ) {
                    Text("Excluir", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showConfirmDelete = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = TextWhite)
                ) {
                    Text("Cancelar")
                }
            },
            containerColor = CharcoalCard
        )
    }
}

@Composable
fun PlayerTabContent(viewModel: MainViewModel) {
    val currentTrack by viewModel.currentPlayingTrack.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val position by viewModel.playbackPosition.collectAsStateWithLifecycle()
    val duration by viewModel.duration.collectAsStateWithLifecycle()
    val isRepeating by viewModel.isRepeating.collectAsStateWithLifecycle()
    val isShuffling by viewModel.isShuffling.collectAsStateWithLifecycle()

    if (currentTrack == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = CharcoalLight,
                    modifier = Modifier.size(80.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "Nenhum áudio selecionado",
                    color = TextGray,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Text(
                    "Selecione um arquivo de áudio na sua aba de Arquivos para reproduzi-lo com recursos avançados",
                    color = TextGray,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }
        }
    } else {
        // Rotating state angles
        val infiniteTransition = rememberInfiniteTransition(label = "VinylRotation")
        val rotationAngle by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(8000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "AngleAnimator"
        )
        val activeAngle = if (isPlaying) rotationAngle else 0f

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Nav bar visual indicator info
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "REPRODUZINDO AGORA",
                    color = AmberPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Excelente Qualidade de Áudio",
                    color = TextGray,
                    fontSize = 11.sp
                )
            }

            // Aesthetic Rotating Disc layout
            Box(
                modifier = Modifier
                    .size(240.dp)
                    .background(Color.Black, CircleShape)
                    .border(6.dp, CharcoalCard, CircleShape)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                // Outer subtle grooves drawings
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .border(1.dp, Color(0xFF151515), CircleShape)
                        .padding(18.dp)
                        .border(1.dp, Color(0xFF202020), CircleShape)
                )

                // Actual Center vinyl album label rotating
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .rotate(activeAngle)
                        .background(
                            Brush.sweepGradient(
                                listOf(CharcoalLight, AmberPrimary, CharcoalLight, AmberDark, CharcoalLight)
                            ),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    // Center spin circle hole
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color.Black, CircleShape)
                            .border(2.dp, Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = AmberPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            // Metadata info labels
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = currentTrack!!.title,
                    color = TextWhite,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = currentTrack!!.artist + " • Local",
                    color = TextGray,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Timeline seek tracker
            Column(modifier = Modifier.fillMaxWidth()) {
                val safeDuration = if (duration > 0) duration else 1
                Slider(
                    value = position.toFloat(),
                    onValueChange = { viewModel.seekSong(it.toLong()) },
                    valueRange = 0f..safeDuration.toFloat(),
                    colors = SliderDefaults.colors(
                        activeTrackColor = AmberPrimary,
                        inactiveTrackColor = ProgressTrack,
                        thumbColor = AmberPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(formatMs(position), color = TextGray, fontSize = 11.sp)
                    Text(formatMs(duration), color = TextGray, fontSize = 11.sp)
                }
            }

            // Primary Playback Controllers row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Shuffle Indicator
                IconButton(onClick = { viewModel.toggleShuffle() }) {
                    Icon(
                        Icons.Default.Shuffle,
                        contentDescription = "Modo Aleatório",
                        tint = if (isShuffling) AmberPrimary else TextGray,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Previous
                IconButton(onClick = { viewModel.previousSong() }) {
                    Icon(
                        Icons.Default.SkipPrevious,
                        contentDescription = "Música Anterior",
                        tint = TextWhite,
                        modifier = Modifier.size(28.dp)
                    )
                }

                // Play / Pause Circle Panel controller
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(AmberPrimary, CircleShape)
                        .clickable {
                            if (isPlaying) viewModel.pauseSong() else viewModel.resumeSong()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Pausar/Tocar",
                        tint = Color.Black,
                        modifier = Modifier.size(32.dp)
                    )
                }

                // Next
                IconButton(onClick = { viewModel.nextSong() }) {
                    Icon(
                        Icons.Default.SkipNext,
                        contentDescription = "Próxima Música",
                        tint = TextWhite,
                        modifier = Modifier.size(28.dp)
                    )
                }

                // Repeat Indicator
                IconButton(onClick = { viewModel.toggleRepeat() }) {
                    Icon(
                        Icons.Default.Repeat,
                        contentDescription = "Repetir Música",
                        tint = if (isRepeating) AmberPrimary else TextGray,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Helper seeker increments row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { viewModel.player.skipBackward() },
                    modifier = Modifier.padding(end = 12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Replay10, contentDescription = "Voltar 10s", tint = TextGray, modifier = Modifier.size(20.dp))
                    }
                }

                IconButton(
                    onClick = { viewModel.player.skipForward() },
                    modifier = Modifier.padding(start = 12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Forward10, contentDescription = "Avançar 10s", tint = TextGray, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}

/**
 * Compact floating bottom player that bridges playback control throughout the active tab views
 */
@Composable
fun MiniPlayerFooter(
    track: DownloadedTrack,
    viewModel: MainViewModel,
    onClick: () -> Unit
) {
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val position by viewModel.playbackPosition.collectAsStateWithLifecycle()
    val duration by viewModel.duration.collectAsStateWithLifecycle()

    val progress = if (duration > 0) position.toFloat() / duration.toFloat() else 0f

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CharcoalCard)
            .clickable { onClick() }
            .testTag("mini_player_footer")
    ) {
        // Flat micro progress indicator at the very top edge of footer
        LinearProgressIndicator(
            progress = { progress },
            color = AmberPrimary,
            trackColor = Color.Transparent,
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Micro disc art rotation
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(Color.Black, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = AmberPrimary,
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Body Info elements
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = track.title,
                    color = TextWhite,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Ouvindo offline",
                    color = TextGray,
                    fontSize = 10.sp
                )
            }

            // Compact Quick buttons
            IconButton(
                onClick = { if (isPlaying) viewModel.pauseSong() else viewModel.resumeSong() }
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = AmberPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }

            IconButton(
                onClick = { viewModel.nextSong() }
            ) {
                Icon(
                    imageVector = Icons.Default.SkipNext,
                    contentDescription = null,
                    tint = TextWhite,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/**
 * Interactive full-bleed Dialog wrapping VideoView for native playback of downloaded MP4 files
 */
@Composable
fun VideoPlayerDialog(
    videoPath: String,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.Black
        ) {
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                // Frame containing Android platform VideoView with simple media controls
                AndroidView(
                    factory = { context ->
                        VideoView(context).apply {
                            setVideoPath(videoPath)
                            val mediaController = android.widget.MediaController(context)
                            mediaController.setAnchorView(this)
                            setMediaController(mediaController)
                            setOnPreparedListener { start() }
                            setOnCompletionListener { onDismiss() }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.Center)
                )

                // Float clear dismiss button
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .padding(24.dp)
                        .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                        .align(Alignment.TopEnd)
                ) {
                    Icon(Icons.Default.Clear, contentDescription = "Fechar player de vídeo", tint = Color.White)
                }
            }
        }
    }
}

/**
 * Configuration dialog for custom Cobalt instance URL and API Key
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDialog(
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    var instanceUrl by remember { mutableStateOf(viewModel.getCustomInstanceUrl() ?: "") }
    var apiKey by remember { mutableStateOf(viewModel.getApiKey() ?: "") }
    var authScheme by remember { mutableStateOf(viewModel.getAuthScheme() ?: "Api-Key") }
    var expandedScheme by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = CharcoalCard),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    "Configurações da Instância",
                    color = TextWhite,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Configure uma instância Cobalt customizada e chave de API",
                    color = TextGray,
                    fontSize = 12.sp
                )

                Spacer(modifier = Modifier.height(20.dp))

                Text("URL da Instância", color = AmberPrimary, fontSize = 11.sp, fontWeight = FontWeight.Black)
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = instanceUrl,
                    onValueChange = { instanceUrl = it },
                    placeholder = { Text("https://seuservidor.com/", color = TextGray) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                        focusedBorderColor = AmberPrimary,
                        unfocusedBorderColor = CharcoalLight,
                        focusedContainerColor = Color.Black,
                        unfocusedContainerColor = Color.Black
                    ),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text("Esquema de Autenticação", color = AmberPrimary, fontSize = 11.sp, fontWeight = FontWeight.Black)
                Spacer(modifier = Modifier.height(6.dp))
                ExposedDropdownMenuBox(
                    expanded = expandedScheme,
                    onExpandedChange = { expandedScheme = !expandedScheme }
                ) {
                    OutlinedButton(
                        onClick = { expandedScheme = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextWhite),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, CharcoalLight)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = authScheme, fontSize = 13.sp)
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        }
                    }
                    DropdownMenu(
                        expanded = expandedScheme,
                        onDismissRequest = { expandedScheme = false },
                        modifier = Modifier.background(CharcoalCard)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Api-Key", color = TextWhite) },
                            onClick = { authScheme = "Api-Key"; expandedScheme = false }
                        )
                        DropdownMenuItem(
                            text = { Text("Bearer", color = TextWhite) },
                            onClick = { authScheme = "Bearer"; expandedScheme = false }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text("Chave de API", color = AmberPrimary, fontSize = 11.sp, fontWeight = FontWeight.Black)
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    placeholder = { Text("sua-chave-api-aqui", color = TextGray) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                        focusedBorderColor = AmberPrimary,
                        unfocusedBorderColor = CharcoalLight,
                        focusedContainerColor = Color.Black,
                        unfocusedContainerColor = Color.Black
                    ),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            instanceUrl = ""
                            apiKey = ""
                            viewModel.setCustomInstanceUrl(null)
                            viewModel.setApiKey(null)
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, ErrorRed)
                    ) {
                        Text("Limpar", fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = {
                            viewModel.setCustomInstanceUrl(instanceUrl.ifBlank { null })
                            viewModel.setApiKey(apiKey.ifBlank { null })
                            viewModel.setAuthScheme(authScheme)
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AmberPrimary,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Salvar", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// Byte unit formatter helper
private fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt().coerceIn(0, units.lastIndex)
    return String.format(Locale.US, "%.1f %s", bytes / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
}

// Timeline duration formatter helper (mm:ss)
private fun formatMs(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.US, "%02d:%02d", minutes, seconds)
}
