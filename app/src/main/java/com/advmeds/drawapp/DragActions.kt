package com.advmeds.drawapp

import android.graphics.Matrix
import android.util.Log
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.unit.Density

fun onTab(
    offset: Offset,
    density: Density,
    currentTextSize: MutableState<Int?>,
    selectTool: MutableState<Any?>,
    afTextTool: MutableState<Any?>,
    straightLine: MutableState<Any?>,
    setTextDialogIsEnable: (Boolean) -> Unit,
    drawBunch: MutableState<DrawBunch>,
    addAfText: () -> Unit,
    addStraightLine: () -> Unit,
    setTextPosition: () -> Unit,
    addCurrentDragObject: (DrawObject) -> Unit,
) {
    if (currentTextSize.value != null) {
        setTextPosition.invoke()
        setTextDialogIsEnable.invoke(true)
    }

    if (afTextTool.value != null) {
        addAfText.invoke()
    }

    if (straightLine.value != null) {
        addStraightLine.invoke()
    }

    if (selectTool.value != null) {
        drawBunch.value
            .asReversed()
            .forEach { item ->
                val isTouched = isPointInsideDrawObject(
                    point = offset, drawObject = item, density = density
                )

                val corners = isPointInsideResizeHandleDrawObject(
                    point = offset,
                    dragObject = item,
                    density = density
                )

                if (isTouched || corners != null) {
                    addCurrentDragObject.invoke(item)
                    return
                }
            }
    }
}

fun onDragStart(
    selectTool: MutableState<Any?>,
    drawBunch: MutableState<DrawBunch>,
    offset: Offset,
    density: Density,
    initResize: (item: DrawObject, corner: Corner?) -> Unit,
    addCurrentDragObject: (DrawObject) -> Unit
) {


//    Log.d("check---", "onDragStart: (selectTool.value  ${selectTool.value}")

    if (selectTool.value != null) {

//        Log.d("check---", "onDragStart: drawBunch  ${drawBunch.value}")
        drawBunch.value
            .asReversed()
            .forEach { item ->
                val isTouched = isPointInsideDrawObject(
                    point = offset, drawObject = item, density = density
                )

                val corner = isPointInsideResizeHandleDrawObject(
                    point = offset,
                    dragObject = item,
                    density = density
                )

//                Log.d("check---", "onDragStart: isTouched $isTouched corner - $corner")

                if (corner != null) {
                    initResize.invoke(item, corner)
                } else if (isTouched) {
                    addCurrentDragObject.invoke(item)
                }

                return
            }
    }
}

fun onDrag(
    change: PointerInputChange,
    selectTool: MutableState<Any?>,
    density: Density,
    isResizing: MutableState<Boolean>,
    currentDragObject: SnapshotStateList<DrawObject>,
    currentResizeCorner: MutableState<Corner?>,
    currentSize: MutableState<Int?>,
    updateDragData: (list: DrawBunch) -> Unit,
    addNewLine: () -> Unit
) {
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
                val translateMatrix = Matrix()

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

            val matrix = Matrix()

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

        updateDragData.invoke(newList)
    }

    if (currentSize.value != null) {
        addNewLine.invoke()
    }
}

fun completeDrag(
    selectTool: MutableState<Any?>,
    drawBunch: MutableState<DrawBunch>,
    currentDragObject: SnapshotStateList<DrawObject>,
    completionOfDrag: (tempDrawBunch: DrawBunch) -> Unit,
    currentSize: MutableState<Int?>,
    completeDrawLine: () -> Unit,
) {
    if (selectTool.value != null) {
        val tempDrawBunch = drawBunch.value.toMutableList()

        currentDragObject.forEach { dragged ->

            val item = drawBunch.value.find { dragged.id == it.id }
            val index = drawBunch.value.indexOf(item)

            if (index > -1) {
                tempDrawBunch[index] = dragged
            }
        }

        completionOfDrag.invoke(tempDrawBunch)
    }

    if (currentSize.value != null) {
        completeDrawLine.invoke()
    }
}