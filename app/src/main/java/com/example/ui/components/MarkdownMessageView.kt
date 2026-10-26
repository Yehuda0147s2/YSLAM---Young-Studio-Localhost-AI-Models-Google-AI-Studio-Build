package com.example.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CodeBackground
import com.example.ui.theme.Inter
import com.example.ui.theme.JetBrainsMono
import com.example.ui.theme.YslamCyan
import com.example.ui.theme.YslamTextPrimary

sealed interface ContentSegment {
    data class TextSegment(val text: String) : ContentSegment
    data class CodeSegment(val language: String, val code: String) : ContentSegment
}

@Composable
fun MarkdownMessageView(
    content: String,
    isStreaming: Boolean = false,
    modifier: Modifier = Modifier,
    onSendToPlayground: ((String, String) -> Unit)? = null
) {
    val segments = remember(content) { parseMarkdownSegments(content) }

    val infiniteTransition = rememberInfiniteTransition(label = "cursor")
    val cursorAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cursorAlpha"
    )

    Column(modifier = modifier.fillMaxWidth()) {
        segments.forEachIndexed { index, segment ->
            when (segment) {
                is ContentSegment.TextSegment -> {
                    if (segment.text.isNotBlank()) {
                        FormattedTextBlock(text = segment.text)
                    }
                }
                is ContentSegment.CodeSegment -> {
                    Spacer(modifier = Modifier.height(6.dp))
                    CodeBlockView(
                        code = segment.code,
                        language = segment.language,
                        onSendToPlayground = onSendToPlayground
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }
        }

        if (isStreaming) {
            Box(
                modifier = Modifier
                    .padding(top = 4.dp)
                    .size(width = 8.dp, height = 16.dp)
                    .alpha(cursorAlpha)
                    .clip(RoundedCornerShape(1.dp))
                    .background(YslamCyan)
            )
        }
    }
}

@Composable
private fun FormattedTextBlock(text: String) {
    val annotatedString = remember(text) {
        buildAnnotatedString {
            val lines = text.lines()
            lines.forEachIndexed { idx, rawLine ->
                val line = rawLine.trimEnd()
                when {
                    line.startsWith("### ") -> {
                        pushStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = 16.sp, color = YslamCyan))
                        append(line.removePrefix("### "))
                        pop()
                    }
                    line.startsWith("## ") -> {
                        pushStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = 18.sp, color = YslamCyan))
                        append(line.removePrefix("## "))
                        pop()
                    }
                    line.startsWith("# ") -> {
                        pushStyle(SpanStyle(fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = YslamCyan))
                        append(line.removePrefix("# "))
                        pop()
                    }
                    line.startsWith("- ") || line.startsWith("* ") -> {
                        pushStyle(SpanStyle(color = YslamCyan))
                        append(" • ")
                        pop()
                        appendStyledLine(line.substring(2))
                    }
                    else -> {
                        appendStyledLine(line)
                    }
                }
                if (idx < lines.size - 1) {
                    append("\n")
                }
            }
        }
    }

    Text(
        text = annotatedString,
        fontFamily = Inter,
        style = MaterialTheme.typography.bodyLarge,
        color = YslamTextPrimary,
        lineHeight = 22.sp
    )
}

private fun androidx.compose.ui.text.AnnotatedString.Builder.appendStyledLine(line: String) {
    var cursor = 0
    val boldRegex = Regex("\\*\\*(.*?)\\*\\*")
    val codeRegex = Regex("`([^`]+)`")

    // Find all inline matches
    val allMatches = (boldRegex.findAll(line).map { it to "bold" } + codeRegex.findAll(line).map { it to "code" })
        .sortedBy { it.first.range.first }

    var lastIdx = 0
    for ((match, type) in allMatches) {
        if (match.range.first >= lastIdx) {
            append(line.substring(lastIdx, match.range.first))
            if (type == "bold") {
                pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                append(match.groupValues[1])
                pop()
            } else if (type == "code") {
                pushStyle(SpanStyle(fontFamily = JetBrainsMono, background = CodeBackground, color = YslamCyan))
                append(" ${match.groupValues[1]} ")
                pop()
            }
            lastIdx = match.range.last + 1
        }
    }
    if (lastIdx < line.length) {
        append(line.substring(lastIdx))
    }
}

fun parseMarkdownSegments(content: String): List<ContentSegment> {
    val segments = mutableListOf<ContentSegment>()
    val codeBlockRegex = Regex("```([a-zA-Z0-9_-]*)[ \\t]*\\r?\\n([\\s\\S]*?)(?:```|\\z)")

    var lastIndex = 0
    for (match in codeBlockRegex.findAll(content)) {
        val start = match.range.first
        if (start > lastIndex) {
            val textBefore = content.substring(lastIndex, start)
            if (textBefore.isNotEmpty()) {
                segments.add(ContentSegment.TextSegment(textBefore))
            }
        }
        val lang = match.groupValues[1].ifBlank { "code" }
        val code = match.groupValues[2].trimEnd()
        segments.add(ContentSegment.CodeSegment(lang, code))
        lastIndex = match.range.last + 1
    }

    if (lastIndex < content.length) {
        val remaining = content.substring(lastIndex)
        if (remaining.isNotEmpty()) {
            segments.add(ContentSegment.TextSegment(remaining))
        }
    }

    if (segments.isEmpty()) {
        segments.add(ContentSegment.TextSegment(content))
    }

    return segments
}
