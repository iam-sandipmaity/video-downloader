package com.localdownloader.ui.screens

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.foundation.layout.Box

@Composable
internal fun rememberBottomSheetScrollGuard(
    listState: LazyListState,
): NestedScrollConnection {
    return remember(listState) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                return consumeEdgeScroll(
                    available = available,
                    canScrollBackward = listState.canScrollBackward,
                    canScrollForward = listState.canScrollForward,
                )
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource,
            ): Offset {
                return consumeEdgeScroll(
                    available = available,
                    canScrollBackward = listState.canScrollBackward,
                    canScrollForward = listState.canScrollForward,
                )
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                return consumeEdgeFling(
                    available = available,
                    canScrollBackward = listState.canScrollBackward,
                    canScrollForward = listState.canScrollForward,
                )
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                return consumeEdgeFling(
                    available = available,
                    canScrollBackward = listState.canScrollBackward,
                    canScrollForward = listState.canScrollForward,
                )
            }
        }
    }
}

@Composable
internal fun BottomSheetGrip(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            shape = androidx.compose.foundation.shape.RoundedCornerShape(999.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.padding(vertical = 6.dp),
        ) {
            Box(modifier = Modifier.padding(horizontal = 22.dp, vertical = 2.dp))
        }
    }
}

private fun consumeEdgeScroll(
    available: Offset,
    canScrollBackward: Boolean,
    canScrollForward: Boolean,
): Offset {
    return when {
        available.y > 0f && !canScrollBackward -> Offset(0f, available.y)
        available.y < 0f && !canScrollForward -> Offset(0f, available.y)
        else -> Offset.Zero
    }
}

private fun consumeEdgeFling(
    available: Velocity,
    canScrollBackward: Boolean,
    canScrollForward: Boolean,
): Velocity {
    return when {
        available.y > 0f && !canScrollBackward -> Velocity(0f, available.y)
        available.y < 0f && !canScrollForward -> Velocity(0f, available.y)
        else -> Velocity.Zero
    }
}
