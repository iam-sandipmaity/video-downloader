package com.localdownloader.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoGestureGuideSheet(
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Text(
                text = "Gesture Control",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            VideoGestureGuideCard(modifier = Modifier.fillMaxWidth())
            Text(
                text = "Swipe horizontally to seek. Swipe up or down on the left for brightness and on the right for volume. Pinch to zoom, then drag to pan.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurface,
                    ),
                ) {
                    Text(
                        text = "Got it",
                        modifier = Modifier.padding(horizontal = 14.dp),
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
fun VideoGestureGuideCard(
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        color = Color(0xFF11171D),
        tonalElevation = 6.dp,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(232.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFF23313C),
                            Color(0xFF10161B),
                        ),
                    ),
                )
                .padding(16.dp),
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawGesturePreviewBackground()
            }
            GestureCallout(
                title = "LEFT / RIGHT",
                subtitle = "Seek",
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 6.dp),
                direction = GestureDirection.Horizontal,
            )
            GestureCallout(
                title = "UP / DOWN",
                subtitle = "Brightness",
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 6.dp),
                direction = GestureDirection.Vertical,
            )
            GestureCallout(
                title = "UP / DOWN",
                subtitle = "Volume",
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 8.dp),
                direction = GestureDirection.Vertical,
            )
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color.White.copy(alpha = 0.11f),
                    border = androidx.compose.foundation.BorderStroke(
                        width = 1.dp,
                        color = Color.White.copy(alpha = 0.42f),
                    ),
                    modifier = Modifier.size(72.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Canvas(modifier = Modifier.size(42.dp)) {
                            drawPinchGlyph()
                        }
                    }
                }
                Text(
                    text = "Zoom and pan",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun GestureCallout(
    title: String,
    subtitle: String,
    direction: GestureDirection,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Canvas(modifier = Modifier.size(width = 44.dp, height = 38.dp)) {
            drawTouchMarker(direction)
        }
        Text(
            text = title,
            textAlign = TextAlign.Center,
            color = Color.White,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
        )
        Text(
            text = subtitle,
            textAlign = TextAlign.Center,
            color = Color.White,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

private enum class GestureDirection {
    Horizontal,
    Vertical,
}

private fun DrawScope.drawGesturePreviewBackground() {
    val stroke = Stroke(width = 2.dp.toPx())
    drawRoundRect(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFF456D7E).copy(alpha = 0.45f),
                Color.Transparent,
            ),
            center = Offset(size.width * 0.28f, size.height * 0.24f),
            radius = size.maxDimension * 0.62f,
        ),
    )
    drawRoundRect(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFF5D3D4D).copy(alpha = 0.46f),
                Color.Transparent,
            ),
            center = Offset(size.width * 0.72f, size.height * 0.5f),
            radius = size.maxDimension * 0.72f,
        ),
    )
    drawRoundRect(
        color = Color.Black.copy(alpha = 0.22f),
    )
    drawRoundRect(
        color = Color.White.copy(alpha = 0.2f),
        style = stroke,
        cornerRadius = CornerRadius(20.dp.toPx()),
        topLeft = Offset(2.dp.toPx(), 2.dp.toPx()),
        size = Size(size.width - 4.dp.toPx(), size.height - 4.dp.toPx()),
    )
    drawLine(
        color = Color.White.copy(alpha = 0.18f),
        start = Offset(size.width / 2f, size.height * 0.2f),
        end = Offset(size.width / 2f, size.height * 0.82f),
        strokeWidth = 1.dp.toPx(),
    )
    drawLine(
        color = Color.White.copy(alpha = 0.1f),
        start = Offset(size.width * 0.18f, size.height * 0.5f),
        end = Offset(size.width * 0.82f, size.height * 0.5f),
        strokeWidth = 1.dp.toPx(),
    )
}

private fun DrawScope.drawTouchMarker(direction: GestureDirection) {
    val white = Color.White
    val muted = white.copy(alpha = 0.7f)
    val palmCenter = Offset(size.width * 0.48f, size.height * 0.62f)
    drawCircle(
        color = white.copy(alpha = 0.94f),
        radius = size.minDimension * 0.22f,
        center = palmCenter,
    )
    drawRoundRect(
        color = white.copy(alpha = 0.94f),
        topLeft = Offset(size.width * 0.46f, size.height * 0.16f),
        size = Size(size.width * 0.14f, size.height * 0.48f),
        cornerRadius = CornerRadius(8.dp.toPx()),
    )
    drawRoundRect(
        color = white.copy(alpha = 0.92f),
        topLeft = Offset(size.width * 0.34f, size.height * 0.42f),
        size = Size(size.width * 0.14f, size.height * 0.32f),
        cornerRadius = CornerRadius(8.dp.toPx()),
    )
    drawRoundRect(
        color = white.copy(alpha = 0.92f),
        topLeft = Offset(size.width * 0.58f, size.height * 0.44f),
        size = Size(size.width * 0.12f, size.height * 0.28f),
        cornerRadius = CornerRadius(8.dp.toPx()),
    )
    when (direction) {
        GestureDirection.Horizontal -> {
            drawLine(
                color = muted,
                start = Offset(size.width * 0.12f, size.height * 0.16f),
                end = Offset(size.width * 0.34f, size.height * 0.16f),
                strokeWidth = 2.dp.toPx(),
            )
            drawLine(
                color = muted,
                start = Offset(size.width * 0.88f, size.height * 0.16f),
                end = Offset(size.width * 0.66f, size.height * 0.16f),
                strokeWidth = 2.dp.toPx(),
            )
            drawArrowHead(Offset(size.width * 0.12f, size.height * 0.16f), left = true)
            drawArrowHead(Offset(size.width * 0.88f, size.height * 0.16f), left = false)
        }

        GestureDirection.Vertical -> {
            drawLine(
                color = muted,
                start = Offset(size.width * 0.78f, size.height * 0.08f),
                end = Offset(size.width * 0.78f, size.height * 0.28f),
                strokeWidth = 2.dp.toPx(),
            )
            drawLine(
                color = muted,
                start = Offset(size.width * 0.78f, size.height * 0.66f),
                end = Offset(size.width * 0.78f, size.height * 0.86f),
                strokeWidth = 2.dp.toPx(),
            )
            drawChevron(Offset(size.width * 0.78f, size.height * 0.08f), up = true)
            drawChevron(Offset(size.width * 0.78f, size.height * 0.86f), up = false)
        }
    }
}

private fun DrawScope.drawPinchGlyph() {
    val stroke = Stroke(width = 4.dp.toPx())
    val white = Color.White.copy(alpha = 0.94f)
    drawLine(
        color = white,
        start = Offset(size.width * 0.18f, size.height * 0.78f),
        end = Offset(size.width * 0.44f, size.height * 0.52f),
        strokeWidth = stroke.width,
    )
    drawLine(
        color = white,
        start = Offset(size.width * 0.82f, size.height * 0.22f),
        end = Offset(size.width * 0.56f, size.height * 0.48f),
        strokeWidth = stroke.width,
    )
    drawCircle(
        color = white,
        radius = 4.dp.toPx(),
        center = Offset(size.width * 0.2f, size.height * 0.76f),
    )
    drawCircle(
        color = white,
        radius = 4.dp.toPx(),
        center = Offset(size.width * 0.8f, size.height * 0.24f),
    )
}

private fun DrawScope.drawArrowHead(point: Offset, left: Boolean) {
    val direction = if (left) 1f else -1f
    val path = Path().apply {
        moveTo(point.x, point.y)
        lineTo(point.x + direction * 7.dp.toPx(), point.y - 5.dp.toPx())
        moveTo(point.x, point.y)
        lineTo(point.x + direction * 7.dp.toPx(), point.y + 5.dp.toPx())
    }
    drawPath(
        path = path,
        color = Color.White.copy(alpha = 0.7f),
        style = Stroke(width = 2.dp.toPx()),
    )
}

private fun DrawScope.drawChevron(point: Offset, up: Boolean) {
    val direction = if (up) 1f else -1f
    val path = Path().apply {
        moveTo(point.x, point.y)
        lineTo(point.x - 5.dp.toPx(), point.y + direction * 7.dp.toPx())
        moveTo(point.x, point.y)
        lineTo(point.x + 5.dp.toPx(), point.y + direction * 7.dp.toPx())
    }
    drawPath(
        path = path,
        color = Color.White.copy(alpha = 0.7f),
        style = Stroke(width = 2.dp.toPx()),
    )
}
