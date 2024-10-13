package com.mrguven.eatplaylist.data.model

import android.graphics.Bitmap

data class SnakeUnit(
    var index: Int = 0,
    var direction: Direction = Direction.RIGHT,
    var previousDirection: Direction = Direction.RIGHT,
    var rotationDirection: RotationDirection = RotationDirection.NO_ROTATION,
    var imageBitmap: Bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888),
)