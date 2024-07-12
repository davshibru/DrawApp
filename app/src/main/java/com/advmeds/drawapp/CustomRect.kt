package com.advmeds.drawapp

import android.graphics.Matrix
import androidx.compose.ui.geometry.Offset

data class CustomRect(
    var topLeft: Offset,
    var topRight: Offset,
    var bottomLeft: Offset,
    var bottomRight: Offset,
    val matrix: Matrix = Matrix(),
) {

    constructor(_bottomLeft: Offset, _topRight : Offset) : this(
        topLeft = Offset(_bottomLeft.x, _topRight.y),
        topRight = _topRight,
        bottomRight = Offset(_topRight.x, _bottomLeft.y),
        bottomLeft = _bottomLeft,
        matrix = Matrix(),
    )

    val width: Float
        get() = topRight.x - topLeft.x

    val height: Float
        get() = bottomLeft.y - topLeft.y

    fun moveTopLeft(offset: Offset) {
        topLeft = offset
        topRight = Offset(topRight.x, offset.y)
        bottomLeft = Offset(offset.x, bottomLeft.y)
    }

    fun moveTopRight(offset: Offset) {
        topRight = offset
        topLeft = Offset(topLeft.x, offset.y)
        bottomRight = Offset(offset.x, bottomRight.y)
    }

    fun moveBottomLeft(offset: Offset) {
        bottomLeft = offset
        topLeft = Offset(offset.x, topLeft.y)
        bottomRight = Offset(bottomRight.x, offset.y)
    }

    fun moveBottomRight(offset: Offset) {
        bottomRight = offset
        topRight = Offset(offset.x, topRight.y)
        bottomLeft = Offset(bottomLeft.x, offset.y)
    }


    fun updateMatrix() {
        matrix.reset()
        matrix.setPolyToPoly(
            floatArrayOf(0f, 0f, width, 0f, 0f, height, width, height),
            0,
            floatArrayOf(topLeft.x, topLeft.y, topRight.x, topRight.y, bottomLeft.x, bottomLeft.y, bottomRight.x, bottomRight.y),
            0,
            4
        )
    }
}