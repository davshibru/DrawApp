package com.advmeds.drawapp

import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.util.Log
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculateCentroidSize
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateRotation
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.drag
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sign

fun drawToBitmap(bitmap: ImageBitmap): ImageBitmap {
    val drawScope = CanvasDrawScope()
    val size = Size(400f, 400f) // simple example of 400px by 400px image

//    val bitmap = ImageBitmap(size.width.toInt(), size.height.toInt())
    val canvas = Canvas(bitmap)

    drawScope.draw(
        density = Density(1f),
        layoutDirection = LayoutDirection.Ltr,
        canvas = canvas,
        size = size,
    ) {
        // Draw whatever you want here; for instance, a white background and a red line.
        drawRect(color = Color.Transparent, topLeft = Offset.Zero, size = size)
        drawLine(
            color = Color.Red,
            start = Offset.Zero,
            end = Offset(size.width, size.height),
            strokeWidth = 5f
        )
    }
    return bitmap
}


suspend fun PointerInputScope.detectAdvancedVerticalDragGestures(
    panZoomLock: Boolean = false,
    touchCount: (touchCount: Int) -> Unit,
    onGesture: (centroid: Offset, pan: Offset, zoom: Float, rotation: Float) -> Unit,
) {
    awaitEachGesture {
        var rotation = 0f
        var zoom = 1f
        var pan = Offset.Zero
        var pastTouchSlop = false
        val touchSlop = viewConfiguration.touchSlop
        var lockedToPanZoom = false

        awaitFirstDown(requireUnconsumed = false)
        do {
            val event = awaitPointerEvent()

            touchCount(event.changes.count())

            val canceled = event.changes.any { it.isConsumed }
            if (!canceled) {
                val zoomChange = event.calculateZoom()
                val rotationChange = event.calculateRotation()
                val panChange = event.calculatePan()

                if (!pastTouchSlop) {
                    zoom *= zoomChange
                    rotation += rotationChange
                    pan += panChange

                    val centroidSize = event.calculateCentroidSize(useCurrent = false)
                    val zoomMotion = abs(1 - zoom) * centroidSize
                    val rotationMotion = abs(rotation * PI.toFloat() * centroidSize / 180f)
                    val panMotion = pan.getDistance()

                    if (zoomMotion > touchSlop ||
                        rotationMotion > touchSlop ||
                        panMotion > touchSlop
                    ) {
                        pastTouchSlop = true
                        lockedToPanZoom = panZoomLock && rotationMotion < touchSlop
                    }
                }

                if (pastTouchSlop) {
                    val centroid = event.calculateCentroid(useCurrent = false)
                    val effectiveRotation = if (lockedToPanZoom) 0f else rotationChange
                    if (effectiveRotation != 0f ||
                        zoomChange != 1f ||
                        panChange != Offset.Zero
                    ) {
                        onGesture(centroid, panChange, zoomChange, effectiveRotation)
                    }
                    event.changes.forEach {
                        if (it.positionChanged()) {
                            it.consume()
                        }
                    }
                }
            } else {
                touchCount(0)
            }
        } while (!canceled && event.changes.any { it.pressed })
    }
}

internal suspend fun PointerInputScope.detectTap(
    onTap: (Offset) -> Unit
) {
    awaitEachGesture {

    }
}

suspend fun PointerInputScope.detectDragGesturesCustom(
    onTap: (Offset) -> Unit,
    onDragStart: (Offset) -> Unit = { },
    onDragEnd: () -> Unit = { },
    onDragCancel: () -> Unit = { },
    onDrag: (change: PointerInputChange, dragAmount: Offset) -> Unit
) {

    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)

        val event = awaitPointerEvent()

        if (event.changes.count() > 1) {
            return@awaitEachGesture
        }

        var drag: PointerInputChange?
        var overSlop = Offset.Zero
        do {
            drag = awaitPointerSlopOrCancellationCustom(
                down.id,
                down.type,
                triggerOnMainAxisSlop = false
            ) { change, over ->
                change.consume()
                overSlop = over
            }
        } while (drag != null && !drag.isConsumed)
        if (drag != null) {
            onDragStart.invoke(drag.position)
            onDrag(drag, overSlop)
            if (
                !drag(drag.id) {
                    onDrag(it, it.positionChange())
                    it.consume()
                }
            ) {
                onDragCancel()
            } else {
                onDragEnd()
            }
        } else {
            onTap.invoke(down.position)
        }
    }
}

internal suspend inline fun AwaitPointerEventScope.awaitPointerSlopOrCancellationCustom(
    pointerId: PointerId,
    pointerType: PointerType,
    pointerDirectionConfig: PointerDirectionConfigCustom = HorizontalPointerDirectionConfigCustom,
    triggerOnMainAxisSlop: Boolean = true,
    onPointerSlopReached: (PointerInputChange, Offset) -> Unit,
): PointerInputChange? {
    if (currentEvent.isPointerUp(pointerId)) {
        return null // The pointer has already been lifted, so the gesture is canceled
    }
    val touchSlop = viewConfiguration.pointerSlopCustom(pointerType)
    var pointer: PointerId = pointerId
    var totalMainPositionChange = 0f
    var totalCrossPositionChange = 0f

    while (true) {
        val event = awaitPointerEvent()
        val dragEvent = event.changes.firstOrNull { it.id == pointer } ?: return null
        if (dragEvent.isConsumed) {
            return null
        } else if (dragEvent.changedToUpIgnoreConsumed()) {
            val otherDown = event.changes.firstOrNull { it.pressed }
            if (otherDown == null) {
                // This is the last "up"
                return null
            } else {
                pointer = otherDown.id
            }
        } else {
            val currentPosition = dragEvent.position
            val previousPosition = dragEvent.previousPosition

            val mainPositionChange = pointerDirectionConfig.mainAxisDelta(currentPosition) -
                    pointerDirectionConfig.mainAxisDelta(previousPosition)

            val crossPositionChange = pointerDirectionConfig.crossAxisDelta(currentPosition) -
                    pointerDirectionConfig.crossAxisDelta(previousPosition)
            totalMainPositionChange += mainPositionChange
            totalCrossPositionChange += crossPositionChange

            val inDirection = if (triggerOnMainAxisSlop) {
                abs(totalMainPositionChange)
            } else {
                pointerDirectionConfig.offsetFromChanges(
                    totalMainPositionChange,
                    totalCrossPositionChange
                ).getDistance()
            }
            if (inDirection < touchSlop) {
                // verify that nothing else consumed the drag event
                awaitPointerEvent(PointerEventPass.Final)
                if (dragEvent.isConsumed) {
                    return null
                }
            } else {
                val postSlopOffset = if (triggerOnMainAxisSlop) {
                    val finalMainPositionChange = totalMainPositionChange -
                            (sign(totalMainPositionChange) * touchSlop)
                    pointerDirectionConfig.offsetFromChanges(
                        finalMainPositionChange,
                        totalCrossPositionChange
                    )
                } else {
                    val offset = pointerDirectionConfig.offsetFromChanges(
                        totalMainPositionChange,
                        totalCrossPositionChange
                    )
                    val touchSlopOffset = offset / inDirection * touchSlop
                    offset - touchSlopOffset
                }

                onPointerSlopReached(
                    dragEvent,
                    postSlopOffset
                )
                if (dragEvent.isConsumed) {
                    return dragEvent
                } else {
                    totalMainPositionChange = 0f
                    totalCrossPositionChange = 0f
                }
            }
        }
    }
}

interface PointerDirectionConfigCustom {
    fun mainAxisDelta(offset: Offset): Float
    fun crossAxisDelta(offset: Offset): Float
    fun offsetFromChanges(mainChange: Float, crossChange: Float): Offset
}

/**
 * Used for monitoring changes on X axis.
 */
internal val HorizontalPointerDirectionConfigCustom = object : PointerDirectionConfigCustom {
    override fun mainAxisDelta(offset: Offset): Float = offset.x
    override fun crossAxisDelta(offset: Offset): Float = offset.y
    override fun offsetFromChanges(mainChange: Float, crossChange: Float): Offset =
        Offset(mainChange, crossChange)
}

/**
 * Used for monitoring changes on Y axis.
 */
internal val VerticalPointerDirectionConfigCustom = object : PointerDirectionConfigCustom {
    override fun mainAxisDelta(offset: Offset): Float = offset.y

    override fun crossAxisDelta(offset: Offset): Float = offset.x

    override fun offsetFromChanges(mainChange: Float, crossChange: Float): Offset =
        Offset(crossChange, mainChange)
}

private fun PointerEvent.isPointerUp(pointerId: PointerId): Boolean =
    changes.firstOrNull { it.id == pointerId }?.pressed != true

internal fun ViewConfiguration.pointerSlopCustom(pointerType: PointerType): Float {
    return when (pointerType) {
        PointerType.Mouse -> touchSlop * mouseToTouchSlopRatio
        else -> touchSlop
    }
}

private val mouseSlop = 0.125.dp
private val defaultTouchSlop = 18.dp // The default touch slop on Android devices
private val mouseToTouchSlopRatio = mouseSlop / defaultTouchSlop

fun getDraggedObjectWidthAndHeight(dragObject: DrawObject, density: Density): Pair<Float, Float> {
    if (dragObject.drawObjectType == DrawMode.Text) {
        return getTextWidthAndHeight((dragObject as DrawText), density)
    }

    if (dragObject.drawObjectType == DrawMode.Line) {
        val line = dragObject as DrawLine
        return line.bounds.width to line.bounds.height
    }

    return 0f to 0f
}
fun getTextWidthAndHeight(textItem: DrawText, density: Density): Pair<Float, Float> {
    val textBounds = Rect()
    val paint = Paint().apply {
        textSize = with(density) { textItem.fontSize.dp.toPx() }
    }
    paint.getTextBounds(textItem.text, 0, textItem.text.length, textBounds)
    val textWidth = paint.measureText(textItem.text)
    val textHeight = textBounds.height()

    return Pair(textWidth, textHeight.toFloat())
}

fun getTextWidthAndHeightM(
    textItem: DrawText,
    density: Density,
    transformMatrix: Matrix
): Pair<Float, Float> {
    val position = textItem.position

    val textBounds = Rect()
    val paint = Paint().apply {
        textSize = with(density) { textItem.fontSize.dp.toPx() }
    }
    paint.getTextBounds(textItem.text, 0, textItem.text.length, textBounds)
    val textWidth = paint.measureText(textItem.text)
    val textHeight = textBounds.height()

    val topRight = floatArrayOf(position.x + textWidth, position.y - textHeight)
    val bottomLeft = floatArrayOf(position.x, position.y)

    // Apply the transformation matrix
    transformMatrix.mapPoints(topRight)
    transformMatrix.mapPoints(bottomLeft)

    val xRange = floatArrayOf(bottomLeft[0], topRight[0])
    xRange.sort()

    val yRange = floatArrayOf(bottomLeft[1], topRight[1])
    yRange.sort()

    return Pair(xRange[1] - xRange[0], yRange[1] - yRange[0])
}

fun getNewPosition(textItem: DrawText, density: Density): Offset {
    val position = textItem.position

    val bottomLeft = floatArrayOf(position.x, position.y)

    textItem.transformMatrix.mapPoints(bottomLeft)

    return Offset(bottomLeft[0], bottomLeft[1])
}

fun getNewPositionByTranslation(
    textItem: DrawText,
    density: Density,
    translationMatrix: Matrix
): Offset {
    val position = textItem.position

    val bottomLeft = floatArrayOf(position.x, position.y)

    translationMatrix.mapPoints(bottomLeft)

    return Offset(bottomLeft[0], bottomLeft[1])
}

fun getNewPositionByTranslation(point: Offset, translationMatrix: Matrix): Offset {
    val bottomLeft = floatArrayOf(point.x, point.y)

    translationMatrix.mapPoints(bottomLeft)

    return Offset(bottomLeft[0], bottomLeft[1])
}

fun inverseMapPoint(matrix: Matrix, point: FloatArray): FloatArray {
    val inverseMatrix = Matrix()
    if (matrix.invert(inverseMatrix)) {
        val mappedPoint = FloatArray(2)
        inverseMatrix.mapPoints(mappedPoint, point)
        return mappedPoint
    } else {
        throw IllegalArgumentException("Matrix inversion failed")
    }
}

fun isPointInsideResizeHandleDrawObject(
    point: Offset,
    dragObject: DrawObject,
    density: Density
): Corner? {
    if (dragObject.drawObjectType == DrawMode.Text) {
        return isPointInsideResizeHandleText(point, (dragObject as DrawText), density)
    }
    if (dragObject.drawObjectType == DrawMode.Line) {
        val line = (dragObject as DrawLine)
        return isPointInsideResizeHandleLine(point, line.bounds, line.transformMatrix)
    }

    return null
}

fun isPointInsideResizeHandleLine(
    point: Offset,
    boundingBox: CustomRect,
    transformMatrix: Matrix
): Corner? {
    val topLeft = boundingBox.topLeft.toFloatArray()
    val topRight = boundingBox.topRight.toFloatArray()
    val bottomRight = boundingBox.bottomRight.toFloatArray()
    val bottomLeft = boundingBox.bottomLeft.toFloatArray()

    Log.d("check---", "isPointInsideResizeHandleLine: bottomLeft - ${boundingBox.bottomLeft}")

    transformMatrix.mapPoints(topLeft)
    transformMatrix.mapPoints(topRight)
    transformMatrix.mapPoints(bottomRight)
    transformMatrix.mapPoints(bottomLeft)

    val radius = 20

    if (Offset(topLeft[0], topLeft[1]).minus(point).getDistanceSquared() < radius * radius) {
        return Corner.TopLeft
    }
    if (Offset(topRight[0], topRight[1]).minus(point).getDistanceSquared() < radius * radius) {
        return Corner.TopRight
    }
    if (Offset(bottomRight[0], bottomRight[1]).minus(point)
            .getDistanceSquared() < radius * radius
    ) {
        return Corner.BottomRight
    }
    if (Offset(bottomLeft[0], bottomLeft[1]).minus(point).getDistanceSquared() < radius * radius) {
        return Corner.BottomLeft
    }

    return null

}

fun isPointInsideResizeHandleText(point: Offset, textItem: DrawText, density: Density): Corner? {
    val position = textItem.position
    val textBounds = Rect()
    val paint = Paint().apply {
        textSize = with(density) { textItem.fontSize.dp.toPx() }
    }
    paint.getTextBounds(textItem.text, 0, textItem.text.length, textBounds)
    val textWidth = paint.measureText(textItem.text)
    val textHeight = textBounds.height().toFloat()

    val topLeft = floatArrayOf(position.x, position.y - textHeight)
    val topRight = floatArrayOf(position.x + textWidth, position.y - textHeight)
    val bottomRight = floatArrayOf(position.x + textWidth, position.y)
    val bottomLeft = floatArrayOf(position.x, position.y)

    // Apply the transformation matrix
    textItem.transformMatrix.mapPoints(topLeft)
    textItem.transformMatrix.mapPoints(topRight)
    textItem.transformMatrix.mapPoints(bottomRight)
    textItem.transformMatrix.mapPoints(bottomLeft)

    val radius = 20

    if (Offset(topLeft[0], topLeft[1]).minus(point).getDistanceSquared() < radius * radius) {
        return Corner.TopLeft
    }
    if (Offset(topRight[0], topRight[1]).minus(point).getDistanceSquared() < radius * radius) {
        return Corner.TopRight
    }
    if (Offset(bottomRight[0], bottomRight[1]).minus(point)
            .getDistanceSquared() < radius * radius
    ) {
        return Corner.BottomRight
    }
    if (Offset(bottomLeft[0], bottomLeft[1]).minus(point).getDistanceSquared() < radius * radius) {
        return Corner.BottomLeft
    }

    return null
}


fun isPointInsideDrawObject(point: Offset, drawObject: DrawObject, density: Density): Boolean {
    if (drawObject.drawObjectType == DrawMode.Text) {
        return isPointInsideText(point, (drawObject as DrawText), density)
    }
    if (drawObject.drawObjectType == DrawMode.Line) {
        return isPointInsideLineArea(point, (drawObject as DrawLine))
    }

    return false
}

fun isPointInsideLineArea(point: Offset, drawLine: DrawLine): Boolean {
    val topRight = drawLine.bounds.topRight.toFloatArray()
    val bottomLeft = drawLine.bounds.bottomLeft.toFloatArray()

    drawLine.transformMatrix.mapPoints(topRight)
    drawLine.transformMatrix.mapPoints(bottomLeft)
    val xRange = floatArrayOf(bottomLeft[0], topRight[0])
    xRange.sort()

    val yRange = floatArrayOf(bottomLeft[1], topRight[1])
    yRange.sort()

    val inX = point.x in xRange[0]..xRange[1]
    val inY = point.y in yRange[0]..yRange[1]

    return inX && inY
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

    val topRight = floatArrayOf(position.x + textWidth, position.y - textHeight)
    val bottomLeft = floatArrayOf(position.x, position.y)

    textItem.transformMatrix.mapPoints(topRight)
    textItem.transformMatrix.mapPoints(bottomLeft)

    val xRange = floatArrayOf(bottomLeft[0], topRight[0])
    xRange.sort()

    val yRange = floatArrayOf(bottomLeft[1], topRight[1])
    yRange.sort()

    val inX = point.x in xRange[0]..xRange[1]
    val inY = point.y in yRange[0]..yRange[1]

    return inX && inY
}

enum class Corner {
    TopLeft,
    TopRight,
    BottomRight,
    BottomLeft,
}

fun calculateBoundingBox(lines: List<Line>): CustomRect {
    var minX = Float.MAX_VALUE
    var maxX = Float.MIN_VALUE
    var minY = Float.MAX_VALUE
    var maxY = Float.MIN_VALUE

    for (line in lines) {
        if (line.start.x < minX) minX = line.start.x
        if (line.start.x > maxX) maxX = line.start.x
        if (line.start.y < minY) minY = line.start.y
        if (line.start.y > maxY) maxY = line.start.y

        if (line.end.x < minX) minX = line.end.x
        if (line.end.x > maxX) maxX = line.end.x
        if (line.end.y < minY) minY = line.end.y
        if (line.end.y > maxY) maxY = line.end.y
    }


    android.util.Log.d("check---", "calculateBoundingBox: minX - $minX")
    android.util.Log.d("check---", "calculateBoundingBox: maxX - $maxX")
    android.util.Log.d("check---", "calculateBoundingBox: minY - $minY")
    android.util.Log.d("check---", "calculateBoundingBox: maxY - $maxY")

    return CustomRect(Offset(minX, maxY), Offset(maxX, minY))
}
