package com.mrguven.eatplaylist.viewmodel

import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mrguven.eatplaylist.data.model.Direction
import com.mrguven.eatplaylist.data.model.RotationDirection
import com.mrguven.eatplaylist.data.model.SnakeUnit
import kotlinx.coroutines.launch

class EatPlaylistViewModel : ViewModel() {
    private var _columns = mutableIntStateOf(0)
    val columns: MutableIntState = _columns

    private var _rows = mutableIntStateOf(0)
    private val rows: MutableIntState = _rows

    private var _cellSize = mutableStateOf(32.dp)
    val cellSize = _cellSize

    private val _snakeUnits = mutableStateListOf<SnakeUnit>()
    val snakeUnits: List<SnakeUnit> = _snakeUnits

    private var _currentDirection = mutableStateOf(Direction.RIGHT)

    private var _targetSnakeUnit = mutableStateOf(SnakeUnit())
    val targetSnakeUnit: MutableState<SnakeUnit> = _targetSnakeUnit

    fun generateNewTarget(newBitmap: Bitmap) {
        viewModelScope.launch {
            if (!newBitmap.sameAs(_targetSnakeUnit.value.imageBitmap)) {
                val targetIndex = generateNewTargetIndex()
                val newSnakeUnit =
                    SnakeUnit(index = targetIndex, imageBitmap = newBitmap)
                _targetSnakeUnit.value = newSnakeUnit
            }
        }
    }

    private fun generateNewTargetIndex(): Int {
        return (0 until rows.intValue * columns.intValue).filterNot { index ->
            snakeUnits.any { it.index == index }
        }.random()
    }

    fun changeDirection(newDirection: Direction) {
        val currentDirection = _currentDirection.value

        fun isOppositeDirection(dir1: Direction, dir2: Direction): Boolean {
            return (dir1 == Direction.UP && dir2 == Direction.DOWN) ||
                    (dir1 == Direction.DOWN && dir2 == Direction.UP) ||
                    (dir1 == Direction.LEFT && dir2 == Direction.RIGHT) ||
                    (dir1 == Direction.RIGHT && dir2 == Direction.LEFT)
        }

        if (_snakeUnits.size == 1 || !isOppositeDirection(newDirection, currentDirection)) {
            _currentDirection.value = newDirection
        }
    }

    fun moveSnake() {
        updateSnake()

        val head = snakeUnits.firstOrNull()
        if (head != null && head.index == targetSnakeUnit.value.index) {
            growSnake()
        }
    }

    private fun growSnake() {
        val lastUnit = _snakeUnits.last()
        val newUnit = _targetSnakeUnit.value.copy()
        newUnit.previousDirection = lastUnit.direction
        newUnit.direction = lastUnit.direction
        _snakeUnits.add(newUnit)
    }

    private fun updateSnake() {
        if (_snakeUnits.isEmpty()) return
        val previousHead = _snakeUnits.first()
        updateSnakeHead()
        updateSnakeBody(previousHead)
    }

    private fun updateSnakeHead() {
        val head = _snakeUnits.first()
        val newHeadDirection = _currentDirection.value
        val newHeadIndex = calculateNewIndex(head.index, newHeadDirection)

        _snakeUnits[0] = head.copy(
            previousDirection = head.direction,
            direction = newHeadDirection,
            rotationDirection = detect90DegreeTurn(head.direction, newHeadDirection),
            index = newHeadIndex
        )
    }

    private fun updateSnakeBody(previousHead: SnakeUnit) {
        var previousUnitIndex = previousHead.index
        var previousUnitDirection = _currentDirection.value

        for (i in 1 until _snakeUnits.size) {
            val currentUnit = _snakeUnits[i]
            val rotatedBitmap = rotateBitmapBasedOnDirection(currentUnit)

            _snakeUnits[i] = currentUnit.copy(
                previousDirection = currentUnit.direction,
                direction = previousUnitDirection,
                rotationDirection = detect90DegreeTurn(
                    currentUnit.previousDirection,
                    currentUnit.direction
                ),
                index = previousUnitIndex,
                imageBitmap = rotatedBitmap
            )

            previousUnitIndex = currentUnit.index
            previousUnitDirection = currentUnit.direction
        }
    }

    private fun rotateBitmapBasedOnDirection(unit: SnakeUnit): Bitmap {
        return when (unit.rotationDirection) {
            RotationDirection.CLOCKWISE -> rotateBitmap(unit.imageBitmap, 90f)
            RotationDirection.COUNTER_CLOCKWISE -> rotateBitmap(unit.imageBitmap, -90f)
            RotationDirection.NO_ROTATION -> unit.imageBitmap
        }
    }

    private fun rotateBitmap(bitmap: Bitmap, angle: Float): Bitmap {
        val matrix = Matrix().apply { postRotate(angle) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun detect90DegreeTurn(
        previousPreviousDirection: Direction,
        previousDirection: Direction
    ): RotationDirection {
        return when (previousPreviousDirection) {
            Direction.UP -> when (previousDirection) {
                Direction.RIGHT -> RotationDirection.CLOCKWISE
                Direction.LEFT -> RotationDirection.COUNTER_CLOCKWISE
                else -> RotationDirection.NO_ROTATION
            }

            Direction.RIGHT -> when (previousDirection) {
                Direction.DOWN -> RotationDirection.CLOCKWISE
                Direction.UP -> RotationDirection.COUNTER_CLOCKWISE
                else -> RotationDirection.NO_ROTATION
            }

            Direction.DOWN -> when (previousDirection) {
                Direction.LEFT -> RotationDirection.CLOCKWISE
                Direction.RIGHT -> RotationDirection.COUNTER_CLOCKWISE
                else -> RotationDirection.NO_ROTATION
            }

            Direction.LEFT -> when (previousDirection) {
                Direction.UP -> RotationDirection.CLOCKWISE
                Direction.DOWN -> RotationDirection.COUNTER_CLOCKWISE
                else -> RotationDirection.NO_ROTATION
            }
        }
    }

    private fun calculateNewIndex(index: Int, direction: Direction): Int {
        return when (direction) {
            Direction.RIGHT -> calculateRightMove(index)
            Direction.LEFT -> calculateLeftMove(index)
            Direction.UP -> calculateUpMove(index)
            Direction.DOWN -> calculateDownMove(index)
        }
    }

    private fun calculateRightMove(index: Int): Int {
        return if ((index + 1) % columns.intValue == 0) index - (columns.intValue - 1) else index + 1
    }

    private fun calculateLeftMove(index: Int): Int {
        return if (index % columns.intValue == 0) index + (columns.intValue - 1) else index - 1
    }

    private fun calculateUpMove(index: Int): Int {
        return if (index - columns.intValue < 0) (columns.intValue * rows.intValue) - (columns.intValue - index) else index - columns.intValue
    }

    private fun calculateDownMove(index: Int): Int {
        return if (index + columns.intValue < (rows.intValue * columns.intValue)) index + columns.intValue else (index + columns.intValue) % (columns.intValue * rows.intValue)
    }

    fun initUiData(rows: Int, columns: Int, cellSize: Dp) {
        _rows.intValue = rows
        _columns.intValue = columns
        _cellSize.value = cellSize
        initSnakeData()
    }

    private fun initSnakeData() {
        val snakeHead = SnakeUnit()
        _snakeUnits.clear()
        _snakeUnits.add(snakeHead)
    }
}