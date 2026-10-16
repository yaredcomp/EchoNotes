package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.StitchCardOutline
import com.example.ui.theme.StitchCyan
import com.example.ui.theme.StitchPurple
import com.example.ui.theme.StitchPurpleText
import com.example.ui.theme.StitchSurfaceVariant
import com.example.ui.theme.StitchTextPrimary
import com.example.ui.theme.StitchTextSecondary

/**
 * Parses markdown inline formatting (**bold**, *italic*, `code`) into an [AnnotatedString].
 */
fun parseMarkdownInline(
    text: String,
    primaryColor: Color,
    codeBackground: Color = Color(0x228B5CF6)
): AnnotatedString {
    return buildAnnotatedString {
        var i = 0
        val len = text.length

        while (i < len) {
            // 1. Check for bold (**text** or __text__)
            if ((i + 1 < len) && (text.startsWith("**", i) || text.startsWith("__", i))) {
                val marker = text.substring(i, i + 2)
                val closingIdx = text.indexOf(marker, i + 2)
                if (closingIdx != -1) {
                    val inner = text.substring(i + 2, closingIdx)
                    pushStyle(SpanStyle(fontWeight = FontWeight.Bold, color = primaryColor))
                    append(inner)
                    pop()
                    i = closingIdx + 2
                    continue
                }
            }

            // 2. Check for inline code (`code`)
            if (text[i] == '`' && (i + 1 >= len || text[i + 1] != '`')) {
                val closingIdx = text.indexOf('`', i + 1)
                if (closingIdx != -1) {
                    val inner = text.substring(i + 1, closingIdx)
                    pushStyle(
                        SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Medium,
                            background = codeBackground,
                            color = primaryColor
                        )
                    )
                    append(" $inner ")
                    pop()
                    i = closingIdx + 1
                    continue
                }
            }

            // 3. Check for italics (*text* or _text_)
            if ((text[i] == '*' || text[i] == '_') && (i + 1 < len) && text[i + 1] != ' ' && text[i + 1] != text[i]) {
                val marker = text[i].toString()
                val closingIdx = text.indexOf(marker, i + 1)
                if (closingIdx != -1 && closingIdx > i + 1) {
                    val inner = text.substring(i + 1, closingIdx)
                    pushStyle(SpanStyle(fontStyle = FontStyle.Italic, color = primaryColor))
                    append(inner)
                    pop()
                    i = closingIdx + 1
                    continue
                }
            }

            // Normal character
            append(text[i])
            i++
        }
    }
}

private sealed class MarkdownBlock {
    data class Header(val level: Int, val text: String) : MarkdownBlock()
    data class Paragraph(val text: String) : MarkdownBlock()
    data class BulletItem(val text: String) : MarkdownBlock()
    data class NumberedItem(val number: String, val text: String) : MarkdownBlock()
    data class CodeBlock(val code: String, val language: String = "") : MarkdownBlock()
    data class Blockquote(val text: String) : MarkdownBlock()
}

/**
 * Parses multiline Markdown text into structured blocks.
 */
private fun parseMarkdownBlocks(rawText: String): List<MarkdownBlock> {
    val blocks = mutableListOf<MarkdownBlock>()
    val lines = rawText.lines()
    var inCodeBlock = false
    val codeBlockBuilder = StringBuilder()
    var codeBlockLang = ""

    for (line in lines) {
        val trimmed = line.trim()

        // Toggle code block
        if (trimmed.startsWith("```")) {
            if (inCodeBlock) {
                blocks.add(MarkdownBlock.CodeBlock(codeBlockBuilder.toString().trimEnd(), codeBlockLang))
                codeBlockBuilder.clear()
                codeBlockLang = ""
                inCodeBlock = false
            } else {
                inCodeBlock = true
                codeBlockLang = trimmed.removePrefix("```").trim()
            }
            continue
        }

        if (inCodeBlock) {
            codeBlockBuilder.append(line).append("\n")
            continue
        }

        if (trimmed.isBlank()) {
            continue
        }

        // Headers: # or ## or ###
        if (trimmed.startsWith("### ")) {
            blocks.add(MarkdownBlock.Header(3, trimmed.removePrefix("### ").trim()))
        } else if (trimmed.startsWith("## ")) {
            blocks.add(MarkdownBlock.Header(2, trimmed.removePrefix("## ").trim()))
        } else if (trimmed.startsWith("# ")) {
            blocks.add(MarkdownBlock.Header(1, trimmed.removePrefix("# ").trim()))
        }
        // Blockquote
        else if (trimmed.startsWith("> ")) {
            blocks.add(MarkdownBlock.Blockquote(trimmed.removePrefix("> ").trim()))
        }
        // Bullet item (- or * or •)
        else if (trimmed.startsWith("- ") || trimmed.startsWith("* ") || trimmed.startsWith("• ")) {
            val content = trimmed.substring(2).trim()
            blocks.add(MarkdownBlock.BulletItem(content))
        }
        // Numbered item (e.g. "1. ", "2. ")
        else if (Regex("^\\d+[.)]\\s+").containsMatchIn(trimmed)) {
            val match = Regex("^(\\d+[.)])\\s+(.*)").find(trimmed)
            if (match != null) {
                val num = match.groupValues[1]
                val content = match.groupValues[2]
                blocks.add(MarkdownBlock.NumberedItem(num, content))
            } else {
                blocks.add(MarkdownBlock.Paragraph(trimmed))
            }
        }
        // Normal paragraph
        else {
            blocks.add(MarkdownBlock.Paragraph(trimmed))
        }
    }

    if (inCodeBlock && codeBlockBuilder.isNotEmpty()) {
        blocks.add(MarkdownBlock.CodeBlock(codeBlockBuilder.toString().trimEnd(), codeBlockLang))
    }

    return blocks
}

/**
 * Rich Markdown renderer for Chat responses, AI summaries, and Key Takeaways.
 * Formats bold, italics, code, bullet lists, numbered lists, blockquotes, and code blocks cleanly.
 */
@Composable
fun RichMarkdownText(
    markdown: String,
    modifier: Modifier = Modifier,
    color: Color = StitchTextPrimary,
    fontSize: TextUnit = 13.sp,
    lineHeight: TextUnit = 20.sp
) {
    val blocks = remember(markdown) { parseMarkdownBlocks(markdown) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        blocks.forEach { block ->
            when (block) {
                is MarkdownBlock.Header -> {
                    val headerSize = when (block.level) {
                        1 -> 18.sp
                        2 -> 16.sp
                        else -> 14.sp
                    }
                    Text(
                        text = parseMarkdownInline(block.text, color),
                        fontSize = headerSize,
                        fontWeight = FontWeight.Bold,
                        color = color,
                        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                    )
                }

                is MarkdownBlock.Paragraph -> {
                    Text(
                        text = parseMarkdownInline(block.text, color),
                        fontSize = fontSize,
                        color = color,
                        lineHeight = lineHeight
                    )
                }

                is MarkdownBlock.BulletItem -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 1.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(top = 6.dp, end = 10.dp)
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(StitchPurple)
                        )
                        Text(
                            text = parseMarkdownInline(block.text, color),
                            fontSize = fontSize,
                            color = color,
                            lineHeight = lineHeight,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                is MarkdownBlock.NumberedItem -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 1.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = block.number,
                            fontSize = fontSize,
                            fontWeight = FontWeight.Bold,
                            color = StitchCyan,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = parseMarkdownInline(block.text, color),
                            fontSize = fontSize,
                            color = color,
                            lineHeight = lineHeight,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                is MarkdownBlock.Blockquote -> {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = StitchSurfaceVariant,
                        border = androidx.compose.foundation.BorderStroke(1.dp, StitchCardOutline),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(10.dp)) {
                            Box(
                                modifier = Modifier
                                    .width(3.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(StitchPurple)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = parseMarkdownInline(block.text, StitchTextSecondary),
                                fontSize = fontSize,
                                color = StitchTextSecondary,
                                fontStyle = FontStyle.Italic,
                                lineHeight = lineHeight
                            )
                        }
                    }
                }

                is MarkdownBlock.CodeBlock -> {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF0F111A),
                        border = androidx.compose.foundation.BorderStroke(1.dp, StitchCardOutline),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            if (block.language.isNotBlank()) {
                                Text(
                                    text = block.language.uppercase(),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = StitchCyan,
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )
                            }
                            Text(
                                text = block.code,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFFE2E8F0),
                                lineHeight = 17.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
