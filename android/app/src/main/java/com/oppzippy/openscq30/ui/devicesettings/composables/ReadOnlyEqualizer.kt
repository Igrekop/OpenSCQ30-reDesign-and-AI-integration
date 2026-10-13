package com.oppzippy.openscq30.ui.devicesettings.composables

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.oppzippy.openscq30.R
import com.oppzippy.openscq30.ui.theme.OpenSCQ30Theme
import java.math.BigDecimal
import java.math.MathContext
import kotlin.math.absoluteValue

@Composable
fun ReadOnlyEqualizer(
    modifier: Modifier,
    color: Color,
    bands: List<UShort>,
    values: List<Short>,
    minValue: Short,
    maxValue: Short,
    fractionDigits: Short,
    drawHorizontalGuide: Boolean = true,
    strokeWidthDp: Dp = 2.dp,
) {
    val contentDescription = bands.take(values.size).map { bandHz ->
        if (bandHz < 1000.toUShort()) {
            stringResource(R.string.hz, bandHz.toInt())
        } else {
            stringResource(
                R.string.khz,
                BigDecimal(bandHz.toInt()).scaleByPowerOfTen(-3).round(MathContext(1)).toPlainString(),
            )
        }
    }.withIndex().joinToString(separator = ", ") { (index, bandHz) ->
        "$bandHz: ${BigDecimal(values[index].toInt()).scaleByPowerOfTen(-fractionDigits)}"
    }

    Canvas(modifier, contentDescription) {
        if (values.isEmpty()) return@Canvas

        val strokeWidth = strokeWidthDp.toPx()
        val padding = Offset(strokeWidth + 4.dp.toPx(), strokeWidth + 4.dp.toPx())
        val sizeWithoutPadding = Offset(
            x = size.width - padding.x * 2f,
            y = size.height - padding.y * 2f,
        )

        val range = (maxValue - minValue).toFloat().coerceAtLeast(1f)
        val zeroValueY = size.height - size.height * (minValue.toFloat().absoluteValue / range)

        // Draw horizontal grid guides
        if (drawHorizontalGuide) {
            // Center zero-dB guide line (more pronounced)
            drawLine(
                color = color.copy(alpha = 0.22f),
                start = Offset(0f, zeroValueY),
                end = Offset(size.width, zeroValueY),
                strokeWidth = 1.dp.toPx(),
            )
            // Top and bottom boundaries
            drawLine(
                color = color.copy(alpha = 0.08f),
                start = Offset(0f, strokeWidth),
                end = Offset(size.width, strokeWidth),
                strokeWidth = 1.dp.toPx(),
            )
            drawLine(
                color = color.copy(alpha = 0.08f),
                start = Offset(0f, size.height - strokeWidth),
                end = Offset(size.width, size.height - strokeWidth),
                strokeWidth = 1.dp.toPx(),
            )
        }

        val unscaledPoints = equalizerLinePoints(
            minValue = minValue,
            maxValue = maxValue,
            values = values,
        )
        val canvasSpacePoints = unscaledPoints.map { it * sizeWithoutPadding + padding }

        if (canvasSpacePoints.size < 2) return@Canvas

        // Generate smooth spline curve
        val splinePath = Path().apply {
            moveTo(canvasSpacePoints.first().x, canvasSpacePoints.first().y)
            for (i in 0 until canvasSpacePoints.size - 1) {
                val p0 = canvasSpacePoints.getOrElse(i - 1) { canvasSpacePoints[i] }
                val p1 = canvasSpacePoints[i]
                val p2 = canvasSpacePoints[i + 1]
                val p3 = canvasSpacePoints.getOrElse(i + 2) { canvasSpacePoints[i + 1] }

                val cp1x = p1.x + (p2.x - p0.x) / 6f
                val cp1y = p1.y + (p2.y - p0.y) / 6f
                val cp2x = p2.x - (p3.x - p1.x) / 6f
                val cp2y = p2.y - (p3.y - p1.y) / 6f

                cubicTo(cp1x, cp1y, cp2x, cp2y, p2.x, p2.y)
            }
        }

        // Shading path beneath the curve with radiant gradient
        val fillPath = Path().apply {
            addPath(splinePath)
            lineTo(canvasSpacePoints.last().x, size.height)
            lineTo(canvasSpacePoints.first().x, size.height)
            close()
        }

        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    color.copy(alpha = 0.35f),
                    color.copy(alpha = 0.08f),
                    Color.Transparent,
                ),
                startY = 0f,
                endY = size.height,
            ),
        )

        // Ambient glow stroke
        drawPath(
            path = splinePath,
            color = color.copy(alpha = 0.22f),
            style = Stroke(width = strokeWidth * 3.5f, cap = StrokeCap.Round),
        )

        // Core crisp curve line
        drawPath(
            path = splinePath,
            color = color,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
        )

        // Draw modern glow dots at each frequency point
        canvasSpacePoints.forEach { point ->
            drawCircle(
                color = color.copy(alpha = 0.3f),
                radius = strokeWidth * 2.2f,
                center = point,
            )
            drawCircle(
                color = color,
                radius = strokeWidth * 1.2f,
                center = point,
            )
        }
    }
}

private fun equalizerLinePoints(minValue: Short, maxValue: Short, values: List<Short>): List<Offset> {
    val range = (maxValue - minValue).toFloat().coerceAtLeast(1f)
    return values.mapIndexed { index, value ->
        val x = if (values.size > 1) index.toFloat() / (values.size - 1).toFloat() else 0.5f
        val y = (value - minValue).toFloat() / range
        Offset(x, (1f - y).coerceIn(0f, 1f))
    }
}

private operator fun Offset.times(other: Offset): Offset = Offset(x * other.x, y * other.y)

@Preview(showBackground = true)
@Composable
private fun PreviewEqualizer() {
    val bands = listOf<UShort>(100u, 200u, 400u, 800u, 1600u, 3200u, 6400u, 12000u)
    OpenSCQ30Theme {
        Surface {
            ReadOnlyEqualizer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                color = MaterialTheme.colorScheme.primary,
                bands = bands,
                values = listOf(60, -40, 20, -10, 80, -90, 30, 60),
                minValue = -120,
                maxValue = 134,
                fractionDigits = 1,
            )
        }
    }
}
