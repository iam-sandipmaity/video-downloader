package com.localdownloader.ui.screens

internal fun remapPinchPanForSizeChange(
    panX: Float,
    panY: Float,
    zoom: Float,
    oldWidth: Int,
    oldHeight: Int,
    newWidth: Int,
    newHeight: Int,
): Pair<Float, Float> {
    if (oldWidth <= 0 || oldHeight <= 0 || newWidth <= 0 || newHeight <= 0) {
        return panX to panY
    }
    val scaledX = panX * (newWidth.toFloat() / oldWidth.toFloat())
    val scaledY = panY * (newHeight.toFloat() / oldHeight.toFloat())
    val maxPanX = ((newWidth * (zoom - 1f)) / 2f).coerceAtLeast(0f)
    val maxPanY = ((newHeight * (zoom - 1f)) / 2f).coerceAtLeast(0f)
    return scaledX.coerceIn(-maxPanX, maxPanX) to scaledY.coerceIn(-maxPanY, maxPanY)
}
