package com.advmeds.drawapp

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.NativeCanvas
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.advmeds.drawapp.ui.theme.DrawAppTheme
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Color as AndroidColor


typealias DrawBunch = List<DrawObject>

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
                    val bitmap = BitmapFactory
                        .decodeResource(resources, R.drawable.img, options)
                        .asImageBitmap()

                    var newBitmap: Bitmap? = null
//                    val image = remember { drawToBitmap(bitmap.asImageBitmap()) }

                    val drawBunch = remember { mutableStateOf<DrawBunch>(emptyList()) }
                    val reDoStack = remember { mutableStateOf<DrawBunch>(emptyList()) }
                    val drawObjectIdCounter = remember { mutableStateOf(0) }

                    val textSizeList = listOf(
                        13,
                        18,
                        25
                    )

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

                    val selectTool = remember {
                        mutableStateOf<Any?>(null)
                    }

                    val currentText = remember {
                        mutableStateOf<String?>(null)
                    }

                    val currentSize = remember {
                        mutableStateOf<Int?>(sizeList[0])
                    }

                    val currentTextSize = remember {
                        mutableStateOf<Int?>(null)
                    }

                    val currentColor = remember {
                        mutableStateOf(colorList[0])
                    }

                    var showDialog by remember {
                        mutableStateOf(false)
                    }

                    val shape = MaterialTheme.shapes.small

                    Box(modifier = Modifier.fillMaxSize()) {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                        ) {

                            item {
                                val density = LocalDensity.current

                                Button(onClick = {
                                    newBitmap =
                                        createBitmapFromLines(bitmap, drawBunch.value, density)

                                    Log.d("check---", "onCreate: $newBitmap")

                                }) {
                                    Text(text = "Send")
                                }
                            }

                            item {
                                Row(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {

                                    Box(
                                        modifier = Modifier
                                            .size(30.dp)
                                            .border(
                                                color = Color.Gray,
                                                width = 1.dp,
                                                shape = shape
                                            )
                                            .background(
                                                color = Color.White,
                                                shape = shape,
                                            )
                                            .clickable {
                                                val newUnDoStack = drawBunch.value.toMutableList()

                                                if (newUnDoStack.isEmpty()) {
                                                    if (reDoStack.value.isEmpty()) {
                                                        return@clickable
                                                    }

                                                    val lastReDoStackObject = reDoStack.value.last()

                                                    val isLastActionWasClear =
                                                        (lastReDoStackObject is DrawClear)

                                                    if (!isLastActionWasClear) {
                                                        return@clickable
                                                    }

                                                    val newUnDo = reDoStack.value

                                                    drawBunch.value = newUnDo
                                                    reDoStack.value = emptyList()


                                                    return@clickable
                                                }

                                                val lastElement = newUnDoStack.removeLast()

                                                val newRedoStack = reDoStack.value.toMutableList()

                                                newRedoStack.add(lastElement)

                                                drawBunch.value = newUnDoStack
                                                reDoStack.value = newRedoStack
                                            }
                                    ) {
                                        Icon(
                                            modifier = Modifier.align(Alignment.Center),
                                            painter = painterResource(id = R.drawable.undo_ic),
                                            contentDescription = null
                                        )
                                    }

                                    Box(
                                        modifier = Modifier
                                            .size(30.dp)
                                            .border(
                                                color = Color.Gray,
                                                width = 1.dp,
                                                shape = shape
                                            )
                                            .background(
                                                color = Color.White,
                                                shape = shape,
                                            )
                                            .clickable {
                                                val newRedoStack = reDoStack.value.toMutableList()

                                                if (newRedoStack.isEmpty()) {
                                                    if (drawBunch.value.isEmpty()) {
                                                        return@clickable
                                                    }

                                                    val lastReDoObject = drawBunch.value.last()

                                                    val isLastUnDoActionWasClear =
                                                        (lastReDoObject is DrawClear)

                                                    if (!isLastUnDoActionWasClear) {
                                                        return@clickable
                                                    }

                                                    val newReDo = drawBunch.value

                                                    drawBunch.value = emptyList()
                                                    reDoStack.value = newReDo

                                                    return@clickable
                                                }

                                                val lastElement = newRedoStack.removeLast()

                                                val newUnDoStack = drawBunch.value.toMutableList()

                                                newUnDoStack.add(lastElement)

                                                drawBunch.value = newUnDoStack
                                                reDoStack.value = newRedoStack
                                            }
                                    ) {
                                        Icon(
                                            modifier = Modifier.align(Alignment.Center),
                                            painter = painterResource(id = R.drawable.redo_ic),
                                            contentDescription = null
                                        )
                                    }

                                    Box(
                                        modifier = Modifier
                                            .size(30.dp)
                                            .border(
                                                color = Color.Gray,
                                                width = 1.dp,
                                                shape = shape
                                            )
                                            .background(
                                                color = Color.White,
                                                shape = shape,
                                            )
                                            .clickable {
                                                val newUnDoStack = drawBunch.value.toMutableList()

                                                if (newUnDoStack.isEmpty()) {
                                                    return@clickable
                                                }

                                                val newRedoStack = newUnDoStack.toMutableList()

                                                val clearState = DrawClear()

                                                newRedoStack.add(clearState)

                                                drawBunch.value = emptyList()
                                                reDoStack.value = newRedoStack
                                            }
                                    ) {
                                        Icon(
                                            modifier = Modifier.align(Alignment.Center),
                                            painter = painterResource(id = R.drawable.clear_ic),
                                            contentDescription = null
                                        )
                                    }

                                    Box(
                                        modifier = Modifier
                                            .size(30.dp)
                                            .border(
                                                color = Color.Gray,
                                                width = 1.dp,
                                                shape = shape
                                            )
                                            .background(
                                                color = if (selectTool.value != null) Color.Gray else Color.White,
                                                shape = shape,
                                            )
                                            .clickable {
                                                currentSize.value = null
                                                currentTextSize.value = null
                                                selectTool.value = Any()
                                            }
                                    ) {
                                        Icon(
                                            modifier = Modifier.align(Alignment.Center),
                                            painter = painterResource(id = R.drawable.mouse_ic),
                                            contentDescription = null
                                        )
                                    }



                                    textSizeList.forEach { size ->
                                        Box(
                                            modifier = Modifier
                                                .size(30.dp)
                                                .border(
                                                    color = Color.Gray,
                                                    width = 1.dp,
                                                    shape = shape
                                                )
                                                .background(
                                                    color = if (currentTextSize.value == size) Color.Gray else Color.White,
                                                    shape = shape,
                                                )
                                                .clickable {
                                                    currentSize.value = null
                                                    currentTextSize.value = size
                                                    selectTool.value = null
                                                }
                                        ) {
                                            Text(
                                                modifier = Modifier.align(Alignment.Center),
                                                text = "T",
                                                fontSize = size.sp
                                            )
                                        }
                                    }


                                }
                            }

                            item {

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {

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
                                                    currentTextSize.value = null
                                                    currentSize.value = size
                                                    selectTool.value = null
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
                                    DrawingScreen(
                                        image = bitmap,
                                        drawObjectIdCounter = drawObjectIdCounter,
                                        currentColor = currentColor,
                                        currentSize = currentSize,
                                        currentTextSize = currentTextSize,
                                        currentText = currentText,
                                        selectTool = selectTool,
                                        drawBunch = drawBunch,
                                        setTextDialogIsEnable = {
                                            showDialog = true
                                        },
                                        addDrawTextObjectInBunch = { text ->
                                            currentText.value = null

                                            val tempDrawObjectList = drawBunch.value.toMutableList()
                                            tempDrawObjectList.add(text)

                                            drawObjectIdCounter.value += 1

                                            drawBunch.value = tempDrawObjectList

                                            reDoStack.value = emptyList()

                                            Log.d(
                                                "check---",
                                                "onCreate: ${drawBunch.value.map { it.id }}"
                                            )
                                        },
                                        addDrawLineObjectInBunch = { line ->
                                            val tempDrawObjectList = drawBunch.value.toMutableList()

                                            tempDrawObjectList.add(line)

                                            drawObjectIdCounter.value += 1

                                            drawBunch.value = tempDrawObjectList
                                            reDoStack.value = emptyList()

                                            Log.d(
                                                "check---",
                                                "onCreate: ${drawBunch.value.map { it.id }}"
                                            )
                                        }
                                    )
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

                        if (showDialog) {
                            DialogContent(
                                onSubmit = { text ->
                                    currentText.value = text
                                    showDialog = false
                                },
                                onDismiss = {
                                    showDialog = false
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun DrawingScreen(
        image: ImageBitmap,
        drawObjectIdCounter: MutableState<Int>,
        currentColor: MutableState<Color>,
        currentSize: MutableState<Int?>,
        currentTextSize: MutableState<Int?>,
        currentText: MutableState<String?>,
        selectTool: MutableState<Any?>,
        drawBunch: MutableState<DrawBunch>,
        setTextDialogIsEnable: (Boolean) -> Unit,
        addDrawTextObjectInBunch: (text: DrawText) -> Unit,
        addDrawLineObjectInBunch: (line: DrawLine) -> Unit,
    ) {
//        val lines = remember { mutableStateListOf<Line>() }

        val initialDragPosition = remember { mutableStateOf(Offset.Zero) }
        val initialSize = remember { mutableStateOf(Offset.Zero) }
        val isResizing = remember { mutableStateOf(false) }
        val currentResizeCorner = remember { mutableStateOf<Corner?>(null) }
        val currentDragPosition = remember { mutableStateOf(Offset.Zero) }
//        val selectRectangle = remember { mutableStateOf<CustomRect?>(null) }

        var textPosition by remember { mutableStateOf(Offset.Zero) }

        val currentLine = remember {
            mutableStateListOf<Line>()
        }

        val currentDragText = remember {
            mutableStateListOf<DrawText>()
        }

        val density = LocalDensity.current

        LaunchedEffect(selectTool.value) {
            if (selectTool.value == null) {
                currentDragText.clear()
                drawBunch.value.forEach { it.isSelected = false }
            }
        }

        LaunchedEffect(currentText.value) {
            if (!currentText.value.isNullOrBlank()) {
                val drawText = DrawText(
                    id = drawObjectIdCounter.value,
                    text = currentText.value!!,
                    color = currentColor.value,
                    position = textPosition,
                    fontSize = currentTextSize.value!!
                )

                textPosition = Offset.Zero

                addDrawTextObjectInBunch.invoke(drawText)
            }
        }

        BoxWithConstraints {
            val maxWidth = maxWidth
            val aspectRatio = image.width.toFloat() / image.height.toFloat()

            val width = if (image.width.dp > maxWidth) maxWidth else image.width.dp
            val height = width / aspectRatio

            Canvas(
                modifier = Modifier
                    .size(width, height)
                    .pointerInput(true) {
                        detectDragGesturesCustom(
                            onTap = { offset ->
//                                drawBunch.forEach { it.isSelected = false }

                                if (currentTextSize.value != null) {
                                    textPosition = offset
                                    setTextDialogIsEnable.invoke(true)
                                }

                                if (selectTool.value != null) {
//                                    selectRectangle.value = null
                                    currentDragText.clear()

                                    Log.d("check---", "DrawingScreen: $drawBunch")

                                    drawBunch.value
//                                        .filterList { drawObjectType == DrawMode.Text }
                                        .asReversed()
                                        .forEachIndexed { index, text ->

                                            val isTouched = isPointInsideText(
                                                offset,
                                                (text as DrawText),
                                                density = density
                                            )


                                            val corners = isPointInsideResizeHandle(
                                                point = offset,
                                                (text as DrawText),
                                                density
                                            )

                                            Log.d("check---", "DrawingScreen: $corners")
                                            if (isTouched || corners != null) {

                                                val (textWidth, textHeight) = getTextWidthAndHeight(
                                                    (drawBunch.value[index] as DrawText),
                                                    density
                                                )

                                                val topRight = Offset(
                                                    text.position.x + textWidth,
                                                    text.position.y - textHeight
                                                )

                                                currentDragText.add(text)
//                                                selectRectangle.value =
//                                                    CustomRect(
//                                                        text.position, topRight
//                                                    )

                                                return@detectDragGesturesCustom
                                            }
                                        }
                                }

                                Log.d("check---", "DrawingScreen: $offset")
                            },
                            onDragStart = { offset ->
                                Log.d("check---", "DrawingScreen: Start drawing\n$offset")

                                currentDragText.clear()

                                if (selectTool.value != null) {
//                                    selectRectangle.value = null
                                    drawBunch.value
//                                        .filterList { drawObjectType == DrawMode.Text }
                                        .asReversed()
                                        .forEachIndexed { index, text ->
                                            val corner = isPointInsideResizeHandle(
                                                offset,
                                                (text as DrawText),
                                                density = density

                                            )
                                            val isTouched = isPointInsideText(
                                                offset,
                                                (text as DrawText),
                                                density = density
                                            )
                                            Log.d("check---", "DrawingScreen: $corner")
                                            if (corner != null) {
                                                currentDragText.add(text)
                                                initialDragPosition.value = offset
                                                initialSize.value = getTextSize(text, density)
                                                isResizing.value = true
                                                currentResizeCorner.value = corner

                                                val (textWidth, textHeight) = getTextWidthAndHeight(
                                                    (drawBunch.value[index] as DrawText),
                                                    density
                                                )

                                                val topRight = Offset(
                                                    text.position.x + textWidth,
                                                    text.position.y - textHeight
                                                )

//                                                selectRectangle.value = CustomRect(
//                                                    text.position, topRight
//                                                )
                                            } else if (isTouched) {
                                                currentDragText.add(text)

                                                return@detectDragGesturesCustom
                                            }
                                        }
                                }

//                                Log.d("check---", "DrawingScreen: $drawBunch")


//                                if (selectTool.value != null) {
//                                    drawBunch.value
//                                        .filterList { drawObjectType == DrawMode.Text }
//                                        .asReversed()
//                                        .forEach { text ->
//                                            if (isPointInsideResizeHandle(
//                                                    offset,
//                                                    (text as DrawText),
//                                                    density = density
//                                                )
//                                            ) {
//                                                text.isSelected = true
//                                                return@forEach
//                                            } else {
//                                                text.isSelected = false
//                                            }
//                                        }
//                                }

                            },
                            onDragEnd = {
                                Log.d("check---", "DrawingScreen: Start end")

                                if (selectTool.value != null) {
                                    val tempDrawBunch = drawBunch.value.toMutableList()

                                    Log.d(
                                        "check---",
                                        "DrawingScreen: ${currentDragText.toList()}"
                                    )

                                    currentDragText.forEach { dragged ->

                                        val item = drawBunch.value.find { dragged.id == it.id }
                                        val index = drawBunch.value.indexOf(item)

                                        Log.d(
                                            "check---",
                                            "DrawingScreen: $index"
                                        )

                                        if (index > -1) {

//                                            Log.d(
//                                                "check---",
//                                                "DrawingScreen: ${selectRectangle.value}"
//                                            )

                                            if (true) {
//                                            selectRectangle.value?.let { scale ->
//                                                val (textWidth, textHeight) = getTextWidthAndHeight(
//                                                    (drawBunch.value[index] as DrawText),
//                                                    density
//                                                )
//
//                                                Log.d(
//                                                    "check---",
//                                                    "DrawingScreen: $textWidth $textHeight"
//                                                )
//
//                                                val newPosition: Offset = getNewPosition(
//                                                    (drawBunch.value[index] as DrawText),
//                                                    density
//                                                )
//
//                                                Log.d(
//                                                    "check---",
//                                                    "DrawingScreen: ${currentResizeCorner.value}"
//                                                )
//
//                                                val scaleX = when (currentResizeCorner.value) {
//                                                    Corner.TopLeft -> (((scale.topLeft.x - textWidth) - dragged.position.x) * -1) / ((initialDragPosition.value.x + textWidth) - dragged.position.x)
//                                                    Corner.BottomLeft -> (((scale.bottomLeft.x - textWidth) - dragged.position.x) * -1) / ((initialDragPosition.value.x + textWidth) - dragged.position.x)
//                                                    Corner.TopRight -> (scale.topRight.x - dragged.position.x) / (initialDragPosition.value.x - dragged.position.x)
//                                                    Corner.BottomRight -> (scale.bottomRight.x - dragged.position.x) / (initialDragPosition.value.x - dragged.position.x)
//                                                    null -> 1f
//                                                }
//
//                                                Log.d(
//                                                    "check---",
//                                                    "DrawingScreen: \n" +
//                                                            "scale.topLeft.x             - ${scale.topLeft.x}\n" +
//                                                            "textWidth                   - ${textWidth}\n" +
//                                                            "dragged.position.x          - ${dragged.position.x}\n" +
//                                                            "initialDragPosition.value.x - ${initialDragPosition.value.x}\n" +
//                                                            "scaleX                      - $scaleX\n" +
//                                                            ""
//                                                )
//
//                                                val scaleY = when (currentResizeCorner.value) {
//                                                    Corner.TopLeft -> (scale.topLeft.y - dragged.position.y) / (initialDragPosition.value.y - dragged.position.y)
//                                                    Corner.TopRight -> (scale.topRight.y - dragged.position.y) / (initialDragPosition.value.y - dragged.position.y)
//                                                    Corner.BottomRight -> (((scale.bottomRight.y + textHeight) - dragged.position.y)) / ((initialDragPosition.value.y + textHeight) - dragged.position.y)
//                                                    Corner.BottomLeft -> (((scale.bottomLeft.y + textHeight) - dragged.position.y)) / ((initialDragPosition.value.y + textHeight) - dragged.position.y)
//
//                                                    null -> 1f
//                                                }
//
//
//                                                // Set pivotPoint as opposite corner
//                                                val pivotPoint = when (currentResizeCorner.value) {
//                                                    Corner.TopLeft -> Offset(
//                                                        dragged.position.x + textWidth,
//                                                        dragged.position.y
//                                                    )
//
//                                                    Corner.TopRight -> Offset(
//                                                        dragged.position.x,
//                                                        dragged.position.y
//                                                    )
//
//                                                    Corner.BottomRight -> Offset(
//                                                        dragged.position.x,
//                                                        dragged.position.y - textHeight
//                                                    )
//
//                                                    Corner.BottomLeft -> Offset(
//                                                        dragged.position.x + textWidth,
//                                                        dragged.position.y - textHeight
//                                                    )
//
//                                                    null -> Offset(
//                                                        dragged.position.x,
//                                                        dragged.position.y
//                                                    )
//                                                }
//                                                dragged.transformMatrix = scale.matrix
////                                                    .setScale(
////                                                        scaleX, scaleY,
////                                                        textWidth / 2f + dragged.position.x,
////                                                        textHeight / 2f + +dragged.position.y
////                                                    )
//
//                                            }
                                            }

                                            tempDrawBunch[index] = dragged

                                        }

                                        drawBunch.value = tempDrawBunch
                                        isResizing.value = false
                                        currentResizeCorner.value = null
                                    }
                                }

                                if (currentSize.value != null) {
                                    val drawLine = DrawLine(
                                        id = drawObjectIdCounter.value,
                                        list = currentLine.toList(),
                                    )
                                    addDrawLineObjectInBunch.invoke(drawLine)
                                    currentLine.clear()
                                }

                            },
                            onDragCancel = {
                                Log.d("check---", "DrawingScreen: Start cancel")

                                if (selectTool.value != null) {
                                    val tempDrawBunch = drawBunch.value.toMutableList()

                                    currentDragText.forEach { dragged ->

                                        val item = drawBunch.value.find { dragged.id == it.id }
                                        val index = drawBunch.value.indexOf(item)

                                        if (index > -1) {
                                            tempDrawBunch[index] = dragged
                                        }
                                    }

                                    drawBunch.value = tempDrawBunch
                                    isResizing.value = false
                                    currentResizeCorner.value = null
                                }

                                if (currentSize.value != null) {
                                    val drawLine = DrawLine(
                                        id = drawObjectIdCounter.value,
                                        list = currentLine.toList(),
                                    )
                                    addDrawLineObjectInBunch.invoke(drawLine)
                                    currentLine.clear()
                                }
                            },
                            onDrag = { change, dragAmount ->

                                currentDragPosition.value = change.position
                                change.consume()
                                if (selectTool.value != null) {

                                    val newList = mutableListOf<DrawText>()

                                    if (true) {
//                                    if (isResizing.value && currentResizeCorner.value != null) {
//                                        val rectangle = selectRectangle.value!!
//
//                                        selectRectangle.value =
//                                            when (currentResizeCorner.value) {
//
//                                                Corner.TopLeft ->
//                                                    rectangle.apply {
//                                                        moveTopLeft(
//                                                            Offset(
//                                                                topLeft.x + dragAmount.x,
//                                                                topLeft.y + dragAmount.y
//                                                            )
//                                                        )
//                                                        updateMatrix()
//                                                    }
//
//                                                Corner.TopRight -> rectangle.apply {
//                                                    moveTopRight(
//                                                        Offset(
//                                                            topRight.x + dragAmount.x,
//                                                            topRight.y + dragAmount.y
//                                                        )
//                                                    )
//                                                    updateMatrix()
//                                                }
//
//                                                Corner.BottomRight -> rectangle.apply {
//                                                    moveBottomRight(
//                                                        Offset(
//                                                            bottomRight.x + dragAmount.x,
//                                                            bottomRight.y + dragAmount.y
//                                                        )
//                                                    )
//                                                    updateMatrix()
//                                                }
//
//                                                Corner.BottomLeft -> rectangle.apply {
//                                                    moveBottomLeft(
//                                                        Offset(
//                                                            bottomLeft.x + dragAmount.x,
//                                                            bottomLeft.y + dragAmount.y
//                                                        )
//                                                    )
//                                                    updateMatrix()
//                                                }
//
//
////                                                Corner.TopLeft -> {
////                                                    rectangle.copy(
////                                                        topLeft = Offset(
////                                                            rectangle.topLeft.x + dragAmount.x,
////                                                            rectangle.topLeft.y + dragAmount.y
////                                                        ),
////                                                        topRight = Offset(
////                                                            rectangle.topRight.x,
////                                                            rectangle.topLeft.y + dragAmount.y
////                                                        ),
////                                                        bottomLeft = Offset(
////                                                            rectangle.topLeft.x + dragAmount.x,
////                                                            rectangle.bottomLeft.y
////                                                        )
////                                                    )
////                                                }
////
////                                                Corner.TopRight -> {
////                                                    rectangle.copy(
////                                                        topRight = Offset(
////                                                            rectangle.topRight.x + dragAmount.x,
////                                                            rectangle.topRight.y + dragAmount.y
////                                                        ),
////                                                        topLeft = Offset(
////                                                            rectangle.topLeft.x,
////                                                            rectangle.topRight.y + dragAmount.y
////                                                        ),
////                                                        bottomRight = Offset(
////                                                            rectangle.topRight.x + dragAmount.x,
////                                                            rectangle.bottomRight.y
////                                                        )
////                                                    )
////                                                }
////
////                                                Corner.BottomRight -> {
////                                                    rectangle.copy(
////                                                        bottomRight = Offset(
////                                                            rectangle.bottomRight.x + dragAmount.x,
////                                                            rectangle.bottomRight.y + dragAmount.y
////                                                        ),
////                                                        topRight = Offset(
////                                                            rectangle.bottomRight.x + dragAmount.x,
////                                                            rectangle.topRight.y
////                                                        ),
////                                                        bottomLeft = Offset(
////                                                            rectangle.bottomLeft.x,
////                                                            rectangle.bottomRight.y + dragAmount.y
////                                                        )
////                                                    )
////                                                }
////
////                                                Corner.BottomLeft -> {
////                                                    rectangle.copy(
////                                                        bottomLeft = Offset(
////                                                            rectangle.bottomLeft.x + dragAmount.x,
////                                                            rectangle.bottomLeft.y + dragAmount.y
////                                                        ),
////                                                        topLeft = Offset(
////                                                            rectangle.bottomLeft.x + dragAmount.x,
////                                                            rectangle.topLeft.y
////                                                        ),
////                                                        bottomRight = Offset(
////                                                            rectangle.bottomRight.x,
////                                                            rectangle.bottomLeft.y + dragAmount.y
////                                                        )
////                                                    )
////                                                }
//
//                                                null -> rectangle
//                                            }
//                                    }
                                    }


                                    currentDragText.forEach {

                                        if (isResizing.value) {

                                            val item =
                                                drawBunch.value.find { static -> it.id == static.id }
                                            val index = drawBunch.value.indexOf(item)

                                            val (textWidth, textHeight) = getTextWidthAndHeight(
                                                (drawBunch.value[index] as DrawText),
                                                density
                                            )

                                            Log.d(
                                                "check---",
                                                "DrawingScreen: $textWidth $textHeight"
                                            )

                                            val newPosition: Offset = getNewPosition(
                                                (drawBunch.value[index] as DrawText),
                                                density
                                            )

                                            val scaleX = when (currentResizeCorner.value) {
                                                Corner.TopLeft, Corner.BottomLeft -> (((change.position.x - textWidth) - newPosition.x) * -1) / ((initialDragPosition.value.x + textWidth) - newPosition.x)
                                                Corner.TopRight, Corner.BottomRight -> (change.position.x - it.position.x) / (initialDragPosition.value.x - it.position.x)
                                                null -> 1f
                                            }
                                            val scaleY = when (currentResizeCorner.value) {
                                                Corner.TopLeft -> (change.position.y - newPosition.y) / (initialDragPosition.value.y - newPosition.y)
                                                Corner.TopRight -> (change.position.y - it.position.y) / (initialDragPosition.value.y - it.position.y)
                                                Corner.BottomRight,
                                                Corner.BottomLeft -> (((change.position.y + textHeight) - it.position.y)) / ((initialDragPosition.value.y + textHeight) - it.position.y)

                                                null -> 1f
                                            }


                                            // Set pivotPoint as opposite corner
                                            val pivotPoint = when (currentResizeCorner.value) {
                                                Corner.TopLeft -> Offset(
                                                    it.position.x + textWidth,
                                                    it.position.y
                                                )

                                                Corner.TopRight -> Offset(
                                                    it.position.x,
                                                    it.position.y
                                                )

                                                Corner.BottomRight -> Offset(
                                                    it.position.x,
                                                    it.position.y - textHeight
                                                )

                                                Corner.BottomLeft -> Offset(
                                                    it.position.x + textWidth,
                                                    it.position.y - textHeight
                                                )

                                                null -> Offset(
                                                    it.position.x,
                                                    it.position.y
                                                )
                                            }

                                            val dragText = it

                                            dragText.transformMatrix.setScale(
                                                scaleX, scaleY,
                                                pivotPoint.x,
                                                pivotPoint.y
                                            )

                                            newList.add(dragText)
                                        } else {
                                            val dragText = it.copy(
                                                position = it.position + dragAmount
                                            )

                                            newList.add(dragText)
                                        }
                                    }

                                    currentDragText.clear()
                                    currentDragText.addAll(newList)

//                                    if (currentDragText.value != null) {
//                                        val position = currentDragText.value!!.position + dragAmount
//                                        currentDragText.value = currentDragText.value!!.copy(
//                                            position = position
//                                        )
//                                    }

//                                    currentDragText.value?.let { currentDrag ->
//                                        val position = currentDrag.position + dragAmount
//
//                                        currentDragText.value = currentDrag.copy(
//                                            position = position
//                                        )
//                                    }

//                                    drawBunch.value
//                                        .filterList { isSelected }
//                                        .forEach { text ->
//                                            (text as DrawText).position += dragAmount
//                                        }

//                                    val item = drawBunch.value.getOrNull(touchIndex)
//
//                                    item?.let { drawObject ->
//
//                                        val tempDrawBunch = drawBunch.value.toMutableList()
//                                        val drawText = (drawObject as DrawText)
//
//                                        currentDragText = drawText
//
////                                        tempDrawBunch[touchIndex] = drawText.copy(
////                                            position = drawText.position + dragAmount
////                                        )
//                                    }
                                }

                                if (currentSize.value != null) {
                                    val line = Line(
                                        start = change.position - dragAmount,
                                        end = change.position,
                                        color = currentColor.value,
                                        strokeWidth = currentSize.value!!.toDp()
                                    )

                                    currentLine.add(line)
                                }


                            }
                        )
                    }
            ) {
                DrawCanvas(image, drawBunch, currentDragText, currentLine, isResizing)

//                selectRectangle.value?.let {
//
//                    DrawRectangleByLines(it)
//                }


//                    drawIntoCanvas {
//                        val paint = Paint().apply {
//                            color = textObject.color.toArgb()
//                            textSize = textObject.fontSize.sp.toPx()
//                        }
//
//                        val textBounds = Rect()
//                        paint.getTextBounds(
//                            textObject.text,
//                            0,
//                            textObject.text.length,
//                            textBounds
//                        )
//                        val textWidth = paint.measureText(textObject.text)
//                        val textHeight = textBounds.height()
//
//                        it.nativeCanvas.drawText(
//                            textObject.text,
//                            textObject.position.x,
//                            textObject.position.y,
//                            paint
//                        )
//
//                        val position =
//                            Offset(textObject.position.x, (textObject.position.y - textHeight))
//
//                        drawResizeHandles(
//                            canvas = it.nativeCanvas,
//                            position = position,
//                            size = Offset(textWidth, textHeight.toFloat()),
//                            resizeHandleSize = 20.dp,
//                        )
//                    }

            }
        }
    }

    private fun DrawScope.DrawRectangleByLines(rectangle: CustomRect) {
        drawLine(Color.Black, rectangle.topLeft, rectangle.topRight)
        drawLine(Color.Black, rectangle.topRight, rectangle.bottomRight)
        drawLine(Color.Black, rectangle.bottomRight, rectangle.bottomLeft)
        drawLine(Color.Black, rectangle.bottomLeft, rectangle.topLeft)


        val cornerSize = 20f
        drawRect(
            color = Color.Red,
            topLeft = Offset(
                rectangle.topLeft.x - cornerSize / 2,
                rectangle.topLeft.y - cornerSize / 2
            ),
            size = androidx.compose.ui.geometry.Size(cornerSize, cornerSize)
        )
        drawRect(
            color = Color.Red,
            topLeft = Offset(
                rectangle.topRight.x - cornerSize / 2,
                rectangle.topRight.y - cornerSize / 2
            ),
            size = androidx.compose.ui.geometry.Size(cornerSize, cornerSize)
        )
        drawRect(
            color = Color.Red,
            topLeft = Offset(
                rectangle.bottomLeft.x - cornerSize / 2,
                rectangle.bottomLeft.y - cornerSize / 2
            ),
            size = androidx.compose.ui.geometry.Size(cornerSize, cornerSize)
        )
        drawRect(
            color = Color.Red,
            topLeft = Offset(
                rectangle.bottomRight.x - cornerSize / 2,
                rectangle.bottomRight.y - cornerSize / 2
            ),
            size = androidx.compose.ui.geometry.Size(cornerSize, cornerSize)
        )
    }


    private fun DrawScope.DrawCanvas(
        image: ImageBitmap,
        drawBunch: MutableState<DrawBunch>,
        currentDragText: SnapshotStateList<DrawText>,
        currentLine: SnapshotStateList<Line>,
        isResizing: MutableState<Boolean>
    ) {
        drawImage(image)

        drawBunch.value.forEachIndexed { index, bunch ->

            if (currentDragText.map { it.id }.contains(bunch.id)) {
                return@forEachIndexed
            }


            when (bunch.drawObjectType) {
                DrawMode.Text -> {
                    val textObject = (bunch as DrawText)

                    //                            drawContext.canvas.nativeCanvas.drawText(
                    //                                textObject.text,
                    //                                textObject.position.x,
                    //                                textObject.position.y,
                    //                                Paint().apply {
                    //                                    color = textObject.color.toArgb()
                    //                                    textSize = textObject.fontSize.sp.toPx()
                    //                                }
                    //                            )


                    drawIntoCanvas { canvas ->
                        val paint = Paint().apply {
                            color = textObject.color.toArgb()
                            textSize = textObject.fontSize.sp.toPx()
                        }
                        val textBounds = Rect()

                        paint.getTextBounds(
                            textObject.text,
                            0,
                            textObject.text.length,
                            textBounds
                        )

                        canvas.save()
                        canvas.nativeCanvas.concat(textObject.transformMatrix)

                        canvas.nativeCanvas.drawText(
                            textObject.text,
                            textObject.position.x,
                            textObject.position.y,
                            paint
                        )

                        canvas.nativeCanvas.restore()
                    }

                    //                            drawIntoCanvas {
                    //                                val paint = Paint().apply {
                    //                                    color = textObject.color.toArgb()
                    //                                    textSize = textObject.fontSize.sp.toPx()
                    //                                }
                    //
                    //                                val textBounds = Rect()
                    //                                paint.getTextBounds(
                    //                                    textObject.text,
                    //                                    0,
                    //                                    textObject.text.length,
                    //                                    textBounds
                    //                                )
                    //                                val textWidth = paint.measureText(textObject.text)
                    //                                val textHeight = textBounds.height()
                    //
                    //                                it.nativeCanvas.drawText(
                    //                                    textObject.text,
                    //                                    textObject.position.x,
                    //                                    textObject.position.y,
                    //                                    paint
                    //                                )
                    //
                    //                                if (textObject.isSelected) {
                    //                                    drawResizeHandles(
                    //                                        canvas = it.nativeCanvas,
                    //                                        position = textObject.position,
                    //                                        size = Offset(textWidth, textHeight.toFloat()),
                    //                                        resizeHandleSize = 20.dp,
                    //                                    )
                    //                                }
                    //                            }
                }

                DrawMode.Line -> {
                    (bunch as DrawLine).list.forEach { line ->
                        drawLine(
                            color = line.color,
                            start = line.start,
                            end = line.end,
                            strokeWidth = line.strokeWidth.toPx(),
                            cap = StrokeCap.Round
                        )
                    }
                }

                DrawMode.Select, DrawMode.Clear -> {}
            }
        }

        currentLine.forEach { line ->
            drawLine(
                color = line.color,
                start = line.start,
                end = line.end,
                strokeWidth = line.strokeWidth.toPx(),
                cap = StrokeCap.Round
            )
        }

        currentDragText.forEach { dragText ->
            //                    drawContext.canvas.nativeCanvas.drawText(
            //                        dragText.text,
            //                        dragText.position.x,
            //                        dragText.position.y,
            //                        Paint().apply {
            //                            color = dragText.color.toArgb()
            //                            textSize = dragText.fontSize.sp.toPx()
            //                        }
            //                    )

            drawIntoCanvas { canvas ->
                val paint = Paint().apply {
                    color = dragText.color.toArgb()
                    textSize = dragText.fontSize.sp.toPx()
                }
                val textBounds = Rect()
                paint.getTextBounds(
                    dragText.text,
                    0,
                    dragText.text.length,
                    textBounds
                )
                val textWidth = paint.measureText(dragText.text)
                val textHeight = textBounds.height()

                canvas.save()
                canvas.nativeCanvas.concat(dragText.transformMatrix)

                canvas.nativeCanvas.drawText(
                    dragText.text,
                    dragText.position.x,
                    dragText.position.y,
                    paint
                )

                val boundsPosition = Offset(
                    x = dragText.position.x,
                    y = dragText.position.y - textHeight
                )
//                                        if (dragText.isSelected) {

                val topLeft = boundsPosition
                val topRight = Offset(boundsPosition.x + textWidth, boundsPosition.y)
                val bottomRight = Offset(
                    boundsPosition.x + textWidth,
                    boundsPosition.y + textHeight.toFloat()
                )
                val bottomLeft =
                    Offset(boundsPosition.x, boundsPosition.y + textHeight.toFloat())

                drawRectangleByOffsets(
                    canvas.nativeCanvas,
                    topLeft,
                    topRight,
                    bottomRight,
                    bottomLeft,
                    paint
                )

//                                        drawBoundingBox(
//                                            canvas = canvas.nativeCanvas,
//                                            position = boundsPosition,
//                                            size = Offset(textWidth, textHeight.toFloat()),
//                                        )
//
//                                        }

                canvas.nativeCanvas.restore()

                if (!isResizing.value) {
    //                            drawResizeHandles(
    //                                canvas = canvas.nativeCanvas,
    //                                position = boundsPosition,
    //                                size = Offset(textWidth, textHeight.toFloat()),
    //                                resizeHandleSize = 20.dp,
    //                            )

                    drawResizeHandles(
                        canvas = canvas.nativeCanvas,
                        position = boundsPosition,
                        size = Offset(textWidth, textHeight.toFloat()),
                        resizeHandleSize = 20.dp,
                    )
                }
            }
        }
    }

    private fun drawRectangleByOffsets(
        canvas: android.graphics.Canvas,
        topLeft: Offset,
        topRight: Offset,
        bottomRight: Offset,
        bottomLeft: Offset,
        paint: Paint
    ) {
        canvas.drawLine(topLeft.x, topLeft.y, topRight.x, topRight.y, paint)
        canvas.drawLine(topRight.x, topRight.y, bottomRight.x, bottomRight.y, paint)
        canvas.drawLine(bottomRight.x, bottomRight.y, bottomLeft.x, bottomLeft.y, paint)
        canvas.drawLine(bottomLeft.x, bottomLeft.y, topLeft.x, topLeft.y, paint)
    }

    fun createBitmapFromLines(image: ImageBitmap, drawBunch: DrawBunch, density: Density): Bitmap {
        val bitmap = image.asAndroidBitmap().copy(Bitmap.Config.ARGB_8888, true)
        val canvas = AndroidCanvas(bitmap)
        val paint = Paint()

        drawBunch.forEach { bunch ->

            when (bunch.drawObjectType) {
                DrawMode.Text -> {
                    val textObject = (bunch as DrawText)

                    paint.color = textObject.color.toArgb()
                    paint.textSize = with(density) { textObject.fontSize.sp.toPx() }
                    canvas.save()
                    canvas.concat(textObject.transformMatrix)
                    canvas.drawText(
                        textObject.text,
                        textObject.position.x,
                        textObject.position.y,
                        paint
                    )
                    canvas.restore()
                }

                DrawMode.Line -> {
                    (bunch as DrawLine).list.forEach { line ->
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
                }

                DrawMode.Select, DrawMode.Clear -> {}
            }
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

    fun drawResizeHandles(
        canvas: android.graphics.Canvas,
        position: Offset,
        size: Offset,
        resizeHandleSize: Dp
    ) {
        val halfResizeHandleSize = resizeHandleSize.value / 2
        val topLeft = Offset(position.x - halfResizeHandleSize, position.y - halfResizeHandleSize)
        val topRight =
            Offset(position.x + size.x - halfResizeHandleSize, position.y - halfResizeHandleSize)
        val bottomLeft =
            Offset(position.x - halfResizeHandleSize, position.y + size.y - halfResizeHandleSize)
        val bottomRight = Offset(
            position.x + size.x - halfResizeHandleSize,
            position.y + size.y - halfResizeHandleSize
        )

        val paint = Paint().apply {
            color = Color.Black.toArgb()
            style = Paint.Style.FILL
        }

        // Draw resize handles
        canvas.drawRect(
            topLeft.x,
            topLeft.y,
            topLeft.x + resizeHandleSize.value,
            topLeft.y + resizeHandleSize.value,
            paint
        )
        canvas.drawRect(
            topRight.x,
            topRight.y,
            topRight.x + resizeHandleSize.value,
            topRight.y + resizeHandleSize.value,
            paint
        )
        canvas.drawRect(
            bottomLeft.x,
            bottomLeft.y,
            bottomLeft.x + resizeHandleSize.value,
            bottomLeft.y + resizeHandleSize.value,
            paint
        )
        canvas.drawRect(
            bottomRight.x,
            bottomRight.y,
            bottomRight.x + resizeHandleSize.value,
            bottomRight.y + resizeHandleSize.value,
            paint
        )
    }
}


enum class DrawMode {
    Text,
    Line,
    Select,
    Clear,
}

interface DrawObject {
    val id: Int
    val drawObjectType: DrawMode
    var isSelected: Boolean
}

data class DrawLine(
    override val id: Int,
    override val drawObjectType: DrawMode = DrawMode.Line,
    override var isSelected: Boolean = false,
    val list: List<Line>,
) : DrawObject

data class DrawText(
    override val id: Int,
    override val drawObjectType: DrawMode = DrawMode.Text,
    override var isSelected: Boolean = true,
    val text: String,
    val color: Color,
    var position: Offset,
    val fontSize: Int,
    var transformMatrix: Matrix = Matrix()
) : DrawObject

data class DrawClear(
    override val id: Int = -1,
    override val drawObjectType: DrawMode = DrawMode.Clear,
    override var isSelected: Boolean = false,
) : DrawObject

data class Line(
    var start: Offset,
    var end: Offset,
    val color: Color = Color.Black,
    val strokeWidth: Dp = 1.dp
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DialogContent(onDismiss: () -> Unit, onSubmit: (String) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable { onDismiss.invoke() },
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = RoundedCornerShape(8.dp),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                var text by remember { mutableStateOf("") }

                TextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Enter text") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row {

                    Button(onClick = onDismiss) {
                        Text("cancel")
                    }
                    Button(
                        enabled = text.isNotBlank(),
                        onClick = {
                            onSubmit.invoke(text)
                        }
                    ) {
                        Text("Submit")
                    }
                }
            }
        }
    }
}

fun getTextSize(textItem: DrawText, density: Density): Offset {
    val paint = Paint().apply {
        textSize = with(density) { textItem.fontSize.dp.toPx() }
    }
    val textBounds = Rect()
    paint.getTextBounds(textItem.text, 0, textItem.text.length, textBounds)
    val textWidth = paint.measureText(textItem.text)
    val textHeight = textBounds.height().toFloat()

    return Offset(textWidth, textHeight)
}

fun linesToPath(lines: List<Line>): Path {
    val path = Path()
    lines.forEach { line ->
        path.moveTo(line.start.x, line.start.y)
        path.lineTo(line.end.x, line.end.y)
    }
    return path
}

