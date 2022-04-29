package com.chargemap.compose.numberpicker

import androidx.compose.animation.core.*
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

private fun <T> getItemIndexForOffset(
    range: List<T>,
    value: T,
    offset: Float,
    halfNumbersColumnHeightPx: Float
): Int {
    val indexOf = range.indexOf(value) - (offset / halfNumbersColumnHeightPx).toInt()
    return maxOf(0, minOf(indexOf, range.count() - 1))
}

@Composable
fun <T> ListItemPicker(
    modifier: Modifier = Modifier,
    label: (T) -> String = { it.toString() },
    value: T,
    onValueChange: (T) -> Unit,
    dividersColor: Color = MaterialTheme.colors.primary,
    items: List<T>,
    textStyle: TextStyle = LocalTextStyle.current,
) {
    val minAlpha = 0.75f
    val maxAlpha = 1.0f
    val verticalMargin = 8.dp
    val numbersColumnHeight = 80.dp
    val halfNumbersColumnHeight = numbersColumnHeight / 2
    val halfNumbersColumnHeightPx = with(LocalDensity.current) { halfNumbersColumnHeight.toPx() }

    val coroutineScope = rememberCoroutineScope()

    val animatedOffset = remember { Animatable(0f) }
        .apply {
            val index = items.indexOf(value)
            val offsetRange = remember(value, items) {
                -((items.count() - 1) - index) * halfNumbersColumnHeightPx to
                        index * halfNumbersColumnHeightPx
            }
            updateBounds(offsetRange.first, offsetRange.second)
        }

    val coercedAnimatedOffset = animatedOffset.value % halfNumbersColumnHeightPx

    val delta = (maxAlpha - minAlpha) / halfNumbersColumnHeightPx

    val fadeIn = minAlpha + delta * abs(coercedAnimatedOffset)
    val fadeOut = maxAlpha - delta * abs(coercedAnimatedOffset)

    val topAlpha = if (coercedAnimatedOffset < 0) fadeOut else fadeIn
    val bottomAlpha = if (coercedAnimatedOffset > 0) fadeOut else fadeIn

    val indexOfElement =
        getItemIndexForOffset(items, value, animatedOffset.value, halfNumbersColumnHeightPx)

    Box(
        modifier = modifier
            .wrapContentSize()
            .draggable(
                orientation = Orientation.Vertical,
                state = rememberDraggableState { deltaY ->
                    coroutineScope.launch {
                        animatedOffset.snapTo(animatedOffset.value + deltaY)
                    }
                },
                onDragStopped = { velocity ->
                    coroutineScope.launch {
                        val endValue = animatedOffset.fling(
                            initialVelocity = velocity,
                            animationSpec = exponentialDecay(frictionMultiplier = 20f),
                            adjustTarget = { target ->
                                val coercedTarget = target % halfNumbersColumnHeightPx
                                val coercedAnchors =
                                    listOf(
                                        -halfNumbersColumnHeightPx,
                                        0f,
                                        halfNumbersColumnHeightPx
                                    )
                                val coercedPoint =
                                    coercedAnchors.minByOrNull { abs(it - coercedTarget) }!!
                                val base =
                                    halfNumbersColumnHeightPx * (target / halfNumbersColumnHeightPx).toInt()
                                coercedPoint + base
                            }
                        ).endState.value

                        val result = items.elementAt(
                            getItemIndexForOffset(items, value, endValue, halfNumbersColumnHeightPx)
                        )
                        onValueChange(result)
                        animatedOffset.snapTo(0f)
                    }
                }
            )
    ) {

        Box(
            modifier = Modifier
                .padding(vertical = verticalMargin, horizontal = 20.dp)
                .height(numbersColumnHeight * 2.5f)
                .clipToBounds()
                .offset { IntOffset(x = 0, y = coercedAnimatedOffset.roundToInt()) }
        ) {
            Labels(
                Modifier.align(Alignment.Center),
                textStyle,
                indexOfElement,
                items,
                label,
                halfNumbersColumnHeight,
                minAlpha,
                topAlpha,
                maxAlpha,
                bottomAlpha
            )
        }

        Box(
            modifier
                .padding(horizontal = 8.dp)
                .width(32.dp)
                .height(32.dp)
                .border(width = 1.dp, color = dividersColor, shape = RoundedCornerShape(3.dp))
                .align(Alignment.Center)
        )
    }
}

@Composable
private fun <T> Labels(
    modifier: Modifier = Modifier,
    textStyle: TextStyle,
    indexOfElement: Int,
    items: List<T>,
    label: (T) -> String,
    halfNumbersColumnHeight: Dp,
    minAlpha: Float,
    topAlpha: Float,
    maxAlpha: Float,
    bottomAlpha: Float
) {
    ProvideTextStyle(textStyle) {
        Label(
            modifier = modifier,
            item = items.getOrNull(indexOfElement - 3),
            label = label,
            offsetY = -halfNumbersColumnHeight * 3,
            alpha = minAlpha
        )

        Label(
            modifier = modifier,
            item = items.getOrNull(indexOfElement - 2),
            label = label,
            offsetY = -halfNumbersColumnHeight * 2,
            alpha = topAlpha
        )

        Label(
            modifier = modifier,
            item = items.getOrNull(indexOfElement - 1),
            label = label,
            offsetY = -halfNumbersColumnHeight,
            alpha = maxAlpha
        )

        Label(
            modifier = modifier,
            item = items.getOrNull(indexOfElement),
            label = label,
            offsetY = 0.dp,
            alpha = maxAlpha
        )

        Label(
            modifier = modifier,
            item = items.getOrNull(indexOfElement + 1),
            label = label,
            offsetY = halfNumbersColumnHeight,
            alpha = maxAlpha
        )

        Label(
            modifier = modifier,
            item = items.getOrNull(indexOfElement + 2),
            label = label,
            offsetY = halfNumbersColumnHeight * 2,
            alpha = bottomAlpha
        )

        Label(
            modifier = modifier,
            item = items.getOrNull(indexOfElement + 3),
            label = label,
            offsetY = halfNumbersColumnHeight * 3,
            alpha = minAlpha
        )
    }
}

@Composable
private fun <T> Label(
    modifier: Modifier,
    item: T?,
    label: (T) -> String,
    offsetY: Dp,
    alpha: Float
) {
    item?.let {
        Label(
            text = label(it),
            modifier = modifier
                .offset(y = offsetY)
                .alpha(alpha)
        )
    }
}

@Composable
private fun Label(text: String, modifier: Modifier) {
    Text(
        modifier = modifier.pointerInput(Unit) {
            detectTapGestures(onLongPress = {
//                Empty to disable text selection
            })
        },
        text = text,
        textAlign = TextAlign.Center,
    )
}

private suspend fun Animatable<Float, AnimationVector1D>.fling(
    initialVelocity: Float,
    animationSpec: DecayAnimationSpec<Float>,
    adjustTarget: ((Float) -> Float)?,
    block: (Animatable<Float, AnimationVector1D>.() -> Unit)? = null,
): AnimationResult<Float, AnimationVector1D> {
    val targetValue = animationSpec.calculateTargetValue(value, initialVelocity)
    val adjustedTarget = adjustTarget?.invoke(targetValue)
    return if (adjustedTarget != null) {
        animateTo(
            targetValue = adjustedTarget,
            initialVelocity = initialVelocity,
            block = block
        )
    } else {
        animateDecay(
            initialVelocity = initialVelocity,
            animationSpec = animationSpec,
            block = block,
        )
    }
}
