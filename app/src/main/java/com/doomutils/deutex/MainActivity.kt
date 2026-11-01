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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

private data class PreviewResult(val bitmap: Bitmap? = null, val error: String? = null)

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

@Composable
private fun DeuTexScreen(nativeStatus: String) {
    val context = LocalContext.current
    var wadInfo by remember { mutableStateOf<WadInfo?>(null) }
    var wadBytes by remember { mutableStateOf<ByteArray?>(null) }
    var fileName by remember { mutableStateOf("Ningún WAD abierto") }
    var message by remember { mutableStateOf("Abre un archivo .wad para explorar su contenido.") }
    var searchQuery by remember { mutableStateOf("") }
    var selectedEntry by remember { mutableStateOf<WadEntry?>(null) }

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
                message = it.message ?: "No se pudo abrir el WAD."
            }
        }
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
                    DoomPatchPreview.decode(bytes, entry, palette)
                }.fold(
                    onSuccess = { PreviewResult(bitmap = it) },
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
                                    .clickable { selectedEntry = entry }
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

    if (selectedEntry != null) {
        val entry = selectedEntry!!
        AlertDialog(
            onDismissRequest = { selectedEntry = null },
            title = { Text(entry.name) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Tamaño: ${formatByteSize(entry.size)}")
                    when {
                        preview == null -> Text("Analizando recurso…")
                        preview?.bitmap != null -> Image(
                            bitmap = preview!!.bitmap!!.asImageBitmap(),
                            contentDescription = "Vista previa de ${entry.name}",
                            modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp),
                            contentScale = ContentScale.Fit
                        )
                        else -> Text(preview?.error ?: "Formato gráfico no compatible.")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedEntry = null }) { Text("Cerrar") }
            }
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
