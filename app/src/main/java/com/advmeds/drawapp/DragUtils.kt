package com.advmeds.drawapp

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.sign

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


internal val HorizontalPointerDirectionConfigCustom = object : PointerDirectionConfigCustom {
    override fun mainAxisDelta(offset: Offset): Float = offset.x
    override fun crossAxisDelta(offset: Offset): Float = offset.y
    override fun offsetFromChanges(mainChange: Float, crossChange: Float): Offset =
        Offset(mainChange, crossChange)
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
private val defaultTouchSlop = 18.dp
private val mouseToTouchSlopRatio = mouseSlop / defaultTouchSlop