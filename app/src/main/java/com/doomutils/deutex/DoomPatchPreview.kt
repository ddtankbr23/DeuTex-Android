package com.doomutils.deutex

import android.graphics.Bitmap
import java.nio.ByteBuffer
import java.nio.ByteOrder

object DoomPatchPreview {
    fun decode(wadBytes: ByteArray, entry: WadEntry, paletteEntry: WadEntry?): Bitmap {
        require(entry.size >= 8L) { "Este recurso es demasiado pequeño para ser un gráfico Doom." }
        val start = entry.offset.toInt()
        val end = (entry.offset + entry.size).toInt()
        require(start >= 0 && end <= wadBytes.size && end >= start) { "Recurso fuera de los límites del WAD." }

        val data = ByteBuffer.wrap(wadBytes, start, end - start).slice().order(ByteOrder.LITTLE_ENDIAN)
        val width = data.short.toInt() and 0xffff
        val height = data.short.toInt() and 0xffff
        data.short // left offset
        data.short // top offset

        require(width in 1..2048 && height in 1..2048 && width.toLong() * height <= 4_194_304L) {
            "Dimensiones de imagen no compatibles o demasiado grandes."
        }
        require(8L + width.toLong() * 4L <= entry.size) { "Tabla de columnas incompleta." }

        val palette = paletteEntry ?: error("No se encontró PLAYPAL. Abre un WAD que contenga la paleta del juego.")
        require(palette.size >= 768L) { "La paleta PLAYPAL está incompleta." }
        val paletteStart = palette.offset.toInt()
        require(paletteStart >= 0 && palette.offset + 768L <= wadBytes.size) { "Paleta fuera de los límites del WAD." }

        val pixels = IntArray(width * height)
        val previousTop = IntArray(width) { -1 }

        for (x in 0 until width) {
            val columnOffset = data.getInt(8 + x * 4)
            require(columnOffset >= 0 && columnOffset < entry.size) { "Desplazamiento de columna no válido." }
            var pos = start + columnOffset
            val limit = end
            var posts = 0
            while (true) {
                require(pos < limit && posts++ < height + 256) { "Columna de imagen truncada." }
                val rawTop = wadBytes[pos++].toInt() and 0xff
                if (rawTop == 255) break
                require(pos + 2 <= limit) { "Cabecera de poste incompleta." }
                val length = wadBytes[pos++].toInt() and 0xff
                pos++ // byte de relleno
                val top = if (previousTop[x] >= 0 && rawTop <= previousTop[x]) rawTop + previousTop[x] else rawTop
                previousTop[x] = top
                require(pos + length + 1 <= limit) { "Datos de píxeles incompletos." }

                for (i in 0 until length) {
                    val y = top + i
                    val colorIndex = wadBytes[pos + i].toInt() and 0xff
                    if (y in 0 until height) {
                        val p = paletteStart + colorIndex * 3
                        val red = wadBytes[p].toInt() and 0xff
                        val green = wadBytes[p + 1].toInt() and 0xff
                        val blue = wadBytes[p + 2].toInt() and 0xff
                        pixels[y * width + x] = (0xff shl 24) or (red shl 16) or (green shl 8) or blue
                    }
                }
                pos += length + 1 // pixels and trailing pad
            }
        }

        return Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)
    }
}
