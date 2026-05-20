package com.localdownloader.ui.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp

object UpdateChangelogSections {
    const val APP = "app"
    const val YT_DLP = "ytdlp"
    const val FFMPEG = "ffmpeg"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateChangelogScreen(
    title: String,
    currentVersion: String?,
    latestVersion: String?,
    summary: String,
    releaseNotes: String?,
    latestDocumentHeading: String = "Release notes",
    bundledDocumentHeading: String? = null,
    overviewText: String = "Read the latest release notes in a cleaner documentation-style view.",
    bundledReleaseNotesAssetName: String? = null,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val bundledDocumentationText = remember(context, bundledReleaseNotesAssetName) {
        loadBundledReleaseNotes(context = context, assetName = bundledReleaseNotesAssetName)
    }
    val latestBlocks = remember(releaseNotes) { parseDocumentationBlocks(releaseNotes) }
    val bundledBlocks = remember(bundledDocumentationText) {
        parseDocumentationBlocks(bundledDocumentationText)
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Changelog") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(innerPadding)
                .padding(horizontal = 22.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Text(
                text = overviewText,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Surface(
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))
                    MetadataLine(label = "Current version", value = currentVersion ?: "Unknown")
                    MetadataLine(label = "Latest version", value = latestVersion ?: "Unknown")
                    MetadataLine(label = "Summary", value = summary)
                }
            }

            if (latestBlocks.isEmpty() && bundledBlocks.isEmpty()) {
                DocumentationSurface(title = latestDocumentHeading) {
                    Text(
                        text = "No detailed changelog is available for this section yet.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                if (latestBlocks.isNotEmpty()) {
                    DocumentationSurface(title = latestDocumentHeading) {
                        DocumentationArticle(blocks = latestBlocks)
                    }
                }
                if (bundledDocumentHeading != null && bundledBlocks.isNotEmpty()) {
                    DocumentationSurface(title = bundledDocumentHeading) {
                        DocumentationArticle(blocks = bundledBlocks)
                    }
                }
            }
        }
    }
}

@Composable
private fun MetadataLine(
    label: String,
    value: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun DocumentationSurface(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        SelectionContainer {
            Column(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 22.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Description,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp),
                    )
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                content()
            }
        }
    }
}

@Composable
private fun DocumentationArticle(
    blocks: List<DocumentationBlock>,
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        blocks.forEach { block ->
            when (block) {
                is DocumentationBlock.Heading -> DocumentationHeading(block)
                is DocumentationBlock.Paragraph -> MarkdownText(
                    text = block.text,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                is DocumentationBlock.BulletList -> DocumentationBullets(block.items)
                is DocumentationBlock.CodeBlock -> DocumentationCodeBlock(block.text)
                DocumentationBlock.Divider -> HorizontalDivider(
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                )
            }
        }
    }
}

@Composable
private fun DocumentationHeading(
    block: DocumentationBlock.Heading,
) {
    val textStyle = when (block.level) {
        1 -> MaterialTheme.typography.headlineSmall
        2 -> MaterialTheme.typography.titleLarge
        else -> MaterialTheme.typography.titleMedium
    }
    Text(
        text = block.text,
        style = textStyle,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface,
    )
}

@Composable
private fun DocumentationBullets(
    items: List<DocumentationListItem>,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items.forEach { item ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Text(
                    text = item.marker,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )
                MarkdownText(
                    text = item.text,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

@Composable
private fun DocumentationCodeBlock(
    text: String,
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontFamily = FontFamily.Monospace,
            )
        }
    }
}

@Composable
private fun MarkdownText(
    text: String,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val linkColor = MaterialTheme.colorScheme.primary
    val inlineCodeBackground = MaterialTheme.colorScheme.surfaceVariant
    val annotatedText = remember(text, linkColor, inlineCodeBackground) {
        buildMarkdownAnnotatedString(
            text = text,
            linkColor = linkColor,
            inlineCodeBackground = inlineCodeBackground,
        )
    }
    Text(
        text = annotatedText,
        modifier = modifier,
        style = style,
        color = color,
    )
}

private sealed interface DocumentationBlock {
    data class Heading(val text: String, val level: Int) : DocumentationBlock
    data class Paragraph(val text: String) : DocumentationBlock
    data class BulletList(val items: List<DocumentationListItem>) : DocumentationBlock
    data class CodeBlock(val text: String) : DocumentationBlock
    object Divider : DocumentationBlock
}

private data class DocumentationListItem(
    val marker: String,
    val text: String,
)

private fun parseDocumentationBlocks(text: String?): List<DocumentationBlock> {
    if (text.isNullOrBlank()) return emptyList()

    val blocks = mutableListOf<DocumentationBlock>()
    val paragraphLines = mutableListOf<String>()
    val bulletLines = mutableListOf<DocumentationListItem>()
    val codeLines = mutableListOf<String>()
    var inCodeBlock = false

    fun flushParagraph() {
        if (paragraphLines.isEmpty()) return
        blocks += DocumentationBlock.Paragraph(paragraphLines.joinToString(" ") { it.trim() })
        paragraphLines.clear()
    }

    fun flushBullets() {
        if (bulletLines.isEmpty()) return
        blocks += DocumentationBlock.BulletList(bulletLines.toList())
        bulletLines.clear()
    }

    fun flushCode() {
        if (codeLines.isEmpty()) return
        blocks += DocumentationBlock.CodeBlock(codeLines.joinToString("\n"))
        codeLines.clear()
    }

    text.lineSequence().forEach { rawLine ->
        val line = rawLine.trimEnd()
        val trimmed = line.trim()

        if (trimmed.startsWith("```")) {
            if (inCodeBlock) {
                flushCode()
            } else {
                flushParagraph()
                flushBullets()
            }
            inCodeBlock = !inCodeBlock
            return@forEach
        }

        if (inCodeBlock) {
            codeLines += line
            return@forEach
        }

        if (trimmed.isBlank()) {
            flushParagraph()
            flushBullets()
            return@forEach
        }

        if (trimmed.matches(Regex("[-*_]{3,}"))) {
            flushParagraph()
            flushBullets()
            blocks += DocumentationBlock.Divider
            return@forEach
        }

        val headingMatch = Regex("^(#{1,6})\\s+(.*)$").find(trimmed)
        if (headingMatch != null) {
            flushParagraph()
            flushBullets()
            blocks += DocumentationBlock.Heading(
                text = headingMatch.groupValues[2].trim(),
                level = headingMatch.groupValues[1].length.coerceAtMost(3),
            )
            return@forEach
        }

        val bulletMatch = Regex("""^([-*+]|\d+\.)\s+(.*)$""").find(trimmed)
        if (bulletMatch != null) {
            flushParagraph()
            val rawMarker = bulletMatch.groupValues[1].trim()
            bulletLines += DocumentationListItem(
                marker = if (rawMarker.endsWith(".")) rawMarker else "\u2022",
                text = bulletMatch.groupValues[2].trim(),
            )
            return@forEach
        }

        paragraphLines += trimmed
    }

    if (inCodeBlock) {
        flushCode()
    }
    flushParagraph()
    flushBullets()

    return blocks
}

private fun buildMarkdownAnnotatedString(
    text: String,
    linkColor: Color,
    inlineCodeBackground: Color,
): AnnotatedString {
    return buildAnnotatedString {
        var index = 0
        while (index < text.length) {
            when {
                text.startsWith("**", index) -> {
                    val end = text.indexOf("**", startIndex = index + 2)
                    if (end > index + 2) {
                        pushStyle(SpanStyle(fontWeight = FontWeight.SemiBold))
                        append(text.substring(index + 2, end))
                        pop()
                        index = end + 2
                        continue
                    }
                }

                text.startsWith("__", index) -> {
                    val end = text.indexOf("__", startIndex = index + 2)
                    if (end > index + 2) {
                        pushStyle(SpanStyle(fontWeight = FontWeight.SemiBold))
                        append(text.substring(index + 2, end))
                        pop()
                        index = end + 2
                        continue
                    }
                }

                text[index] == '`' -> {
                    val end = text.indexOf('`', startIndex = index + 1)
                    if (end > index + 1) {
                        pushStyle(
                            SpanStyle(
                                fontFamily = FontFamily.Monospace,
                                background = inlineCodeBackground,
                            ),
                        )
                        append(text.substring(index + 1, end))
                        pop()
                        index = end + 1
                        continue
                    }
                }

                text[index] == '[' -> {
                    val closingBracket = text.indexOf(']', startIndex = index + 1)
                    if (closingBracket > index + 1 &&
                        closingBracket + 1 < text.length &&
                        text[closingBracket + 1] == '('
                    ) {
                        val closingParen = text.indexOf(')', startIndex = closingBracket + 2)
                        if (closingParen > closingBracket + 2) {
                            pushStyle(
                                SpanStyle(
                                    color = linkColor,
                                    textDecoration = TextDecoration.Underline,
                                ),
                            )
                            append(text.substring(index + 1, closingBracket))
                            pop()
                            index = closingParen + 1
                            continue
                        }
                    }
                }

                text[index] == '*' -> {
                    val end = text.indexOf('*', startIndex = index + 1)
                    if (end > index + 1) {
                        pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                        append(text.substring(index + 1, end))
                        pop()
                        index = end + 1
                        continue
                    }
                }

                text[index] == '_' -> {
                    val end = text.indexOf('_', startIndex = index + 1)
                    if (end > index + 1) {
                        pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                        append(text.substring(index + 1, end))
                        pop()
                        index = end + 1
                        continue
                    }
                }
            }

            append(text[index])
            index += 1
        }
    }
}

private fun loadBundledReleaseNotes(
    context: Context,
    assetName: String?,
): String? {
    if (assetName.isNullOrBlank()) return null
    return runCatching {
        context.assets.open(assetName).bufferedReader().use { it.readText() }
    }.getOrNull()
}
