package com.advmeds.drawapp

import android.graphics.Matrix
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class DrawMode {
    Text,
    Line,
    Select,
    Clear,
}


interface DrawObject {
    val id: Int
    val drawObjectType: DrawMode

    var transformMatrix: Matrix
    var cumulativeScaleX: Float
    var cumulativeScaleY: Float
    var cumulativeTranslationX: Float
    var cumulativeTranslationY: Float
}

data class DrawLine(
    override val id: Int,
    override val drawObjectType: DrawMode = DrawMode.Line,
    val list: List<Line>,
    val bounds: CustomRect = CustomRect(),
    override var transformMatrix: Matrix = Matrix(),
    override var cumulativeScaleX: Float = 1f,
    override var cumulativeScaleY: Float = 1f,
    override var cumulativeTranslationX: Float = 0f,
    override var cumulativeTranslationY: Float = 0f,
) : DrawObject

data class DrawText(
    override val id: Int,
    override val drawObjectType: DrawMode = DrawMode.Text,
    val text: String,
    val color: Color,
    var position: Offset,
    val fontSize: Int,
    override var transformMatrix: Matrix = Matrix(),
    override var cumulativeScaleX: Float = 1f,
    override var cumulativeScaleY: Float = 1f,
    override var cumulativeTranslationX: Float = 0f,
    override var cumulativeTranslationY: Float = 0f,
) : DrawObject

data class DrawClear(
    override val id: Int = -1,
    override val drawObjectType: DrawMode = DrawMode.Clear,
    override var transformMatrix: Matrix = Matrix(),
    override var cumulativeScaleX: Float = 1f,
    override var cumulativeScaleY: Float = 1f,
    override var cumulativeTranslationX: Float = 0f,
    override var cumulativeTranslationY: Float = 0f,
) : DrawObject

data class Line(
    var start: Offset,
    var end: Offset,
    val color: Color = Color.Black,
    val strokeWidth: Dp = 1.dp
)
