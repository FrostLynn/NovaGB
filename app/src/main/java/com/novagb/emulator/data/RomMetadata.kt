package com.novagb.emulator.data

data class RomMetadata(
    val id: String,
    val title: String,
    val uriString: String,
    val isAsset: Boolean = false,
    val isCgb: Boolean = false,
    val cartridgeType: String = "MBC1",
    val romSizeBytes: Long = 0,
    val lastPlayedTimestamp: Long = 0,
    val totalPlayTimeSeconds: Long = 0,
    val bannerColorSeed: Int = 0
)
