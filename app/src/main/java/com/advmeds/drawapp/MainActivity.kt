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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.advmeds.drawapp.ui.theme.DrawAppTheme


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

                    val afTextTool = remember {
                        mutableStateOf<Any?>(null)
                    }

                    val straightLine = remember {
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

                    val turnTool: (ToolType) -> Unit = {
                        currentSize.value = null
                        currentTextSize.value = null
                        selectTool.value = null
                        afTextTool.value = null
                        straightLine.value = null

                        when (it) {
                            is ToolType.Text -> currentTextSize.value = it.textSize
                            is ToolType.Line -> currentSize.value = it.lineSize
                            ToolType.Select -> selectTool.value = Any()
                            ToolType.AfText -> afTextTool.value = Any()
                            ToolType.StraightLine -> straightLine.value = Any()
                        }
                    }

                    Box(modifier = Modifier.fillMaxSize()) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                        ) {

                            item {
                                val density = LocalDensity.current

                                Button(
                                    onClick = {
                                        newBitmap =
                                            createBitmapFromLines(
                                                image = bitmap,
                                                canvasWidth = canvasWidth,
                                                canvasHeight = canvasHeight,
                                                drawBunch = drawBunch.value,
                                                density = density
                                            )


                                    }
                                ) {
                                    Text(text = "Send")
                                }
                            }

                            item {
                                Divider()
                            }

                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(top = 10.dp, bottom = 7.dp), // TODO add res
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    ButtonItem(
                                        content = { modifier ->
                                            Icon(
                                                modifier = modifier,
                                                painter = painterResource(id = R.drawable.undo_ic),
                                                contentDescription = null
                                            )
                                        },
                                        onClick = {
                                            val newUnDoStack = drawBunch.value.toMutableList()

                                            if (newUnDoStack.isEmpty()) {
                                                if (reDoStack.value.isEmpty()) {
                                                    return@ButtonItem
                                                }

                                                val lastReDoStackObject = reDoStack.value.last()

                                                val isLastActionWasClear =
                                                    (lastReDoStackObject is DrawClear)

                                                if (!isLastActionWasClear) {
                                                    return@ButtonItem
                                                }

                                                val newUnDo = reDoStack.value

                                                drawBunch.value = newUnDo
                                                reDoStack.value = emptyList()

                                                return@ButtonItem
                                            }

                                            val lastElement = newUnDoStack.removeLast()

                                            val newRedoStack = reDoStack.value.toMutableList()

                                            newRedoStack.add(lastElement)

                                            drawBunch.value = newUnDoStack
                                            reDoStack.value = newRedoStack
                                        }
                                    )

                                    ButtonItem(
                                        content = { modifier ->
                                            Icon(
                                                modifier = modifier,
                                                painter = painterResource(id = R.drawable.redo_ic),
                                                contentDescription = null
                                            )
                                        },
                                        onClick = {
                                            val newRedoStack = reDoStack.value.toMutableList()

                                            if (newRedoStack.isEmpty()) {
                                                if (drawBunch.value.isEmpty()) {
                                                    return@ButtonItem
                                                }

                                                val lastReDoObject = drawBunch.value.last()

                                                val isLastUnDoActionWasClear =
                                                    (lastReDoObject is DrawClear)

                                                if (!isLastUnDoActionWasClear) {
                                                    return@ButtonItem
                                                }

                                                val newReDo = drawBunch.value

                                                drawBunch.value = emptyList()
                                                reDoStack.value = newReDo

                                                return@ButtonItem
                                            }

                                            val lastElement = newRedoStack.removeLast()

                                            val newUnDoStack = drawBunch.value.toMutableList()

                                            newUnDoStack.add(lastElement)

                                            drawBunch.value = newUnDoStack
                                            reDoStack.value = newRedoStack
                                        }
                                    )

                                    ButtonItem(
                                        content = { modifier ->
                                            Icon(
                                                modifier = modifier,
                                                painter = painterResource(id = R.drawable.clear_ic),
                                                contentDescription = null
                                            )
                                        },
                                        onClick = {
                                            val newUnDoStack = drawBunch.value.toMutableList()

                                            if (newUnDoStack.isEmpty()) {
                                                return@ButtonItem
                                            }

                                            val newRedoStack = newUnDoStack.toMutableList()

                                            val clearState = DrawClear()

                                            newRedoStack.add(clearState)

                                            drawBunch.value = emptyList()
                                            reDoStack.value = newRedoStack
                                        }
                                    )

                                    ButtonItem(
                                        isSelected = selectTool.value != null,
                                        content = { modifier ->
                                            Icon(
                                                modifier = modifier,
                                                painter = painterResource(id = R.drawable.mouse_ic),
                                                contentDescription = null
                                            )
                                        }
                                    ) {
                                        turnTool.invoke(ToolType.Select)
                                    }

                                    Box(
                                        Modifier
                                            .width(1.dp)
                                            .height(30.dp)
                                            .background(color = Color.Black) // TODO change to gray
                                    )

                                    textSizeList.forEach { size ->
                                        ButtonItem(
                                            isSelected = currentTextSize.value == size,
                                            content = { modifier ->
                                                Text(
                                                    modifier = modifier,
                                                    text = "T",
                                                    fontSize = size.sp
                                                )
                                            }
                                        ) {
                                            turnTool.invoke(ToolType.Text(size))
                                        }
                                    }
                                }
                            }

                            item {

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 10.dp, top = 7.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {

                                    sizeList.forEach { size ->

                                        ButtonItem(
                                            isSelected = currentSize.value == size,
                                            content = { modifier ->
                                                Box(
                                                    modifier = modifier
                                                        .size((size * 3).dp)
                                                        .background(
                                                            color = Color.Black,
                                                            shape = CircleShape
                                                        )
                                                )
                                            }
                                        ) {
                                            turnTool.invoke(ToolType.Line(size))
                                        }
                                    }

                                    colorList.forEach { color ->

                                        ButtonItem(
                                            isSelected = currentColor.value == color,
                                            content = { modifier ->
                                                Box(
                                                    modifier = modifier
                                                        .size(20.dp)
                                                        .background(
                                                            color = color,
                                                            shape = RoundedCornerShape(4.dp)
                                                        )
                                                )
                                            }
                                        ) {
                                            currentColor.value = color
                                        }
                                    }

                                    ButtonItem(
                                        isSelected = afTextTool.value != null,
                                        content = { modifier ->
                                            Text(
                                                modifier = modifier,
                                                text = "AF",
                                                fontSize = 20.sp
                                            )
                                        }
                                    ) {
                                        turnTool.invoke(ToolType.AfText)
                                    }

                                    ButtonItem(
                                        isSelected = straightLine.value != null,
                                        content = { modifier ->
                                            Box(
                                                modifier = modifier
                                                    .height(3.dp)
                                                    .width(20.dp)
                                                    .background(Color.Black)
                                            )
                                        }
                                    ) {
                                        turnTool.invoke(ToolType.StraightLine)
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
                                        afTextTool = afTextTool,
                                        straightLine = straightLine,
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
    private fun ButtonItem(
        isSelected: Boolean = false,
        content: @Composable (modifier: Modifier) -> Unit,
        onClick: () -> Unit,
    ) {
        val shape = MaterialTheme.shapes.small
        Box(
            modifier = Modifier
                .size(30.dp)
                .border(
                    color = Color.Gray,
                    width = 1.dp,
                    shape = shape
                )
                .background(
                    color = if (isSelected) Color.Gray else Color.White,
                    shape = shape,
                )
                .clickable(onClick = onClick)
        ) {
            content.invoke(Modifier.align(Alignment.Center))
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
        afTextTool: MutableState<Any?>,
        straightLine: MutableState<Any?>,
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
                    .clipToBounds()
                    .pointerInput(true) {
                        detectDragGesturesCustom(
                            onTap = { offset ->
                                currentDragObject.clear()

                                onTab(
                                    offset = offset,
                                    density = density,
                                    selectTool = selectTool,
                                    afTextTool = afTextTool,
                                    straightLine = straightLine,
                                    currentTextSize = currentTextSize,
                                    drawBunch = drawBunch,
                                    setTextDialogIsEnable = setTextDialogIsEnable,
                                    addAfText = {
                                        val drawText = DrawText(
                                            id = drawObjectIdCounter.value,
                                            text = "AF",
                                            color = currentColor.value,
                                            position = offset,
                                            fontSize = 25
                                        )

                                        addDrawTextObjectInBunch.invoke(drawText)
                                    },
                                    addStraightLine = {
                                        currentLine.clear()

                                        val line = Line(
                                            start = offset,
                                            end = offset.copy(
                                                x = offset.x + 50f
                                            ),
                                            color = currentColor.value,
                                            strokeWidth = 7.dp
                                        )

                                        currentLine.add(line)

                                        val bounds = calculateBoundingBox(currentLine)

                                        val drawLine = DrawLine(
                                            id = drawObjectIdCounter.value,
                                            list = currentLine.toList(),
                                            bounds = bounds,
                                        )
                                        addDrawLineObjectInBunch.invoke(drawLine)
                                        currentLine.clear()
                                    },
                                    setTextPosition = {
                                        textPosition = offset
                                    },
                                    addCurrentDragObject = { drawObject ->
                                        currentDragObject.add(drawObject)
                                    }
                                )
                            },
                            onDragStart = { offset ->
                                currentDragObject.clear()

                                onDragStart(
                                    selectTool = selectTool,
                                    drawBunch = drawBunch,
                                    offset = offset,
                                    density = density,
                                    initResize = { item, corner ->
                                        currentDragObject.add(item)
                                        isResizing.value = true
                                        currentResizeCorner.value = corner
                                    },
                                    addCurrentDragObject = { dragObject ->
                                        currentDragObject.add(dragObject)
                                    }
                                )
                            },
                            onDragEnd = {
                                completeDrag(
                                    selectTool = selectTool,
                                    drawBunch = drawBunch,
                                    currentDragObject = currentDragObject,
                                    currentSize = currentSize,
                                    completionOfDrag = { tempDrawBunch ->
                                        drawBunch.value = tempDrawBunch
                                        isResizing.value = false
                                        currentResizeCorner.value = null
                                    },
                                    completeDrawLine = {
                                        val bounds = calculateBoundingBox(currentLine)

                                        val drawLine = DrawLine(
                                            id = drawObjectIdCounter.value,
                                            list = currentLine.toList(),
                                            bounds = bounds,
                                        )
                                        addDrawLineObjectInBunch.invoke(drawLine)
                                        currentLine.clear()
                                    },
                                )
                            },
                            onDragCancel = {
                                completeDrag(
                                    selectTool = selectTool,
                                    drawBunch = drawBunch,
                                    currentDragObject = currentDragObject,
                                    currentSize = currentSize,
                                    completionOfDrag = { tempDrawBunch ->
                                        drawBunch.value = tempDrawBunch
                                        isResizing.value = false
                                        currentResizeCorner.value = null
                                    },
                                    completeDrawLine = {
                                        val bounds = calculateBoundingBox(currentLine)

                                        val drawLine = DrawLine(
                                            id = drawObjectIdCounter.value,
                                            list = currentLine.toList(),
                                            bounds = bounds,
                                        )
                                        addDrawLineObjectInBunch.invoke(drawLine)
                                        currentLine.clear()
                                    },
                                )
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                onDrag(
                                    selectTool = selectTool,
                                    currentDragObject = currentDragObject,
                                    density = density,
                                    isResizing = isResizing,
                                    currentResizeCorner = currentResizeCorner,
                                    change = change,
                                    currentSize = currentSize,
                                    updateDragData = { list ->
                                        currentDragObject.clear()
                                        currentDragObject.addAll(list)
                                    },
                                    addNewLine = {
                                        val line = Line(
                                            start = change.position - dragAmount,
                                            end = change.position,
                                            color = currentColor.value,
                                            strokeWidth = currentSize.value!!.dp
                                        )

                                        currentLine.add(line)
                                    }
                                )
                            }
                        )
                    }
            ) {
                val canvasSize = size

                if (!canvasSizeInvokeFlag) {
                    setCanvasSize.invoke(canvasSize)
                    canvasSizeInvokeFlag = true
                }

                drawBackGroundImage(canvasSize, image)

                drawStaticDrawObject(drawBunch, currentDragObject, density)

                drawCurrentDrawLine(currentLine)

                drawCurrentGragAndMoveDrawObjects(currentDragObject, density, isResizing)
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

}

sealed class ToolType {
    data class Text(val textSize: Int) : ToolType()
    object AfText : ToolType()
    data class Line(val lineSize: Int) : ToolType()
    object StraightLine : ToolType()
    object Select : ToolType()
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