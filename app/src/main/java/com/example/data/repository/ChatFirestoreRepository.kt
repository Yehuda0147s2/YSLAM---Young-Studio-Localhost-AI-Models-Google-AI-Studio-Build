package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.R
import com.example.data.local.ChatMessageEntity
import com.example.data.local.ChatSessionEntity
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

class ChatFirestoreRepository(private val db: FirebaseFirestore) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    private fun getUserId(): String? = auth.currentUser?.uid

    suspend fun saveSessionToCloud(session: ChatSessionEntity, messages: List<ChatMessageEntity>) {
        val uid = getUserId() ?: return
        val sessionDoc = db.collection("users").document(uid).collection("sessions").document(session.id)

        try {
            val sessionPayload = mapOf(
                "id" to session.id,
                "userId" to uid,
                "title" to session.title,
                "backendType" to session.backendType,
                "modelName" to session.modelName,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            sessionDoc.set(sessionPayload, SetOptions.merge()).await()

            // Save messages
            val batch = db.batch()
            messages.forEach { msg ->
                val msgDoc = sessionDoc.collection("messages").document(msg.id)
                val msgPayload = mapOf(
                    "id" to msg.id,
                    "sessionId" to session.id,
                    "userId" to uid,
                    "role" to msg.role,
                    "content" to msg.content,
                    "modelUsed" to msg.modelUsed,
                    "latencyMs" to msg.latencyMs,
                    "createdAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null }
                batch.set(msgDoc, msgPayload, SetOptions.merge())
            }
            batch.commit().await()
            Log.d("ChatFirestore", "Session ${session.id} synced to cloud")
        } catch (e: Exception) {
            Log.e("ChatFirestore", "Failed to sync session to cloud", e)
        }
    }

    suspend fun deleteCloudSession(sessionId: String) {
        val uid = getUserId() ?: return
        try {
            val sessionDoc = db.collection("users").document(uid).collection("sessions").document(sessionId)
            sessionDoc.delete().await()
        } catch (e: Exception) {
            Log.e("ChatFirestore", "Failed to delete cloud session", e)
        }
    }
}
