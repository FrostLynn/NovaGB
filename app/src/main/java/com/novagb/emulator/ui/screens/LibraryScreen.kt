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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.novagb.emulator.data.RomMetadata
import com.novagb.emulator.data.RomRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class LibraryFilter(val label: String) {
    ALL("All"),
    DMG("Game Boy"),
    CGB("Color")
}

enum class LibrarySort(val label: String) {
    RECENT("Recent"),
    NAME("A-Z"),
    SIZE("Size")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    onLaunchGame: (RomMetadata) -> Unit,
    onOpenSettings: () -> Unit
) {
    val context = LocalContext.current
    val repository = remember { RomRepository(context) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(LibraryFilter.ALL) }
    var selectedSort by remember { mutableStateOf(LibrarySort.RECENT) }

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

                // Filter & Sort Bar
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Filter chips
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            LibraryFilter.entries.forEach { filter ->
                                val isSelected = selectedFilter == filter
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(if (isSelected) Color(0xFF00E5FF) else Color(0xFF1E212A))
                                        .clickable { selectedFilter = filter }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = filter.label,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isSelected) Color.Black else Color(0xFF8E95A5)
                                    )
                                }
                            }
                        }

                        // Sort chips
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Sort:", fontSize = 10.sp, color = Color(0xFF6B7280))
                            LibrarySort.entries.forEach { sort ->
                                val isSelected = selectedSort == sort
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isSelected) Color(0xFF282D3A) else Color.Transparent)
                                        .border(
                                            0.5.dp,
                                            if (isSelected) Color(0xFF00E5FF).copy(alpha = 0.5f) else Color.Transparent,
                                            RoundedCornerShape(6.dp)
                                        )
                                        .clickable { selectedSort = sort }
                                        .padding(horizontal = 6.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = sort.label,
                                        fontSize = 10.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) Color(0xFF00E5FF) else Color(0xFF7A8090)
                                    )
                                }
                            }
                        }
                    }
                }

                val filteredList = romList
                    .filter {
                        val matchesSearch = it.title.contains(searchQuery, ignoreCase = true)
                        val matchesFilter = when (selectedFilter) {
                            LibraryFilter.ALL -> true
                            LibraryFilter.DMG -> !it.isCgb
                            LibraryFilter.CGB -> it.isCgb
                        }
                        matchesSearch && matchesFilter
                    }
                    .let { list ->
                        when (selectedSort) {
                            LibrarySort.RECENT -> list.sortedByDescending { it.lastPlayedTimestamp }
                            LibrarySort.NAME -> list.sortedBy { it.title.lowercase(Locale.getDefault()) }
                            LibrarySort.SIZE -> list.sortedByDescending { it.romSizeBytes }
                        }
                    }

                item {
                    Text(
                        text = "GAMES (${filteredList.size})",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF8E95A5),
                        letterSpacing = 1.sp
                    )
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

/**
 * Authentic miniature Game Boy cartridge illustration with custom label and top notch.
 */
@Composable
fun RetroCartridgeThumbnail(
    isCgb: Boolean,
    title: String,
    modifier: Modifier = Modifier,
    width: Dp = 50.dp,
    height: Dp = 60.dp
) {
    val bodyColor = if (isCgb) Color(0xFF1E2836) else Color(0xFF6E727E)
    val bodyBorder = if (isCgb) Color(0xFF00E5FF).copy(alpha = 0.6f) else Color(0xFF8E929E)
    val notchColor = if (isCgb) Color(0xFF141A24) else Color(0xFF5A5E68)
    val labelBg = if (isCgb) {
        Brush.verticalGradient(listOf(Color(0xFF2C1E38), Color(0xFF13101E)))
    } else {
        Brush.verticalGradient(listOf(Color(0xFFD4D8E2), Color(0xFFAAB0C0)))
    }
    val labelText = if (isCgb) Color(0xFFFF2A6D) else Color(0xFF16181F)

    Box(
        modifier = modifier
            .size(width = width, height = height)
            .shadow(4.dp, RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 3.dp, bottomEnd = 3.dp))
            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 3.dp, bottomEnd = 3.dp))
            .background(bodyColor)
            .border(1.dp, bodyBorder, RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 3.dp, bottomEnd = 3.dp))
            .padding(2.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Notch & Grip Grooves
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.75f)
                    .height(4.dp)
                    .clip(RoundedCornerShape(bottomStart = 3.dp, bottomEnd = 3.dp))
                    .background(notchColor)
            )

            Spacer(modifier = Modifier.height(2.dp))

            // Cartridge Sticker Label
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(2.dp))
                    .background(labelBg)
                    .border(0.5.dp, Color.Black.copy(alpha = 0.3f), RoundedCornerShape(2.dp))
                    .padding(horizontal = 2.dp, vertical = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Text(
                        text = if (isCgb) "COLOR" else "GAME BOY",
                        fontSize = 6.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isCgb) Color(0xFF00E5FF) else Color(0xFF4A4E5C),
                        maxLines = 1
                    )
                    Text(
                        text = title.take(8).uppercase(Locale.getDefault()),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black,
                        color = labelText,
                        maxLines = 2,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "▲",
                        fontSize = 5.sp,
                        color = if (isCgb) Color(0xFF8E95A5) else Color(0xFF6B7280)
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
                textAlign = TextAlign.Center
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RetroCartridgeThumbnail(
                isCgb = game.isCgb,
                title = game.title,
                width = 54.dp,
                height = 66.dp
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
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

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = game.title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${maxOf(1L, game.romSizeBytes / 1024)} KB • ${if (game.isCgb) "Color" else "Game Boy"}",
                        fontSize = 12.sp,
                        color = Color(0xFF8E95A5)
                    )

                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFF00E5FF),
                        modifier = Modifier.clickable { onPlayClick() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(16.dp)
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
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RetroCartridgeThumbnail(
                isCgb = game.isCgb,
                title = game.title,
                width = 46.dp,
                height = 56.dp
            )

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
