package com.doomutils.deutex

import java.util.Locale

/** Format-neutral vocabulary shared by archive readers and resource editors. */
enum class ArchiveFormat { WAD, ZIP, UNKNOWN }

enum class ResourceKind { IMAGE, AUDIO, MUSIC, TEXT_SCRIPT, MAP, MODEL, UNKNOWN }

enum class ScriptLanguage(val extensions: Set<String>) {
    ZSCRIPT(setOf("zs", "zsc")),
    ACS(setOf("acs")),
    DECORATE(setOf("decorate")),
    MAPINFO(setOf("mapinfo", "zmapinfo")),
    TEXTURE_DEFINITIONS(setOf("textures")),
    MODELDEF(setOf("modeldef")),
    SNDINFO(setOf("sndinfo")),
    TERRAIN(setOf("terrain")),
    MENUDEF(setOf("menudef")),
    CVARINFO(setOf("cvarinfo")),
    UNKNOWN(emptySet());

    companion object {
        fun fromFileName(fileName: String): ScriptLanguage {
            val extension = fileName.substringAfterLast('.', "").lowercase(Locale.ROOT)
            return entries.firstOrNull { extension in it.extensions } ?: UNKNOWN
        }
    }
}

/** Detect by signature first; extensions are only a fallback for empty files. */
object ArchiveFormatDetector {
    fun detect(fileName: String, header: ByteArray): ArchiveFormat {
        if (header.size >= 4) {
            val magic = header.copyOfRange(0, 4).toString(Charsets.US_ASCII)
            if (magic == "IWAD" || magic == "PWAD") return ArchiveFormat.WAD
            if (header[0] == 0x50.toByte() && header[1] == 0x4b.toByte() &&
                header[2] in byteArrayOf(0x03, 0x05, 0x07).map { it.toByte() } &&
                header[3] in byteArrayOf(0x04, 0x06, 0x08).map { it.toByte() }
            ) return ArchiveFormat.ZIP
        }
        if (header.isEmpty()) {
            return when (fileName.substringAfterLast('.', "").lowercase(Locale.ROOT)) {
                "wad" -> ArchiveFormat.WAD
                "zip", "pk3", "pk7" -> ArchiveFormat.ZIP
                else -> ArchiveFormat.UNKNOWN
            }
        }
        return ArchiveFormat.UNKNOWN
    }
}

object ResourceKindClassifier {
    private val imageExtensions = setOf("png", "jpg", "jpeg", "bmp", "gif", "tga", "pcx")
    private val audioExtensions = setOf("wav", "ogg", "flac", "mp3")
    private val musicExtensions = setOf("mid", "midi", "mus", "ogg")
    private val mapExtensions = setOf("wad", "map", "udmf")
    private val modelExtensions = setOf("md2", "md3", "obj", "gltf", "glb")

    fun classify(path: String): ResourceKind {
        val extension = path.substringAfterLast('.', "").lowercase(Locale.ROOT)
        if (ScriptLanguage.fromFileName(path) != ScriptLanguage.UNKNOWN) return ResourceKind.TEXT_SCRIPT
        return when (extension) {
            in imageExtensions -> ResourceKind.IMAGE
            in audioExtensions -> ResourceKind.AUDIO
            in musicExtensions -> ResourceKind.MUSIC
            in mapExtensions -> ResourceKind.MAP
            in modelExtensions -> ResourceKind.MODEL
            else -> ResourceKind.UNKNOWN
        }
    }
}
