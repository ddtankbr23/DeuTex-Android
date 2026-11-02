package com.doomutils.deutex

import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Locale
import java.util.zip.CRC32

private data class PreviewResult(
    val bitmap: Bitmap? = null,
    val error: String? = null,
    val offsetX: Int = 0,
    val offsetY: Int = 0
)

private fun formatByteSize(bytes: Long): String {
    if (bytes < 1000L) return "$bytes B"
    val units = arrayOf("KB", "MB", "GB", "TB")
    var value = bytes.toDouble()
    var unitIndex = -1
    do {
        value /= 1000.0
        unitIndex++
    } while (value >= 1000.0 && unitIndex < units.lastIndex)
    return String.format(Locale.ROOT, "%.1f %s", value, units[unitIndex])
}

private fun readGraphicOffsets(bytes: ByteArray, entry: WadEntry): Pair<Int, Int> {
    if (entry.size < 8L || entry.offset < 0L || entry.offset + 8L > bytes.size.toLong()) {
        return 0 to 0
    }
    val start = entry.offset.toInt()
    val isPng = bytes[start] == 0x89.toByte() &&
        bytes[start + 1] == 0x50.toByte() &&
        bytes[start + 2] == 0x4E.toByte() &&
        bytes[start + 3] == 0x47.toByte()
    if (isPng) {
        // SLADE-compatible PNG offsets are stored in the ancillary "grAb" chunk.
        val end = (entry.offset + entry.size).coerceAtMost(bytes.size.toLong()).toInt()
        var position = start + 8
        while (position + 12 <= end) {
            val chunkLength = ByteBuffer.wrap(bytes, position, 4)
                .order(ByteOrder.BIG_ENDIAN).int.toLong() and 0xffffffffL
            val dataStart = position + 8
            val chunkEnd = dataStart.toLong() + chunkLength + 4L
            if (chunkEnd > end.toLong() || chunkLength > Int.MAX_VALUE) break
            val isGrab = bytes[position + 4] == 'g'.code.toByte() &&
                bytes[position + 5] == 'r'.code.toByte() &&
                bytes[position + 6] == 'A'.code.toByte() &&
                bytes[position + 7] == 'b'.code.toByte()
            if (isGrab && chunkLength >= 8L) {
                val offsets = ByteBuffer.wrap(bytes, dataStart, 8).order(ByteOrder.BIG_ENDIAN)
                return offsets.int to offsets.int
            }
            position = chunkEnd.toInt()
        }
        return 0 to 0
    }
    val header = ByteBuffer.wrap(bytes, start + 4, 4).order(ByteOrder.LITTLE_ENDIAN)
    return header.short.toInt() to header.short.toInt()
}

private fun applyOffsetEditsToWad(
    source: ByteArray,
    entries: List<WadEntry>,
    edits: Map<String, Pair<Int, Int>>
): ByteArray {
    val output = source.copyOf()
    for (entry in entries) {
        val offsets = edits[entryKey(entry)] ?: continue
        val (x, y) = offsets
        require(entry.offset >= 0L && entry.size >= 8L &&
            entry.offset + entry.size <= output.size.toLong()
        ) { "El recurso ${entry.name} está fuera de los límites del WAD." }
        val start = entry.offset.toInt()
        val isPng = output[start] == 0x89.toByte() &&
            output[start + 1] == 0x50.toByte() &&
            output[start + 2] == 0x4E.toByte() &&
            output[start + 3] == 0x47.toByte()

        if (isPng) {
            val end = (entry.offset + entry.size).toInt()
            var position = start + 8
            var updated = false
            while (position + 12 <= end) {
                val length = ByteBuffer.wrap(output, position, 4)
                    .order(ByteOrder.BIG_ENDIAN).int.toLong() and 0xffffffffL
                val dataStart = position + 8
                val chunkEnd = dataStart.toLong() + length + 4L
                if (chunkEnd > end.toLong() || length > Int.MAX_VALUE) break
                val isGrab = output[position + 4] == 'g'.code.toByte() &&
                    output[position + 5] == 'r'.code.toByte() &&
                    output[position + 6] == 'A'.code.toByte() &&
                    output[position + 7] == 'b'.code.toByte()
                if (isGrab) {
                    require(length >= 8L) { "El bloque grAb de ${entry.name} está incompleto." }
                    ByteBuffer.wrap(output, dataStart, 8)
                        .order(ByteOrder.BIG_ENDIAN)
                        .putInt(x)
                        .putInt(y)
                    val crc = CRC32()
                    crc.update(output, position + 4, 4 + length.toInt())
                    ByteBuffer.wrap(output, (dataStart + length.toInt()), 4)
                        .order(ByteOrder.BIG_ENDIAN)
                        .putInt(crc.value.toInt())
                    updated = true
                    break
                }
                position = chunkEnd.toInt()
            }
            require(updated) {
                "El PNG ${entry.name} no contiene un bloque grAb. No se modificó para evitar alterar su estructura."
            }
        } else {
            require(x in Short.MIN_VALUE..Short.MAX_VALUE && y in Short.MIN_VALUE..Short.MAX_VALUE) {
                "Los offsets de un gráfico Doom clásico deben estar entre -32768 y 32767."
            }
            output[start + 4] = (x and 0xff).toByte()
            output[start + 5] = ((x shr 8) and 0xff).toByte()
            output[start + 6] = (y and 0xff).toByte()
            output[start + 7] = ((y shr 8) and 0xff).toByte()
        }
    }
    return output
}

private fun entryKey(entry: WadEntry): String = "${entry.offset}:${entry.name}"

@Composable
private fun DeuTexScreen(nativeStatus: String) {
    val context = LocalContext.current
    var wadInfo by remember { mutableStateOf<WadInfo?>(null) }
    var wadBytes by remember { mutableStateOf<ByteArray?>(null) }
    var fileName by remember { mutableStateOf("Ningún WAD abierto") }
    var message by remember { mutableStateOf("Abre un archivo .wad para explorar su contenido.") }
    var searchQuery by remember { mutableStateOf("") }
    var selectedEntry by remember { mutableStateOf<WadEntry?>(null) }
    var editorOpen by remember { mutableStateOf(false) }
    val editedOffsets = remember { mutableStateMapOf<String, Pair<Int, Int>>() }

    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            runCatching {
                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    ?: error("No se pudo leer el archivo.")
                val parsed = WadReader.parse(bytes)
                wadBytes = bytes
                wadInfo = parsed
                selectedEntry = null
                editorOpen = false
                editedOffsets.clear()
                searchQuery = ""
                fileName = context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameColumn = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameColumn >= 0 && cursor.moveToFirst()) cursor.getString(nameColumn) else null
                } ?: "Archivo WAD"
                message = "${parsed.type} · ${parsed.entries.size} recursos encontrados"
            }.onFailure {
                wadInfo = null
                wadBytes = null
                selectedEntry = null
                editorOpen = false
                editedOffsets.clear()
                message = it.message ?: "No se pudo abrir el WAD."
            }
        }
    }

    var pendingSaveBytes by remember { mutableStateOf<ByteArray?>(null) }
    val saveWadLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri: Uri? ->
        val bytesToSave = pendingSaveBytes
        if (uri != null && bytesToSave != null) {
            runCatching {
                context.contentResolver.openOutputStream(uri, "w")?.use { it.write(bytesToSave) }
                    ?: error("No se pudo crear el archivo de salida.")
                wadBytes = bytesToSave
                editedOffsets.clear()
                message = "${wadInfo?.type ?: "WAD"} · cambios guardados en la copia exportada"
            }.onFailure {
                message = it.message ?: "No se pudo guardar el WAD."
            }
        }
        pendingSaveBytes = null
    }

    val preview by produceState<PreviewResult?>(initialValue = null, selectedEntry, wadBytes, wadInfo) {
        val entry = selectedEntry
        val bytes = wadBytes
        val info = wadInfo
        value = if (entry == null || bytes == null || info == null) {
            null
        } else {
            withContext(Dispatchers.Default) {
                runCatching {
                    val palette = info.entries.firstOrNull { it.name.equals("PLAYPAL", ignoreCase = true) }
                    val bitmap = DoomPatchPreview.decode(bytes, entry, palette)
                    val offsets = readGraphicOffsets(bytes, entry)
                    PreviewResult(bitmap = bitmap, offsetX = offsets.first, offsetY = offsets.second)
                }.fold(
                    onSuccess = { it },
                    onFailure = { PreviewResult(error = it.message ?: "No se pudo interpretar este gráfico.") }
                )
            }
        }
    }

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(
                modifier = Modifier.fillMaxSize().padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("DeuTex Android", style = MaterialTheme.typography.headlineMedium)
                Text("Explorador de archivos WAD · ARM64", style = MaterialTheme.typography.bodyMedium)
                Button(onClick = { picker.launch(arrayOf("*/*")) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Abrir archivo WAD")
                }
                Button(
                    onClick = {
                        val source = wadBytes
                        val info = wadInfo
                        if (source != null && info != null && editedOffsets.isNotEmpty()) {
                            runCatching {
                                pendingSaveBytes = applyOffsetEditsToWad(source, info.entries, editedOffsets.toMap())
                                val baseName = fileName.substringBeforeLast('.', fileName)
                                saveWadLauncher.launch("${baseName}_editado.wad")
                            }.onFailure {
                                message = it.message ?: "No se pudieron preparar los cambios."
                            }
                        }
                    },
                    enabled = wadBytes != null && editedOffsets.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (editedOffsets.isEmpty()) "Guardar WAD (sin cambios)" else "Exportar WAD modificado")
                }
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(fileName, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                        Text(message, style = MaterialTheme.typography.bodyMedium)
                        Text(nativeStatus, style = MaterialTheme.typography.labelSmall)
                    }
                }
                wadInfo?.let { info ->
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Buscar recurso") },
                        placeholder = { Text("Ej. PLAYPAL, MAP01, D_*") },
                        singleLine = true
                    )
                    val query = searchQuery.trim().lowercase(Locale.ROOT)
                    val filteredEntries = remember(info.entries, query) {
                        info.entries.withIndex().filter { (_, entry) ->
                            query.isEmpty() || entry.name.lowercase(Locale.ROOT).contains(query)
                        }
                    }
                    Text(
                        "Mostrando ${filteredEntries.size} de ${info.entries.size} recursos · Toca uno para ver si es un gráfico compatible",
                        style = MaterialTheme.typography.labelMedium
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Nombre del recurso", style = MaterialTheme.typography.titleSmall)
                        Text("Tamaño", style = MaterialTheme.typography.titleSmall)
                    }
                    Divider()
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        items(
                            items = filteredEntries,
                            key = { indexed -> "${indexed.value.offset}:${indexed.value.name}:${indexed.index}" },
                            contentType = { "wad_entry" }
                        ) { indexed ->
                            val entry = indexed.value
                            Row(
                                Modifier.fillMaxWidth()
                                    .clickable {
                                        editorOpen = false
                                        selectedEntry = entry
                                    }
                                    .padding(vertical = 7.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${indexed.index + 1}. ${entry.name}",
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1
                                )
                                Text(formatByteSize(entry.size), style = MaterialTheme.typography.bodySmall)
                            }
                            Divider()
                        }
                    }
                }
                if (wadInfo == null) {
                    Text(
                        "Primera versión: lectura de cabeceras y listado de recursos. La previsualización gráfica está en desarrollo.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }

    val entry = selectedEntry
    if (entry != null && !editorOpen) {
        val key = entryKey(entry)
        val currentOffsets = editedOffsets[key]
        val offsetX = currentOffsets?.first ?: preview?.offsetX ?: 0
        val offsetY = currentOffsets?.second ?: preview?.offsetY ?: 0
        AlertDialog(
            onDismissRequest = { selectedEntry = null },
            title = { Text(entry.name) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Tamaño: ${formatByteSize(entry.size)}")
                    when {
                        preview == null -> Text("Analizando recurso…")
                        preview?.bitmap != null -> {
                            Image(
                                bitmap = preview!!.bitmap!!.asImageBitmap(),
                                contentDescription = "Vista previa de ${entry.name}",
                                modifier = Modifier.fillMaxWidth().heightIn(max = 360.dp),
                                contentScale = ContentScale.Fit
                            )
                            Text("Offset X: $offsetX · Offset Y: $offsetY")
                            if (currentOffsets != null) {
                                Text("Cambios temporales sin guardar en el WAD.", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                        else -> Text(preview?.error ?: "Formato gráfico no compatible.")
                    }
                }
            },
            confirmButton = {
                Row {
                    if (preview?.bitmap != null) {
                        TextButton(onClick = { editorOpen = true }) { Text("Editar") }
                    }
                    TextButton(onClick = { selectedEntry = null }) { Text("Cerrar") }
                }
            }
        )
    }

    if (entry != null && editorOpen && preview?.bitmap != null) {
        val key = entryKey(entry)
        val currentOffsets = editedOffsets[key]
        GraphicEditorDialog(
            bitmap = preview!!.bitmap!!,
            initialOffsetX = currentOffsets?.first ?: preview!!.offsetX,
            initialOffsetY = currentOffsets?.second ?: preview!!.offsetY,
            onApply = { x, y ->
                editedOffsets[key] = x to y
                editorOpen = false
            },
            onDismiss = { editorOpen = false }
        )
    }
}

class MainActivity : ComponentActivity() {
    external fun nativeEngineStatus(): String

    companion object {
        init { System.loadLibrary("deutex_native") }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { DeuTexScreen(nativeEngineStatus()) }
    }
}
