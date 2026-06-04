package com.localdownloader

import android.content.Context
import android.content.Intent

data class AppOpenRequest(
    val route: String,
    val taskId: String? = null,
)

object AppLaunchRouter {
    private const val ACTION_OPEN_ROUTE = "com.localdownloader.action.OPEN_ROUTE"
    private const val EXTRA_ROUTE = "extra_route"
    private const val EXTRA_TASK_ID = "extra_task_id"

    const val ROUTE_BROWSER = "browser"
    const val ROUTE_DOWNLOADS = "downloads"
    const val ROUTE_DOWNLOAD_QUEUE = "download_queue"
    const val ROUTE_MUSIC = "music"
    const val ROUTE_MORE = "more"
    const val ROUTE_PLAYER = "player"

    fun buildIntent(
        context: Context,
        route: String,
        taskId: String? = null,
    ): Intent {
        return Intent().setClass(context, MainActivity::class.java).apply {
            action = ACTION_OPEN_ROUTE
            setPackage(context.packageName)
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_ROUTE, route)
            taskId?.takeIf { it.isNotBlank() }?.let { putExtra(EXTRA_TASK_ID, it) }
        }
    }

    fun fromIntent(intent: Intent?): AppOpenRequest? {
        intent ?: return null
        if (intent.action != ACTION_OPEN_ROUTE && !intent.hasExtra(EXTRA_ROUTE)) return null

        val route = intent.getStringExtra(EXTRA_ROUTE)?.trim().orEmpty()
        if (route.isBlank()) return null

        return AppOpenRequest(
            route = route,
            taskId = intent.getStringExtra(EXTRA_TASK_ID)?.trim()?.ifBlank { null },
        )
    }
}
