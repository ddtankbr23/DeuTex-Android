package com.doomutils.deutex

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.RectF
import android.view.MotionEvent
import android.view.View
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import kotlin.math.min
import kotlin.math.roundToInt

private class GraphicCanvasView(
    context: Context,
    private val bitmap: Bitmap,
    private val onOffsetsChanged: (Int, Int) -> Unit
) : View(context) {
    var offsetX: Int = 0
        set(value) { field = value; invalidate() }
    var offsetY: Int = 0
        set(value) { field = value; invalidate() }
    var zoom: Float = 1f
        set(value) { field = value; invalidate() }

    private val checkerPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val imagePaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val crossPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.RED
        strokeWidth = resources.displayMetrics.density * 1.5f
        style = Paint.Style.STROKE
    }
    private val density = resources.displayMetrics.density
    private var lastX = 0f
    private var lastY = 0f

    override fun onDraw(canvas: AndroidCanvas) {
        super.onDraw(canvas)
        val cell = 18f * density
        checkerPaint.color = AndroidColor.rgb(185, 185, 185)
        canvas.drawColor(checkerPaint.color)
        checkerPaint.color = AndroidColor.rgb(225, 225, 225)
        var y = 0f
        while (y < height) {
            var x = 0f
            while (x < width) {
                if (((x / cell).toInt() + (y / cell).toInt()) % 2 == 0) {
                    canvas.drawRect(x, y, x + cell, y + cell, checkerPaint)
                }
                x += cell
            }
            y += cell
        }

        val fit = min(width.toFloat() / bitmap.width, height.toFloat() / bitmap.height)
            .coerceAtLeast(0.01f) * zoom
        val centerX = width / 2f
        val centerY = height / 2f
        val left = centerX - offsetX * fit
        val top = centerY - offsetY * fit
        val dst = RectF(left, top, left + bitmap.width * fit, top + bitmap.height * fit)
        canvas.drawBitmap(bitmap, null, dst, imagePaint)

        val arm = 14f * density
        canvas.drawLine(centerX - arm, centerY, centerX + arm, centerY, crossPaint)
        canvas.drawLine(centerX, centerY - arm, centerX, centerY + arm, crossPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                lastX = event.x
                lastY = event.y
                parent?.requestDisallowInterceptTouchEvent(true)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val fit = min(width.toFloat() / bitmap.width, height.toFloat() / bitmap.height)
                    .coerceAtLeast(0.01f) * zoom
                val dx = event.x - lastX
                val dy = event.y - lastY
                offsetX -= (dx / fit).roundToInt()
                offsetY -= (dy / fit).roundToInt()
                lastX = event.x
                lastY = event.y
                onOffsetsChanged(offsetX, offsetY)
                invalidate()
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                parent?.requestDisallowInterceptTouchEvent(false)
                performClick()
                return true
            }
        }
        return true
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }
}

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
    val offsetXText = remember { mutableStateOf(initialOffsetX.toString()) }
    val offsetYText = remember { mutableStateOf(initialOffsetY.toString()) }
    val zoom = remember { mutableFloatStateOf(1f) }
    val canvasView = remember(bitmap) {
        mutableStateOf<GraphicCanvasView?>(null)
    }

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
                AndroidView(
                    factory = { context ->
                        GraphicCanvasView(context, bitmap) { x, y ->
                            offsetX.intValue = x
                            offsetY.intValue = y
                            offsetXText.value = x.toString()
                            offsetYText.value = y.toString()
                        }.also {
                            it.offsetX = offsetX.intValue
                            it.offsetY = offsetY.intValue
                            it.zoom = zoom.floatValue
                            canvasView.value = it
                        }
                    },
                    update = { view ->
                        view.offsetX = offsetX.intValue
                        view.offsetY = offsetY.intValue
                        view.zoom = zoom.floatValue
                    },
                    modifier = Modifier.fillMaxWidth().height(320.dp)
                )
                Text("Zoom: ${(zoom.floatValue * 100).roundToInt()}%")
                Slider(
                    value = zoom.floatValue,
                    onValueChange = {
                        zoom.floatValue = it
                        canvasView.value?.zoom = it
                    },
                    valueRange = 0.25f..4f
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = offsetXText.value,
                        onValueChange = { value ->
                            offsetXText.value = value
                            value.toIntOrNull()?.let {
                                offsetX.intValue = it
                                canvasView.value?.offsetX = it
                            }
                        },
                        modifier = Modifier.weight(1f),
                        label = { Text("Offset X") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = offsetYText.value,
                        onValueChange = { value ->
                            offsetYText.value = value
                            value.toIntOrNull()?.let {
                                offsetY.intValue = it
                                canvasView.value?.offsetY = it
                            }
                        },
                        modifier = Modifier.weight(1f),
                        label = { Text("Offset Y") },
                        singleLine = true
                    )
                }
                Divider()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.End
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
