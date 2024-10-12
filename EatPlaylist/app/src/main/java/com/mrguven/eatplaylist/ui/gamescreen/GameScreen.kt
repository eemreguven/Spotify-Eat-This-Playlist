package com.mrguven.eatplaylist.ui.gamescreen

import android.graphics.Bitmap
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.palette.graphics.Palette
import com.mrguven.eatplaylist.data.model.Direction
import com.mrguven.eatplaylist.data.model.SnakeUnit
import com.mrguven.eatplaylist.viewmodel.EatPlaylistViewModel
import kotlinx.coroutines.delay
import kotlin.math.absoluteValue

const val EFFECT_MILLISECOND = 300
const val ANIMATION_MILLISECOND = 500

@Composable
fun GameScreen(
    modifier: Modifier, viewModel: EatPlaylistViewModel, onCurrentSongEaten: () -> Unit
) {
    LaunchedEffect(viewModel.snakeUnits.size) {
        onCurrentSongEaten()
    }

    LaunchedEffect(viewModel.snakeUnits.first()) {
        delay(EFFECT_MILLISECOND.toLong())
        viewModel.moveSnake()
    }

    DrawGameScreen(modifier = modifier, viewModel = viewModel)
}

@Composable
fun DrawGameScreen(modifier: Modifier, viewModel: EatPlaylistViewModel) {
    val cellSize = viewModel.cellSize.value
    val horizontalFactor = viewModel.columns.intValue
    val targetSnakeUnit = viewModel.targetSnakeUnit.value

    val targetColor = remember(targetSnakeUnit) {
        calculateAverageColor(targetSnakeUnit.imageBitmap).copy(0.6f)
    }

    val backgroundColor by animateColorAsState(
        targetColor, animationSpec = tween(
            durationMillis = EFFECT_MILLISECOND, easing = LinearEasing
        ), label = "GameScreen Background Color"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor)
            .padding(5.dp)
            .handleDirectionDrag(viewModel)
    ) {
        if (targetSnakeUnit.index != viewModel.snakeUnits.first().index) {
            DrawTargetSnakeUnit(targetSnakeUnit, horizontalFactor, cellSize)
        }
        viewModel.snakeUnits.drop(1).forEach { snakeUnit ->
            DrawSnakeBody(snakeUnit, horizontalFactor, cellSize)
        }
        DrawSnakeHead(viewModel.snakeUnits.first(), horizontalFactor, cellSize)
    }
}

fun Modifier.handleDirectionDrag(
    viewModel: EatPlaylistViewModel, threshold: Float = 5f
): Modifier {
    return pointerInput(Unit) {
        detectDragGestures(onDragEnd = { }, onDragStart = { }, onDrag = { change, dragAmount ->
            val (x, y) = dragAmount
            when {
                x.absoluteValue > y.absoluteValue && x.absoluteValue > threshold -> {
                    viewModel.changeDirection(if (x > 0) Direction.RIGHT else Direction.LEFT)
                }

                y.absoluteValue > x.absoluteValue && y.absoluteValue > threshold -> {
                    viewModel.changeDirection(if (y > 0) Direction.DOWN else Direction.UP)
                }
            }
            change.consume()
        })
    }
}

@Composable
fun DrawSnakeHead(
    snakeUnit: SnakeUnit, horizontalFactor: Int, cellSize: Dp
) {
    val cellSizePx = dpToPx(cellSize)
    val (x, y) = calculateCanvasCoordinates(snakeUnit.index, horizontalFactor, cellSizePx)
    val (adjustedX, adjustedY) = getPaddingAdjustedCoordinates(x, y, snakeUnit.direction)

    Canvas(modifier = Modifier.fillMaxSize()) {
        val rect = Rect(Offset(adjustedX, adjustedY), Size(cellSizePx, cellSizePx))
        val trianglePath = createTrianglePath(rect, snakeUnit.direction)

        drawIntoCanvas { canvas ->
            canvas.drawOutline(
                outline = Outline.Generic(trianglePath),
                paint = Paint().apply {
                    color = Color.White
                    pathEffect = PathEffect.cornerPathEffect(rect.maxDimension / 5)
                })
        }
    }
}

@Composable
fun DrawSnakeBody(
    snakeUnit: SnakeUnit, horizontalFactor: Int, cellSize: Dp
) {
    val cellSizePx = dpToPx(cellSize)
    val (x, y) = calculateCanvasCoordinates(snakeUnit.index, horizontalFactor, cellSizePx)

    Canvas(modifier = Modifier.size(cellSize)) {
        val roundedRect = RoundRect(
            left = x,
            top = y,
            right = x + cellSizePx,
            bottom = y + cellSizePx,
            cornerRadius = CornerRadius(10f, 10f)
        )
        val path = Path().apply { addRoundRect(roundedRect, Path.Direction.Clockwise) }

        with(drawContext.canvas) {
            clipPath(path)
            drawImageRect(
                image = snakeUnit.imageBitmap.asImageBitmap(),
                paint = Paint(),
                dstOffset = IntOffset(x.toInt(), y.toInt()),
                dstSize = IntSize(cellSizePx.toInt(), cellSizePx.toInt()),
                srcOffset = IntOffset.Zero,
                srcSize = IntSize(snakeUnit.imageBitmap.width, snakeUnit.imageBitmap.height)
            )
        }
    }
}

@Composable
fun DrawTargetSnakeUnit(
    snakeUnit: SnakeUnit, horizontalFactor: Int, cellSize: Dp
) {
    val cellSizePx = dpToPx(cellSize)
    val (x, y) = calculateCanvasCoordinates(snakeUnit.index, horizontalFactor, cellSizePx)

    val scaleFraction by rememberInfiniteTransition(label = "Pulsating Animation").animateFloat(
        initialValue = 1f, targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "Scale Animation"
    )

    Canvas(modifier = Modifier.size(cellSize)) {
        val scaledCellSizePx = cellSizePx * scaleFraction
        val offset = Offset(
            x = x + (cellSize.toPx() - scaledCellSizePx) / 2,
            y = y + (cellSize.toPx() - scaledCellSizePx) / 2
        )
        snakeUnit.imageBitmap.let { imageBitmap ->
            val roundedRect = RoundRect(
                left = offset.x,
                top = offset.y,
                right = offset.x + scaledCellSizePx,
                bottom = offset.y + scaledCellSizePx,
                cornerRadius = CornerRadius(10f, 10f)
            )
            val path =
                Path().apply { addRoundRect(roundRect = roundedRect, Path.Direction.Clockwise) }
            with(drawContext.canvas) {
                clipPath(path)
                drawImageRect(
                    image = imageBitmap.asImageBitmap(),
                    paint = Paint(),
                    dstOffset = IntOffset(offset.x.toInt(), offset.y.toInt()),
                    dstSize = IntSize(scaledCellSizePx.toInt(), scaledCellSizePx.toInt()),
                    srcOffset = IntOffset.Zero,
                    srcSize = IntSize(imageBitmap.width, imageBitmap.height)
                )
            }
        }
    }
}

@Composable
fun getPaddingAdjustedCoordinates(x: Float, y: Float, direction: Direction): Pair<Float, Float> {
    val padding = dpToPx(dp = 2.dp)
    return when (direction) {
        Direction.RIGHT -> x + padding to y
        Direction.LEFT -> x - padding to y
        Direction.UP -> x to y - padding
        Direction.DOWN -> x to y + padding
    }
}

@Composable
fun dpToPx(dp: Dp): Float {
    return with(LocalDensity.current) { dp.toPx() }
}

fun calculateCanvasCoordinates(
    index: Int, horizontalFactor: Int, cellSizePx: Float
): Pair<Float, Float> {
    val x = (index % horizontalFactor) * cellSizePx
    val y = (index / horizontalFactor) * cellSizePx
    return Pair(x, y)
}

fun calculateAverageColor(bitmap: Bitmap): Color {
    val scaledBitmap = Bitmap.createScaledBitmap(bitmap, 16, 16, true)
    val palette = Palette.from(scaledBitmap).generate()
    val dominantColor = palette.getDominantColor(Color.Transparent.toArgb())
    return Color(dominantColor)
}

fun createTrianglePath(rect: Rect, direction: Direction): Path {
    return Path().apply {
        when (direction) {
            Direction.UP -> {
                moveTo(rect.topCenter)
                lineTo(rect.bottomRight)
                lineTo(rect.bottomLeft)
            }

            Direction.DOWN -> {
                moveTo(rect.bottomCenter)
                lineTo(rect.topRight)
                lineTo(rect.topLeft)
            }

            Direction.RIGHT -> {
                moveTo(rect.centerRight)
                lineTo(rect.topLeft)
                lineTo(rect.bottomLeft)
            }

            Direction.LEFT -> {
                moveTo(rect.centerLeft)
                lineTo(rect.topRight)
                lineTo(rect.bottomRight)
            }
        }
        close()
    }
}

fun Path.moveTo(offset: Offset) = moveTo(offset.x, offset.y)
fun Path.lineTo(offset: Offset) = lineTo(offset.x, offset.y)
