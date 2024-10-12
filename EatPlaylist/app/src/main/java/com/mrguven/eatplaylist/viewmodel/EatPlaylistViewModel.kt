package com.mrguven.eatplaylist.viewmodel

import android.graphics.Bitmap
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
        _snakeUnits.add(_targetSnakeUnit.value)
    }

    private fun updateSnake() {
        if (_snakeUnits.isEmpty()) return

        val head = _snakeUnits.first()
        val newHeadDirection = _currentDirection.value
        val newHeadIndex = calculateNewIndex(head.index, newHeadDirection)
        _snakeUnits[0] = head.copy(
            previousDirection = head.direction,
            direction = newHeadDirection,
            index = newHeadIndex
        )

        var previousUnitIndex = head.index
        var previousUnitDirection = newHeadDirection

        for (i in 1 until _snakeUnits.size) {
            val currentUnit = _snakeUnits[i]

            _snakeUnits[i] = currentUnit.copy(
                previousDirection = currentUnit.direction,
                index = previousUnitIndex,
                direction = previousUnitDirection
            )

            previousUnitIndex = currentUnit.index
            previousUnitDirection = currentUnit.direction
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