package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val userId: String = "",
    val email: String = "",
    val displayName: String = "",
    val photoUrl: String? = null,
    @ServerTimestamp val createdAt: com.google.firebase.Timestamp? = null,
    @ServerTimestamp val lastLoginAt: com.google.firebase.Timestamp? = null
)

class UserRepository(private val db: FirebaseFirestore) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    suspend fun syncUserProfile(user: FirebaseUser) {
        val uid = user.uid
        val docRef = db.collection("users").document(uid)

        try {
            val snapshot = docRef.get().await()
            if (!snapshot.exists()) {
                val data = mapOf(
                    "userId" to uid,
                    "email" to (user.email ?: "developer@yslam.local"),
                    "displayName" to (user.displayName ?: "YSLAM Developer"),
                    "photoUrl" to user.photoUrl?.toString(),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "lastLoginAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null }

                docRef.set(data).await()
            } else {
                val updateData = mapOf(
                    "email" to (user.email ?: "developer@yslam.local"),
                    "displayName" to (user.displayName ?: "YSLAM Developer"),
                    "photoUrl" to user.photoUrl?.toString(),
                    "lastLoginAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null }

                docRef.set(updateData, SetOptions.merge()).await()
            }
        } catch (e: Exception) {
            Log.e("UserRepository", "Failed to sync user profile to Firestore", e)
        }
    }
}
