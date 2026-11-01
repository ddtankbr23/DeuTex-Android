package com.doomutils.deutex

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.nio.ByteBuffer
import java.nio.ByteOrder

object DoomPatchPreview {
    private val pngSignature = byteArrayOf(
        0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
    )

    fun decode(wadBytes: ByteArray, entry: WadEntry, paletteEntry: WadEntry?): Bitmap {
        require(entry.offset >= 0L && entry.size >= 0L &&
            entry.offset + entry.size <= wadBytes.size.toLong()
        ) { "Recurso fuera de los límites del WAD." }

        val start = entry.offset.toInt()
        val size = entry.size.toInt()
        require(size > 0) { "El recurso está vacío." }

        // PNG is self-contained: its pixels, color and alpha are stored in the image.
        if (size >= pngSignature.size &&
            pngSignature.indices.all { wadBytes[start + it] == pngSignature[it] }
        ) {
            return BitmapFactory.decodeByteArray(wadBytes, start, size)
                ?: error("El recurso tiene firma PNG, pero Android no pudo decodificar la imagen.")
        }

        require(size >= 8) { "Este recurso no parece ser un PNG ni un gráfico Doom compatible." }
        val end = start + size
        val data = ByteBuffer.wrap(wadBytes, start, size).slice().order(ByteOrder.LITTLE_ENDIAN)
        val width = data.short.toInt() and 0xffff
        val height = data.short.toInt() and 0xffff
        data.short // left offset
        data.short // top offset

        require(width in 1..2048 && height in 1..2048 && width.toLong() * height <= 4_194_304L) {
            "Dimensiones de imagen no compatibles o demasiado grandes."
        }
        require(8L + width.toLong() * 4L <= entry.size) { "Tabla de columnas incompleta." }

        val palette = paletteEntry ?: error(
            "Este gráfico Doom necesita PLAYPAL. Selecciona un IWAD base (por ejemplo DOOM.WAD) para usar su paleta."
        )
        require(palette.size >= 768L) { "La paleta PLAYPAL está incompleta." }
        require(palette.offset >= 0L && palette.offset + 768L <= wadBytes.size.toLong()) {
            "Paleta fuera de los límites del WAD."
        }
        val paletteStart = palette.offset.toInt()

        val pixels = IntArray(width * height)
        val previousTop = IntArray(width) { -1 }

        for (x in 0 until width) {
            val columnOffset = data.getInt(8 + x * 4)
            require(columnOffset >= 0 && columnOffset < entry.size) {
                "Desplazamiento de columna no válido."
            }
            var pos = start + columnOffset
            var posts = 0
            while (true) {
                require(pos < end && posts++ < height + 256) { "Columna de imagen truncada." }
                val rawTop = wadBytes[pos++].toInt() and 0xff
                if (rawTop == 255) break
                require(pos + 2 <= end) { "Cabecera de poste incompleta." }
                val length = wadBytes[pos++].toInt() and 0xff
                pos++ // byte de relleno
                val top = if (previousTop[x] >= 0 && rawTop <= previousTop[x]) {
                    rawTop + previousTop[x]
                } else rawTop
                previousTop[x] = top
                require(pos + length + 1 <= end) { "Datos de píxeles incompletos." }

                for (i in 0 until length) {
                    val y = top + i
                    val colorIndex = wadBytes[pos + i].toInt() and 0xff
                    if (y in 0 until height) {
                        val p = paletteStart + colorIndex * 3
                        val red = wadBytes[p].toInt() and 0xff
                        val green = wadBytes[p + 1].toInt() and 0xff
                        val blue = wadBytes[p + 2].toInt() and 0xff
                        pixels[y * width + x] =
                            (0xff shl 24) or (red shl 16) or (green shl 8) or blue
                    }
                }
                pos += length + 1 // pixels and trailing pad
            }
        }

        return Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)
    }
}
