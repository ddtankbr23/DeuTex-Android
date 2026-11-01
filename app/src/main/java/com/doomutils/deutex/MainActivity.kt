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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

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

@Composable
private fun DeuTexScreen(nativeStatus: String) {
    val context = LocalContext.current
    var wadInfo by remember { mutableStateOf<WadInfo?>(null) }
    var fileName by remember { mutableStateOf("Ningún WAD abierto") }
    var message by remember { mutableStateOf("Abre un archivo .wad para explorar su contenido.") }

    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            runCatching {
                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    ?: error("No se pudo leer el archivo.")
                val parsed = WadReader.parse(bytes)
                wadInfo = parsed
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
                        Text(fileName, style = MaterialTheme.typography.titleMedium)
                        Text(message, style = MaterialTheme.typography.bodyMedium)
                        Text(nativeStatus, style = MaterialTheme.typography.labelSmall)
                    }
                }
                wadInfo?.let { info ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Nombre del recurso", style = MaterialTheme.typography.titleSmall)
                        Text("Tamaño", style = MaterialTheme.typography.titleSmall)
                    }
                    Divider()
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        itemsIndexed(
                            items = info.entries,
                            key = { index, entry -> "${entry.offset}:${entry.name}:$index" },
                            contentType = { _, _ -> "wad_entry" }
                        ) { index, entry ->
                            Row(
                                Modifier.fillMaxWidth().padding(vertical = 7.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${index + 1}. ${entry.name}",
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1
                                )
                                Text("${entry.size} B", style = MaterialTheme.typography.bodySmall)
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
