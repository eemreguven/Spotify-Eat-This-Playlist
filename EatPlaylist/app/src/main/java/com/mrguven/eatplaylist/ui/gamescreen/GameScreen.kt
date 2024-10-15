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

const val EFFECT_MILLISECOND = 300

@Composable
fun GameScreen(
    modifier: Modifier, viewModel: EatPlaylistViewModel, onCurrentSongEaten: () -> Unit
) {
    LaunchedEffect(viewModel.snakeUnits.size) {
        onCurrentSongEaten()
    }

    LaunchedEffect(Unit) {
        while (true) {
            viewModel.moveSnake()
            delay(EFFECT_MILLISECOND.toLong())
        }
    }

    DrawGameScreen(modifier = modifier, viewModel = viewModel)
}

@Composable
fun DrawGameScreen(modifier: Modifier, viewModel: EatPlaylistViewModel) {
    val cellSizePx = viewModel.cellSizePx.floatValue
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
            DrawTargetSnakeUnit(targetSnakeUnit, cellSizePx)
        }
        viewModel.snakeUnits.drop(1).forEach { snakeUnit ->
            DrawSnakeBody(snakeUnit, cellSizePx)
        }
        DrawSnakeHead(viewModel.snakeUnits.first(), cellSizePx)
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
    snakeUnit: SnakeUnit, cellSizePx: Float
) {
    val targetRotationAngle = snakeUnit.rotationAngle
    val rotationAngle by animateFloatAsState(
        targetValue = targetRotationAngle,
        animationSpec = tween(durationMillis = EFFECT_MILLISECOND, easing = LinearEasing),
        label = "Snake Head Rotation Angle"
    )

    val animatedX by animateFloatAsState(
        targetValue = snakeUnit.position.x,
        animationSpec = tween(durationMillis = EFFECT_MILLISECOND, easing = LinearEasing),
        label = "Animated Snake Body X Position"
    )
    val animatedY by animateFloatAsState(
        targetValue = snakeUnit.position.y,
        animationSpec = tween(durationMillis = EFFECT_MILLISECOND, easing = LinearEasing),
        label = "Animated Snake Body Y Position"
    )

    when (snakeUnit.rotationDirection) {
        RotationDirection.NO_ROTATION ->
            DrawStaticSnakeHead(Offset(animatedX, animatedY), snakeUnit.direction, cellSizePx)

        else ->
            DrawRotatedSnakeHead(snakeUnit, rotationAngle, cellSizePx)
    }
}

@Composable
private fun DrawStaticSnakeHead(
    position: Offset, direction: Direction, cellSizePx: Float,
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val rect = Rect(position, Size(cellSizePx, cellSizePx))
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
    snakeUnit: SnakeUnit, rotationAngle: Float, cellSizePx: Float,
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val rect =
            Rect(snakeUnit.previousPosition, Size(cellSizePx, cellSizePx))
        val trianglePath = createTrianglePath(rect, snakeUnit.previousDirection)

        rotate(degrees = rotationAngle, pivot = snakeUnit.pivotOffset) {
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
    snakeUnit: SnakeUnit, cellSizePx: Float
) {
    val targetRotationAngle = snakeUnit.rotationAngle
    val rotationAngle by animateFloatAsState(
        targetValue = targetRotationAngle,
        animationSpec = tween(durationMillis = EFFECT_MILLISECOND, easing = LinearEasing),
        label = "Snake Body Rotation Angle"
    )

    val animatedX by animateFloatAsState(
        targetValue = snakeUnit.position.x,
        animationSpec = tween(durationMillis = EFFECT_MILLISECOND, easing = LinearEasing),
        label = "Animated Snake Body X Position"
    )
    val animatedY by animateFloatAsState(
        targetValue = snakeUnit.position.y,
        animationSpec = tween(durationMillis = EFFECT_MILLISECOND, easing = LinearEasing),
        label = "Animated Snake Body Y Position"
    )

    when (snakeUnit.rotationDirection) {
        RotationDirection.NO_ROTATION ->
            DrawStaticImage(snakeUnit.imageBitmap, Offset(animatedX, animatedY), cellSizePx)

        else ->
            DrawRotatedImage(snakeUnit, rotationAngle, cellSizePx)
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
    snakeUnit: SnakeUnit, rotationAngle: Float, cellSizePx: Float
) {
    Canvas(modifier = Modifier.size(cellSizePx.dp)) {
        val roundedRect = createRoundedRect(snakeUnit.previousPosition, cellSizePx)
        val path = Path().apply { addRoundRect(roundedRect, Path.Direction.Clockwise) }

        rotate(degrees = rotationAngle, pivot = snakeUnit.pivotOffset) {
            with(drawContext.canvas) {
                clipPath(path)
                drawImageRect(
                    image = snakeUnit.imageBitmap.asImageBitmap(),
                    paint = Paint(),
                    dstOffset = IntOffset(
                        snakeUnit.previousPosition.x.toInt(),
                        snakeUnit.previousPosition.y.toInt()
                    ),
                    dstSize = IntSize(cellSizePx.toInt(), cellSizePx.toInt()),
                    srcOffset = IntOffset.Zero,
                    srcSize = IntSize(snakeUnit.imageBitmap.width, snakeUnit.imageBitmap.height)
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

@Composable
fun DrawTargetSnakeUnit(
    snakeUnit: SnakeUnit, cellSizePx: Float
) {
    val transition = rememberInfiniteTransition(label = "Pulsating Animation")
    val scaleFraction by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = EFFECT_MILLISECOND, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "Scale Animation"
    )

    Canvas(modifier = Modifier.size(cellSizePx.dp)) {
        val scaledCellSizePx = cellSizePx * scaleFraction
        val offset = Offset(
            x = snakeUnit.position.x + (cellSizePx - scaledCellSizePx) / 2,
            y = snakeUnit.position.y + (cellSizePx - scaledCellSizePx) / 2
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
