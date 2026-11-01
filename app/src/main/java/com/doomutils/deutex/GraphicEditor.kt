package com.doomutils.deutex

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import kotlin.math.min
import kotlin.math.roundToInt

@Composable
fun GraphicEditorDialog(
    bitmap: Bitmap,
    initialOffsetX: Int,
    initialOffsetY: Int,
    onApply: (Int, Int) -> Unit,
    onDismiss: () -> Unit
) {
    val offsetX = remember(bitmap, initialOffsetX, initialOffsetY) {
        mutableIntStateOf(initialOffsetX)
    }
    val offsetY = remember(bitmap, initialOffsetX, initialOffsetY) {
        mutableIntStateOf(initialOffsetY)
    }
    val offsetXText = remember(offsetX.intValue) {
        mutableStateOf(offsetX.intValue.toString())
    }
    val offsetYText = remember(offsetY.intValue) {
        mutableStateOf(offsetY.intValue.toString())
    }
    val zoom = remember { mutableFloatStateOf(1f) }
    val image = remember(bitmap) { bitmap.asImageBitmap() }

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
                        .heightIn(min = 260.dp, max = 390.dp)
                        .pointerInput(bitmap, zoom.floatValue) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                val fit = min(
                                    size.width.toFloat() / bitmap.width,
                                    size.height.toFloat() / bitmap.height
                                ).coerceAtLeast(0.01f) * zoom.floatValue
                                offsetX.intValue -= (dragAmount.x / fit).roundToInt()
                                offsetY.intValue -= (dragAmount.y / fit).roundToInt()
                            }
                        }
                ) {
                    val cell = 18.dp.toPx()
                    drawRect(Color(0xFFB8B8B8))
                    var y = 0f
                    while (y < size.height) {
                        var x = 0f
                        while (x < size.width) {
                            if (((x / cell).toInt() + (y / cell).toInt()) % 2 == 0) {
                                drawRect(
                                    color = Color(0xFFE5E5E5),
                                    topLeft = Offset(x, y),
                                    size = Size(cell, cell)
                                )
                            }
                            x += cell
                        }
                        y += cell
                    }

                    val fit = min(
                        size.width / bitmap.width.toFloat(),
                        size.height / bitmap.height.toFloat()
                    ).coerceAtLeast(0.01f) * zoom.floatValue
                    val imageWidth = (bitmap.width * fit).roundToInt().coerceAtLeast(1)
                    val imageHeight = (bitmap.height * fit).roundToInt().coerceAtLeast(1)
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val left = center.x - offsetX.intValue * fit
                    val top = center.y - offsetY.intValue * fit
                    drawImage(
                        image = image,
                        dstOffset = IntOffset(left.roundToInt(), top.roundToInt()),
                        dstSize = IntSize(imageWidth, imageHeight)
                    )
                    val arm = 14.dp.toPx()
                    drawLine(Color.Red, Offset(center.x - arm, center.y), Offset(center.x + arm, center.y), 1.5.dp.toPx())
                    drawLine(Color.Red, Offset(center.x, center.y - arm), Offset(center.x, center.y + arm), 1.5.dp.toPx())
                }
                Text("Zoom: ${(zoom.floatValue * 100).roundToInt()}%")
                Slider(
                    value = zoom.floatValue,
                    onValueChange = { zoom.floatValue = it },
                    valueRange = 0.25f..4f
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = offsetXText.value,
                        onValueChange = { value ->
                            offsetXText.value = value
                            value.toIntOrNull()?.let { offsetX.intValue = it }
                        },
                        modifier = Modifier.weight(1f),
                        label = { Text("Offset X") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = offsetYText.value,
                        onValueChange = { value ->
                            offsetYText.value = value
                            value.toIntOrNull()?.let { offsetY.intValue = it }
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
                        onClick = { onApply(offsetX.intValue, offsetY.intValue) },
                        modifier = Modifier.padding(start = 8.dp)
                    ) { Text("Aplicar") }
                }
            }
        }
    }
}
