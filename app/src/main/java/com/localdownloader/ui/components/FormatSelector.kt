package com.localdownloader.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.localdownloader.domain.models.MediaFormat

@Composable
fun FormatSelector(
    formats: List<MediaFormat>,
    selectedFormatId: String?,
    onFormatSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = "Available Formats",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            items(
                items = formats,
                key = { it.formatId },
            ) { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onFormatSelected(item.formatId) }
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    RadioButton(
                        selected = item.formatId == selectedFormatId,
                        onClick = { onFormatSelected(item.formatId) },
                    )
                    Text(
                        text = item.asReadableLabel(),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}
