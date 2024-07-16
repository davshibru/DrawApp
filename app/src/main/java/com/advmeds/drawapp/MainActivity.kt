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
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
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
                        .decodeResource(resources, R.drawable.img_2, options)
                        .asImageBitmap()

                    var newBitmap: Bitmap? = null

                    var canvasWidth by remember { mutableStateOf(0f) }
                    var canvasHeight by remember { mutableStateOf(0f) }


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
                                        createBitmapFromLines(
                                            image = bitmap,
                                            canvasWidth = canvasWidth,
                                            canvasHeight = canvasHeight,
                                            drawBunch = drawBunch.value,
                                            density = density
                                        )


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
                                        setCanvasSize = { canvasSize ->
                                            canvasHeight = canvasSize.height
                                            canvasWidth = canvasSize.width
                                        },
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

                                            val tempDrawObjectList =
                                                drawBunch.value.toMutableList()
                                            tempDrawObjectList.add(text)

                                            drawObjectIdCounter.value += 1

                                            drawBunch.value = tempDrawObjectList

                                            reDoStack.value = emptyList()
                                        },
                                        addDrawLineObjectInBunch = { line ->
                                            val tempDrawObjectList =
                                                drawBunch.value.toMutableList()

                                            tempDrawObjectList.add(line)

                                            drawObjectIdCounter.value += 1

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
        drawBunch: MutableState<DrawBunch>,
        currentColor: MutableState<Color>,
        currentSize: MutableState<Int?>,
        currentTextSize: MutableState<Int?>,
        currentText: MutableState<String?>,
        selectTool: MutableState<Any?>,
        setCanvasSize: (canvasSize: Size) -> Unit,
        setTextDialogIsEnable: (Boolean) -> Unit,
        addDrawTextObjectInBunch: (text: DrawText) -> Unit,
        addDrawLineObjectInBunch: (line: DrawLine) -> Unit,
    ) {
        val density = LocalDensity.current
        var canvasSizeInvokeFlag by remember { mutableStateOf(false) }

        val currentResizeCorner = remember { mutableStateOf<Corner?>(null) }
        var textPosition by remember { mutableStateOf(Offset.Zero) }
        val currentLine = remember { mutableStateListOf<Line>() }
        val isResizing = remember { mutableStateOf(false) }
        val currentDragObject = remember { mutableStateListOf<DrawObject>() }

        LaunchedEffects(
            selectTool = selectTool,
            currentText = currentText,
            drawObjectIdCounter = drawObjectIdCounter,
            currentColor = currentColor,
            textPosition = textPosition,
            currentTextSize = currentTextSize,
            addDrawTextObjectInBunch = addDrawTextObjectInBunch,
            clearCurrentDragObjectList = {
                currentDragObject.clear()
            },
            resetTextPosition = {
                textPosition = Offset.Zero
            }
        )

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

                                if (currentTextSize.value != null) {
                                    textPosition = offset
                                    setTextDialogIsEnable.invoke(true)
                                }

                                if (selectTool.value != null) {
                                    currentDragObject.clear()

                                    drawBunch.value
                                        .asReversed()
                                        .forEach { item ->

                                            val isTouched = isPointInsideDrawObject(
                                                offset, item, density = density
                                            )

                                            val corners = isPointInsideResizeHandleDrawObject(
                                                point = offset,
                                                item,
                                                density
                                            )

                                            if (isTouched || corners != null) {

                                                currentDragObject.add(item)

                                                val (textWidth, textHeight) = getDraggedObjectWidthAndHeight(
                                                    item,
                                                    density
                                                )

                                                val position = when (item.drawObjectType) {
                                                    DrawMode.Text -> (item as DrawText).position
                                                    DrawMode.Line -> (item as DrawLine).bounds.bottomLeft
                                                    DrawMode.Select -> Offset.Zero
                                                    DrawMode.Clear -> Offset.Zero
                                                }

                                                val centerPivot = Offset(
                                                    position.x + textWidth / 2,
                                                    position.y - textHeight / 2,
                                                )

                                                Log.d(
                                                    "check---",
                                                    "DrawingScreen: centerPivot - $centerPivot"
                                                )

                                                return@detectDragGesturesCustom
                                            }


                                        }
                                }

                                Log.d("check---", "DrawingScreen: $offset")
                            },
                            onDragStart = { offset ->

                                currentDragObject.clear()

                                if (selectTool.value != null) {
                                    drawBunch.value
//                                        .filterList { drawObjectType == DrawMode.Text }
                                        .asReversed()
                                        .forEachIndexed { index, item ->
                                            val isTouched = isPointInsideDrawObject(
                                                offset, item, density = density
                                            )

                                            val corner = isPointInsideResizeHandleDrawObject(
                                                point = offset,
                                                item,
                                                density
                                            )

                                            if (corner != null) {
                                                currentDragObject.add(item)
                                                isResizing.value = true
                                                currentResizeCorner.value = corner
                                            } else if (isTouched) {
                                                currentDragObject.add(item)

                                                return@detectDragGesturesCustom
                                            }
                                        }
                                }
                            },
                            onDragEnd = {
                                Log.d("check---", "DrawingScreen: Start end")

                                if (selectTool.value != null) {
                                    val tempDrawBunch = drawBunch.value.toMutableList()

                                    currentDragObject.forEach { dragged ->

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
                                    val bounds = calculateBoundingBox(currentLine)

                                    val drawLine = DrawLine(
                                        id = drawObjectIdCounter.value,
                                        list = currentLine.toList(),
                                        bounds = bounds,
                                    )
                                    addDrawLineObjectInBunch.invoke(drawLine)
                                    currentLine.clear()
                                }

                            },
                            onDragCancel = {
                                Log.d("check---", "DrawingScreen: Start cancel")

                                if (selectTool.value != null) {
                                    val tempDrawBunch = drawBunch.value.toMutableList()

                                    currentDragObject.forEach { dragged ->

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
                                    val bounds = calculateBoundingBox(currentLine)

                                    val drawLine = DrawLine(
                                        id = drawObjectIdCounter.value,
                                        list = currentLine.toList(),
                                        bounds = bounds,
                                    )
                                    addDrawLineObjectInBunch.invoke(drawLine)
                                    currentLine.clear()
                                }
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                if (selectTool.value != null) {

                                    val newList = mutableListOf<DrawObject>()

                                    currentDragObject.forEach { drawObject ->
                                        val (textWidth, textHeight) = getDraggedObjectWidthAndHeight(
                                            drawObject,
                                            density
                                        )

                                        val position = when (drawObject.drawObjectType) {
                                            DrawMode.Text -> (drawObject as DrawText).position
                                            DrawMode.Line -> (drawObject as DrawLine).bounds.bottomLeft
                                            DrawMode.Select -> Offset.Zero
                                            DrawMode.Clear -> Offset.Zero
                                        }


                                        val centerPivot = Offset(
                                            position.x + textWidth / 2,
                                            position.y - textHeight / 2,
                                        )

                                        if (isResizing.value) {
                                            val translateMatrix = android.graphics.Matrix()

                                            translateMatrix.postTranslate(
                                                drawObject.cumulativeTranslationX,
                                                drawObject.cumulativeTranslationY,
                                            )

                                            val newPosition = getNewPositionByTranslation(
                                                position,
                                                translationMatrix = translateMatrix
                                            )

                                            val newCenterPoint = Offset(
                                                newPosition.x + textWidth / 2,
                                                newPosition.y - textHeight / 2,
                                            )

                                            val scaleX = when (currentResizeCorner.value) {
                                                Corner.TopLeft, Corner.BottomLeft -> ((change.position.x) - newCenterPoint.x) / (newPosition.x - newCenterPoint.x)
                                                Corner.TopRight, Corner.BottomRight -> (change.position.x - newCenterPoint.x) / ((newPosition.x + textWidth) - newCenterPoint.x)
                                                null -> 1f
                                            }
                                            val scaleY = when (currentResizeCorner.value) {
                                                Corner.TopLeft -> (change.position.y - newCenterPoint.y) / ((newPosition.y - textHeight) - newCenterPoint.y)
                                                Corner.TopRight -> (change.position.y - newCenterPoint.y) / ((newPosition.y - textHeight) - newCenterPoint.y)

                                                Corner.BottomRight, Corner.BottomLeft -> (((change.position.y) - newCenterPoint.y)) / (newPosition.y - newCenterPoint.y)
                                                null -> 1f
                                            }

                                            drawObject.cumulativeScaleX = scaleX
                                            drawObject.cumulativeScaleY = scaleY
                                        } else {
                                            drawObject.cumulativeTranslationX =
                                                (change.position.x - position.x) - textWidth / 2
                                            drawObject.cumulativeTranslationY =
                                                (change.position.y - position.y) + textHeight / 2
                                        }

                                        val matrix = android.graphics.Matrix()

                                        matrix.setScale(
                                            drawObject.cumulativeScaleX,
                                            drawObject.cumulativeScaleY,
                                            centerPivot.x,
                                            centerPivot.y
                                        )

                                        matrix.postTranslate(
                                            drawObject.cumulativeTranslationX,
                                            drawObject.cumulativeTranslationY,
                                        )

                                        drawObject.transformMatrix = matrix

                                        newList.add(drawObject)
                                    }

                                    currentDragObject.clear()
                                    currentDragObject.addAll(newList)
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
                val canvasSize = size

                if (!canvasSizeInvokeFlag) {
                    setCanvasSize.invoke(canvasSize)
                    canvasSizeInvokeFlag = true
                }

                drawIntoCanvas { canvas ->
                    val dest = Rect(0, 0, canvasSize.width.toInt(), canvasSize.height.toInt())
                    val paint = Paint()
                    paint.isFilterBitmap = true
                    canvas.nativeCanvas.drawBitmap(image.asAndroidBitmap(), null, dest, paint)

                }

                drawBunch.value.forEachIndexed { index, bunch ->

                    if (currentDragObject.map { it.id }.contains(bunch.id)) {
                        return@forEachIndexed
                    }


                    when (bunch.drawObjectType) {
                        DrawMode.Text -> {
                            val textObject = (bunch as DrawText)

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
                        }

                        DrawMode.Line -> {

                            val drawLine = (bunch as DrawLine)

                            drawIntoCanvas { canvas ->
                                val paint = Paint()

                                canvas.save()
                                canvas.nativeCanvas.concat(drawLine.transformMatrix)
                                drawLine.list.forEach { line ->
                                    paint.color = line.color.toArgb()
                                    paint.strokeWidth = with(density) { line.strokeWidth.toPx() }
                                    paint.strokeCap = Paint.Cap.ROUND


                                    canvas.nativeCanvas.drawLine(
                                        line.start.x,
                                        line.start.y,
                                        line.end.x,
                                        line.end.y,
                                        paint
                                    )
                                }

                                canvas.nativeCanvas.restore()
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

                currentDragObject.forEach { dragObject ->

                    val (textWidth, textHeight) = getDraggedObjectWidthAndHeight(
                        dragObject,
                        density
                    )

                    when (dragObject.drawObjectType) {
                        DrawMode.Text -> {
                            val dragText = (dragObject as DrawText)



                            drawIntoCanvas { canvas ->
                                val paint = Paint().apply {
                                    color = dragText.color.toArgb()
                                    textSize = dragText.fontSize.sp.toPx()
                                }

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

                                val topLeft = boundsPosition
                                val topRight =
                                    Offset(boundsPosition.x + textWidth, boundsPosition.y)
                                val bottomRight = Offset(
                                    boundsPosition.x + textWidth,
                                    boundsPosition.y + textHeight.toFloat()
                                )
                                val bottomLeft =
                                    Offset(
                                        boundsPosition.x,
                                        boundsPosition.y + textHeight.toFloat()
                                    )

                                drawRectangleByOffsets(
                                    canvas.nativeCanvas,
                                    topLeft,
                                    topRight,
                                    bottomRight,
                                    bottomLeft,
                                    paint
                                )

                                canvas.nativeCanvas.restore()
                            }

                            if (!isResizing.value) {
                                drawIntoCanvas { canvas ->

                                    val originalBottomLeft = dragText.position
                                    val originalTopRight = Offset(
                                        originalBottomLeft.x + textWidth,
                                        originalBottomLeft.y - textHeight
                                    )

                                    val bottomLeft = getNewPositionByTranslation(
                                        originalBottomLeft, dragText.transformMatrix
                                    )

                                    val topRight = getNewPositionByTranslation(
                                        originalTopRight, dragText.transformMatrix
                                    )

                                    drawResizeHandles(
                                        canvas = canvas.nativeCanvas,
                                        bottomLeft = bottomLeft,
                                        topRight = topRight,
                                        resizeHandleSize = 20.dp,
                                    )
                                }
                            }
                        }

                        DrawMode.Line -> {
                            val dragLine = (dragObject as DrawLine)

                            drawIntoCanvas { canvas ->
                                val paint = Paint()

                                canvas.save()
                                canvas.nativeCanvas.concat(dragLine.transformMatrix)
                                dragLine.list.forEach { line ->
                                    paint.color = line.color.toArgb()
                                    paint.strokeWidth = with(density) { line.strokeWidth.toPx() }
                                    paint.strokeCap = Paint.Cap.ROUND


                                    canvas.nativeCanvas.drawLine(
                                        line.start.x,
                                        line.start.y,
                                        line.end.x,
                                        line.end.y,
                                        paint
                                    )
                                }

                                val boundingBoxPaint = Paint().apply {
                                    color = android.graphics.Color.BLACK
                                    style = Paint.Style.STROKE
                                    strokeWidth = 1f
                                }

                                drawBoundingBox(
                                    canvas = canvas.nativeCanvas,
                                    position = dragLine.bounds.bottomLeft,
                                    size = Offset(dragLine.bounds.width, -dragLine.bounds.height),
                                    paint = boundingBoxPaint,
                                )

                                canvas.nativeCanvas.restore()
                            }

                            if (!isResizing.value) {
                                drawIntoCanvas { canvas ->

                                    val originalBottomLeft = dragLine.bounds.bottomLeft
                                    val originalTopRight = Offset(
                                        originalBottomLeft.x + textWidth,
                                        originalBottomLeft.y - textHeight
                                    )

                                    val bottomLeft = getNewPositionByTranslation(
                                        originalBottomLeft, dragLine.transformMatrix
                                    )

                                    val topRight = getNewPositionByTranslation(
                                        originalTopRight, dragLine.transformMatrix
                                    )

                                    drawResizeHandles(
                                        canvas = canvas.nativeCanvas,
                                        bottomLeft = bottomLeft,
                                        topRight = topRight,
                                        resizeHandleSize = 20.dp,
                                    )
                                }
                            }
                        }

                        DrawMode.Select, DrawMode.Clear -> {}
                    }
                }
            }
        }
    }

    @Composable
    private fun LaunchedEffects(
        selectTool: MutableState<Any?>,
        clearCurrentDragObjectList: () -> Unit,
        currentText: MutableState<String?>,
        drawObjectIdCounter: MutableState<Int>,
        currentColor: MutableState<Color>,
        textPosition: Offset,
        currentTextSize: MutableState<Int?>,
        resetTextPosition: () -> Unit,
        addDrawTextObjectInBunch: (text: DrawText) -> Unit
    ) {
        LaunchedEffect(selectTool.value) {
            if (selectTool.value == null) {
                clearCurrentDragObjectList.invoke()
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

                resetTextPosition.invoke()

                addDrawTextObjectInBunch.invoke(drawText)
            }
        }
    }

    private fun createBitmapFromLines(
        image: ImageBitmap,
        canvasWidth: Float,
        canvasHeight: Float,
        drawBunch: DrawBunch,
        density: Density,
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(
            canvasWidth.toInt(),
            canvasHeight.toInt(),
            Bitmap.Config.ARGB_8888
        )
        val canvas = AndroidCanvas(bitmap)
        val paint = Paint()

        val dest = Rect(0, 0, canvasWidth.toInt(), canvasHeight.toInt())
        paint.isFilterBitmap = true
        canvas.drawBitmap(image.asAndroidBitmap(), null, dest, paint)

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
                        canvas.save()
                        canvas.concat(bunch.transformMatrix)

//                        val matrix = Matrix()
//                        matrix.setScale(scaleX, scaleY)

//                        canvas.concat(matrix)
                        canvas.drawLine(
                            line.start.x,
                            line.start.y,
                            line.end.x,
                            line.end.y,
                            paint
                        )
                        canvas.restore()
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

    private fun drawResizeHandles(
        canvas: android.graphics.Canvas,
        bottomLeft: Offset,
        topRight: Offset,
        resizeHandleSize: Dp
    ) {
        val halfResizeHandleSize = resizeHandleSize.value / 2
        val topLeftRect =
            Offset(bottomLeft.x - halfResizeHandleSize, topRight.y - halfResizeHandleSize)
        val topRightRect =
            Offset(topRight.x - halfResizeHandleSize, topRight.y - halfResizeHandleSize)
        val bottomLeftRect =
            Offset(bottomLeft.x - halfResizeHandleSize, bottomLeft.y - halfResizeHandleSize)
        val bottomRightRect = Offset(
            topRight.x - halfResizeHandleSize,
            bottomLeft.y - halfResizeHandleSize
        )

        val paint = Paint().apply {
            color = Color.Black.toArgb()
            style = Paint.Style.FILL
        }

        // Draw resize handles
        canvas.drawRect(
            topLeftRect.x,
            topLeftRect.y,
            topLeftRect.x + resizeHandleSize.value,
            topLeftRect.y + resizeHandleSize.value,
            paint
        )
        canvas.drawRect(
            topRightRect.x,
            topRightRect.y,
            topRightRect.x + resizeHandleSize.value,
            topRightRect.y + resizeHandleSize.value,
            paint
        )
        canvas.drawRect(
            bottomLeftRect.x,
            bottomLeftRect.y,
            bottomLeftRect.x + resizeHandleSize.value,
            bottomLeftRect.y + resizeHandleSize.value,
            paint
        )
        canvas.drawRect(
            bottomRightRect.x,
            bottomRightRect.y,
            bottomRightRect.x + resizeHandleSize.value,
            bottomRightRect.y + resizeHandleSize.value,
            paint
        )
    }
}

fun drawBoundingBox(
    canvas: android.graphics.Canvas,
    position: Offset,
    size: Offset,
    paint: Paint
) {
    canvas.drawRect(
        position.x,
        position.y,
        position.x + size.x,
        position.y + size.y,
        paint
    )
}

fun drawRectangleByOffsets(
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


enum class DrawMode {
    Text,
    Line,
    Select,
    Clear,
}


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