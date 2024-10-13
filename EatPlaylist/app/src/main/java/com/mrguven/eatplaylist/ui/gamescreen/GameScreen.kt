package com.mrguven.eatplaylist.ui.gamescreen

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowInsetsControllerCompat
import androidx.palette.graphics.Palette
import com.mrguven.eatplaylist.data.model.Direction
import com.mrguven.eatplaylist.data.model.RotationDirection
import com.mrguven.eatplaylist.data.model.SnakeUnit
import com.mrguven.eatplaylist.viewmodel.EatPlaylistViewModel
import kotlinx.coroutines.delay
import kotlin.math.absoluteValue

const val EFFECT_MILLISECOND = 400
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
        targetValue = targetColor,
        animationSpec = tween(
            durationMillis = EFFECT_MILLISECOND,
            easing = LinearEasing
        ), label = "GameScreen Background Color"
    )
    UpdateSystemBarsColor(backgroundColor)

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

    val targetRotationAngle = calculateRotationAngle(snakeUnit.rotationDirection)
    val rotationAngle by animateFloatAsState(
        targetValue = targetRotationAngle,
        animationSpec = tween(durationMillis = EFFECT_MILLISECOND / 2),
        label = "Snake Head Rotation Angle"
    )

    val previousPosition =
        remember(snakeUnit.rotationDirection, snakeUnit.direction, adjustedX, adjustedY) {
            calculatePreviousPosition(snakeUnit.direction, adjustedX, adjustedY, cellSizePx)
        }

    val pivotOffset =
        remember(snakeUnit.rotationDirection, snakeUnit.direction, adjustedX, adjustedY) {
            calculatePivotOffset(
                snakeUnit.rotationDirection, snakeUnit.direction, adjustedX, adjustedY, cellSizePx
            )
        }

    when (snakeUnit.rotationDirection) {
        RotationDirection.NO_ROTATION ->
            DrawStaticSnakeHead(adjustedX, adjustedY, snakeUnit.direction, cellSizePx)

        else ->
            DrawRotatedSnakeHead(
                previousPosition,
                rotationAngle,
                pivotOffset,
                snakeUnit.previousDirection,
                cellSizePx
            )
    }
}

@Composable
private fun DrawStaticSnakeHead(
    x: Float, y: Float, direction: Direction, cellSizePx: Float,
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val rect = Rect(Offset(x, y), Size(cellSizePx, cellSizePx))
        val trianglePath = createTrianglePath(rect, direction)

        drawIntoCanvas { canvas ->
            canvas.drawOutline(outline = Outline.Generic(trianglePath), paint = Paint().apply {
                color = Color.White
                pathEffect = PathEffect.cornerPathEffect(rect.maxDimension / 5)
            })
        }
    }
}

@Composable
private fun DrawRotatedSnakeHead(
    previousPosition: Offset,
    rotationAngle: Float,
    pivotOffset: Offset,
    previousDirection: Direction,
    cellSizePx: Float,
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val rect =
            Rect(Offset(previousPosition.x, previousPosition.y), Size(cellSizePx, cellSizePx))
        val trianglePath = createTrianglePath(rect, previousDirection)

        rotate(degrees = rotationAngle, pivot = pivotOffset) {
            drawIntoCanvas { canvas ->
                canvas.drawOutline(outline = Outline.Generic(trianglePath), paint = Paint().apply {
                    color = Color.White
                    pathEffect = PathEffect.cornerPathEffect(rect.maxDimension / 5)
                })
            }
        }
    }
}

@Composable
fun DrawSnakeBody(
    snakeUnit: SnakeUnit, horizontalFactor: Int, cellSize: Dp
) {
    val cellSizePx = dpToPx(cellSize)
    val (x, y) = remember(snakeUnit.index, horizontalFactor, cellSizePx) {
        calculateCanvasCoordinates(snakeUnit.index, horizontalFactor, cellSizePx)
    }

    val targetRotationAngle = calculateRotationAngle(snakeUnit.rotationDirection)
    val rotationAngle by animateFloatAsState(
        targetValue = targetRotationAngle,
        animationSpec = tween(durationMillis = EFFECT_MILLISECOND / 2),
        label = "Snake Body Rotation Angle"
    )

    val previousPosition =
        remember(snakeUnit.rotationDirection, snakeUnit.previousDirection, x, y) {
            calculatePreviousPosition(snakeUnit.previousDirection, x, y, cellSizePx)
        }

    val pivotOffset = remember(snakeUnit.rotationDirection, snakeUnit.previousDirection, x, y) {
        calculatePivotOffset(
            snakeUnit.rotationDirection, snakeUnit.previousDirection, x, y, cellSizePx
        )
    }

    when (snakeUnit.rotationDirection) {
        RotationDirection.NO_ROTATION ->
            DrawStaticImage(snakeUnit.imageBitmap, Offset(x, y), cellSizePx)

        else ->
            DrawRotatedImage(
                snakeUnit.imageBitmap, rotationAngle, pivotOffset, previousPosition, cellSizePx
            )
    }
}

@Composable
private fun DrawStaticImage(
    bitmap: Bitmap, position: Offset, cellSizePx: Float
) {
    Canvas(modifier = Modifier.size(cellSizePx.dp)) {
        val roundedRect = createRoundedRect(position, cellSizePx)
        val path = Path().apply { addRoundRect(roundedRect, Path.Direction.Clockwise) }

        with(drawContext.canvas) {
            clipPath(path)
            drawImageRect(
                image = bitmap.asImageBitmap(),
                paint = Paint(),
                dstOffset = IntOffset(position.x.toInt(), position.y.toInt()),
                dstSize = IntSize(cellSizePx.toInt(), cellSizePx.toInt()),
                srcOffset = IntOffset.Zero,
                srcSize = IntSize(bitmap.width, bitmap.height)
            )
        }
    }
}

@Composable
private fun DrawRotatedImage(
    bitmap: Bitmap, rotationAngle: Float, pivotOffset: Offset, position: Offset, cellSizePx: Float
) {
    Canvas(modifier = Modifier.size(cellSizePx.dp)) {
        val roundedRect = createRoundedRect(position, cellSizePx)
        val path = Path().apply { addRoundRect(roundedRect, Path.Direction.Clockwise) }

        rotate(degrees = rotationAngle, pivot = pivotOffset) {
            with(drawContext.canvas) {
                clipPath(path)
                drawImageRect(
                    image = bitmap.asImageBitmap(),
                    paint = Paint(),
                    dstOffset = IntOffset(position.x.toInt(), position.y.toInt()),
                    dstSize = IntSize(cellSizePx.toInt(), cellSizePx.toInt()),
                    srcOffset = IntOffset.Zero,
                    srcSize = IntSize(bitmap.width, bitmap.height)
                )
            }
        }
    }
}

private fun createRoundedRect(position: Offset, cellSizePx: Float) = RoundRect(
    left = position.x,
    top = position.y,
    right = position.x + cellSizePx,
    bottom = position.y + cellSizePx,
    cornerRadius = CornerRadius(10f, 10f)
)

fun calculateRotationAngle(rotationDirection: RotationDirection): Float {
    return when (rotationDirection) {
        RotationDirection.CLOCKWISE -> 90f
        RotationDirection.COUNTER_CLOCKWISE -> -90f
        RotationDirection.NO_ROTATION -> 0f
    }
}

private fun calculatePreviousPosition(
    previousDirection: Direction, x: Float, y: Float, cellSizePx: Float
): Offset {
    return when (previousDirection) {
        Direction.RIGHT -> Offset(x - cellSizePx, y)
        Direction.DOWN -> Offset(x, y - cellSizePx)
        Direction.LEFT -> Offset(x + cellSizePx, y)
        Direction.UP -> Offset(x, y + cellSizePx)
    }
}

private fun calculatePivotOffset(
    rotationDirection: RotationDirection,
    previousDirection: Direction,
    x: Float,
    y: Float,
    cellSizePx: Float
): Offset {
    return when (rotationDirection) {
        RotationDirection.CLOCKWISE -> when (previousDirection) {
            Direction.RIGHT -> Offset(x, y + cellSizePx)
            Direction.DOWN -> Offset(x, y)
            Direction.LEFT -> Offset(x + cellSizePx, y)
            Direction.UP -> Offset(x + cellSizePx, y + cellSizePx)
        }

        RotationDirection.COUNTER_CLOCKWISE -> when (previousDirection) {
            Direction.LEFT -> Offset(x + cellSizePx, y + cellSizePx)
            Direction.DOWN -> Offset(x + cellSizePx, y)
            Direction.RIGHT -> Offset(x, y)
            Direction.UP -> Offset(x, y + cellSizePx)
        }

        RotationDirection.NO_ROTATION -> when (previousDirection) {
            Direction.RIGHT -> Offset(x, y)
            Direction.DOWN -> Offset(x, y)
            Direction.LEFT -> Offset(x, y)
            Direction.UP -> Offset(x, y)
        }
    }
}

@Composable
fun DrawTargetSnakeUnit(
    snakeUnit: SnakeUnit, horizontalFactor: Int, cellSize: Dp
) {
    val cellSizePx = dpToPx(cellSize)
    val (x, y) = calculateCanvasCoordinates(snakeUnit.index, horizontalFactor, cellSizePx)

    val transition = rememberInfiniteTransition(label = "Pulsating Animation")
    val scaleFraction by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = EFFECT_MILLISECOND, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "Scale Animation"
    )

    Canvas(modifier = Modifier.size(cellSize)) {
        val scaledCellSizePx = cellSizePx * scaleFraction
        val offset = Offset(
            x = x + (cellSizePx - scaledCellSizePx) / 2,
            y = y + (cellSizePx - scaledCellSizePx) / 2
        )

        val roundedRect = RoundRect(
            left = offset.x,
            top = offset.y,
            right = offset.x + scaledCellSizePx,
            bottom = offset.y + scaledCellSizePx,
            cornerRadius = CornerRadius(10f, 10f)
        )
        val path = Path().apply {
            addRoundRect(roundRect = roundedRect, Path.Direction.Clockwise)
        }

        with(drawContext.canvas) {
            clipPath(path)
            drawImageRect(
                image = snakeUnit.imageBitmap.asImageBitmap(),
                paint = Paint(),
                dstOffset = IntOffset(offset.x.toInt(), offset.y.toInt()),
                dstSize = IntSize(scaledCellSizePx.toInt(), scaledCellSizePx.toInt()),
                srcOffset = IntOffset.Zero,
                srcSize = IntSize(snakeUnit.imageBitmap.width, snakeUnit.imageBitmap.height)
            )
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

@Composable
fun UpdateSystemBarsColor(backgroundColor: Color) {
    val activity = LocalContext.current as ComponentActivity

    LaunchedEffect(backgroundColor) {
        val systemUiController =
            WindowInsetsControllerCompat(activity.window, activity.window.decorView)

        activity.window.statusBarColor = backgroundColor.toArgb()
        activity.window.navigationBarColor = backgroundColor.toArgb()

        val useDarkIcons = backgroundColor.luminance() > 0.5
        systemUiController.isAppearanceLightStatusBars = useDarkIcons
        systemUiController.isAppearanceLightNavigationBars = useDarkIcons
    }
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
