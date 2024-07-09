package com.advmeds.drawapp

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Paint
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
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
                    var wasCleared by remember { mutableStateOf(false) }

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
                                                color = if (selectTool.value != null) Color.Gray else Color.White,
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
                                                    Log.d(
                                                        "check---",
                                                        "onCreate: isLastActionWasClear $isLastActionWasClear"
                                                    )

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
                                                color = if (selectTool.value != null) Color.Gray else Color.White,
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
                                                color = if (selectTool.value != null) Color.Gray else Color.White,
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

                                                Log.d(
                                                    "check---",
                                                    "onCreate: drawBunch.value - ${drawBunch.value} \nreDoStack.value - ${reDoStack.value} "
                                                )
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
                                            drawBunch.value = tempDrawObjectList

                                            reDoStack.value = emptyList()

                                            Log.d("check---", "onCreate: $drawBunch")
                                        },
                                        addDrawLineObjectInBunch = { line ->
                                            val tempDrawObjectList = drawBunch.value.toMutableList()
                                            tempDrawObjectList.add(line)

                                            drawBunch.value = tempDrawObjectList
                                            reDoStack.value = emptyList()
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

        var textPosition by remember { mutableStateOf(Offset.Zero) }

        val currentLine = remember {
            mutableStateListOf<Line>()
        }

        val currentDragText = remember {
            mutableStateOf<DrawText?>(null)
        }

        var touchIndex by remember {
            mutableStateOf(-1)
        }

        val density = LocalDensity.current

        LaunchedEffect(selectTool.value) {
            if (selectTool.value == null) {
                drawBunch.value.forEach { it.isSelected = false }
            }
        }

        LaunchedEffect(currentText.value) {
            if (!currentText.value.isNullOrBlank()) {
                val drawText = DrawText(
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
                                    touchIndex = -1

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

                                            Log.d("check---", "DrawingScreen: $isTouched")
                                            if (isTouched) {
                                                touchIndex = index
                                                return@detectDragGesturesCustom
                                            }

//                                            if (result
//                                            ) {
//                                                text.isSelected = true
//                                                return@forEach
//                                            }
                                        }
                                }

                                Log.d("check---", "DrawingScreen: $offset")
                            },
                            onDragStart = { offset ->
                                Log.d("check---", "DrawingScreen: Start drawing")

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

                                    currentDragText.value?.let {
                                        tempDrawBunch[touchIndex] = it
                                    }

                                    currentDragText.value = null
                                }

                                if (currentSize.value != null) {
                                    val drawLine = DrawLine(
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

                                    currentDragText.value?.let {
                                        tempDrawBunch[touchIndex] = it
                                    }

                                    currentDragText.value = null
                                }

                                if (currentSize.value != null) {
                                    val drawLine = DrawLine(
                                        list = currentLine.toList(),
                                    )
                                    addDrawLineObjectInBunch.invoke(drawLine)
                                    currentLine.clear()
                                }
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()

                                if (selectTool.value != null) {

                                    currentDragText.value?.let { currentDrag ->
                                        val position = currentDrag.position + dragAmount

                                        currentDragText.value = currentDrag.copy(
                                            position = position
                                        )
                                    }

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
                drawImage(image)

                drawBunch.value.forEachIndexed { index, bunch ->

                    if (touchIndex == index) {
                        return@forEachIndexed
                    }


                    when (bunch.drawObjectType) {
                        DrawMode.Text -> {
                            val textObject = (bunch as DrawText)

                            drawContext.canvas.nativeCanvas.drawText(
                                textObject.text,
                                textObject.position.x,
                                textObject.position.y,
                                Paint().apply {
                                    color = textObject.color.toArgb()
                                    textSize = textObject.fontSize.sp.toPx()
                                }
                            )

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

                    canvas.drawText(
                        textObject.text,
                        textObject.position.x,
                        textObject.position.y,
                        paint
                    )
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
    val drawObjectType: DrawMode
    var isSelected: Boolean
}

data class DrawLine(
    override val drawObjectType: DrawMode = DrawMode.Line,
    override var isSelected: Boolean = false,
    val list: List<Line>,
) : DrawObject

data class DrawText(
    override val drawObjectType: DrawMode = DrawMode.Text,
    override var isSelected: Boolean = true,
    val text: String,
    val color: Color,
    var position: Offset,
    val fontSize: Int,
) : DrawObject

data class DrawClear(
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

fun isPointInsideText(point: Offset, textItem: DrawText, density: Density): Boolean {
    val position = textItem.position
    val textBounds = Rect()
    val paint = Paint().apply {
        textSize = with(density) { textItem.fontSize.dp.toPx() }
    }
    paint.getTextBounds(textItem.text, 0, textItem.text.length, textBounds)
    val textWidth = paint.measureText(textItem.text)
    val textHeight = textBounds.height()

    Log.d("check---", "isPointInsideResizeHandle: p $position")
    Log.d("check---", "isPointInsideResizeHandle: s ($textWidth, $textHeight)")
    Log.d("check---", "isPointInsideResizeHandle: o $point")

//    val topLeft = Offset(position.x, position.y)
//    val bottomRight = Offset(position.x + textWidth, position.y + textHeight)

    val bottomLeft = Offset(position.x, position.y)
    val topLeft = Offset(position.x, position.y - textHeight)
    val topRight = Offset(position.x + textWidth, position.y - textHeight)
    val bottomRight = Offset(position.x + textWidth, position.y)

    val inLeft = point.x > topLeft.x
    Log.d("check---", "isPointInsideResizeHandle: inLeft  $inLeft \n(${point.x} > ${topLeft.x}) ")
    val inTop = point.y > topRight.y
    Log.d("check---", "isPointInsideResizeHandle: inTop $inTop \n (${point.y} > ${topRight.y}) ")
    val inRight = point.x < topRight.x
    Log.d(
        "check---",
        "isPointInsideResizeHandle: inRight $inRight \n (${point.x} > ${topRight.x}) "
    )
    val inBottom = point.y < bottomRight.y
    Log.d(
        "check---",
        "isPointInsideResizeHandle: inBottom $inBottom \n (${point.y} > ${bottomRight.y}) "
    )

    return inLeft && inTop && inRight && inBottom
//    return point.x in topLeft.x..bottomRight.x && point.y in topLeft.y..bottomRight.y
}

//fun isPointInsideResizeHandle(point: Offset, textItem: DrawText, density: Density): Boolean {
//    val resizeHandleSize = 20.dp.value
//    val position = textItem.position
//    val size = with(density) {
//        Offset(
//            textItem.text.length * textItem.fontSize.dp.toPx(),
//            textItem.fontSize.dp.toPx()
//        )
//    }
//
//
//    Log.d("check---", "isPointInsideResizeHandle: p $position")
//    Log.d("check---", "isPointInsideResizeHandle: s $size")
//    Log.d("check---", "isPointInsideResizeHandle: o $point")
//
//    val topLeft = Offset(position.x, position.y)
//    val topRight = Offset(position.x + size.x, position.y)
//    val bottomLeft = Offset(position.x, position.y + size.y)
//    val bottomRight = Offset(position.x + size.x, position.y + size.y)
//
//    val handleBounds = RectF(
//        /* left = */ topLeft.x - resizeHandleSize,
//        /* top = */ topLeft.y - resizeHandleSize,
//        /* right = */ bottomRight.x + resizeHandleSize,
//        /* bottom = */ bottomRight.y + resizeHandleSize
//    )
//
//
//    return handleBounds.contains(point.x, point.y)
//}


//private fun isTouched(
//    drawText: DrawText,
//    touchPosition: Offset,
//    radius: Float,
//    density: Density
//): Boolean {
////    return center.minus(touchPosition).getDistanceSquared() < radius * radius
//
//    val bonusArea = 20f
//    val position = drawText.position
//    val size = with(density) {
//        Offset(
//            drawText.text.length * drawText.fontSize.dp.toPx(),
//            drawText.fontSize.dp.toPx()
//        )
//    }
//
//
//    val topLeft = Offset(position.x, position.y)
//
//
//    val bounds = RectF(
//        position.x - bonusArea,
//        position.y - bonusArea,
//
//        )
//
//    return
//}
