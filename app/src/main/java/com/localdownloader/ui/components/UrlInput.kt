package com.localdownloader.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun UrlInput(
    url: String,
    onUrlChanged: (String) -> Unit,
    onAnalyzeClicked: () -> Unit,
    isAnalyzing: Boolean,
    modifier: Modifier = Modifier,
) {
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedTextField(
            value = url,
            onValueChange = onUrlChanged,
            label = { Text("Video URL") },
            placeholder = { Text("https://youtube.com/watch?v=...") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(0.dp),
                ) {
                    // Paste button — always visible
                    IconButton(
                        onClick = {
                            scope.launch {
                                val text = clipboardManager.getText()?.text?.trim()
                                if (!text.isNullOrBlank()) onUrlChanged(text)
                            }
                        },
                        modifier = Modifier.size(40.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentPaste,
                            contentDescription = "Paste URL",
                        )
                    }
                    // Clear button — only when there is text
                    if (url.isNotEmpty()) {
                        IconButton(
                            onClick = { onUrlChanged("") },
                            modifier = Modifier.size(40.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear URL",
                            )
                        }
                    }
                }
            },
        )
        Button(
            onClick = onAnalyzeClicked,
            enabled = !isAnalyzing,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
        ) {
            Text(if (isAnalyzing) "Analyzing..." else "Analyze")
        }
    }
}
