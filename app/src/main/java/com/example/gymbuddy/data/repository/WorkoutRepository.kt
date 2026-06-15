package com.example.gymbuddy.data.repository

import com.example.gymbuddy.data.model.WorkoutExercise
import com.example.gymbuddy.data.model.WorkoutSession
import com.example.gymbuddy.data.model.WorkoutSet
import com.example.gymbuddy.data.model.toFirestoreMap
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Firestore yolu: workouts/{uid}/sessions/{sessionId}
 */
class WorkoutRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    private fun sessionsRef(uid: String) =
        firestore.collection("workouts").document(uid).collection("sessions")

    // ── Workout Kaydet ─────────────────────────────────────────────
    suspend fun saveWorkout(session: WorkoutSession): Result<Unit> {
        return try {
            sessionsRef(session.uid)
                .document(session.id)
                .set(session.toFirestoreMap())
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Tüm Workout'ları Gerçek Zamanlı Dinle ────────────────────
    fun observeWorkouts(uid: String): Flow<List<WorkoutSession>> = callbackFlow {
        val listener = sessionsRef(uid)
            .orderBy("dateTimestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val sessions = snapshot?.documents?.mapNotNull { doc ->
                    parseWorkoutSession(doc.data ?: return@mapNotNull null)
                } ?: emptyList()
                trySend(sessions)
            }
        awaitClose { listener.remove() }
    }

    // ── Workout Sil ────────────────────────────────────────────────
    suspend fun deleteWorkout(uid: String, sessionId: String): Result<Unit> {
        return try {
            sessionsRef(uid).document(sessionId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Firestore Map → WorkoutSession ─────────────────────────────
    @Suppress("UNCHECKED_CAST")
    private fun parseWorkoutSession(map: Map<String, Any>): WorkoutSession? {
        return try {
            val exercises = (map["exercises"] as? List<Map<String, Any>> ?: emptyList())
                .map { exMap ->
                    WorkoutExercise(
                        id = exMap["id"] as? String ?: "",
                        name = exMap["name"] as? String ?: "",
                        sets = (exMap["sets"] as? List<Map<String, Any>> ?: emptyList())
                            .map { setMap ->
                                WorkoutSet(
                                    setNumber = (setMap["setNumber"] as? Long)?.toInt() ?: 1,
                                    weightKg = setMap["weightKg"] as? String ?: "",
                                    reps = setMap["reps"] as? String ?: ""
                                )
                            }
                    )
                }

            WorkoutSession(
                id = map["id"] as? String ?: "",
                uid = map["uid"] as? String ?: "",
                date = map["date"] as? String ?: "",
                dateTimestamp = map["dateTimestamp"] as? Long ?: 0L,
                exercises = exercises,
                totalVolumeKg = (map["totalVolumeKg"] as? Number)?.toFloat() ?: 0f,
                createdAt = map["createdAt"] as? Long ?: 0L
            )
        } catch (e: Exception) {
            null
        }
    }
}
