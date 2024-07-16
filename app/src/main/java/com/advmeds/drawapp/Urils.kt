package com.advmeds.drawapp

import android.graphics.Bitmap
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
import androidx.compose.ui.graphics.asAndroidBitmap
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
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sign

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


fun getNewPositionByTranslation(point: Offset, translationMatrix: Matrix): Offset {
    val bottomLeft = floatArrayOf(point.x, point.y)

    translationMatrix.mapPoints(bottomLeft)

    return Offset(bottomLeft[0], bottomLeft[1])
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

    transformMatrix.mapPoints(topLeft)
    transformMatrix.mapPoints(topRight)
    transformMatrix.mapPoints(bottomRight)
    transformMatrix.mapPoints(bottomLeft)

    val radius = 40

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

    return CustomRect(Offset(minX, maxY), Offset(maxX, minY))
}

fun Color.toArgb(): Int {
    return android.graphics.Color.argb(
        (alpha * 255).toInt(),
        (red * 255).toInt(),
        (green * 255).toInt(),
        (blue * 255).toInt()
    )
}

fun createBitmapFromLines(
    image: ImageBitmap,
    canvasWidth: Float,
    canvasHeight: Float,
    drawBunch: DrawBunch,
    density: Density,
): Bitmap {
    val bitmap = Bitmap.createBitmap(
        canvasWidth.toInt(), canvasHeight.toInt(), Bitmap.Config.ARGB_8888
    )

    val canvas = android.graphics.Canvas(bitmap)
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
