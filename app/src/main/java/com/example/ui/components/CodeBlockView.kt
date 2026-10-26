package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CodeBackground
import com.example.ui.theme.CodeComment
import com.example.ui.theme.CodeFunction
import com.example.ui.theme.CodeKeyword
import com.example.ui.theme.CodeNumber
import com.example.ui.theme.CodeString
import com.example.ui.theme.CodeType
import com.example.ui.theme.JetBrainsMono
import com.example.ui.theme.YslamBorder
import com.example.ui.theme.YslamCyan
import com.example.ui.theme.YslamGreen
import com.example.ui.theme.YslamSurfaceVariant
import com.example.ui.theme.YslamTextMuted
import com.example.ui.theme.YslamTextPrimary
import com.example.ui.theme.YslamTextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun CodeBlockView(
    code: String,
    language: String = "code",
    modifier: Modifier = Modifier,
    onSendToPlayground: ((String, String) -> Unit)? = null
) {
    val context = LocalContext.current
    var isCopied by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    val lines = remember(code) { code.lines() }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(CodeBackground)
            .border(1.dp, YslamBorder, RoundedCornerShape(8.dp))
            .testTag("code_block_view")
    ) {
        // Code Block Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(YslamSurfaceVariant)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(YslamCyan)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = language.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = YslamCyan
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "${lines.size} lines",
                    style = MaterialTheme.typography.labelSmall,
                    color = YslamTextMuted
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (onSendToPlayground != null) {
                    IconButton(
                        onClick = { onSendToPlayground(code, language) },
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("send_to_playground_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Send to Playground",
                            tint = YslamGreen,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                IconButton(
                    onClick = {
                        val clipboard =
                            context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("code", code)
                        clipboard.setPrimaryClip(clip)
                        isCopied = true
                        Toast.makeText(context, "Code copied to clipboard", Toast.LENGTH_SHORT).show()
                        coroutineScope.launch {
                            delay(2000)
                            isCopied = false
                        }
                    },
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("copy_code_button")
                ) {
                    Icon(
                        imageVector = if (isCopied) Icons.Default.Check else Icons.Default.ContentCopy,
                        contentDescription = "Copy Code",
                        tint = if (isCopied) YslamGreen else YslamTextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // Code Lines with syntax highlighting
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(vertical = 10.dp, horizontal = 12.dp)
        ) {
            // Line numbers column
            Column(
                modifier = Modifier.padding(end = 12.dp)
            ) {
                lines.indices.forEach { index ->
                    Text(
                        text = "${index + 1}",
                        fontFamily = JetBrainsMono,
                        fontSize = 12.sp,
                        lineHeight = 20.sp,
                        color = YslamTextMuted
                    )
                }
            }

            // Syntax highlighted code lines
            Column {
                lines.forEach { line ->
                    Text(
                        text = highlightCodeLine(line, language),
                        fontFamily = JetBrainsMono,
                        fontSize = 12.sp,
                        lineHeight = 20.sp,
                        color = YslamTextPrimary
                    )
                }
            }
        }
    }
}

fun highlightCodeLine(line: String, language: String): AnnotatedString {
    return buildAnnotatedString {
        append(line)
        val text = line

        // 1. Comments
        val commentIndex = when {
            text.contains("//") -> text.indexOf("//")
            text.contains("#") -> text.indexOf("#")
            text.contains("--") -> text.indexOf("--")
            else -> -1
        }
        if (commentIndex >= 0) {
            addStyle(SpanStyle(color = CodeComment), commentIndex, text.length)
            return@buildAnnotatedString
        }

        // 2. Strings
        val stringRegex = Regex("\"[^\"]*\"|'[^']*'")
        for (match in stringRegex.findAll(text)) {
            addStyle(SpanStyle(color = CodeString), match.range.first, match.range.last + 1)
        }

        // 3. Numbers
        val numberRegex = Regex("\\b\\d+(\\.\\d+)?\\b")
        for (match in numberRegex.findAll(text)) {
            addStyle(SpanStyle(color = CodeNumber), match.range.first, match.range.last + 1)
        }

        // 4. Common keywords
        val keywords = setOf(
            "val", "var", "fun", "class", "interface", "package", "import", "return", "if", "else",
            "while", "for", "in", "when", "data", "sealed", "object", "override", "suspend", "private",
            "public", "protected", "internal", "const", "let", "function", "async", "await", "def",
            "from", "type", "export", "default", "struct", "impl", "fn", "mut", "SELECT", "FROM", "WHERE"
        )
        val wordRegex = Regex("\\b[a-zA-Z_][a-zA-Z0-9_]*\\b")
        for (match in wordRegex.findAll(text)) {
            val word = match.value
            if (word in keywords) {
                addStyle(SpanStyle(color = CodeKeyword), match.range.first, match.range.last + 1)
            } else if (word.first().isUpperCase()) {
                addStyle(SpanStyle(color = CodeType), match.range.first, match.range.last + 1)
            } else if (text.getOrNull(match.range.last + 1) == '(') {
                addStyle(SpanStyle(color = CodeFunction), match.range.first, match.range.last + 1)
            }
        }
    }
}
