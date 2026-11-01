package com.doomutils.deutex

import java.nio.ByteBuffer
import java.nio.ByteOrder

data class WadEntry(
    val name: String,
    val offset: Long,
    val size: Long
)

data class WadInfo(
    val type: String,
    val entries: List<WadEntry>
)

object WadReader {
    fun parse(bytes: ByteArray): WadInfo {
        require(bytes.size >= 12) { "El archivo es demasiado pequeño para ser un WAD." }
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        val magic = String(bytes, 0, 4, Charsets.US_ASCII)
        require(magic == "IWAD" || magic == "PWAD") { "Cabecera no válida: no es un IWAD/PWAD." }

        val count = buffer.getInt(4)
        val directoryOffset = buffer.getInt(8)
        require(count >= 0 && count <= 1_000_000) { "Número de entradas no válido." }
        require(directoryOffset >= 0) { "Directorio WAD no válido." }

        val directoryEnd = directoryOffset.toLong() + count.toLong() * 16L
        require(directoryEnd <= bytes.size.toLong()) { "El directorio está fuera del archivo." }

        val entries = ArrayList<WadEntry>(count)
        repeat(count) { index ->
            val pos = directoryOffset + index * 16
            val offset = buffer.getInt(pos).toLong() and 0xffffffffL
            val size = buffer.getInt(pos + 4).toLong() and 0xffffffffL
            val rawName = bytes.copyOfRange(pos + 8, pos + 16)
            val name = rawName.takeWhile { it.toInt() != 0 }
                .toByteArray().toString(Charsets.US_ASCII).trim()
            require(offset + size <= bytes.size.toLong()) {
                "La entrada ${index + 1} ($name) apunta fuera del archivo."
            }
            entries += WadEntry(name.ifBlank { "(sin nombre)" }, offset, size)
        }
        return WadInfo(magic, entries)
    }
}
