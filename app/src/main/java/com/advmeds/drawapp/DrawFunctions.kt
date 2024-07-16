package com.advmeds.drawapp

import android.graphics.Paint
import android.graphics.Rect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


fun DrawScope.drawBackGroundImage(
    canvasSize: Size,
    image: ImageBitmap
) {
    drawIntoCanvas { canvas ->
        val dest = Rect(0, 0, canvasSize.width.toInt(), canvasSize.height.toInt())
        val paint = Paint()
        paint.isFilterBitmap = true
        canvas.nativeCanvas.drawBitmap(image.asAndroidBitmap(), null, dest, paint)

    }
}

fun DrawScope.drawStaticDrawObject(
    drawBunch: MutableState<DrawBunch>,
    currentDragObject: SnapshotStateList<DrawObject>,
    density: Density,
) {
    drawBunch.value.forEach { bunch ->

        if (currentDragObject.map { it.id }.contains(bunch.id)) {
            return@forEach
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
}

fun DrawScope.drawCurrentDrawLine(currentLine: SnapshotStateList<Line>) {
    currentLine.forEach { line ->
        drawLine(
            color = line.color,
            start = line.start,
            end = line.end,
            strokeWidth = line.strokeWidth.toPx(),
            cap = StrokeCap.Round
        )
    }
}


fun DrawScope.drawCurrentGragAndMoveDrawObjects(
    currentDragObject: SnapshotStateList<DrawObject>,
    density: Density,
    isResizing: MutableState<Boolean>
) {
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