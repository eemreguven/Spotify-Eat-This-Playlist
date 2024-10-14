package com.mrguven.eatplaylist.viewmodel

import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mrguven.eatplaylist.data.model.Direction
import com.mrguven.eatplaylist.data.model.RotationDirection
import com.mrguven.eatplaylist.data.model.SnakeUnit
import kotlinx.coroutines.launch

class EatPlaylistViewModel : ViewModel() {
    private var _columns = mutableIntStateOf(0)
    private var _rows = mutableIntStateOf(0)

    private var _cellSizePx = mutableFloatStateOf(0f)
    val cellSizePx = _cellSizePx

    private var _currentDirection = mutableStateOf(Direction.RIGHT)

    private val _snakeUnits = mutableStateListOf<SnakeUnit>()
    val snakeUnits: List<SnakeUnit> = _snakeUnits

    private var _targetSnakeUnit = mutableStateOf(SnakeUnit())
    val targetSnakeUnit: MutableState<SnakeUnit> = _targetSnakeUnit

    fun generateNewTarget(newBitmap: Bitmap) {
        viewModelScope.launch {
            if (!newBitmap.sameAs(_targetSnakeUnit.value.imageBitmap)) {
                val targetIndex = generateNewTargetIndex()
                val position = calculateCanvasPosition(targetIndex)
                val newSnakeUnit =
                    SnakeUnit(
                        index = targetIndex,
                        imageBitmap = newBitmap,
                        position = position,
                        previousPosition = position,
                        pivotOffset = position
                    )
                _targetSnakeUnit.value = newSnakeUnit
            }
        }
    }

    private fun generateNewTargetIndex(): Int {
        return (0 until _rows.intValue * _columns.intValue).filterNot { index ->
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
        val newDirection = _currentDirection.value
        val newHeadIndex = calculateNewIndex(head.index, newDirection)

        val newPosition = calculateCanvasPosition(newHeadIndex)
        val paddingAdjustedPosition =
            calculatePaddingAdjustedPosition(newPosition, newDirection)
        val previousPosition = calculatePreviousPosition(newDirection, paddingAdjustedPosition)
        val previousDirection = head.direction

        val rotationDirection = detect90DegreeTurn(previousDirection, newDirection)
        val rotationAngle = calculateRotationAngle(rotationDirection)
        val pivotOffset =
            calculatePivotOffset(rotationDirection, newDirection, paddingAdjustedPosition)

        _snakeUnits[0] = head.copy(
            index = newHeadIndex,
            position = paddingAdjustedPosition,
            previousPosition = previousPosition,
            direction = newDirection,
            previousDirection = previousDirection,
            rotationDirection = rotationDirection,
            rotationAngle = rotationAngle,
            pivotOffset = pivotOffset
        )
    }

    private fun updateSnakeBody(previousHead: SnakeUnit) {
        var newIndex = previousHead.index
        var newDirection = _currentDirection.value

        for (i in 1 until _snakeUnits.size) {
            val currentUnit = _snakeUnits[i]

            val previousDirection = currentUnit.direction
            val newPosition = calculateCanvasPosition(newIndex)
            val previousPosition = calculatePreviousPosition(previousDirection, newPosition)
            val rotationDirection =
                detect90DegreeTurn(currentUnit.previousDirection, currentUnit.direction)

            val rotationAngle = calculateRotationAngle(rotationDirection)
            val pivotOffset =
                calculatePivotOffset(rotationDirection, previousDirection, newPosition)
            val rotatedBitmap = rotateBitmapBasedOnDirection(currentUnit)

            _snakeUnits[i] = currentUnit.copy(
                index = newIndex,
                position = newPosition,
                previousPosition = previousPosition,
                direction = newDirection,
                previousDirection = previousDirection,
                rotationDirection = rotationDirection,
                imageBitmap = rotatedBitmap,
                pivotOffset = pivotOffset,
                rotationAngle = rotationAngle
            )
            newIndex = currentUnit.index
            newDirection = currentUnit.direction
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

    private fun calculateCanvasPosition(index: Int): Offset {
        val x = (index % _columns.intValue) * cellSizePx.floatValue
        val y = (index / _columns.intValue) * cellSizePx.floatValue
        return Offset(x, y)
    }

    private fun calculatePaddingAdjustedPosition(
        position: Offset,
        direction: Direction
    ): Offset {
        val padding = _cellSizePx.floatValue / 10
        return when (direction) {
            Direction.RIGHT -> Offset(position.x + padding, position.y)
            Direction.LEFT -> Offset(position.x - padding, position.y)
            Direction.UP -> Offset(position.x, position.y - padding)
            Direction.DOWN -> Offset(position.x, position.y + padding)
        }
    }

    private fun calculateRotationAngle(rotationDirection: RotationDirection): Float {
        return when (rotationDirection) {
            RotationDirection.CLOCKWISE -> 90f
            RotationDirection.COUNTER_CLOCKWISE -> -90f
            RotationDirection.NO_ROTATION -> 0f
        }
    }

    private fun calculatePreviousPosition(
        previousDirection: Direction, position: Offset,
    ): Offset {
        return when (previousDirection) {
            Direction.RIGHT -> Offset(position.x - cellSizePx.floatValue, position.y)
            Direction.DOWN -> Offset(position.x, position.y - cellSizePx.floatValue)
            Direction.LEFT -> Offset(position.x + cellSizePx.floatValue, position.y)
            Direction.UP -> Offset(position.x, position.y + cellSizePx.floatValue)
        }
    }

    private fun calculatePivotOffset(
        rotationDirection: RotationDirection,
        previousDirection: Direction,
        position: Offset
    ): Offset {
        return when (rotationDirection) {
            RotationDirection.CLOCKWISE -> when (previousDirection) {
                Direction.RIGHT -> Offset(position.x, position.y + cellSizePx.floatValue)
                Direction.DOWN -> Offset(position.x, position.y)
                Direction.LEFT -> Offset(position.x + cellSizePx.floatValue, position.y)
                Direction.UP -> Offset(
                    position.x + cellSizePx.floatValue,
                    position.y + cellSizePx.floatValue
                )
            }

            RotationDirection.COUNTER_CLOCKWISE -> when (previousDirection) {
                Direction.LEFT -> Offset(
                    position.x + cellSizePx.floatValue,
                    position.y + cellSizePx.floatValue
                )

                Direction.DOWN -> Offset(position.x + cellSizePx.floatValue, position.y)
                Direction.RIGHT -> Offset(position.x, position.y)
                Direction.UP -> Offset(position.x, position.y + cellSizePx.floatValue)
            }

            RotationDirection.NO_ROTATION -> when (previousDirection) {
                Direction.RIGHT -> Offset(position.x, position.y)
                Direction.DOWN -> Offset(position.x, position.y)
                Direction.LEFT -> Offset(position.x, position.y)
                Direction.UP -> Offset(position.x, position.y)
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
        return if ((index + 1) % _columns.intValue == 0) index - (_columns.intValue - 1) else index + 1
    }

    private fun calculateLeftMove(index: Int): Int {
        return if (index % _columns.intValue == 0) index + (_columns.intValue - 1) else index - 1
    }

    private fun calculateUpMove(index: Int): Int {
        return if (index - _columns.intValue < 0) (_columns.intValue * _rows.intValue) - (_columns.intValue - index) else index - _columns.intValue
    }

    private fun calculateDownMove(index: Int): Int {
        return if (index + _columns.intValue < (_rows.intValue * _columns.intValue)) index + _columns.intValue else (index + _columns.intValue) % (_columns.intValue * _rows.intValue)
    }

    fun initGridData(rows: Int, columns: Int, cellSizePx: Float) {
        _rows.intValue = rows
        _columns.intValue = columns
        _cellSizePx.floatValue = cellSizePx
        initSnakeData()
    }

    private fun initSnakeData() {
        val snakeHead = SnakeUnit()
        _snakeUnits.clear()
        _snakeUnits.add(snakeHead)
    }
}