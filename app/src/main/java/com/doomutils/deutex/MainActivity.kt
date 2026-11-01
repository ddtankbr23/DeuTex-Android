package com.doomutils.deutex

import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.util.Locale

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
    var fileName by remember { mutableStateOf("Ningún WAD abierto") }
    var message by remember { mutableStateOf("Abre un archivo .wad para explorar su contenido.") }
    var searchQuery by remember { mutableStateOf("") }

    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            runCatching {
                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    ?: error("No se pudo leer el archivo.")
                val parsed = WadReader.parse(bytes)
                wadInfo = parsed
                searchQuery = ""
                fileName = context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameColumn = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameColumn >= 0 && cursor.moveToFirst()) cursor.getString(nameColumn) else null
                } ?: "Archivo WAD"
                message = "${parsed.type} · ${parsed.entries.size} recursos encontrados"
            }.onFailure {
                wadInfo = null
                message = it.message ?: "No se pudo abrir el WAD."
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
                        "Mostrando ${filteredEntries.size} de ${info.entries.size} recursos",
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
                                Modifier.fillMaxWidth().padding(vertical = 7.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${indexed.index + 1}. ${entry.name}",
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1
                                )
                                Text(formatByteSize(entry.size.toLong()), style = MaterialTheme.typography.bodySmall)
                            }
                            Divider()
                        }
                    }
                }
                if (wadInfo == null) {
                    Text(
                        "Primera versión: lectura de cabeceras y listado de recursos. La previsualización y las operaciones de DeuTex se añadirán en las siguientes etapas.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
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
