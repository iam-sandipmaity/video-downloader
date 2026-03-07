package com.localdownloader.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.localdownloader.domain.models.MediaFormat
import com.localdownloader.ui.components.FormatSelector

@Composable
fun FormatSelectionScreen(
    formats: List<MediaFormat>,
    selectedFormatId: String?,
    onFormatSelected: (String) -> Unit,
    onApply: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        FormatSelector(
            formats = formats,
            selectedFormatId = selectedFormatId,
            onFormatSelected = onFormatSelected,
            modifier = Modifier.height(360.dp),
        )
        Button(
            onClick = onApply,
            enabled = selectedFormatId != null,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Use selected format")
        }
    }
}
