package com.advmeds.drawapp

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import android.graphics.Color as AndroidColor
import android.graphics.Canvas as AndroidCanvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.advmeds.drawapp.ui.theme.DrawAppTheme
import android.graphics.Paint
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DrawAppTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val options = BitmapFactory.Options()
                    options.inMutable = true
                    val bitmap = BitmapFactory.decodeResource(resources, R.drawable.img, options)
                        .asImageBitmap()

                    var newBitmap: Bitmap? = null
//                    val image = remember { drawToBitmap(bitmap.asImageBitmap()) }

                    val lines = remember { mutableStateListOf<Line>() }

                    val colorList = listOf(
                        Color.Black,
                        Color.Blue,
                        Color.Green,
                        Color.Yellow,
                        Color.Red,
                    )

                    val sizeList = listOf(
                        1,
                        3,
                        5,
                        7,
                    )

                    val currentSize = remember {
                        mutableStateOf(sizeList[0])
                    }

                    val currentColor = remember {
                        mutableStateOf(colorList[0])
                    }

                    val scroll = rememberScrollState()

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                    ) {

                        item {
                            val density = LocalDensity.current

                            Button(onClick = {
                                newBitmap = createBitmapFromLines(bitmap, lines, density)

                                Log.d("check---", "onCreate: $newBitmap")

                            }) {
                                Text(text = "Send")
                            }
                        }

                        item {

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {

                                val shape = MaterialTheme.shapes.small

                                sizeList.forEach { size ->
                                    Box(
                                        modifier = Modifier
                                            .size(30.dp)
                                            .border(
                                                color = Color.Gray,
                                                width = 1.dp,
                                                shape = shape
                                            )
                                            .background(
                                                color = if (currentSize.value == size) Color.Gray else Color.White,
                                                shape = shape,
                                            )
                                            .clickable {
                                                currentSize.value = size
                                            }
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size((size * 3).dp)
                                                .background(
                                                    color = Color.Black,
                                                    shape = CircleShape
                                                )
                                                .align(Alignment.Center)
                                        )
                                    }
                                }

                                colorList.forEachIndexed { index, color ->

                                    Box(
                                        modifier = Modifier
                                            .size(30.dp)
                                            .border(
                                                color = Color.Gray,
                                                width = 1.dp,
                                                shape = shape
                                            )
                                            .background(
                                                color = if (currentColor.value == color) Color.Gray else Color.White
                                            )
                                            .clickable {
                                                currentColor.value = color
                                            }
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(20.dp)
                                                .background(color = color, shape = shape)
                                                .align(Alignment.Center)
                                        )
                                    }
                                }
                            }
                        }
                        item {
                            Divider()
                        }

                        item {

                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                            ) {
                                DrawingScreen(bitmap, currentColor, currentSize, lines)
                            }
                        }


                        item {
                            Divider()
                        }

                        item {
                            Image(
                                painter = painterResource(id = R.drawable.img),
                                contentDescription = null
                            )
                        }

                        item {
                            Spacer(modifier = Modifier.height(150.dp))
                        }
                        item {
                            Box(modifier = Modifier.size(bitmap.width.dp, bitmap.height.dp)) {
                                newBitmap?.let {
                                    Image(
                                        modifier = Modifier.fillMaxSize(),
                                        bitmap = it.asImageBitmap(),
                                        contentDescription = null
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }


    @Composable
    private fun DrawingScreen(
        image: ImageBitmap,
        currentColorIndex: MutableState<Color>,
        currentSize: MutableState<Int>,
        lines: SnapshotStateList<Line>,
    ) {
//        val lines = remember { mutableStateListOf<Line>() }

        BoxWithConstraints {
            val maxWidth = maxWidth
            val aspectRatio = image.width.toFloat() / image.height.toFloat()

            val width = if (image.width.dp > maxWidth) maxWidth else image.width.dp
            val height = width / aspectRatio

            Canvas(
                modifier = Modifier
                    .size(width, height)
                    .pointerInput(true) {
                        detectDragGesturesCustom { change, dragAmount ->
                            change.consume()
                            val line = Line(
                                start = change.position - dragAmount,
                                end = change.position,
                                color = currentColorIndex.value,
                                strokeWidth = currentSize.value.toDp()
                            )
                            lines.add(line)
                        }
                    }
            ) {
                drawImage(image)

                lines.forEach { line ->
                    drawLine(
                        color = line.color,
                        start = line.start,
                        end = line.end,
                        strokeWidth = line.strokeWidth.toPx(),
                        cap = StrokeCap.Round
                    )
                }
            }
        }

    }

    fun createBitmapFromLines(image: ImageBitmap, lines: List<Line>, density: Density): Bitmap {
        val bitmap = image.asAndroidBitmap().copy(Bitmap.Config.ARGB_8888, true)
        val canvas = AndroidCanvas(bitmap)
        val paint = Paint()

        lines.forEach { line ->
            paint.color = line.color.toArgb()
            paint.strokeWidth = with(density) { line.strokeWidth.toPx() }
            paint.strokeCap = Paint.Cap.ROUND
            canvas.drawLine(
                line.start.x,
                line.start.y,
                line.end.x,
                line.end.y,
                paint
            )
        }

        return bitmap
    }

    fun Color.toArgb(): Int {
        return AndroidColor.argb(
            (alpha * 255).toInt(),
            (red * 255).toInt(),
            (green * 255).toInt(),
            (blue * 255).toInt()
        )
    }
}


data class Line(
    val start: Offset,
    val end: Offset,
    val color: Color = Color.Black,
    val strokeWidth: Dp = 1.dp
)
