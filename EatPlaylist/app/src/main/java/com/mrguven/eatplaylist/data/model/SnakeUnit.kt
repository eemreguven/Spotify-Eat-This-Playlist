package com.mrguven.eatplaylist.data.model

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Offset

data class SnakeUnit(
    var index: Int = 0,
    var position: Offset = Offset(0f, 0f),
    var previousPosition: Offset = Offset(0f, 0f),
    var direction: Direction = Direction.RIGHT,
    var previousDirection: Direction = Direction.RIGHT,
    var rotationDirection: RotationDirection = RotationDirection.NO_ROTATION,
    var rotationAngle: Float = 0f,
    var pivotOffset: Offset = Offset(0f, 0f),
    var imageBitmap: Bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
)
