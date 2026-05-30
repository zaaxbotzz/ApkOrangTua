package com.example.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.LocationLog

@Composable
fun MockMap(
    activeLocation: LocationLog?,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()

    // Pulsing animation for child location marker
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseRadius by infiniteTransition.animateFloat(
        initialValue = 10f,
        targetValue = 35f,
        animationSpec = infiniteSpec(),
        label = "radius"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 0f,
        animationSpec = infiniteSpec(),
        label = "alpha"
    )

    // Smooth coordinate transition when switching simulated location
    val targetLat = activeLocation?.latitude ?: -6.1950
    val targetLng = activeLocation?.longitude ?: 106.8025

    // Map bounding boxes for rendering coordinates relative to canvas center
    // Let's map target coordinates around Jakarta Center (~ -6.18, 106.81)
    val centerLat = -6.1900
    val centerLng = 106.8150
    val scaleX = 4000f // Scaling factor for longitude
    val scaleY = -4000f // Scaling factor for latitude (negative because coordinates increase upwards, canvas downwards)

    val animatedLat by animateFloatAsState(targetValue = targetLat.toFloat(), animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessLow), label = "lat")
    val animatedLng by animateFloatAsState(targetValue = targetLng.toFloat(), animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessLow), label = "lng")

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(260.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFE8F1F5))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val centerX = canvasWidth / 2f
            val centerY = canvasHeight / 2f

            // --- 1. Draw Simulated Street Map Grid ---
            val gridColor = Color(0xFFD0DFE5)
            val streetColor = Color.White
            
            // Draw secondary major grid streets
            // Horizontal streets
            val horizStreets = listOf(centerY - 80f, centerY + 60f, centerY - 200f, centerY + 180f)
            for (y in horizStreets) {
                drawRect(
                    color = streetColor,
                    topLeft = Offset(0f, y - 8f),
                    size = Size(canvasWidth, 16f)
                )
                drawLine(
                    color = gridColor,
                    start = Offset(0f, y),
                    end = Offset(canvasWidth, y),
                    strokeWidth = 1f
                )
            }

            // Vertical streets
            val vertStreets = listOf(centerX - 160f, centerX + 40f, centerX + 260f, centerX - 270f)
            for (x in vertStreets) {
                drawRect(
                    color = streetColor,
                    topLeft = Offset(x - 8f, 0f),
                    size = Size(16f, canvasHeight)
                )
                drawLine(
                    color = gridColor,
                    start = Offset(x, 0f),
                    end = Offset(x, canvasHeight),
                    strokeWidth = 1f
                )
            }

            // --- 2. Draw Geofenced Safe Zones ---
            // Zone 1: Rumah (-6.1950, 106.8025)
            val homeX = centerX + ((106.8025 - centerLng) * scaleX).toFloat()
            val homeY = centerY + ((-6.1950 - centerLat) * scaleY).toFloat()
            val safeBlueZone = Color(0x3344BAAA)
            val safeBlueBorder = Color(0xFF009688)

            drawCircle(
                color = safeBlueZone,
                radius = 120f,
                center = Offset(homeX, homeY)
            )
            drawCircle(
                color = safeBlueBorder,
                radius = 120f,
                center = Offset(homeX, homeY),
                style = Stroke(
                    width = 2f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                )
            )

            // Zone 2: Sekolah (-6.1895, 106.8210)
            val schoolX = centerX + ((106.8210 - centerLng) * scaleX).toFloat()
            val schoolY = centerY + ((-6.1895 - centerLat) * scaleY).toFloat()
            drawCircle(
                color = safeBlueZone,
                radius = 100f,
                center = Offset(schoolX, schoolY)
            )
            drawCircle(
                color = safeBlueBorder,
                radius = 100f,
                center = Offset(schoolX, schoolY),
                style = Stroke(
                    width = 2f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                )
            )

            // --- 3. Draw Landmark Labels on map ---
            // Label Home
            drawCircle(Color(0xFF009688), radius = 6f, center = Offset(homeX, homeY))
            drawText(
                textMeasurer = textMeasurer,
                text = "🏠 Rumah (Zona Aman)",
                topLeft = Offset(homeX + 10f, homeY - 26f),
                style = androidx.compose.ui.text.TextStyle(
                    fontSize = 11.sp,
                    color = Color(0xFF00796B),
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                )
            )

            // Label School
            drawCircle(Color(0xFF009688), radius = 6f, center = Offset(schoolX, schoolY))
            drawText(
                textMeasurer = textMeasurer,
                text = "🏫 SMPN 12 (Zona Aman)",
                topLeft = Offset(schoolX + 10f, schoolY - 26f),
                style = androidx.compose.ui.text.TextStyle(
                    fontSize = 11.sp,
                    color = Color(0xFF00796B),
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                )
            )

            // --- 4. Draw Current Dynamic Child Node Pulse & Pointer ---
            val childX = centerX + ((animatedLng - centerLng) * scaleX).toFloat()
            val childY = centerY + ((animatedLat - centerLat) * scaleY).toFloat()

            // Outer pulsing radar
            drawCircle(
                color = Color(0x661E88E5),
                radius = pulseRadius,
                center = Offset(childX, childY)
            )
            // Inner marker shadow
            drawCircle(
                color = Color(0x33000000),
                radius = 8f,
                center = Offset(childX + 2f, childY + 2f)
            )
            // Core blue GPS pointer
            drawCircle(
                color = Color(0xFF1E88E5),
                radius = 10f,
                center = Offset(childX, childY)
            )
            drawCircle(
                color = Color.White,
                radius = 4f,
                center = Offset(childX, childY)
            )
        }

        // Floating location status tag
        Box(
            modifier = Modifier
                .padding(12.dp)
                .align(Alignment.BottomStart)
                .background(Color.White.copy(alpha = 0.9f), RoundedCornerShape(8.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Column {
                Text(
                    text = "Lokasi: ${activeLocation?.placeName ?: "Mencari GPS..."}",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFF2C3E50),
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                )
                Text(
                    text = "GPS: %.4f, %.4f • %s".format(
                        activeLocation?.latitude ?: -6.1950,
                        activeLocation?.longitude ?: 106.8025,
                        activeLocation?.accuracy ?: "Akurasi 5m"
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    fontSize = 9.sp
                )
            }
        }
    }
}

private fun infiniteSpec(): InfiniteRepeatableSpec<Float> {
    return infiniteRepeatable(
        animation = tween(durationMillis = 1500, easing = LinearEasing),
        repeatMode = RepeatMode.Restart
    )
}
