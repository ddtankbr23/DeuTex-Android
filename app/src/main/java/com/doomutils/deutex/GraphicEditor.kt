package com.doomutils.deutex

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawImage
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import android.graphics.Bitmap
import kotlin.math.roundToInt
import kotlin.math.min

@androidx.compose.runtime.Composable
fun GraphicEditorDialog(
    bitmap: Bitmap,
    initialOffsetX: Int,
    initialOffsetY: Int,
    onApply: (Int, Int) -> Unit,
    onDismiss: () -> Unit
) {
    var offsetX by androidx.compose.runtime.remember(bitmap, initialOffsetX) {
        androidx.compose.runtime.mutableIntStateOf(initialOffsetX)
    }
    var offsetY by androidx.compose.runtime.remember(bitmap, initialOffsetY) {
        androidx.compose.runtime.mutableIntStateOf(initialOffsetY)
    }
    var offsetXText by androidx.compose.runtime.remember(offsetX) {
        androidx.compose.runtime.mutableStateOf(offsetX.toString())
    }
    var offsetYText by androidx.compose.runtime.remember(offsetY) {
        androidx.compose.runtime.mutableStateOf(offsetY.toString())
    }
    var zoom by androidx.compose.runtime.remember { androidx.compose.runtime.mutableFloatStateOf(1f) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Editor gráfico", style = MaterialTheme.typography.headlineSmall)
                Text(
                    "Arrastra la imagen para ajustar su punto de origen. La cruz marca el origen.",
                    style = MaterialTheme.typography.bodySmall
                )
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .heightIn(min = 260.dp, max = 390.dp)
                        .pointerInput(bitmap, zoom) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                val fit = min(
                                    size.width.toFloat() / bitmap.width,
                                    size.height.toFloat() / bitmap.height
                                ).coerceAtLeast(0.01f) * zoom
                                offsetX -= (dragAmount.x / fit).roundToInt()
                                offsetY -= (dragAmount.y / fit).roundToInt()
                            }
                        }
                ) {
                    val cell = 18.dp.toPx()
                    drawRect(Color(0xFFB8B8B8))
                    var y = 0f
                    while (y < size.height) {
                        var x = 0f
                        while (x < size.width) {
                            val column = (x / cell).toInt()
                            val row = (y / cell).toInt()
                            if ((column + row) % 2 == 0) {
                                drawRect(
                                    color = Color(0xFFE5E5E5),
                                    topLeft = Offset(x, y),
                                    size = androidx.compose.ui.geometry.Size(cell, cell)
                                )
                            }
                            x += cell
                        }
                        y += cell
                    }

                    val fit = min(
                        size.width / bitmap.width.toFloat(),
                        size.height / bitmap.height.toFloat()
                    ).coerceAtLeast(0.01f) * zoom
                    val imageWidth = (bitmap.width * fit).roundToInt().coerceAtLeast(1)
                    val imageHeight = (bitmap.height * fit).roundToInt().coerceAtLeast(1)
                    val left = size.width / 2f - offsetX * fit
                    val top = size.height / 2f - offsetY * fit
                    drawImage(
                        image = bitmap.asImageBitmap(),
                        dstOffset = IntOffset(left.roundToInt(), top.roundToInt()),
                        dstSize = IntSize(imageWidth, imageHeight)
                    )
                    val center = Offset(size.width / 2f, size.height / 2f)
                    drawLine(Color.Red, Offset(center.x - 14.dp.toPx(), center.y), Offset(center.x + 14.dp.toPx(), center.y), 1.5.dp.toPx())
                    drawLine(Color.Red, Offset(center.x, center.y - 14.dp.toPx()), Offset(center.x, center.y + 14.dp.toPx()), 1.5.dp.toPx())
                }
                Text("Zoom: ${(zoom * 100).roundToInt()}%")
                Slider(value = zoom, onValueChange = { zoom = it }, valueRange = 0.25f..4f)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = offsetXText,
                        onValueChange = { value ->
                            offsetXText = value
                            value.toIntOrNull()?.let { offsetX = it }
                        },
                        modifier = Modifier.weight(1f),
                        label = { Text("Offset X") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = offsetYText,
                        onValueChange = { value ->
                            offsetYText = value
                            value.toIntOrNull()?.let { offsetY = it }
                        },
                        modifier = Modifier.weight(1f),
                        label = { Text("Offset Y") },
                        singleLine = true
                    )
                }
                Divider()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(onClick = onDismiss) { Text("Cancelar") }
                    Button(
                        onClick = { onApply(offsetX, offsetY) },
                        modifier = Modifier.padding(start = 8.dp)
                    ) { Text("Aplicar") }
                }
            }
        }
    }
}
