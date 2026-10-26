package com.example

import com.example.backend.BackendType
import com.example.backend.LocalBackend
import com.example.ui.components.ContentSegment
import com.example.ui.components.parseMarkdownSegments
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkdownAndBackendUnitTest {

    @Test
    fun testParseMarkdownSegmentsWithCodeBlock() {
        val markdown = "Here is the code:\n```kotlin\nfun main() {\n    println(\"Hello\")\n}\n```\nDone."
        val segments = parseMarkdownSegments(markdown)

        assertTrue(segments.size >= 3)
        assertTrue(segments[0] is ContentSegment.TextSegment)
        assertTrue(segments[1] is ContentSegment.CodeSegment)
        val codeSegment = segments[1] as ContentSegment.CodeSegment
        assertEquals("kotlin", codeSegment.language)
        assertTrue(codeSegment.code.contains("println"))
    }

    @Test
    fun testLocalBackendEndpointConfiguration() {
        val backend = LocalBackend("http://10.0.2.2:11434", "llama3")
        assertEquals(BackendType.LOCAL, backend.type)
        assertEquals("http://10.0.2.2:11434", backend.endpointUrl)

        backend.updateEndpoint("http://192.168.1.50:11434/", "mistral")
        assertEquals("http://192.168.1.50:11434", backend.endpointUrl)
        assertEquals("mistral", backend.selectedModel)
    }
}
