package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_snippets")
data class SavedSnippetEntity(
    @PrimaryKey val id: String,
    val title: String,
    val language: String,
    val systemPrompt: String,
    val userPrompt: String,
    val generatedCode: String,
    val temperature: Float,
    val topP: Float,
    val modelUsed: String,
    val createdAt: Long = System.currentTimeMillis()
)
