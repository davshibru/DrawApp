package com.advmeds.drawapp

import androidx.compose.ui.geometry.Offset

data class CustomRect(
    var topLeft: Offset,
    var topRight: Offset,
    var bottomLeft: Offset,
    var bottomRight: Offset,
) {

    constructor() : this(
        topLeft = Offset.Zero,
        topRight = Offset.Zero,
        bottomLeft = Offset.Zero,
        bottomRight = Offset.Zero,
    )

    constructor(_bottomLeft: Offset, _topRight: Offset) : this(
        topLeft = Offset(_bottomLeft.x, _topRight.y),
        topRight = _topRight,
        bottomRight = Offset(_topRight.x, _bottomLeft.y),
        bottomLeft = _bottomLeft,
    )

    val width: Float
        get() = topRight.x - topLeft.x

    val height: Float
        get() = bottomLeft.y - topLeft.y

    fun contains(x: Float, y: Float): Boolean {
        val xRange = floatArrayOf(bottomLeft.x, topRight.x)
        xRange.sort()

        val yRange = floatArrayOf(bottomLeft.y, topRight.y)
        yRange.sort()


        val inX = x in xRange[0]..xRange[1]
        val inY = y in yRange[0]..yRange[1]

//    return inLeft && inTop && inRight && inBottom
        return inX && inY
    }


}

fun Offset.toFloatArray() :  FloatArray = floatArrayOf(x, y)