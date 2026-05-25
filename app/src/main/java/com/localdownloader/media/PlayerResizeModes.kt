package com.localdownloader.media

/**
 * Mirrors Media3 AspectRatioFrameLayout resize mode values so app state and UI
 * options do not depend on unstable Media3 constants directly.
 */
object PlayerResizeModes {
    const val FIT = 0
    const val FIXED_WIDTH = 1
    const val FIXED_HEIGHT = 2
    const val FILL = 3
    const val ZOOM = 4
}
