package com.novagb.emulator.data

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import com.novagb.emulator.core.GameBoy
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentLinkedDeque

object EmulatorLogger {
    private const val MAX_LOGS = 500
    private val logs = ConcurrentLinkedDeque<String>()
    private val timeFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)

    init {
        logInfo("System", "NovaGB Diagnostic Logger initialized")
    }

    fun logInfo(tag: String, message: String) {
        appendEntry("INFO", tag, message)
    }

    fun logWarn(tag: String, message: String) {
        appendEntry("WARN", tag, message)
    }

    fun logError(tag: String, message: String, throwable: Throwable? = null, gameBoy: GameBoy? = null) {
        val sb = StringBuilder()
        sb.append(message)
        if (gameBoy != null) {
            sb.append("\n--- GameBoy State Dump ---")
            val cart = gameBoy.cartridge
            if (cart != null) {
                sb.append("\nCartridge: ${cart.header.title}")
                sb.append(" (CGB: ${cart.header.isCgb}, Type: 0x${cart.header.cartridgeType.toString(16)})")
            } else {
                sb.append("\nCartridge: None")
            }
            sb.append("\nCPU: PC=0x${gameBoy.cpu.pc.toString(16).padStart(4, '0')}, SP=0x${gameBoy.cpu.sp.toString(16).padStart(4, '0')}")
            sb.append(", A=0x${gameBoy.cpu.a.toString(16).padStart(2, '0')}, F=0x${gameBoy.cpu.f.toString(16).padStart(2, '0')}")
            sb.append(", B=0x${gameBoy.cpu.b.toString(16).padStart(2, '0')}, C=0x${gameBoy.cpu.c.toString(16).padStart(2, '0')}")
            sb.append(", D=0x${gameBoy.cpu.d.toString(16).padStart(2, '0')}, E=0x${gameBoy.cpu.e.toString(16).padStart(2, '0')}")
            sb.append(", H=0x${gameBoy.cpu.h.toString(16).padStart(2, '0')}, L=0x${gameBoy.cpu.l.toString(16).padStart(2, '0')}")
            sb.append(", IME=${gameBoy.cpu.ime}, Halted=${gameBoy.cpu.halted}")
            sb.append("\nPPU: LCDC=0x${gameBoy.ppu.lcdc.toString(16).padStart(2, '0')}, STAT=0x${gameBoy.ppu.stat.toString(16).padStart(2, '0')}")
            sb.append(", LY=${gameBoy.ppu.ly}, LYC=${gameBoy.ppu.lyc}")
            sb.append(", SCX=${gameBoy.ppu.scx}, SCY=${gameBoy.ppu.scy}, WX=${gameBoy.ppu.wx}, WY=${gameBoy.ppu.wy}")
            sb.append("\nTimer: DIV=0x${gameBoy.timer.div.toString(16)}, TIMA=${gameBoy.timer.tima}, TMA=${gameBoy.timer.tma}, TAC=${gameBoy.timer.tac}")
            sb.append("\nInterrupts: IF=0x${gameBoy.mmu.readByte(0xFF0F).toString(16)}, IE=0x${gameBoy.mmu.readByte(0xFFFF).toString(16)}")
        }
        if (throwable != null) {
            val sw = StringWriter()
            throwable.printStackTrace(PrintWriter(sw))
            sb.append("\n--- Stack Trace ---\n").append(sw.toString())
        }
        appendEntry("ERROR", tag, sb.toString())
    }

    private fun appendEntry(level: String, tag: String, text: String) {
        val time = timeFormat.format(Date())
        val entry = "[$time] [$level/$tag] $text"
        logs.addLast(entry)
        while (logs.size > MAX_LOGS) {
            logs.pollFirst()
        }
    }

    fun getFormattedLogs(): String {
        return logs.joinToString("\n")
    }

    fun getLogCount(): Int = logs.size

    fun clear() {
        logs.clear()
        logInfo("System", "Logs cleared")
    }

    fun copyToClipboard(context: Context): Boolean {
        return try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            if (clipboard != null) {
                val clip = ClipData.newPlainText("NovaGB Logs", getFormattedLogs())
                clipboard.setPrimaryClip(clip)
                true
            } else {
                false
            }
        } catch (_: Exception) {
            false
        }
    }

    fun exportToFile(context: Context): File? {
        return try {
            val dir = context.getExternalFilesDir(null) ?: context.filesDir
            val file = File(dir, "novagb_logs.txt")
            file.writeText(getFormattedLogs())
            file
        } catch (_: Exception) {
            null
        }
    }
}
