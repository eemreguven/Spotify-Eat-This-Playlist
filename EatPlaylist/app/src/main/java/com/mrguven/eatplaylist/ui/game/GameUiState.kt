package com.mrguven.eatplaylist.ui.game

import com.mrguven.eatplaylist.data.model.SnakeUnit

data class GameUiState(
    val cellSizePx: Float = 0f,
    val snakeUnits: List<SnakeUnit> = listOf(SnakeUnit()),
    val targetSnakeUnit: SnakeUnit = SnakeUnit()
)