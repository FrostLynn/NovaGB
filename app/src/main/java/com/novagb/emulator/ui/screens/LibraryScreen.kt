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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import com.novagb.emulator.data.RomMetadata
import com.novagb.emulator.data.RomRepository
import com.novagb.emulator.ui.theme.AccentAmber
import com.novagb.emulator.ui.theme.AccentDanger
import com.novagb.emulator.ui.theme.AccentPrimary
import com.novagb.emulator.ui.theme.AccentSecondary
import com.novagb.emulator.ui.theme.AccentSuccess
import com.novagb.emulator.ui.theme.BadgeCgbBg
import com.novagb.emulator.ui.theme.BadgeDmgBg
import com.novagb.emulator.ui.theme.CartridgeCgbBody
import com.novagb.emulator.ui.theme.CartridgeCgbNotch
import com.novagb.emulator.ui.theme.CartridgeDmgBody
import com.novagb.emulator.ui.theme.CartridgeDmgBorder
import com.novagb.emulator.ui.theme.CartridgeDmgNotch
import com.novagb.emulator.ui.theme.DarkBackground
import com.novagb.emulator.ui.theme.DarkBorder
import com.novagb.emulator.ui.theme.DarkSurface
import com.novagb.emulator.ui.theme.DarkSurfaceSubtle
import com.novagb.emulator.ui.theme.DarkSurfaceVariant
import com.novagb.emulator.ui.theme.TextMuted
import com.novagb.emulator.ui.theme.TextPrimary
import com.novagb.emulator.ui.theme.TextSecondary
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
    var gameToDelete by remember { mutableStateOf<RomMetadata?>(null) }

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
                                .background(AccentPrimary),
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
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "NovaGB",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(DarkSurfaceVariant)
                                        .border(0.5.dp, DarkBorder, RoundedCornerShape(4.dp))
                                        .padding(horizontal = 5.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "DMG • CGB",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = AccentPrimary,
                                        letterSpacing = 0.5.sp
                                    )
                                }
                            }
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = TextSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBackground
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { romPickerLauncher.launch(arrayOf("*/*")) },
                containerColor = AccentPrimary,
                contentColor = Color.Black,
                shape = CircleShape,
                modifier = Modifier.size(56.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Import ROM",
                    modifier = Modifier.size(26.dp)
                )
            }
        },
        containerColor = DarkBackground
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
                        placeholder = {
                            Text(
                                text = "Search ROM collection...",
                                color = TextMuted,
                                fontSize = 14.sp
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = AccentPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(
                                    onClick = { searchQuery = "" },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Clear search",
                                        tint = TextSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(DarkSurfaceVariant),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentPrimary,
                            unfocusedBorderColor = DarkBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            cursorColor = AccentPrimary
                        ),
                        textStyle = TextStyle(fontSize = 14.sp),
                        singleLine = true
                    )
                }

                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            LibraryFilter.entries.forEach { filter ->
                                val isSelected = selectedFilter == filter
                                val count = when (filter) {
                                    LibraryFilter.ALL -> romList.size
                                    LibraryFilter.DMG -> romList.count { !it.isCgb }
                                    LibraryFilter.CGB -> romList.count { it.isCgb }
                                }

                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = if (isSelected) AccentPrimary else DarkSurfaceVariant,
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (isSelected) AccentPrimary else DarkBorder
                                    ),
                                    modifier = Modifier
                                        .heightIn(min = 40.dp)
                                        .clickable { selectedFilter = filter }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${filter.label} ($count)",
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) Color.Black else TextSecondary
                                        )
                                    }
                                }
                            }
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Sort:", fontSize = 11.sp, color = TextSecondary)
                            LibrarySort.entries.forEach { sort ->
                                val isSelected = selectedSort == sort
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) DarkSurfaceVariant else Color.Transparent,
                                    border = androidx.compose.foundation.BorderStroke(
                                        0.5.dp,
                                        if (isSelected) AccentPrimary.copy(alpha = 0.6f) else Color.Transparent
                                    ),
                                    modifier = Modifier
                                        .heightIn(min = 36.dp)
                                        .clickable { selectedSort = sort }
                                ) {
                                    Box(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = sort.label,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) AccentPrimary else TextSecondary
                                        )
                                    }
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
                        color = TextMuted,
                        letterSpacing = 1.sp
                    )
                }

                items(filteredList, key = { it.id }) { game ->
                    GameCardItem(
                        game = game,
                        onClick = { handleLaunchGame(game) },
                        onDeleteClick = { gameToDelete = game }
                    )
                }
            }
        }

        gameToDelete?.let { game ->
            AlertDialog(
                onDismissRequest = { gameToDelete = null },
                title = {
                    Text(
                        text = "Remove from Library",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = TextPrimary
                    )
                },
                text = {
                    Text(
                        text = "Are you sure you want to remove \"${game.title}\" from your library? The ROM file on your device storage will not be affected.",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            handleDeleteGame(game)
                            gameToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentDanger),
                        modifier = Modifier.heightIn(min = 44.dp)
                    ) {
                        Text("Remove", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = { gameToDelete = null },
                        modifier = Modifier.heightIn(min = 44.dp)
                    ) {
                        Text("Cancel", color = TextPrimary)
                    }
                },
                containerColor = DarkSurface,
                shape = RoundedCornerShape(16.dp)
            )
        }
    }
}

/**
 * Game cover art component that fetches official box art from Libretro CDN,
 * falling back gracefully to the miniature RetroCartridgeThumbnail when offline or missing.
 */
@Composable
fun GameCoverArt(
    coverUrl: String?,
    isCgb: Boolean,
    title: String,
    width: Dp,
    height: Dp,
    modifier: Modifier = Modifier
) {
    if (coverUrl.isNullOrBlank()) {
        RetroCartridgeThumbnail(
            isCgb = isCgb,
            title = title,
            width = width,
            height = height,
            modifier = modifier
        )
    } else {
        SubcomposeAsyncImage(
            model = coverUrl,
            contentDescription = "$title box art",
            modifier = modifier
                .size(width = width, height = height)
                .clip(RoundedCornerShape(6.dp))
                .border(1.dp, DarkBorder, RoundedCornerShape(6.dp)),
            contentScale = ContentScale.Crop,
            loading = {
                RetroCartridgeThumbnail(
                    isCgb = isCgb,
                    title = title,
                    width = width,
                    height = height
                )
            },
            error = {
                RetroCartridgeThumbnail(
                    isCgb = isCgb,
                    title = title,
                    width = width,
                    height = height
                )
            }
        )
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
    val bodyColor = if (isCgb) CartridgeCgbBody else CartridgeDmgBody
    val bodyBorder = if (isCgb) AccentPrimary.copy(alpha = 0.6f) else CartridgeDmgBorder
    val notchColor = if (isCgb) CartridgeCgbNotch else CartridgeDmgNotch
    val labelBg = if (isCgb) {
        Brush.verticalGradient(listOf(Color(0xFF2C1E38), Color(0xFF13101E)))
    } else {
        Brush.verticalGradient(listOf(Color(0xFFD4D8E2), Color(0xFFAAB0C0)))
    }
    val labelText = if (isCgb) AccentSecondary else Color(0xFF16181F)

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
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.75f)
                    .height(4.dp)
                    .clip(RoundedCornerShape(bottomStart = 3.dp, bottomEnd = 3.dp))
                    .background(notchColor)
            )

            Spacer(modifier = Modifier.height(2.dp))

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
                        color = if (isCgb) AccentPrimary else Color(0xFF4A4E5C),
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
                        color = if (isCgb) TextMuted else TextSecondary
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
            .background(DarkSurface)
            .border(1.dp, DarkBorder, RoundedCornerShape(16.dp))
            .padding(vertical = 40.dp, horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(DarkSurfaceVariant)
                    .border(1.dp, DarkBorder, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.SportsEsports,
                    contentDescription = null,
                    tint = AccentPrimary,
                    modifier = Modifier.size(32.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Your Library is Empty",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Tap below or use the '+' button to load Game Boy (.gb) and Game Boy Color (.gbc) ROMs from your device.",
                fontSize = 13.sp,
                color = TextMuted,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )
            Spacer(modifier = Modifier.height(20.dp))
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = AccentPrimary,
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .clickable { onImportClick() }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "ADD ROM",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        letterSpacing = 0.5.sp
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
                    colors = listOf(DarkSurfaceVariant, DarkSurfaceSubtle)
                )
            )
            .border(1.dp, DarkBorder, RoundedCornerShape(16.dp))
            .padding(18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GameCoverArt(
                coverUrl = game.coverUrl,
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(AccentSuccess)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "READY TO PLAY",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentPrimary,
                            letterSpacing = 1.sp
                        )
                    }

                    Text(
                        text = game.cartridgeType,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextSecondary
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = game.title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
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
                        color = TextMuted
                    )

                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = AccentPrimary,
                        modifier = Modifier
                            .heightIn(min = 40.dp)
                            .clickable { onPlayClick() }
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
}

@Composable
private fun GameCardItem(
    game: RomMetadata,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit
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
            .background(DarkSurface)
            .border(1.dp, DarkBorder, RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            GameCoverArt(
                coverUrl = game.coverUrl,
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
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (game.isCgb) BadgeCgbBg else BadgeDmgBg)
                            .border(
                                0.5.dp,
                                if (game.isCgb) AccentPrimary.copy(alpha = 0.5f) else AccentAmber.copy(alpha = 0.5f),
                                RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = if (game.isCgb) "CGB" else "DMG",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (game.isCgb) AccentPrimary else AccentAmber
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Text(
                        text = "${maxOf(1L, game.romSizeBytes / 1024)} KB • $dateStr",
                        fontSize = 12.sp,
                        color = TextMuted
                    )
                }
            }

            IconButton(
                onClick = onDeleteClick,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Remove ROM",
                    tint = TextMuted,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
