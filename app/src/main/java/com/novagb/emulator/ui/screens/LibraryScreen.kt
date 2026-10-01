package com.novagb.emulator.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.novagb.emulator.data.RomMetadata
import com.novagb.emulator.data.RomRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    onLaunchGame: (RomMetadata) -> Unit,
    onOpenSettings: () -> Unit
) {
    val context = LocalContext.current
    val repository = remember { RomRepository(context) }
    var searchQuery by remember { mutableStateOf("") }

    val romList = remember {
        mutableStateListOf<RomMetadata>().apply {
            addAll(repository.loadRomLibrary())
        }
    }

    val handleLaunchGame: (RomMetadata) -> Unit = { game ->
        val updatedGame = game.copy(lastPlayedTimestamp = System.currentTimeMillis())
        romList.removeAll { it.id == game.id || it.uriString == game.uriString }
        romList.add(0, updatedGame)
        repository.saveRomLibrary(romList.toList())
        onLaunchGame(updatedGame)
    }

    val handleDeleteGame: (RomMetadata) -> Unit = { game ->
        romList.removeAll { it.id == game.id }
        repository.saveRomLibrary(romList.toList())
    }

    val romPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(uri, takeFlags)
            } catch (_: Exception) {}
            try {
                val bytes = repository.readRomBytes(uri.toString(), false)
                val meta = repository.parseRomMetadata(uri, bytes)
                romList.removeAll { it.uriString == meta.uriString || it.title == meta.title }
                romList.add(0, meta)
                repository.saveRomLibrary(romList.toList())
                handleLaunchGame(meta)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF00E5FF)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.SportsEsports,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "NovaGB",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = Color.White
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = Color(0xFFD0D5E0)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF101114)
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { romPickerLauncher.launch(arrayOf("*/*")) },
                containerColor = Color(0xFF00E5FF),
                contentColor = Color.Black,
                shape = CircleShape
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Import ROM")
            }
        },
        containerColor = Color(0xFF101114)
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (romList.isEmpty()) {
                item {
                    EmptyLibraryCard(onImportClick = { romPickerLauncher.launch(arrayOf("*/*")) })
                }
            } else {
                item {
                    romList.firstOrNull()?.let { lastGame ->
                        HeroResumeCard(
                            game = lastGame,
                            onPlayClick = { handleLaunchGame(lastGame) }
                        )
                    }
                }

                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search your ROM collection...", color = Color(0xFF6B7280)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF181A20)),
                        singleLine = true
                    )
                }

                item {
                    Text(
                        text = "GAMES LIBRARY (${romList.size})",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF8E95A5),
                        letterSpacing = 1.sp
                    )
                }

                val filteredList = romList.filter {
                    it.title.contains(searchQuery, ignoreCase = true)
                }

                items(filteredList, key = { it.id }) { game ->
                    GameCardItem(
                        game = game,
                        onClick = { handleLaunchGame(game) },
                        onDelete = { handleDeleteGame(game) }
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyLibraryCard(onImportClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF181A22))
            .border(1.dp, Color(0xFF262A36), RoundedCornerShape(16.dp))
            .padding(vertical = 36.dp, horizontal = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF20232C)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.SportsEsports,
                    contentDescription = null,
                    tint = Color(0xFF00E5FF),
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "No Games in Library",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Tap below or use the '+' button to load Game Boy (.gb / .gbc) ROMs from your device.",
                fontSize = 13.sp,
                color = Color(0xFF8E95A5),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(18.dp))
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF00E5FF),
                modifier = Modifier.clickable { onImportClick() }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "ADD ROM",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun HeroResumeCard(
    game: RomMetadata,
    onPlayClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFF1F232D), Color(0xFF14161C))
                )
            )
            .border(1.dp, Color(0xFF2C313E), RoundedCornerShape(16.dp))
            .padding(18.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "READY TO PLAY",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF00E5FF),
                    letterSpacing = 1.sp
                )
                Text(
                    text = game.cartridgeType,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF9DA3AF)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = game.title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${maxOf(1L, game.romSizeBytes / 1024)} KB • ${if (game.isCgb) "Game Boy Color" else "Game Boy"}",
                    fontSize = 12.sp,
                    color = Color(0xFF8E95A5)
                )

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFF00E5FF),
                    modifier = Modifier.clickable { onPlayClick() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "PLAY",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GameCardItem(
    game: RomMetadata,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val dateStr = remember(game.lastPlayedTimestamp) {
        if (game.lastPlayedTimestamp > 0) {
            SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(game.lastPlayedTimestamp))
        } else "Never played"
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF181A22))
            .border(1.dp, Color(0xFF262A36), RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color(0xFF323746), Color(0xFF20232C))
                        )
                    )
                    .border(1.dp, Color(0xFF454B5E), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (game.isCgb) "CGB" else "DMG",
                    color = if (game.isCgb) Color(0xFFFF2A6D) else Color(0xFF00E5FF),
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = game.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row {
                    Text(
                        text = "${maxOf(1L, game.romSizeBytes / 1024)} KB",
                        fontSize = 12.sp,
                        color = Color(0xFF8E95A5)
                    )
                    Text(
                        text = " • ",
                        fontSize = 12.sp,
                        color = Color(0xFF8E95A5)
                    )
                    Text(
                        text = dateStr,
                        fontSize = 12.sp,
                        color = Color(0xFF8E95A5)
                    )
                }
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Remove ROM",
                    tint = Color(0xFF6B7280),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}